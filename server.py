#!/usr/bin/env python3
"""
BitDive MCP Server
Proxies tool calls to BitDive monitoring API endpoints (/mcp/*)
using X-BitDive-MCP-Token for authentication.
"""
import os
import json
import httpx
import re
import asyncio
from collections import Counter
from urllib.parse import parse_qsl, quote, unquote, urlparse, urlunparse
from mcp.server.fastmcp import FastMCP

# ── Configuration ───────────────────────────────────────────────
BITDIVE_API_URL = os.getenv(
    "BITDIVE_API_URL",
    "https://cloud.bitdive.io/monitoring-api"
)
BITDIVE_MCP_TOKEN = os.getenv(
    "BITDIVE_MCP_TOKEN",
    ""
)
BITDIVE_SKIP_VERIFY = os.getenv("BITDIVE_SKIP_VERIFY", "false").lower() == "true"
TIMEOUT = 30.0

# ── MCP Server ──────────────────────────────────────────────────
mcp = FastMCP(
    "BitDive",
    instructions=(
        "BitDive monitoring and tracing MCP server.\n\n"
        "CRITICAL WORKFLOW FOR DISCOVERING AND REPRODUCING TRACES:\n"
        "1. DISCOVERY: If you need a method signature, ALWAYS use get_heatmap_all_system or get_heatmap_for_module. It returns all methods (even with 0 calls).\n"
        "2. FIND TRACE: Once you know the exact className and methodName from the heatmap, use find_trace_between_time to fetch historical call_ids.\n"
        "3. REPRODUCE: Pass the call_id to get_reproduction_command to get a CURL/PowerShell command to manually trigger the endpoint.\n"
        "4. UPDATE CACHE: Execute the reproduction command, wait 45s, and the trace will be in the hot cache (get_last_calls) ready for test generation."
    ),
)


# ── HTTP helpers ────────────────────────────────────────────────
def _auth_headers() -> dict[str, str]:
    """Build auth headers and fail fast if the token is not configured."""
    if not BITDIVE_MCP_TOKEN:
        raise RuntimeError(
            "BITDIVE_MCP_TOKEN is not set. Export it before starting the MCP server."
        )
    return {"X-BitDive-MCP-Token": BITDIVE_MCP_TOKEN}


async def _get(path: str, params: dict | None = None):
    """Make an authenticated GET request to BitDive API."""
    headers = _auth_headers()
    async with httpx.AsyncClient(timeout=TIMEOUT, verify=not BITDIVE_SKIP_VERIFY) as client:
        resp = await client.get(
            f"{BITDIVE_API_URL}{path}",
            headers=headers,
            params=params,
        )
        resp.raise_for_status()
        return resp.json()


async def _post_json(path: str, body: dict, params: dict | None = None):
    """Make an authenticated POST request with JSON body to BitDive API."""
    headers = _auth_headers()
    async with httpx.AsyncClient(timeout=TIMEOUT, verify=not BITDIVE_SKIP_VERIFY) as client:
        resp = await client.post(
            f"{BITDIVE_API_URL}{path}",
            headers=headers,
            json=body,
            params=params,
        )
        resp.raise_for_status()
        if resp.status_code != 204 and resp.text.strip():
            return resp.json()
        return {}


async def _delete(path: str, params: dict | None = None):
    """Make an authenticated DELETE request to BitDive API."""
    headers = _auth_headers()
    async with httpx.AsyncClient(timeout=TIMEOUT, verify=not BITDIVE_SKIP_VERIFY) as client:
        resp = await client.delete(
            f"{BITDIVE_API_URL}{path}",
            headers=headers,
            params=params,
        )
        resp.raise_for_status()
        if resp.status_code != 204 and resp.text.strip():
            return resp.json()
        return {}


def _decode_repeatedly(value: str, max_rounds: int = 3) -> str:
    """Decode URL-encoded text until it stabilizes or the guard limit is reached."""
    decoded = value
    for _ in range(max_rounds):
        next_value = unquote(decoded)
        if next_value == decoded:
            break
        decoded = next_value
    return decoded


def _normalize_reproduction_url(raw_url: str) -> str:
    """Make captured URLs usable from the host machine."""
    try:
        parsed = urlparse(raw_url)
    except Exception:
        return raw_url

    hostname = parsed.hostname or ""
    port = parsed.port

    # Internal Docker DNS names are not usable from the host shell.
    if hostname.endswith("-ms"):
        netloc = f"localhost:{port}" if port else "localhost"
    else:
        netloc = parsed.netloc

    if parsed.query:
        query_pairs = [
            (key, _decode_repeatedly(value))
            for key, value in parse_qsl(parsed.query, keep_blank_values=True)
        ]
        query = "&".join(
            f"{quote(key, safe='')}={quote(value, safe='')}"
            for key, value in query_pairs
        )
    else:
        query = parsed.query

    return urlunparse(parsed._replace(netloc=netloc, query=query))


def _should_skip_reproduction_header(header_name: str) -> bool:
    normalized = header_name.lower()
    if normalized in {
        "@class",
        "host",
        "content-length",
        "connection",
        "accept-encoding",
        "expect",
    }:
        return True
    return normalized.startswith("x-bitdiv-")


def _escape_single_quotes(value: str) -> str:
    return value.replace("'", "'\"'\"'")


def _escape_powershell_single_quotes(value: str) -> str:
    return value.replace("'", "''")


def _normalize_sql(sql: str) -> str:
    """Removes specific IDs and values from SQL to group similar queries.
    Example: 'where id=123' -> 'where id=?'
    """
    if not sql:
        return ""
    # Replace numeric values in quotes: '123' -> '?'
    sql = re.sub(r"'\d+'", "'?'", sql)
    # Replace standalone numeric values: = 123 -> = ?
    sql = re.sub(r"=\s*\d+", "=?", sql)
    # Replace values in IN clauses: IN (1, 2, 3) -> IN (?)
    sql = re.sub(r"IN\s*\([^)]+\)", "IN(?)", sql, flags=re.IGNORECASE)
    # Replace UUIDs
    sql = re.sub(r"'[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}'", "?", sql)
    # Handle PostgreSQL type casting: ('1'::int4) -> (?)
    sql = re.sub(r"\('\?'::\w+\)", "(?)", sql)
    # Remove extra spaces
    sql = re.sub(r"\s+", " ", sql)
    return sql.strip()


_VOLATILE_KEY_RE = re.compile(
    r"(^|\.)(id|.*Id|traceId|spanId|messageId|callId|uuid|timestamp|date|createdAt|updatedAt)$",
    re.IGNORECASE,
)
_UUID_RE = re.compile(
    r"\b[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\b",
    re.IGNORECASE,
)
_ISO_DATE_RE = re.compile(r"^\d{4}-\d{2}-\d{2}[T ][0-9:\.\-+Z]*$")


def _normalize_typed_key(key: str) -> str:
    """Collapse BitDive's typed map keys into plain field names."""
    if key.startswith("string:") and key.count(":") >= 2:
        return key.split(":", 2)[2]
    return key


def _looks_like_json_blob(value: str) -> bool:
    if not isinstance(value, str):
        return False
    trimmed = value.strip()
    return (
        trimmed.startswith("{")
        or trimmed.startswith("[")
        or trimmed.startswith("\"{")
        or trimmed.startswith("\"[")
    )


def _safe_json_loads(raw: str):
    """Parse JSON when possible, including quoted JSON payloads."""
    if not isinstance(raw, str):
        return raw
    text = raw.strip()
    if not text:
        return raw
    try:
        parsed = json.loads(text)
    except Exception:
        return raw
    if isinstance(parsed, str) and _looks_like_json_blob(parsed):
        return _safe_json_loads(parsed)
    return parsed


def _normalize_payload(value):
    """Generic normalization for BitDive typed JSON structures."""
    if isinstance(value, str):
        parsed = _safe_json_loads(value)
        if parsed is value:
            return value.strip()
        return _normalize_payload(parsed)

    if isinstance(value, list):
        if len(value) == 2 and isinstance(value[0], str) and value[0].startswith("java."):
            return _normalize_payload(value[1])
        return [_normalize_payload(item) for item in value]

    if isinstance(value, dict):
        normalized = {}
        class_name = value.get("@class")
        if class_name and not class_name.startswith("java.util."):
            normalized["__class__"] = class_name.rsplit(".", 1)[-1]
        for key, item in value.items():
            if key == "@class":
                continue
            normalized[_normalize_typed_key(key)] = _normalize_payload(item)
        if set(normalized.keys()) == {"parIndex", "paramType", "val"}:
            return {
                "index": normalized.get("parIndex"),
                "type": normalized.get("paramType"),
                "value": normalized.get("val"),
            }
        return normalized

    return value


