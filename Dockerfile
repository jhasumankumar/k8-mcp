FROM eclipse-temurin:25-jre-noble AS runtime

# Install kubectl
RUN apt-get update && apt-get install -y curl apt-transport-https gnupg && \
    curl -fsSL https://pkgs.k8s.io/core:/stable:/v1.32/deb/Release.key \
      | gpg --dearmor -o /etc/apt/keyrings/kubernetes-apt-keyring.gpg && \
    echo 'deb [signed-by=/etc/apt/keyrings/kubernetes-apt-keyring.gpg] https://pkgs.k8s.io/core:/stable:/v1.32/deb/ /' \
      > /etc/apt/sources.list.d/kubernetes.list && \
    apt-get update && apt-get install -y kubectl && \
    rm -rf /var/lib/apt/lists/*

# Install istioctl (latest stable release)
RUN ISTIO_VERSION=$(curl -sL https://api.github.com/repos/istio/istio/releases/latest \
      | grep '"tag_name"' | sed 's/.*"v\([^"]*\)".*/\1/') && \
    curl -sL "https://github.com/istio/istio/releases/download/${ISTIO_VERSION}/istioctl-${ISTIO_VERSION}-linux-amd64.tar.gz" \
      | tar -xz -C /usr/local/bin istioctl && \
    chmod +x /usr/local/bin/istioctl

WORKDIR /app
COPY target/k8-mcp-*.jar app.jar

# Run as non-root user; mount kubeconfig at /home/mcpuser/.kube/config
RUN useradd -r -u 1001 -m mcpuser
USER mcpuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
