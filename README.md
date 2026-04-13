# k8-mcp

A Spring AI MCP (Model Context Protocol) server that exposes `kubectl` and `istioctl` commands as callable tools for AI assistants. Connect Claude, GitHub Copilot, or any MCP-compatible AI assistant to query and inspect your Kubernetes cluster and Istio service mesh using natural language.

## Prerequisites

- A `kubeconfig` file with cluster access (minimum `view` ClusterRole)
- **Local run**: `kubectl` and `istioctl` available in your `PATH`
- **Docker run**: only Docker required (kubectl and istioctl are bundled in the image)

## Deploying to Kubernetes

Manifests are in `.k8/`:

| File | Resource | Purpose |
|---|---|---|
| `namespace.yaml` | `Namespace/k8-mcp` | Dedicated namespace with `istio-injection: enabled` |
| `serviceaccount.yaml` | `ServiceAccount/k8-mcp` | Pod identity |
| `clusterrole.yaml` | `ClusterRole/k8-mcp-readonly` | Read-only access to pods, nodes, configmaps, Istio CRDs |
| `clusterrolebinding.yaml` | `ClusterRoleBinding/k8-mcp-readonly` | Binds ServiceAccount to ClusterRole |
| `deployment.yaml` | `Deployment/k8-mcp` | App container, non-root, Istio sidecar injected via namespace label |
| `service.yaml` | `Service/k8-mcp` | ClusterIP on port 8080 |
| `virtualservice.yaml` | `VirtualService/k8-mcp` | Istio routing with timeout disabled for SSE long-lived connections |
| `authorizationpolicy.yaml` | `AuthorizationPolicy/k8-mcp` | Allow-list for MCP, REST API, and Swagger endpoints |

When running inside the cluster, `kubectl` and `istioctl` automatically use the pod's service account token — no kubeconfig mount is needed.

```bash
# 1. Build and push your image
./mvnw clean package -DskipTests
docker build -t ghcr.io/your-org/k8-mcp:1.0.0 .
docker push ghcr.io/your-org/k8-mcp:1.0.0

# 2. Update the image in .k8/deployment.yaml, then apply
kubectl apply -f .k8/

# 3. Verify
kubectl rollout status deployment/k8-mcp -n k8-mcp
kubectl get pods -n k8-mcp
```

To reach the server from outside the cluster (for connecting an AI assistant):

```bash
kubectl port-forward svc/k8-mcp 8080:8080 -n k8-mcp
# MCP SSE endpoint is now available at http://localhost:8080/sse
```

## Running with Docker (recommended)

```bash
# Build
./mvnw clean package -DskipTests
docker build -t k8-mcp .

# Run (mounts your local kubeconfig read-only)
docker run -v ~/.kube:/home/mcpuser/.kube:ro -p 8080:8080 k8-mcp
```

To use a custom kubeconfig path, set the `KUBECONFIG` environment variable:

```bash
docker run -v /path/to/kubeconfig:/home/mcpuser/.kube/config:ro \
           -e KUBECONFIG=/home/mcpuser/.kube/config \
           -p 8080:8080 k8-mcp
```

## Running Locally

```bash
./mvnw spring-boot:run
```

The server starts on `http://localhost:8080`.

| Endpoint | Description |
|---|---|
| `GET /sse` | MCP SSE endpoint (connect AI assistants here) |
| `GET /swagger-ui.html` | Interactive API docs — browse and inspect all tools |
| `GET /api-docs` | Raw OpenAPI JSON spec |
| `GET /api/tools` | List all registered MCP tools (name, description, JSON Schema) |
| `GET /api/tools/{name}` | Get a specific tool by name, e.g. `/api/tools/getPods` |

## Configuring with GitHub Copilot (VS Code)

Create or update `.vscode/mcp.json` in your workspace:

```json
{
  "servers": {
    "k8-mcp": {
      "type": "sse",
      "url": "http://localhost:8080/sse"
    }
  }
}
```