def _summarize_scalar(value) -> str:
    text = str(value)
    if len(text) > 180:
        return f"{text[:177]}..."
    return text


def _is_volatile_change(path: str, before, after) -> bool:
    if _VOLATILE_KEY_RE.search(path):
        return True
    for value in (before, after):
        if isinstance(value, str) and (_UUID_RE.search(value) or _ISO_DATE_RE.match(value)):
            return True
    return False


def _diff_values(before, after, path: str = "", changes: list | None = None, *, ignore_volatile: bool = False):
    """Recursively diff normalized payloads."""
    if changes is None:
        changes = []

    if before == after:
        return changes

    if type(before) != type(after):
        if not (ignore_volatile and _is_volatile_change(path, before, after)):
            changes.append(f"{path or '$'}: type {type(before).__name__} -> {type(after).__name__}")
        return changes

    if isinstance(before, dict):
        before_keys = set(before.keys())
        after_keys = set(after.keys())
        for key in sorted(before_keys - after_keys):
            sub_path = f"{path}.{key}" if path else key
            if ignore_volatile and _is_volatile_change(sub_path, before.get(key), None):
                continue
            changes.append(f"{sub_path}: removed")
        for key in sorted(after_keys - before_keys):
            sub_path = f"{path}.{key}" if path else key
            if ignore_volatile and _is_volatile_change(sub_path, None, after.get(key)):
                continue
            changes.append(f"{sub_path}: added={_summarize_scalar(after[key])}")
        for key in sorted(before_keys & after_keys):
            sub_path = f"{path}.{key}" if path else key
            _diff_values(before[key], after[key], sub_path, changes, ignore_volatile=ignore_volatile)
        return changes

    if isinstance(before, list):
        if len(before) != len(after):
            if not (ignore_volatile and _is_volatile_change(path, len(before), len(after))):
                changes.append(f"{path or '$'}: list length {len(before)} -> {len(after)}")
        for index, (before_item, after_item) in enumerate(zip(before[:5], after[:5])):
            _diff_values(before_item, after_item, f"{path}[{index}]", changes, ignore_volatile=ignore_volatile)
        return changes

    if isinstance(before, str) and isinstance(after, str):
        if len(before) > 240 or len(after) > 240:
            if before != after and not (ignore_volatile and _is_volatile_change(path, before, after)):
                changes.append(
                    f"{path or '$'}: text changed "
                    f"(len {len(before)} -> {len(after)}, before={_summarize_scalar(before)}, after={_summarize_scalar(after)})"
                )
            return changes

    if not (ignore_volatile and _is_volatile_change(path, before, after)):
        changes.append(
            f"{path or '$'}: {_summarize_scalar(before)} -> {_summarize_scalar(after)}"
        )
    return changes


def _signature(node: dict) -> str:
    return f"{_short_class(node.get('className', '?'))}.{node.get('methodName', '?')}()"


def _build_contract_entries(trace: dict) -> list[dict]:
    """Extract generic request/response contracts for every node in the trace tree."""
    entries = []
    path_counts: Counter[str] = Counter()

    def _walk(node: dict, parent_path: str = ""):
        signature = _signature(node)
        path_counts[parent_path] += 1
        ordinal = path_counts[parent_path]
        path = f"{parent_path}/{signature}[{ordinal}]"

        request_contract = {}
        args_payload = _normalize_payload(node.get("args"))
        if args_payload not in (None, "", [], {}):
            request_contract["args"] = args_payload
        body_payload = _normalize_payload(node.get("bodyRest"))
        if body_payload not in (None, "", [], {}):
            request_contract["body"] = body_payload
        header_payload = _normalize_payload(node.get("headerRest"))
        if header_payload not in (None, "", [], {}):
            request_contract["headers"] = header_payload
        url_payload = node.get("urlRest") or node.get("url")
        if url_payload:
            request_contract["url"] = url_payload

        response_contract = {}
        return_payload = _normalize_payload(node.get("methodReturn"))
        if return_payload not in (None, "", [], {}):
            response_contract["return"] = return_payload
        status = node.get("codeResponse")
        if status:
            response_contract["status"] = status
        error_message = node.get("errorCallMessage")
        if error_message:
            response_contract["error"] = error_message

        rest_contracts = []
        for rest in node.get("restCalls", []):
            rest_contracts.append(
                {
                    "method": rest.get("methodRest") or rest.get("method"),
                    "uri": rest.get("uri"),
                    "status": rest.get("statusCode"),
                    "requestHeaders": _normalize_payload(rest.get("headers")),
                    "requestBody": _normalize_payload(rest.get("body")),
                    "responseHeaders": _normalize_payload(rest.get("responseHeaders")),
                    "responseBody": _normalize_payload(rest.get("responseBody")),
                    "error": rest.get("errorCallMessage"),
                }
            )

        entry = {
            "path": path,
            "signature": signature,
            "operationType": node.get("operationType"),
            "request": request_contract,
            "response": response_contract,
            "restCalls": rest_contracts,
            "delta": node.get("callTimeDelta") or 0,
        }
        entries.append(entry)

        for child in node.get("childCalls", []):
            _walk(child, path)

    _walk(trace)
    return entries


def _index_contract_entries(entries: list[dict]) -> dict[str, dict]:
    return {entry["path"]: entry for entry in entries}


def _format_contract_section(before: dict, after: dict) -> list[str]:
    lines = []

    root_before = {
        "request": before.get("request", {}),
        "response": before.get("response", {}),
    }
    root_after = {
        "request": after.get("request", {}),
        "response": after.get("response", {}),
    }
    root_changes = _diff_values(root_before, root_after, ignore_volatile=True)
    if root_changes:
        lines.append("ROOT CONTRACT CHANGES:")
        lines.extend(f"  - {change}" for change in root_changes[:15])
        if len(root_changes) > 15:
            lines.append(f"  ... and {len(root_changes) - 15} more root changes")
        lines.append("")

    request_changes = _diff_values(before.get("request", {}), after.get("request", {}), ignore_volatile=True)
    if request_changes:
        lines.append("ROOT REQUEST DIFF:")
        lines.extend(f"  - {change}" for change in request_changes[:10])
        lines.append("")

    response_changes = _diff_values(before.get("response", {}), after.get("response", {}), ignore_volatile=True)
    if response_changes:
        lines.append("ROOT RESPONSE DIFF:")
        lines.extend(f"  - {change}" for change in response_changes[:10])
        lines.append("")

    return lines


def _format_path_contract_changes(before_entries: list[dict], after_entries: list[dict]) -> list[str]:
    lines = []
    before_index = _index_contract_entries(before_entries)
    after_index = _index_contract_entries(after_entries)

    changed_nodes = []
    for path in sorted(set(before_index.keys()) & set(after_index.keys())):
        before_entry = before_index[path]
        after_entry = after_index[path]
        changes = _diff_values(
            {
                "request": before_entry.get("request", {}),
                "response": before_entry.get("response", {}),
                "restCalls": before_entry.get("restCalls", []),
            },
            {
                "request": after_entry.get("request", {}),
                "response": after_entry.get("response", {}),
                "restCalls": after_entry.get("restCalls", []),
            },
            ignore_volatile=True,
        )
        if changes:
            changed_nodes.append((path, before_entry["signature"], changes))

    if changed_nodes:
        lines.append("PAYLOAD / CONTRACT DRIFT:")
        for path, signature, changes in changed_nodes[:8]:
            lines.append(f"  {signature} @ {path}")
            for change in changes[:4]:
                lines.append(f"    - {change}")
            if len(changes) > 4:
                lines.append(f"    ... and {len(changes) - 4} more changes")
        if len(changed_nodes) > 8:
            lines.append(f"  ... and {len(changed_nodes) - 8} more changed nodes")
        lines.append("")

    added_paths = sorted(set(after_index.keys()) - set(before_index.keys()))
    removed_paths = sorted(set(before_index.keys()) - set(after_index.keys()))
    if added_paths or removed_paths:
        lines.append("TRACE PATH CHANGES:")
        for path in added_paths[:6]:
            lines.append(f"  + {path}")
        for path in removed_paths[:6]:
            lines.append(f"  - {path}")
        if len(added_paths) > 6 or len(removed_paths) > 6:
            lines.append("  ... additional path changes omitted")
        lines.append("")

    downstream_changes = []
    for path in sorted(set(before_index.keys()) & set(after_index.keys())):
        before_rest = before_index[path].get("restCalls", [])
        after_rest = after_index[path].get("restCalls", [])
        changes = _diff_values(before_rest, after_rest, ignore_volatile=True)
        if changes:
            downstream_changes.append((path, before_index[path]["signature"], changes))
    if downstream_changes:
        lines.append("DOWNSTREAM HTTP CONTRACT CHANGES:")
        for path, signature, changes in downstream_changes[:6]:
            lines.append(f"  {signature} @ {path}")
            for change in changes[:4]:
                lines.append(f"    - {change}")
        if len(downstream_changes) > 6:
            lines.append(f"  ... and {len(downstream_changes) - 6} more downstream changes")
        lines.append("")

    return lines


