# k8-mcp

A Spring AI MCP (Model Context Protocol) server that exposes `kubectl` and `istioctl` commands as callable tools for AI assistants. Connect Claude, GitHub Copilot, or any MCP-compatible AI assistant to query and inspect your Kubernetes cluster and Istio service mesh using natural language.

## Prerequisites

- **Local run**: `kubectl` and `istioctl` in your `PATH`, and a `kubeconfig` with cluster access
- **Docker run**: Docker only — `kubectl` and `istioctl` are bundled in the image
- **Kubernetes deploy**: Helm 3

## Testing with Minikube

`k8.yaml` is a self-contained manifest for local testing. It deploys the server into a `mcp-test` namespace with an Istio ingress gateway, so you can test the full mesh setup on a local cluster.

**Prerequisites:**

- [Minikube](https://minikube.sigs.k8s.io/) running
- Istio installed with ingress gateway:

```bash
istioctl install \
  --set profile=ambient \
  --set 'components.ingressGateways[0].enabled=true' \
  --set 'components.ingressGateways[0].name=istio-ingressgateway' -y
```

**Deploy:**

```bash
# 1. Build the image inside Minikube's Docker daemon (so imagePullPolicy:Never works)
eval $(minikube docker-env)
./mvnw clean package -DskipTests
docker build -t k8-mcp:latest .

# 2. Apply all resources
kubectl apply -f k8.yaml

# 3. Verify the pod is running
kubectl get pods -n mcp-test
```

**Access:**

```bash
# Option A — port-forward (simplest)
kubectl port-forward svc/k8-mcp 8080:8080 -n mcp-test

# Option B — via Istio ingress gateway
minikube tunnel   # keep this running in a separate terminal
kubectl get svc istio-ingressgateway -n istio-system   # get EXTERNAL-IP
curl http://<EXTERNAL-IP>/sse
```

**Connect an AI assistant** (once port-forward or tunnel is active):

```bash
# Claude Code — project-scoped
claude mcp add --transport sse --scope project k8-mcp http://localhost:8080/sse
```

## Deploying to Kubernetes (Helm)

`.k8/` is a Helm chart. All configurable values are in `.k8/values.yaml`.

When running inside the cluster, `kubectl` and `istioctl` automatically use the pod's service account token — no kubeconfig mount needed.

```bash
# 1. Build and push your image
./mvnw clean package -DskipTests
docker build -t ghcr.io/your-org/k8-mcp:1.0.0 .
docker push ghcr.io/your-org/k8-mcp:1.0.0

# 2. Install (set your image)
helm install k8-mcp .k8/ \
  --set image.repository=ghcr.io/your-org/k8-mcp \
  --set image.tag=1.0.0

# 3. Verify
kubectl rollout status deployment/k8-mcp -n k8-mcp
kubectl get pods -n k8-mcp
```

**Key values** (see `.k8/values.yaml` for the full list):

| Value | Default | Purpose |
|---|---|---|
| `image.repository` | `k8-mcp` | Container image |
| `image.tag` | `latest` | Image tag |
| `namespace` | `k8-mcp` | Deployment namespace |
| `springdoc.apiDocsPath` | `/api-docs` | OpenAPI JSON endpoint path |
| `springdoc.swaggerUiPath` | `/swagger-ui.html` | Swagger UI path |
| `app.command.timeoutSeconds` | `30` | Subprocess timeout |
| `app.command.maxOutputChars` | `50000` | Output truncation limit |
| `istio.ingressGateway.enabled` | `false` | Deploy Gateway + ingress VirtualService |
| `istio.tokenCreatorRole.enabled` | `false` | Grant token/port-forward rights in istio-system (needed for `proxy-status`) |

To reach the server from outside the cluster:

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

