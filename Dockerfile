# Stage 1: Download kubectl and istioctl into a throwaway Alpine layer.
# Both are static Go binaries — no libc dependency, safe to copy into any Linux image.
# TARGETARCH is set automatically by BuildKit: "amd64" on x86-64, "arm64" on Apple Silicon.
FROM --platform=$BUILDPLATFORM alpine:3.21 AS tools

ARG TARGETARCH

RUN apk add --no-cache curl && \
    curl -fsSL "https://dl.k8s.io/release/v1.32.0/bin/linux/${TARGETARCH}/kubectl" \
         -o /usr/local/bin/kubectl && \
    chmod +x /usr/local/bin/kubectl && \
    ISTIO_ARCH=$([ "$TARGETARCH" = "arm64" ] && echo "arm64" || echo "amd64") && \
    curl -fsSL "https://github.com/istio/istio/releases/download/1.24.5/istioctl-1.24.5-linux-${ISTIO_ARCH}.tar.gz" \
         | tar -xz -C /usr/local/bin istioctl && \
    chmod +x /usr/local/bin/istioctl

# Stage 2: Minimal JRE runtime on Alpine (~150 MB vs ~420 MB for Ubuntu Noble).
# curl, apt, and all build-time tooling are left behind in stage 1.
FROM eclipse-temurin:25-jre-alpine

COPY --from=tools /usr/local/bin/kubectl  /usr/local/bin/kubectl
COPY --from=tools /usr/local/bin/istioctl /usr/local/bin/istioctl

WORKDIR /app
COPY target/k8-mcp-*.jar app.jar

RUN adduser -D -u 1001 mcpuser
USER mcpuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