# ═══════════════════════════════════════════════════════════════
#  Dashboard / HeatMap  (from HeadMapTools.java)
# ═══════════════════════════════════════════════════════════════

def _format_heatmap(modules: list) -> str:
    """Convert raw heatmap JSON into a compact human-readable summary.
    Strips history[], alert fields, and other verbose data.
    """
    lines = []
    for mod in modules:
        mod_name = mod.get("moduleName", "?")
        lines.append(f"\n📦 Module: {mod_name}")
        for svc in mod.get("services", []):
            svc_name = svc.get("serviceName", "?")
            svc_calls = svc.get("callCountWeb", 0)
            svc_errs = svc.get("errorCount", 0)
            svc_avg = svc.get("avgCallTimeWeb", 0)
            svc_summary = f"{svc_calls} calls, {svc_avg:.0f}ms avg"
            if svc_errs:
                svc_summary += f", ⚠ {svc_errs} errors"
            lines.append(f"  🔹 {svc_name} ({svc_summary})")
            for cls in svc.get("classes", []):
                cls_name = (cls.get("className") or "").rsplit(".", 1)[-1]
                for ip in cls.get("inPoints", []):
                    m_name = ip.get("inPointName", "?")
                    calls = ip.get("callCountWeb", 0) or ip.get("callCountScheduler", 0)
                    avg = ip.get("avgCallTimeWeb", 0) or ip.get("avgCallTimeScheduler", 0)
                    errs = ip.get("errorCount", 0)
                    sql_count = ip.get("sqlCallCount", 0)
                    rest_count = ip.get("restCallCount", 0)
                    c4xx = ip.get("count4xx", 0)
                    c5xx = ip.get("count5xx", 0)
                    q_send = ip.get("queueSendCount", 0)
                    q_consume = ip.get("queueConsumerCount", 0)
                    parts = [f"{calls} calls", f"{avg:.0f}ms"]
                    if errs:
                        parts.append(f"⚠ {errs} err")
                    if c4xx:
                        parts.append(f"{c4xx}×4xx")
                    if c5xx:
                        parts.append(f"{c5xx}×5xx")
                    if sql_count:
                        parts.append(f"{sql_count} SQL")
                    if rest_count:
                        parts.append(f"{rest_count} REST")
                    if q_send or q_consume:
                        parts.append(f"Q:{q_send}↑{q_consume}↓")
                    lines.append(f"    {cls_name}.{m_name}(): {' | '.join(parts)}")
    return "\n".join(lines) if lines else "No heatmap data"


@mcp.tool()
async def get_heatmap_all_system(last_minutes: int = 10) -> str:
    """Returns system performance metrics (heatmap) for ALL modules and services.
    Shows error counts, call counts, average response times,
    SQL/REST/Queue metrics for each module → service → class → method.
    """
    last_minutes = min(last_minutes, 30)
    data = await _get("/mcp/Dashboard/HeatMap", {"LastMinutes": last_minutes})
    return _format_heatmap(data)


@mcp.tool()
async def get_heatmap_for_module(module_name: str, last_minutes: int = 10) -> str:
    """Returns performance metrics (heatmap) for a specific module.
    Filters the full heatmap to only the given module.
    """
    last_minutes = min(last_minutes, 30)
    data = await _get("/mcp/Dashboard/HeatMap", {"LastMinutes": last_minutes})
    filtered = [m for m in data if m.get("moduleName") == module_name]
    return _format_heatmap(filtered)


@mcp.tool()
async def get_heatmap_for_service(
    module_name: str, service_name: str, last_minutes: int = 10
) -> str:
    """Returns performance metrics (heatmap) for a specific module and service."""
    last_minutes = min(last_minutes, 30)
    data = await _get("/mcp/Dashboard/HeatMap", {"LastMinutes": last_minutes})
    filtered = []
    for m in data:
        if m.get("moduleName") == module_name:
            services = [
                s for s in m.get("services", [])
                if s.get("serviceName") == service_name
            ]
            if services:
                filtered.append({**m, "services": services})
    return _format_heatmap(filtered)


# ═══════════════════════════════════════════════════════════════
#  Last Call Service  (from LastCallTools.java)
# ═══════════════════════════════════════════════════════════════

@mcp.tool()
async def get_last_calls(module_name: str, service_name: str) -> str:
    """Returns a list of recent method executions with their trace IDs
    for the given module and service. Use this to find call IDs
    for deeper trace investigation.
    """
    data = await _get("/mcp/LastCallService/getData", {
        "moduleName": module_name,
        "serviceName": service_name,
    })
    if not data or not isinstance(data, list):
        return "No recent calls found."
    lines = [f"Recent calls for {module_name}/{service_name}:\n"]
    for item in data:
        tid = item.get("traceId") or item.get("messageId") or "?"
        cls = (item.get("className") or "").rsplit(".", 1)[-1]
        method = item.get("methodName", "?")
        dt = (item.get("callDateTime") or "")[:19].replace("T", " ")
        lines.append(f"  {tid}  {cls}.{method}()  {dt}")
    return "\n".join(lines)


# ═══════════════════════════════════════════════════════════════
#  Find Trace  (from TraceTools.java)
# ═══════════════════════════════════════════════════════════════

@mcp.tool()
async def find_trace_all(call_id: str) -> str:
    """Returns the full call trace tree for the specified call ID.
    Shows the complete hierarchy of method calls, SQL queries,
    REST calls, and queue operations within a single request.
    """
    data = await _get("/mcp/FindTrace/findTraceAll", {"callId": call_id})
    return json.dumps(data, ensure_ascii=False, default=str)


@mcp.tool()
async def find_trace_for_method(
    call_id: str, class_name: str, method_name: str
) -> str:
    """Returns the call trace for a specific method within the given call ID.
    Use this to drill down into a particular method's execution details.
    """
    data = await _get("/mcp/FindTrace/findTraceForMethod", {
        "callId": call_id,
        "className": class_name,
        "methodName": method_name,
    })
    return json.dumps(data, ensure_ascii=False, default=str)


@mcp.tool()
async def find_trace_between_time(
    class_name: str,
    method_name: str,
    begin_date: str,
    end_date: str,
) -> str:
    """Returns method call traces between two timestamps.
    Dates must be in ISO-8601 format with timezone offset,
    e.g. '2024-01-15T10:30:00+03:00'.
    """
    data = await _get("/mcp/FindTrace/findTraceForMethodBetweenTime", {
        "className": class_name,
        "methodName": method_name,
        "beginDate": begin_date,
        "endDate": end_date,
    })
    return json.dumps(data, ensure_ascii=False, default=str)


@mcp.tool()
async def get_trace_names_batch(call_ids: list[str]) -> str:
    """Takes a list of trace call IDs and returns a quick mapping 
    of each ID to its short className and methodName.
    Use this to quickly identify unknown call IDs (e.g. from test script groups).
    """
    if not call_ids:
        return "No call IDs provided."
    
    # Cap to prevent too many requests
    call_ids = call_ids[:35]
    
    results = []
    for cid in call_ids:
        try:
            data = await _get("/mcp/FindTrace/findTraceAll", {"callId": cid})
            if data:
                c_name = data.get("className", "?").split('.')[-1]
                m_name = data.get("methodName", "?")
                results.append(f"{cid} -> {c_name}.{m_name}")
            else:
                results.append(f"{cid} -> Not Found")
        except Exception as e:
            results.append(f"{cid} -> Error: {str(e)}")
            
    return "\n".join(results)


