# BitDive MCP Server

MCP server for BitDive monitoring and QA tools. The repository is intentionally simple: one Python entry point, [`server.py`](./server.py), which exposes BitDive API operations over MCP `stdio`.

## What This Repository Contains

- [`server.py`](./server.py): the MCP server
- [`bitdive.mcp.json`](./bitdive.mcp.json): example MCP client config
- [`openapi.json`](./openapi.json): reference API schema snapshot

## Requirements

- Python 3.11 or newer
- Installed Python packages required by [`server.py`](./server.py)
- A valid BitDive MCP token

If the required packages are not installed yet:

```powershell
pip install -r requirements.txt
```

## MCP Client Setup

The normal usage pattern is to point your MCP client directly at [`server.py`](./server.py).

Example configuration:

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

This is the only required connection pattern:

- `command`: Python executable available in your environment
- `args[0]`: absolute path to `server.py`
- `env.BITDIVE_MCP_TOKEN`: your BitDive token

## Available Tools

The server exposes the following MCP tools.

### Discovery

- `get_heatmap_all_system`: shows modules, services, entrypoints, error counts, SQL activity, REST activity, and average timings across the whole system
- `get_heatmap_for_module`: same heatmap view filtered to one module
- `get_heatmap_for_service`: same heatmap view filtered to one module and one service

Use these when you need to discover the real module, service, class, or method names before looking up traces.

### Trace Lookup

- `get_last_calls(module_name, service_name)`: returns recent call IDs for a service
- `find_trace_between_time(class_name, method_name, begin_date, end_date)`: finds historical traces for one method in a time range
- `get_trace_names_batch(call_ids)`: maps trace IDs to short `Class.method` names
- `get_reproduction_command(call_id)`: reconstructs curl and PowerShell commands from a captured request

Use these to find the exact trace you want to inspect or replay.

### Trace Inspection

- `find_trace_all(call_id)`: returns the full raw trace JSON
- `find_trace_for_method(call_id, class_name, method_name)`: returns one method subtree from a trace
- `find_trace_summary(call_id)`: returns a readable execution tree with timings, SQL, REST calls, queue calls, return values, and errors

Use `find_trace_summary` by default. Use raw JSON only when you need full payload details.

### Trace Comparison

- `compare_traces(before_call_id, after_call_id)`: compares two traces and shows timing changes, new or removed method calls, SQL drift, REST drift, errors, and likely N+1 patterns
- `compare_trace_evolution(call_ids)`: compares multiple traces in chronological order to show how a method changed over time

These tools are the fastest way to prove what changed after a code modification.

### Method Search

- `search_methods_short(query, limit)`: lightweight search for documented methods
- `search_methods_full(query, limit)`: fuller method search with more detailed metadata

Use these when you know only a keyword, business term, or partial method name.

### Test Management

- `get_all_test_scripts()`: lists all BitDive test groups
- `get_script_data(test_script_id)`: lists class-level entries inside one test group
- `get_script_data_test(test_script_data_id)`: lists method-level tests for one class entry
- `get_tests_by_call_for_test_script(script_data_test_id)`: returns detailed generated-test payloads for one entry
- `get_test_failure_details(test_script_id)`: summarizes pass/fail results and available failure details
- `create_test_group(name, test_type, call_id_list)`: creates a new BitDive test group from trace IDs
- `enabled_test_script(test_script_id, enabled)`: enables or disables a test group
- `delete_test_script(test_script_id)`: deletes a test group
- `regenerate_tests_by_call_for_test_script(script_data_test_id, new_call_ids)`: refreshes one method or class entry with new trace data
- `update_existing_test_group(test_script_id, module_name, service_name, new_call_ids)`: refreshes an existing group with latest or supplied trace IDs
- `auto_generate_tests_for_service(module_name, service_name, test_name, test_type)`: creates a new group using the latest trace for each discovered method in a service

Prefer refreshing existing test groups when behavior changed intentionally. Create a new group only when you actually need a new baseline set.

## Local Run

You can also start the server directly:

```powershell
python server.py
```

In practice, it is usually launched by Cursor, Antigravity, or another MCP client.

## Notes

- `BITDIVE_MCP_TOKEN` is required. The server fails fast if it is missing.
- Do not commit a real token to a public repository.
- [`bitdive.mcp.json`](./bitdive.mcp.json) is a local example, not a safe public secret store.
- [`openapi.json`](./openapi.json) is kept only as a reference snapshot.
- Trace indexing can lag after a fresh manual request. If a new trace does not appear immediately, wait about 30-45 seconds and retry.