Alternatively, add to your VS Code user settings (`settings.json`):

```json
{
  "github.copilot.chat.mcp.servers": {
    "k8-mcp": {
      "type": "sse",
      "url": "http://localhost:8080/sse"
    }
  }
}
```

## Configuring with Claude Desktop

Add to your Claude Desktop config file:

- **macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
- **Windows**: `%APPDATA%\Claude\claude_desktop_config.json`

```json
{
  "mcpServers": {
    "k8-mcp": {
      "type": "sse",
      "url": "http://localhost:8080/sse"
    }
  }
}
```

Start the server first (`docker run` or `./mvnw spring-boot:run`), then restart Claude Desktop.

## Configuring with Claude Code (CLI)

Add to `~/.claude/settings.json`:

```json
{
  "mcpServers": {
    "k8-mcp": {
      "type": "sse",
      "url": "http://localhost:8080/sse"
    }
  }
}
```

Or run from the CLI:

```bash
claude mcp add --transport sse k8-mcp http://localhost:8080/sse
```

## Available Tools

### kubectl Tools

| Tool | Parameters | Description |
|---|---|---|
| `getPods` | `namespace?` | List pods (wide format with node). Null namespace = all namespaces. |
| `getPodsByLabel` | `namespace?`, `labelSelector` | Filter pods by label, e.g. `app=frontend,env=prod`. |
| `getNodes` | — | List all nodes with status, roles, version, and OS info. |
| `getPodLogs` | `namespace`, `podName` | Last 100 log lines from a pod. |
| `describePod` | `namespace`, `podName` | Full pod details: events, resources, env vars, volumes, conditions. |
| `describeNode` | `nodeName` | Full node details: capacity, conditions, taints, system info. |
| `getConfigMaps` | `namespace?` | List ConfigMaps. Null namespace = all namespaces. |
| `getPodsByNode` | `nodeName` | All pods scheduled on a specific node. |
| `getPodsByStatus` | `namespace?`, `status` | Filter pods by phase: `Running`, `Pending`, `Failed`, `Succeeded`, `Unknown`. |
| `getIstioCrd` | `crdKind`, `namespace?` | Get Istio CRDs in YAML: `ServiceEntry`, `VirtualService`, `DestinationRule`, `EnvoyFilter`. |

### istioctl Tools

| Tool | Parameters | Description |
|---|---|---|
| `proxyStatus` | `namespace?` | xDS sync status of all Envoy proxies. Detects out-of-sync sidecars. |
| `proxyConfigListener` | `namespace`, `podName` | Envoy listener config for a pod: inbound/outbound listeners, filter chains. |
| `proxyConfigEndpoint` | `namespace`, `podName` | Envoy endpoint config: upstream addresses, health status, locality. |
| `proxyConfigOutboundClusters` | `namespace`, `podName` | Outbound cluster config: circuit breakers, timeouts, TLS, load balancing. |
| `proxyConfigOutboundRoutes` | `namespace`, `podName` | Outbound route config: reflects applied VirtualService routing rules. |

## Example Prompts

Once connected to your AI assistant:

- _"Show me all pods in the production namespace"_
- _"Get logs from the api-gateway pod in the prod namespace"_
- _"Which pods are in a Failed state across the cluster?"_
- _"Describe the node worker-1 and check for any pressure conditions"_
- _"Show me all VirtualServices in the istio-system namespace"_
- _"What's the proxy status of all sidecars in the payments namespace?"_
- _"Show the outbound clusters for the checkout pod in the prod namespace"_

## Configuration Reference

| Property | Default | Description |
|---|---|---|
| `app.command.timeout-seconds` | `30` | Default subprocess timeout in seconds |
| `app.command.max-output-chars` | `50000` | Maximum output chars before truncation |
| `app.kubectl.path` | `kubectl` | Path to kubectl binary |
| `app.istioctl.path` | `istioctl` | Path to istioctl binary |