@mcp.tool()
async def get_reproduction_command(call_id: str) -> str:
    """Returns a curl command and structured info to reproduce the web request from a trace.
    Extracts URL, method, headers, and body from the recorded BitDive trace.
    """
    trace = await _get("/mcp/FindTrace/findTraceAll", {"callId": call_id})
    
    op_type = trace.get("operationType", "WEB_GET")
    method = op_type.replace("WEB_", "") if op_type.startswith("WEB_") else "GET"
    url = _normalize_reproduction_url(
        trace.get("urlRest") or trace.get("url") or "http://localhost:8080/???"
    )
    
    # Parse headers (Format: {"string:java.lang.String:user-agent": ["java.util.ArrayList", ["..."]]})
    raw_headers = trace.get("headerRest")
    headers = {}
    if raw_headers:
        if isinstance(raw_headers, str):
            try:
                raw_headers = json.loads(raw_headers)
            except:
                pass
        
        if isinstance(raw_headers, dict):
            for k, v in raw_headers.items():
                # Clean up Java-serialized keys like "string:java.lang.String:user-agent"
                key = k.split(":")[-1] if ":" in k else k
                if _should_skip_reproduction_header(key):
                    continue
                # Handle BitDive/Java list format
                if isinstance(v, list) and len(v) == 2 and v[0] == "java.util.ArrayList":
                    val_list = v[1]
                    if val_list and isinstance(val_list, list):
                        headers[key] = val_list[0]
                elif isinstance(v, list) and v:
                    headers[key] = v[0]
                else:
                    headers[key] = str(v)

    body = trace.get("bodyRest")
    
    # Generate CURL command
    curl = f"curl -X {method} '{url}'"
    for k, v in headers.items():
        curl += f" -H '{_escape_single_quotes(k)}: {_escape_single_quotes(str(v))}'"
    
    if body:
        # If body is a string (often JSON), escape single quotes for shell
        body_str = json.dumps(body) if not isinstance(body, str) else body
        curl += f" -d '{_escape_single_quotes(body_str)}'"

    # Generate PowerShell Invoke-RestMethod
    ps_headers = "@{" + "; ".join(
        [f"'{_escape_powershell_single_quotes(k)}'='{_escape_powershell_single_quotes(str(v))}'" for k, v in headers.items()]
    ) + "}"
    ps = (
        f"Invoke-RestMethod -Method {method} "
        f"-Uri '{_escape_powershell_single_quotes(url)}' -Headers {ps_headers}"
    )
    if body:
        ps += (
            f" -Body '{_escape_powershell_single_quotes(body_str)}'"
            f" -ContentType 'application/json'"
        )

    return (
        f"REPRODUCTION COMMANDS for Call {call_id}:\n\n"
        f"--- BASH / CURL ---\n{curl}\n\n"
        f"--- POWERSHELL ---\n{ps}\n\n"
        f"--- DETAILS ---\n"
        f"Method: {method}\n"
        f"URL:    {url}\n"
        f"Body:   {body or '(empty)'}"
    )


# ═══════════════════════════════════════════════════════════════
#  Method Documentation  (from api-docs.json /mcp/MethodDoc/*)
# ═══════════════════════════════════════════════════════════════

@mcp.tool()
async def search_methods_short(query: str, limit: int = 10) -> str:
    """Search for method documentation by query string.
    Returns short summaries of matching methods.
    """
    data = await _get("/mcp/MethodDoc/searchShort", {
        "q": query,
        "limit": limit,
    })
    return json.dumps(data, ensure_ascii=False, default=str)


@mcp.tool()
async def search_methods_full(query: str, limit: int = 3) -> str:
    """Search for method documentation by query string.
    Returns full details including call statistics and trace info.
    """
    data = await _get("/mcp/MethodDoc/searchFull", {
        "q": query,
        "limit": limit,
    })
    return json.dumps(data, ensure_ascii=False, default=str)


# ═══════════════════════════════════════════════════════════════
#  Test Management (mirrors frontend QA flow via /mcp/Testing/*)
# ═══════════════════════════════════════════════════════════════


@mcp.tool()
async def create_test_group(
    name: str,
    test_type: str,
    call_id_list: list[str],
) -> str:
    """Creates a NEW test group in BitDive from a list of call (trace) IDs.

    ⚠️ WARNING: This creates a BRAND NEW test group with a new UUID.
    If you need to UPDATE an existing test group, use update_existing_test_group instead.
    Check TestControllerTestAbstract.java for existing test group UUIDs before creating new ones.
    New groups will NOT be executed by Maven unless their UUID is added to the Java test file.

    Args:
        name: Human-readable test name (e.g., "Faculty Service Unit Tests")
        test_type: One of "UNIT", "COMPONENT", or "INTEGRATION"
        call_id_list: List of trace/call IDs to include in the test
    
    Returns: Created test group info (id, name, type)
    """
    body = {
        "name": name,
        "type": test_type.upper(),
        "testDataRules": {
            "callIdList": call_id_list
        }
    }
    data = await _post_json("/mcp/Testing/createTestGroup", body)
    return json.dumps(data, ensure_ascii=False, default=str)

@mcp.tool()
async def get_all_test_scripts() -> str:
    """Returns all test scripts (test groups) from the system."""
    data = await _get("/mcp/Testing/getAllTestScript")
    if not data or not isinstance(data, list):
        return "No test scripts found."
    lines = [f"Test Scripts ({len(data)} total):\n"]
    lines.append(f"{'ID':>38} | {'Name':<35} | {'Type':<6} | {'Status':<4} | Classes")
    lines.append("-" * 100)
    for script in data:
        sid = script.get("id", "?")
        name = (script.get("name") or "?")[:35]
        stype = (script.get("type") or "?")[:6]
        rs = script.get("resultSuccess")
        status = "✅" if rs and rs.get("success") else "❌" if rs else "—"
        n_classes = len(script.get("scriptDataDTOList", []))
        enabled = script.get("enabled", True)
        prefix = "  " if enabled else "🚫"
        lines.append(f"{prefix}{sid} | {name:<35} | {stype:<6} | {status:<4} | {n_classes}")
    return "\n".join(lines)

@mcp.tool()
async def get_script_data(test_script_id: str) -> str:
    """Returns script data for a given test group ID."""
    data = await _get("/mcp/Testing/getScriptData", {"testScriptId": test_script_id})
    if not data or not isinstance(data, list):
        return "No script data found."
    lines = [f"Script data for {test_script_id} ({len(data)} entries):\n"]
    for entry in data:
        eid = entry.get("id", "?")
        cls = (entry.get("className") or "").rsplit(".", 1)[-1]
        svc = entry.get("serviceName", "?")
        enabled = "✅" if entry.get("enabled") else "❌"
        rs = entry.get("resultSuccess")
        result = "✅" if rs and rs.get("success") else "❌" if rs else "—"
        # Show which trace IDs were used (callIdData)
        call_ids = entry.get("callIdData") or "—"
        lines.append(f"  {eid}  {cls} ({svc})  enabled={enabled} result={result}  calls={call_ids}")
    return "\n".join(lines)

@mcp.tool()
async def get_script_data_test(test_script_data_id: str) -> str:
    """Returns the tests under a specific script data record."""
    data = await _get("/mcp/Testing/getScriptDataTest", {"testScriptDataId": test_script_data_id})
    return json.dumps(data, ensure_ascii=False, default=str)

async def _find_test_context(script_data_test_id: str):
    """Resolve a method-level test ID to its parent script data and group via MCP-only APIs."""
    all_groups = await _get("/mcp/Testing/getAllTestScript")

    for group in all_groups or []:
        group_id = group.get("id")
        if not group_id:
            continue

        try:
            script_data_entries = await _get("/mcp/Testing/getScriptData", {"testScriptId": group_id})
        except Exception:
            continue

        for entry in script_data_entries or []:
            entry_id = entry.get("id")
            if not entry_id:
                continue

            try:
                tests = await _get("/mcp/Testing/getScriptDataTest", {"testScriptDataId": entry_id})
            except Exception:
                continue

            for test in tests or []:
                if test.get("id") == script_data_test_id:
                    return {
                        "group_id": group_id,
                        "script_data": entry,
                        "target_test": test,
                    }

    return None


async def _build_replace_payload(script_data_test_id: str) -> dict:
    """Rebuild replace payload without relying on MCP getTestsByCallForTestScript."""
    context = await _find_test_context(script_data_test_id)
    if not context:
        raise RuntimeError(
            f"Could not resolve test id {script_data_test_id} through MCP-accessible APIs."
        )

    target_test = context["target_test"]
    source_message_id = target_test.get("sourceMessageId")
    if not source_message_id:
        raise RuntimeError(
            f"Test {script_data_test_id} does not expose sourceMessageId, cannot rebuild replace payload."
        )

    group_id = context["group_id"]
    script_data_entries = await _get("/mcp/Testing/getScriptData", {"testScriptId": group_id})

    aggregated_tests = []
    seen_test_ids = set()
    for entry in script_data_entries or []:
        entry_id = entry.get("id")
        if not entry_id:
            continue

        try:
            tests = await _get("/mcp/Testing/getScriptDataTest", {"testScriptDataId": entry_id})
        except Exception:
            continue

        for test in tests or []:
            test_id = test.get("id")
            if not test_id or test_id in seen_test_ids:
                continue
            if test.get("sourceMessageId") != source_message_id:
                continue

            aggregated_tests.append({
                "testName": test.get("name", ""),
                "scriptDataTest": test_id,
            })
            seen_test_ids.add(test_id)

    if not aggregated_tests:
        raise RuntimeError(
            f"Could not rebuild test cluster for sourceMessageId {source_message_id}."
        )

    entrypoint = target_test.get("entrypoint") or {}
    return {
        "tests": aggregated_tests,
        "moduleName": context["script_data"].get("moduleName"),
        "serviceName": context["script_data"].get("serviceName"),
        "className": entrypoint.get("beanClass") or context["script_data"].get("className"),
        "methodName": entrypoint.get("method") or "",
        "testScriptIdList": [group_id],
        "testScriptId": group_id,
        "callId": source_message_id,
    }

