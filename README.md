# BitDive MCP Server

[![Python](https://img.shields.io/badge/Python-3.11%2B-3776AB?logo=python&logoColor=white)](#requirements)
[![MCP](https://img.shields.io/badge/MCP-FastMCP-111111)](#running-the-server)
[![Transport](https://img.shields.io/badge/Transport-stdio%20%7C%20streamable--http-0A7EA4)](#running-the-server)
[![BitDive](https://img.shields.io/badge/BitDive-Monitoring%20API-1F6FEB)](https://bitdive.io/)

Python MCP server for BitDive trace analysis, request reproduction, and regression management.

This repository exposes BitDive monitoring and QA operations to MCP clients such as Cursor, Claude Desktop, and other agent runtimes. The implementation lives in `server.py` and connects to the BitDive Monitoring API while adding its own formatting, normalization, and comparison logic on top.

> Use this repository from the `python-mcp-server` branch.

## Demo

[![Watch the BitDive demo](https://img.youtube.com/vi/WqtLXcODz8I/maxresdefault.jpg)](https://www.youtube.com/watch?v=WqtLXcODz8I)

Watch the BitDive product demo on YouTube:

- https://www.youtube.com/watch?v=WqtLXcODz8I

## Overview

This server is not just a thin API proxy.

It wraps BitDive API endpoints and makes them usable for agent workflows:

- compact heatmap summaries for discovery
- readable trace summaries instead of raw JSON only
- Bash and PowerShell reproduction commands from captured requests
- before/after trace comparison with payload and contract drift reporting
- SQL normalization and volatile-field filtering to reduce noisy diffs
- test-group inspection and regression-management flows

## What It Is For

Use this server when an AI agent or developer needs to:

- discover which module, service, class, or entrypoint is active
- fetch recent or historical traces
- inspect a trace without manually parsing BitDive JSON
- reproduce a captured web request locally
- compare two traces after a code change
- track how behavior evolved across multiple runs
- inspect and update BitDive test groups

## Tool Inventory

The current server exposes 23 MCP tools.

| Group | Tools |
| --- | --- |
| Discovery | `get_heatmap_all_system`, `get_heatmap_for_module`, `get_heatmap_for_service` |
| Recent traces | `get_last_calls` |
| Trace lookup | `find_trace_all`, `find_trace_for_method`, `find_trace_between_time`, `get_trace_names_batch` |
| Reproduction | `get_reproduction_command` |
| Method docs | `search_methods_short`, `search_methods_full` |
| Test management | `create_test_group`, `get_all_test_scripts`, `get_script_data`, `get_script_data_test`, `get_tests_by_call_for_test_script`, `delete_test_script`, `enabled_test_script`, `regenerate_tests_by_call_for_test_script`, `get_test_failure_details` |
| Trace intelligence | `find_trace_summary`, `compare_traces`, `compare_trace_evolution` |

## What The Code Adds

Several important behaviors are implemented inside `server.py`, not just delegated to the backend API.

### Trace readability

- `find_trace_summary` builds a readable execution tree
- SQL, REST, queue calls, timings, return values, and errors are formatted for direct MCP output

### Trace comparison

- `compare_traces` detects method-path drift
- payload and contract changes are compared after normalizing Java-serialized structures
- volatile fields such as IDs, UUIDs, timestamps, `traceId`, and `callId` can be ignored for cleaner diffs
- SQL execution deltas are grouped and normalized to surface likely N+1 patterns

### Reproduction workflow

- captured request URLs are normalized so internal Docker hostnames can be replayed from the host shell
- `curl` and PowerShell commands are generated from recorded headers, method, URL, and body

### Test-management helpers

- the server can rebuild replacement payloads through MCP-accessible APIs when direct helper data is not available
- test-group inspection is formatted for quick agent use instead of raw response browsing

## Runtime Model

| Layer | Responsibility |
| --- | --- |
| BitDive backend | Stores traces, monitoring data, and test metadata |
| `mcp-server` | Exposes MCP tools and adds comparison, normalization, and formatting logic |
| MCP client | Cursor, Claude Desktop, or another runtime invoking the tools |

## Requirements

- Python 3.11+
- `httpx`
- `mcp`
- a valid BitDive MCP token

Install dependencies:

```bash
pip install -r requirements.txt
```

## Configuration

### Environment variables

| Variable | Purpose | Default |
| --- | --- | --- |
| `BITDIVE_MCP_TOKEN` | Default token when a tool call does not pass `mcp_token` | none |
| `BITDIVE_API_URL` | Base BitDive Monitoring API URL | `https://cloud.bitdive.io/monitoring-api` |
| `BITDIVE_SKIP_VERIFY` | Disable TLS certificate verification when set to `true` | `false` |
| `MCP_TRANSPORT` | MCP transport mode | `stdio` |
| `MCP_HOST` | Host for HTTP mode | `0.0.0.0` |
| `MCP_PORT` | Port for HTTP mode | `8000` |

Every tool also accepts an optional `mcp_token` parameter. If omitted, the server falls back to `BITDIVE_MCP_TOKEN`.

## Running The Server

### `stdio` mode

```bash
python server.py
```

### `streamable-http` mode

```bash
MCP_TRANSPORT=streamable-http MCP_HOST=0.0.0.0 MCP_PORT=8000 python server.py
```

## Example MCP Client Configuration

```json
{
  "mcpServers": {
    "bitdive": {
      "command": "python",
      "args": [
        "/absolute/path/to/server.py"
      ],
      "env": {
        "BITDIVE_MCP_TOKEN": "your-token"
      }
    }
  }
}
```

## Repository Contents

| Path | Purpose |
| --- | --- |
| `server.py` | MCP server implementation |
| `requirements.txt` | Python dependencies |

## Notes

- The server fails fast if no MCP token is available.
- Fresh traces may not appear in the hot cache immediately after replay; the built-in workflow expects a short wait before checking recent calls again.
- This repository is the MCP bridge and trace-intelligence layer. It does not capture JVM events itself and it does not execute JUnit replay tests by itself.
