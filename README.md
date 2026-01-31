# BitDive MCP Server

A Spring Boot application acting as a **Model Context Protocol (MCP)** server. It provides AI agents with tools to interact with the BitDive monitoring system, enabling retrieval of trace data, service maps, and error analysis.

## 🚀 Features

This server exposes tools that allow AI assistants to query monitoring data directly:

### Trace Tools
*   **`findTraceAll`**: Returns the full call trace for a specified Call ID.
*   **`findTraceForMethod`**: Retrieves the trace for a specific method within a given Call ID.
*   **`findTraceForMethodBetweenTime`**: Searches for specific method executions within a defined time range.

### Monitoring & Performance Tools
*   **`getCurrentHeapMapAllSystem`**: Returns system performance metrics for the entire system (Heat Map).
*   **`getCurrentHeapMapForModule`**: Returns performance metrics filtered by a specific module.
*   **`getCurrentHeapMapForModuleAndForService`**: Returns performance metrics for a specific service within a module.
*   **`getCurrentHeapMapForModuleAndForServiceClass`**: Returns performance metrics drilling down to a specific class within a service.
*   **`getLastCallService`**: Retrieves a list of recent execution traces (calls) for a specific service.

## 🛠 Technology Stack

*   **Java**: 17
*   **Framework**: Spring Boot 3.2.0
*   **AI Integration**: Spring AI 1.0.0 (MCP Server WebFlux)
*   **Database**: PostgreSQL
*   **Security**: HashiCorp Vault, Bouncy Castle

## ⚙️ Configuration

The application runs on port `8089` by default. Configuration is managed via `application.yml` and can be overridden with environment variables:

| Variable | Description | Default |
| :--- | :--- | :--- |
| `POSTGRES_URL` | JDBC URL | `jdbc:postgresql://37.27.0.220:5432/data-bitdive` |
| `POSTGRES_USER` | DB Username | `citizix_user` |
| `POSTGRES_PASS` | DB Password | `S3cret` |
| `VAULT_URL` | Vault URL | `https://sandbox.bitdive.io/vault` |
| `TOKEN_SECRET` | Token Secret | *(See application.yml)* |

## 📦 How to Run

### Prerequisites
*   JDK 17 or higher
*   Maven (wrapper provided)

### Build & Run
1.  **Build the project**:
    ```bash
    ./mvnw clean package
    ```

2.  **Start the server**:
    ```bash
    ./mvnw spring-boot:run
    ```

### MCP Connection
Once running, the server exposes the following endpoints for MCP clients:
*   **SSE Endpoint**: `http://localhost:8089/sse`
*   **Message Chat Endpoint**: `http://localhost:8089/mcp/message`

To use this with an MCP Client (like Cursor), add it to your configuration as an SSE server.

## 🐳 Deployment

The `pom.xml` is configured to copy the resulting JAR file to `../docker/docker-file-mcp-server` during the `package` phase, facilitating Docker builds.

## 🔌 Client Integration (Cursor / Claude)

To connect your AI tool to the BitDive MCP Server, follow these steps:

### 1. Configure the MCP Server
Add the following configuration to your `mcp.json` (usually located in `.cursor/mcp.json` or similar).

**Example: Connection to BitDive Cloud (SaaS)**
```json
{
  "mcpServers": {
    "bitdive": {
      "url": "https://cloud.bitdive.io/mcp/sse",
      "name": "BitDive MCP Server"
    }
  }
}
```

**For Self-Hosted Infrastructure:**
If you have deployed the [BitDive Infrastructure](https://github.com/bitDive/infrastructure) on your own server, replace `https://cloud.bitdive.io` with your custom domain (e.g., `https://your-domain.com/mcp/sse`).

### 2. Authentication (Temporary)
*Current limitations require passing the API key via prompts/rules. A seamless "One-Click Integration" is coming in the next sprint.*

1.  **Deploy Infrastructure**: You need a running instance of the BitDive platform.
    *   👉 [Get started with BitDive Infrastructure](https://github.com/bitDive/infrastructure)
2.  **Get your Token**: Log in to your BitDive dashboard and navigate to the **MCP Integration** section to copy your API Key.
3.  **Add User Rule**: In your AI tool (e.g., Cursor Settings > General > Rules for AI), add the following line:
    ```text
    for API key for mcp bitDive access use <YOUR_TOKEN_HERE>
    ```