@mcp.tool()
async def get_tests_by_call_for_test_script(script_data_test_id: str) -> str:
    """Returns replace payload for a specific method-level test using MCP-only APIs."""
    data = await _build_replace_payload(script_data_test_id)
    return json.dumps(data, ensure_ascii=False, default=str)

@mcp.tool()
async def delete_test_script(test_script_id: str) -> str:
    """Deletes an entire test script (group)."""
    data = await _delete("/mcp/Testing/deleteTestScript", {"testScriptId": test_script_id})
    return json.dumps(data, ensure_ascii=False, default=str)

@mcp.tool()
async def enabled_test_script(test_script_id: str, enabled: bool = True) -> str:
    """Enables or disables a test script."""
    data = await _post_json(
        "/mcp/Testing/enabledTestScript", {},
        params={"testScriptId": test_script_id, "enabled": str(enabled).lower()},
    )
    return json.dumps(data, ensure_ascii=False, default=str)

@mcp.tool()
async def regenerate_tests_by_call_for_test_script(
    script_data_test_id: str,
    new_call_ids: list[str]
) -> str:
    """Replaces a method test cluster using only MCP endpoints that are currently accessible."""
    body = await _build_replace_payload(script_data_test_id)
    body["newCallIds"] = new_call_ids
    data = await _post_json("/mcp/Testing/regenerateTestsByCallForTestScript", body)
    return json.dumps(data, ensure_ascii=False, default=str)



async def auto_generate_tests_for_service(
    module_name: str,
    service_name: str,
    test_name: str = "",
    test_type: str = "UNIT",
) -> str:
    """Automatically creates tests for ALL methods of a service.
    
    Flow:
    1. Fetches all recent calls for the service via get_last_calls
    2. Groups calls by className + methodName
    3. Picks the LATEST call (by callDateTime) for each unique method
    4. Creates a test group with all those call IDs
    
    This replicates the "Fill with last calls" + "Generate" flow from
    the BitDive QA frontend, but in a single MCP call.
    
    Args:
        module_name: Module name (e.g., "n6ri19tck6y")
        service_name: Service name (e.g., "faculty-microservice")
        test_name: Optional test name (auto-generated if empty)
        test_type: "UNIT" (default), "COMPONENT", or "INTEGRATION"
    
    Returns: Summary of what was created (methods covered, call IDs used)
    """
    # Step 1: Fetch all last calls
    raw_data = await _get("/mcp/LastCallService/getData", {
        "moduleName": module_name,
        "serviceName": service_name,
    })

    if not raw_data:
        return "No calls found for this service. Make some API calls first."

    # Step 1b: Flatten nested structures — API may return various formats
    all_calls = []
    if isinstance(raw_data, list):
        for item in raw_data:
            if isinstance(item, dict):
                # Could be a direct call record or a wrapper with nested lists
                if "className" in item and "methodName" in item:
                    all_calls.append(item)
                else:
                    # Try to extract nested call records
                    for key, val in item.items():
                        if isinstance(val, list):
                            for sub in val:
                                if isinstance(sub, dict) and "className" in sub:
                                    all_calls.append(sub)
            elif isinstance(item, list):
                for sub in item:
                    if isinstance(sub, dict) and "className" in sub:
                        all_calls.append(sub)
    elif isinstance(raw_data, dict):
        # Single wrapper object
        for key, val in raw_data.items():
            if isinstance(val, list):
                for sub in val:
                    if isinstance(sub, dict) and "className" in sub:
                        all_calls.append(sub)

    if not all_calls:
        return f"No valid call records found. Raw data type: {type(raw_data).__name__}, keys: {list(raw_data.keys()) if isinstance(raw_data, dict) else 'N/A'}"

    # Step 2: Group by className + methodName, keep latest per method
    method_map: dict[str, dict] = {}  # key -> best call
    for call in all_calls:
        cls = call.get("className", "")
        method = call.get("methodName", "")
        dt = call.get("callDateTime", "")
        trace_id = call.get("traceId", "") or call.get("messageId", "")

        if not cls or not method or not trace_id:
            continue

        key = f"{cls}.{method}"
        existing = method_map.get(key)
        if existing is None or dt > existing["callDateTime"]:
            method_map[key] = {
                "className": cls,
                "methodName": method,
                "traceId": trace_id,
                "callDateTime": dt,
            }

    if not method_map:
        return "No valid method calls found to create tests from."

    # Step 3: Collect call IDs
    call_ids = [v["traceId"] for v in method_map.values()]

    # Step 4: Generate test name if not provided
    if not test_name:
        test_name = f"{service_name} Auto Tests ({len(call_ids)} methods)"

    # Step 5: Create the test group
    body = {
        "name": test_name,
        "type": test_type.upper(),
        "testDataRules": {
            "callIdList": call_ids
        }
    }

    result = None
    create_error = None

    try:
        result = await _post_json("/mcp/Testing/createTestGroup", body)
    except Exception as e:
        create_error = str(e)

    # Step 6: Build summary
    lines = []
    if result:
        # result can be a list or dict depending on API changes
        group_id = result[0].get('id', '?') if isinstance(result, list) and result else (result.get('id', '?') if isinstance(result, dict) else '?')
        lines += [
            f"✅ Test group created: {test_name}",
            f"Type: {test_type.upper()}",
            f"Methods covered: {len(method_map)}",
            f"Test group ID/IDs: {group_id}",
        ]
    else:
        lines += [
            f"⚠️ Could not auto-create test group (auth issue: {create_error})",
            f"However, all data is ready. You can create the test via BitDive UI.",
            f"",
            f"Test name: {test_name}",
            f"Type: {test_type.upper()}",
            f"Methods found: {len(method_map)}",
            f"Call IDs: {json.dumps(call_ids)}",
        ]

    lines += ["", "Methods included:"]
    for key, info in sorted(method_map.items()):
        short_cls = info["className"].rsplit(".", 1)[-1]
        lines.append(
            f"  • {short_cls}.{info['methodName']}() "
            f"[call: {info['traceId'][:12]}... @ {info['callDateTime']}]"
        )

    return "\n".join(lines)


async def update_existing_test_group(
    test_script_id: str,
    module_name: str,
    service_name: str,
    new_call_ids: list[str] | None = None,
) -> str:
    """Updates an EXISTING test group with new trace data.
    Use this instead of create_test_group when you want to refresh tests
    for an existing test script (e.g., after code changes).
    
    If new_call_ids is not provided, automatically fetches the latest calls
    for the service (equivalent to "Fill with last calls" in BitDive UI).
    
    Args:
        test_script_id: UUID of the existing test group (from TestControllerTestAbstract.java)
        module_name: Module name (e.g., "n6ri19tck6y")
        service_name: Service name (e.g., "faculty-microservice")
        new_call_ids: Optional list of specific trace IDs to use. If empty, auto-fills from latest calls.
    
    Returns: Summary of updated methods
    """
    # Step 1: Get current script data to find all class-level entries
    script_data = await _get("/mcp/Testing/getScriptData", {"testScriptId": test_script_id})
    if not script_data:
        return json.dumps({"error": f"Test script {test_script_id} not found"})

    # Step 2: If no call IDs provided, fetch latest calls
    if not new_call_ids:
        raw_data = await _get("/mcp/LastCallService/getData", {
            "moduleName": module_name,
            "serviceName": service_name,
        })
        # Extract call IDs from the response
        new_call_ids = []
        if isinstance(raw_data, list):
            for item in raw_data:
                if isinstance(item, dict):
                    tid = item.get("traceId") or item.get("messageId")
                    if tid and tid not in new_call_ids:
                        new_call_ids.append(tid)
        if not new_call_ids:
            return "No recent calls found to update tests with."

    # Step 3: Iterate over each class entry and regenerate
    results = []
    errors = []
    for entry in script_data:
        entry_id = entry.get("id", "")
        class_name = entry.get("className", "")
        entry_service = entry.get("serviceName", "")
        
        # Only update entries matching our service
        if entry_service != service_name:
            results.append(f"  ⏭ {class_name} — skipped (service: {entry_service})")
            continue

        try:
            # 1. First, fetch the method-level tests for this class entry
            test_entries = await _get("/mcp/Testing/getScriptDataTest", {"testScriptDataId": entry_id})
            
            if not test_entries or not isinstance(test_entries, list):
                errors.append(f"  ⚠ {class_name} — no test entries found to replace")
                continue
            
            # 2. Get the test ID and fetch its comprehensive metadata (which contains the original callId)
            test_id = test_entries[0].get("id")
            existing_data = await _get("/mcp/Testing/getTestsByCallForTestScript", {"scriptDataTestId": test_id})
            
            call_id = existing_data.get("callId") if existing_data else None
            method_name = existing_data.get("methodName") if existing_data else test_entries[0].get("entrypoint", {}).get("method")
            
            # 3. Build a precise payload to replace ONLY the tests in this class
            body = {
                "tests": [
                    {"testName": t.get("name", ""), "scriptDataTest": t.get("id")}
                    for t in test_entries if t.get("id")
                ],
                "moduleName": module_name,
                "serviceName": service_name,
                "className": class_name,
                "methodName": method_name,
                "testScriptIdList": [test_script_id],
                "callId": call_id,
                "newCallIds": new_call_ids
            }

            await _post_json("/mcp/Testing/regenerateTestsByCallForTestScript", body)
            results.append(f"  ✅ {class_name} — regenerated")
        except Exception as e:
            errors.append(f"  ❌ {class_name} — {str(e)[:100]}")

    lines = [
        f"Update results for test script {test_script_id}:",
        f"Call IDs used: {new_call_ids[:3]}{'...' if len(new_call_ids) > 3 else ''}",
        "",
    ]
    if results:
        lines.append("Results:")
        lines.extend(results)
    if errors:
        lines.append("\nErrors:")
        lines.extend(errors)

    return "\n".join(lines)


@mcp.tool()
async def get_test_failure_details(test_script_id: str) -> str:
    """Returns a summary of all test results for a test script.
    Shows which classes passed/failed and provides details for failures.
    Use this to understand WHY tests failed without opening BitDive UI.
    
    Args:
        test_script_id: UUID of the test group
    """
    script_data = await _get("/mcp/Testing/getScriptData", {"testScriptId": test_script_id})
    if not script_data:
        return json.dumps({"error": f"Test script {test_script_id} not found"})

    lines = [f"Test Results for script {test_script_id}:", ""]
    passed = 0
    failed = 0
    no_result = 0

    for entry in script_data:
        class_name = _short_class(entry.get("className", "?"))
        service = entry.get("serviceName", "?")
        result = entry.get("resultSuccess")
        
        if result is None:
            no_result += 1
            lines.append(f"  ⬜ {class_name} ({service}) — not run yet")
        elif result.get("success"):
            passed += 1
            date = result.get("dateCall", "?")[:19]
            lines.append(f"  ✅ {class_name} ({service}) — passed @ {date}")
        else:
            failed += 1
            date = result.get("dateCall", "?")[:19]
            lines.append(f"  ❌ {class_name} ({service}) — FAILED @ {date}")
            
            # Try to get detailed failure info
            entry_id = entry.get("id", "")
            try:
                detail = await _get("/mcp/Testing/getTestsByCallForTestScript", {"scriptDataTestId": entry_id})
                if detail and detail.get("tests"):
                    for test in detail["tests"][:2]:  # Show first 2 tests max
                        test_name = test.get("name", "?")[:80]
                        expected = _truncate(test.get("expectedResult", ""), 150)
                        actual = _truncate(test.get("actualResult", ""), 150)
                        if expected or actual:
                            lines.append(f"       Test: {test_name}")
                            if expected:
                                lines.append(f"       Expected: {expected}")
                            if actual:
                                lines.append(f"       Actual:   {actual}")
            except:
                pass  # Detailed info not available

    summary = f"\nSummary: {passed} passed, {failed} failed, {no_result} not run (total: {len(script_data)})"
    lines.insert(1, summary)
    
    return "\n".join(lines)


@mcp.tool()
async def compare_trace_evolution(call_ids: list[str]) -> str:
    """Compares N traces chronologically to show the evolution of a method.
    Useful for tracking how a method changed across multiple deployments.
    Pass call IDs in chronological order (oldest first).
    
    Args:
        call_ids: List of 2+ call IDs to compare, ordered oldest → newest
    """
    if len(call_ids) < 2:
        return "Need at least 2 call IDs to compare."
    
    # Fetch all traces
    traces = []
    for cid in call_ids:
        trace = await _get("/mcp/FindTrace/findTraceAll", {"callId": cid})
        traces.append(trace)
    
    def _extract_metrics(trace):
        methods = []
        errors = []
        sqls = 0
        rests = 0
        
        def _walk(node):
            nonlocal sqls, rests
            cls = _short_class(node.get("className", "?"))
            m = node.get("methodName", "?")
            methods.append(f"{cls}.{m}()")
            sqls += len(node.get("sqlCalls", []))
            rests += len(node.get("restCalls", []))
            err = node.get("errorCallMessage")
            if err:
                errors.append(f"{cls}.{m}(): {_truncate(err, 80)}")
            for ch in node.get("childCalls", []):
                _walk(ch)
        
        _walk(trace)
        return {
            "time": trace.get("callTimeDelta", 0),
            "status": trace.get("codeResponse", 0),
            "methods": set(methods),
            "method_count": len(methods),
            "errors": errors,
            "sqls": sqls,
            "rests": rests,
        }
    
    all_metrics = [_extract_metrics(t) for t in traces]
    cls = _short_class(traces[0].get("className", "?"))
    method = traces[0].get("methodName", "?")
    
    lines = [
        f"=== TRACE EVOLUTION: {cls}.{method}() ===",
        f"Comparing {len(call_ids)} versions:",
        "",
    ]
    
    # Overview table
    lines.append("VERSION OVERVIEW:")
    for i, (cid, m) in enumerate(zip(call_ids, all_metrics), 1):
        status_icon = "✅" if m["status"] == 200 else "❌"
        err_str = f" | {len(m['errors'])} errors" if m["errors"] else ""
        lines.append(
            f"  v{i} ({cid[:8]}...): {status_icon} {m['status']} | "
            f"{m['time']:.0f}ms | {m['method_count']} calls | "
            f"{m['sqls']} SQL | {m['rests']} REST{err_str}"
        )
    lines.append("")
    
    # Step-by-step changes
    lines.append("CHANGES BETWEEN VERSIONS:")
    for i in range(1, len(all_metrics)):
        prev = all_metrics[i - 1]
        curr = all_metrics[i]
        lines.append(f"\n  v{i} → v{i+1} ({call_ids[i][:8]}...):")
        
        # Time change
        time_diff = curr["time"] - prev["time"]
        time_pct = (time_diff / prev["time"] * 100) if prev["time"] > 0 else 0
        icon = "🔺" if time_diff > 0 else "🔽"
        lines.append(f"    {icon} Time: {prev['time']:.0f}ms → {curr['time']:.0f}ms ({time_pct:+.0f}%)")
        
        # Status change
        if prev["status"] != curr["status"]:
            lines.append(f"    🔄 Status: {prev['status']} → {curr['status']}")
        
        # New/removed methods
        new_methods = curr["methods"] - prev["methods"]
        removed_methods = prev["methods"] - curr["methods"]
        if new_methods:
            lines.append(f"    + Added {len(new_methods)} methods: {', '.join(sorted(new_methods)[:5])}{'...' if len(new_methods) > 5 else ''}")
        if removed_methods:
            lines.append(f"    - Removed {len(removed_methods)} methods: {', '.join(sorted(removed_methods)[:5])}{'...' if len(removed_methods) > 5 else ''}")
        
        # SQL change
        if prev["sqls"] != curr["sqls"]:
            lines.append(f"    📊 SQL: {prev['sqls']} → {curr['sqls']}")
        
        # Error changes
        prev_err_set = set(prev["errors"])
        curr_err_set = set(curr["errors"])
        fixed = prev_err_set - curr_err_set
        new_errs = curr_err_set - prev_err_set
        if fixed:
            for e in sorted(fixed):
                lines.append(f"    ✅ Fixed: {e}")
        if new_errs:
            for e in sorted(new_errs):
                lines.append(f"    🚨 New error: {e}")
        if not new_methods and not removed_methods and prev["sqls"] == curr["sqls"] and not fixed and not new_errs:
            lines.append("    (no structural changes)")
    
    return "\n".join(lines)


# ═══════════════════════════════════════════════════════════════
#  Trace Intelligence (summaries & comparison)
# ═══════════════════════════════════════════════════════════════

def _short_class(full_class: str) -> str:
    """com.microservices.faculty.repository.StudentRepository -> StudentRepository"""
    return full_class.rsplit(".", 1)[-1] if full_class else full_class


def _truncate(text: str, max_len: int = 200) -> str:
    """Truncate long strings but keep them useful."""
    if not text or len(text) <= max_len:
        return text
    return text[:max_len] + "..."


def _truncate_mongo_doc(raw: str, max_len: int = 200) -> str:
    """Truncate MongoDB document strings to show only key fields.
    Extracts operation type, collection, and key field names
    instead of showing entire documents with long AI responses.
    """
    if not raw or len(raw) <= max_len:
        return raw
    
    # Try to extract key info from REPLACE/INSERT operations
    # Format: [REPLACE collection {doc} filter: {filter}]
    try:
        # Extract operation and collection
        op_match = re.match(r'\[(\w+)\s+(\w+)\s+\{', raw)
        if op_match:
            operation = op_match.group(1)
            collection = op_match.group(2)
            
            # Try to parse the JSON portion to get field names
            json_start = raw.index('{')
            # Find the filter portion
            filter_idx = raw.find('filter:')
            if filter_idx > 0:
                doc_str = raw[json_start:filter_idx].strip()
                filter_str = raw[filter_idx:]
            else:
                doc_str = raw[json_start:]
                filter_str = ""
            
            # Try to extract field names from the doc
            try:
                doc = json.loads(doc_str)
                # Show field names and shortened values
                fields = []
                for k, v in doc.items():
                    if k.startswith("_"):
                        continue
                    if isinstance(v, str) and len(v) > 50:
                        fields.append(f'{k}="{v[:30]}..."')
                    else:
                        fields.append(f"{k}={_truncate(str(v), 30)}")
                filter_short = _truncate(filter_str, 60) if filter_str else ""
                fields_str = ', '.join(fields[:6])
                return f"[{operation} {collection} " + "{" + fields_str + "}]" + f" {filter_short}"
            except:
                pass  # JSON parsing failed, fall through
    except:
        pass
    
    # Fallback: simple truncation
    return _truncate(raw, max_len)


def _summarize_node(node: dict, depth: int = 0) -> list[str]:
    """Recursively summarize a trace node into readable lines."""
    lines = []
    cls = _short_class(node.get("className", "?"))
    method = node.get("methodName", "?")
    delta = node.get("callTimeDelta")
    delta_str = f"{delta:.2f}ms" if delta is not None else "?ms"
    op = node.get("operationType", "")
    err = node.get("errorCallMessage")
    ret = node.get("methodReturn")
    code = node.get("codeResponse", 0)
    indent = "  " * depth

    # Main line
    label = f"{indent}{cls}.{method}() — {delta_str} [{op}]"
    if code and code >= 400:
        label += f" HTTP {code}"
    if err:
        label += f" ERROR: {_truncate(err, 120)}"
    lines.append(label)

    # SQL calls
    for sql in node.get("sqlCalls", []):
        sql_text = sql.get("sql", "?")
        sql_delta = sql.get("sqlCallTimeDelta") or sql.get("callTimeDelta")
        sql_delta_str = f"{sql_delta:.2f}ms" if sql_delta is not None else ""
        sql_err = sql.get("errorCallMessage")
        line = f"{indent}  SQL: {_truncate(sql_text, 150)}"
        if sql_delta_str:
            line += f" ({sql_delta_str})"
        if sql_err:
            line += f" ERROR: {sql_err}"
        lines.append(line)

    # REST calls
    for rest in node.get("restCalls", []):
        uri = rest.get("uri", "?")
        rest_method = rest.get("method", "?")
        status = rest.get("statusCode", "?")
        rest_delta = rest.get("callTimeDelta")
        rest_str = f"{rest_delta:.2f}ms" if rest_delta else ""
        lines.append(f"{indent}  REST: {rest_method} {uri} -> {status} ({rest_str})")

    # Queue calls
    for q in node.get("queueCalls", []):
        topic = q.get("queueTopic", "?")
        lines.append(f"{indent}  QUEUE: {topic}")

    # Return value (only for root or important nodes)
    if depth == 0 and ret:
        lines.append(f"{indent}  Return: {_truncate(ret, 300)}")

    # Child calls
    for child in node.get("childCalls", []):
        lines.extend(_summarize_node(child, depth + 1))

    return lines


def _build_summary(trace: dict) -> str:
    """Build a complete human-readable summary from a trace tree."""
    cls = _short_class(trace.get("className", "?"))
    method = trace.get("methodName", "?")
    delta = trace.get("callTimeDelta")
    delta_str = f"{delta:.2f}ms" if delta is not None else "?ms"
    op = trace.get("operationType", "")
    code = trace.get("codeResponse", 0)
    err = trace.get("errorCallMessage")

    # Count totals
    sql_count = 0
    rest_count = 0
    child_count = 0

    def _count(node):
        nonlocal sql_count, rest_count, child_count
        sql_count += len(node.get("sqlCalls", []))
        rest_count += len(node.get("restCalls", []))
        for ch in node.get("childCalls", []):
            child_count += 1
            _count(ch)

    _count(trace)

    header = (
        f"Method: {cls}.{method}()\n"
        f"Type: {op} | Status: {code} | Time: {delta_str}\n"
        f"Totals: {child_count} child calls, {sql_count} SQL, {rest_count} REST"
    )
    if err:
        header += f"\nERROR: {_truncate(err, 200)}"

    header += f"\nTrace ID: {trace.get('traceId', '?')}"
    header += f"\nCall ID: {trace.get('messageId', '?')}"

    steps = _summarize_node(trace)
    # Remove the root line (already in header), keep children indented
    body = "\n".join(steps[1:]) if len(steps) > 1 else "(no child calls)"

    return f"{header}\n\nExecution tree:\n{body}"


@mcp.tool()
async def find_trace_summary(call_id: str) -> str:
    """Returns a human-readable summary of a call trace.
    Shows the execution tree with method names, timings, SQL queries,
    REST calls, return values, and errors in a compact format.
    Use this instead of find_trace_all when you need to understand
    what a method does without parsing raw JSON.
    """
    data = await _get("/mcp/FindTrace/findTraceAll", {"callId": call_id})
    return _build_summary(data)


@mcp.tool()
async def compare_traces(before_call_id: str, after_call_id: str) -> str:
    """Compares two call traces side-by-side (BEFORE vs AFTER).
    Shows differences in timing, SQL queries, child calls, and errors.
    Use after making a code change to verify the impact.
    """
    before = await _get("/mcp/FindTrace/findTraceAll", {"callId": before_call_id})
    after = await _get("/mcp/FindTrace/findTraceAll", {"callId": after_call_id})
    before_contracts = _build_contract_entries(before) if before else []
    after_contracts = _build_contract_entries(after) if after else []

    def _collect_data(node, data):
        cls = _short_class(node.get("className", "?"))
        m = node.get("methodName", "?")
        method_str = f"{cls}.{m}()"
        delta = node.get("callTimeDelta") or 0
        
        data["methods"].append(method_str)
        if delta > 0:
            data["timings"].append((method_str, delta))
            
        err = node.get("errorCallMessage")
        if err:
            data["errors"].append(f"[{method_str}] {_truncate(err, 200)}")
            
        for sql in node.get("sqlCalls", []):
            sql_raw = sql.get("sql", "?")
            # Improved NoSQL redirection: handle leading spaces and square bracket markers
            sql_stripped = sql_raw.strip()
            if sql_stripped.startswith("["):
                if "facultydb" in sql_stripped or "mongo" in sql_stripped.lower():
                    # Truncate MongoDB documents to key fields only
                    mongo_short = _truncate_mongo_doc(sql_stripped)
                    data["mongos"].append(mongo_short)
                elif "cassandra" in sql_stripped.lower() or "keyspace" in sql_stripped.lower() or "faculty_ks" in sql_stripped.lower():
                    data["cassandras"].append(sql_stripped)
                else:
                    # Generic NoSQL bracketed call
                    data["sqls"].append(sql_raw)
            else:
                sql_norm = _normalize_sql(sql_raw)
                data["sqls"].append(sql_norm)
            
            sql_err = sql.get("errorCallMessage")
            if sql_err:
                data["errors"].append(f"[DB Error in {method_str}] {_truncate(sql_err, 200)}")
        
        for m in node.get("mongoCalls", []):
            m_op = m.get("operationType", "QUERY")
            m_collection = m.get("collection", "?")
            m_filter = m.get("filter", "")
            m_str = f"MONGO {m_op} {m_collection} {m_filter}"
            data["mongos"].append(m_str)
            m_err = m.get("errorCallMessage")
            if m_err:
                data["errors"].append(f"[MONGO in {method_str}] {_truncate(m_err, 200)}")

        for c in node.get("cassandraCalls", []):
            c_query = c.get("query", "?")
            data["cassandras"].append(c_query)
            c_err = c.get("errorCallMessage")
            if c_err:
                data["errors"].append(f"[CASSANDRA in {method_str}] {_truncate(c_err, 200)}")
                
        for rest in node.get("restCalls", []):
            uri = rest.get("uri", "?")
            rest_method = rest.get("methodRest", "?")
            rest_str = f"{rest_method} {uri}"
            data["rests"].append(rest_str)
            rest_err = rest.get("errorCallMessage")
            if rest_err:
                data["errors"].append(f"[REST {rest_str}] {_truncate(rest_err, 200)}")
                
        for ch in node.get("childCalls", []):
            _collect_data(ch, data)

    def _init_data():
        return {
            "methods": [], "timings": [], "errors": [], 
            "sqls": [], "rests": [], "mongos": [], "cassandras": []
        }

    b_data = _init_data()
    a_data = _init_data()
    _collect_data(before, b_data)
    _collect_data(after, a_data)

    # Calculate frequencies for N+1 detection
    b_methods_count = Counter(b_data["methods"])
    a_methods_count = Counter(a_data["methods"])
    b_sqls_count = Counter(b_data["sqls"])
    a_sqls_count = Counter(a_data["sqls"])
    b_rests_count = Counter(b_data["rests"])
    a_rests_count = Counter(a_data["rests"])
    b_mongos_count = Counter(b_data["mongos"])
    a_mongos_count = Counter(a_data["mongos"])
    b_cassandras_count = Counter(b_data["cassandras"])
    a_cassandras_count = Counter(a_data["cassandras"])

    b_delta = before.get("callTimeDelta", 0)
    a_delta = after.get("callTimeDelta", 0)
    root_label = f"{_short_class(before.get('className','?'))}.{before.get('methodName','?')}()"
    if root_label == "?.?()":
        root_label = f"{_short_class(after.get('className','?'))}.{after.get('methodName','?')}()"

    lines = [
        f"=== TRACE COMPARISON ===",
        f"Method: {root_label}",
        f"",
        f"BEFORE ({before_call_id[:8]}...):",
        f"  Time: {b_delta:.2f}ms | Status: {before.get('codeResponse', '?')} | SQLs: {len(b_data['sqls'])} | Steps: {len(b_data['methods'])}",
        f"AFTER ({after_call_id[:8]}...):",
        f"  Time: {a_delta:.2f}ms | Status: {after.get('codeResponse', '?')} | SQLs: {len(a_data['sqls'])} | Steps: {len(a_data['methods'])}",
        f"",
    ]

    if before_contracts and after_contracts:
        lines.extend(_format_contract_section(before_contracts[0], after_contracts[0]))
        lines.extend(_format_path_contract_changes(before_contracts, after_contracts))


    # --- Section: Methods Diff ---
    all_methods = set(b_methods_count.keys()) | set(a_methods_count.keys())
    method_diffs = []
    for m in sorted(all_methods):
        bc = b_methods_count.get(m, 0)
        ac = a_methods_count.get(m, 0)
        if bc == 0:
            method_diffs.append(f"  + {m} (new method)")
        elif ac == 0:
            method_diffs.append(f"  - {m} (method removed)")
        elif bc != ac:
            method_diffs.append(f"  Δ {m}: {bc} -> {ac} calls (Potential logic change)")
    
    if method_diffs:
        lines.append("METHOD CHANGES:")
        lines.extend(method_diffs[:15])
        if len(method_diffs) > 15:
            lines.append(f"  ... and {len(method_diffs)-15} more method changes")
        lines.append("")

    # --- Section: SQL Diff (N+1 Detector) ---
    all_sqls = set(b_sqls_count.keys()) | set(a_sqls_count.keys())
    sql_diffs = []
    n1_warnings = []
    
    for s in all_sqls:
        bc = b_sqls_count.get(s, 0)
        ac = a_sqls_count.get(s, 0)
        s_trunc = _truncate(s, 100)
        
        if bc == 0:
            sql_diffs.append(f"  + {s_trunc} (new query)")
            if ac > 5:
                n1_warnings.append(f"  🚨 N+1 ALERT: New query executed {ac} times!")
        elif ac == 0:
            sql_diffs.append(f"  - {s_trunc} (query removed)")
        elif bc != ac:
            sql_diffs.append(f"  Δ {s_trunc}: {bc} -> {ac} executions")
            if ac > bc * 2 and ac > 5:
                n1_warnings.append(f"  🚨 N+1 ALERT: Query executions jumped from {bc} to {ac}!")

    if sql_diffs:
        lines.append("SQL QUERY CHANGES:")
        lines.extend(sql_diffs[:10])
        if len(sql_diffs) > 10:
            lines.append(f"  ... and {len(sql_diffs)-10} more SQL changes")
        if n1_warnings:
            lines.extend(["", "⚠️ PERFORMANCE WARNINGS:"] + n1_warnings)
        lines.append("")

    # --- Section: REST Diff ---
    all_rests = set(b_rests_count.keys()) | set(a_rests_count.keys())
    rest_diffs = []
    for r in all_rests:
        bc = b_rests_count.get(r, 0)
        ac = a_rests_count.get(r, 0)
        if bc == 0:
            rest_diffs.append(f"  + {r} (new REST call)")
        elif ac == 0:
            rest_diffs.append(f"  - {r} (REST call removed)")
    
    if rest_diffs:
        lines.append("REST CALL CHANGES:")
        lines.extend(rest_diffs)
        lines.append("")

    # --- Section: Mongo Diff ---
    all_mongos = set(b_mongos_count.keys()) | set(a_mongos_count.keys())
    mongo_diffs = []
    for m in all_mongos:
        bc, ac = b_mongos_count.get(m, 0), a_mongos_count.get(m, 0)
        if bc != ac:
            if bc == 0: mongo_diffs.append(f"  + {m} (new query)")
            elif ac == 0: mongo_diffs.append(f"  - {m} (query removed)")
            else: mongo_diffs.append(f"  ~ {m} ({bc} -> {ac} queries)")
    if mongo_diffs:
        lines.append("MONGO CHANGES (NoSQL):")
        lines.extend(mongo_diffs[:10])
        if len(mongo_diffs) > 10: lines.append(f"  ... and {len(mongo_diffs)-10} more Mongo changes")
        lines.append("")

    # --- Section: Cassandra Diff ---
    all_cass = set(b_cassandras_count.keys()) | set(a_cassandras_count.keys())
    cass_diffs = []
    for c in all_cass:
        bc, ac = b_cassandras_count.get(c, 0), a_cassandras_count.get(c, 0)
        if bc != ac:
            if bc == 0: cass_diffs.append(f"  + {c} (new query)")
            elif ac == 0: cass_diffs.append(f"  - {c} (query removed)")
            else: cass_diffs.append(f"  ~ {c} ({bc} -> {ac} queries)")
    if cass_diffs:
        lines.append("CASSANDRA CHANGES:")
        lines.extend(cass_diffs[:10])
        if len(cass_diffs) > 10: lines.append(f"  ... and {len(cass_diffs)-10} more Cassandra changes")
        lines.append("")

    # --- Section: Errors Diff ---
    b_errors = set(b_data["errors"])
    a_errors = set(a_data["errors"])
    
    new_errors = a_errors - b_errors
    fixed_errors = b_errors - a_errors
    persistent_errors = a_errors & b_errors

    if new_errors or fixed_errors or persistent_errors:
        lines.append("ERROR CHANGES:")
        if fixed_errors:
            for e in sorted(fixed_errors):
                lines.append(f"  ✅ FIXED: {e}")
        if new_errors:
            for e in sorted(new_errors):
                lines.append(f"  🚨 NEW ERROR: {e}")
        if persistent_errors:
            for e in sorted(persistent_errors):
                lines.append(f"  ⚠️ STILL PRESENT: {e}")
        lines.append("")

    # Bottlenecks
    if a_data["timings"]:
        lines.append("⏱️ SLOWEST OPERATIONS (AFTER):")
        top_slowest = sorted(a_data["timings"], key=lambda x: x[1], reverse=True)[:3]
        for m, t in top_slowest:
            lines.append(f"  • {t:.2f}ms : {m}")

    # Return value
    b_ret = _truncate(before.get("methodReturn", ""), 200)
    a_ret = _truncate(after.get("methodReturn", ""), 200)
    if b_ret != a_ret:
        lines += ["", "RETURN VALUE CHANGED:", f"  BEFORE: {b_ret or '(none)'}", f"  AFTER:  {a_ret or '(none)'}"]

    return "\n".join(lines)


# ── Entry point ─────────────────────────────────────────────────
if __name__ == "__main__":
    mcp.run(transport="stdio")
