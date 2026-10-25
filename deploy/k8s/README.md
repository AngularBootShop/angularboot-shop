# Kubernetes Deployment Guide (Production & Local)

This directory contains hardened Kubernetes manifests for deploying the **Shop Microservices System**.

## 1. Security Architecture & Hardening
- **Namespace-level Pod Security Admission**: Enforces `restricted` profile (`pod-security.kubernetes.io/enforce: restricted`).
- **Pod Security Contexts**:
  - `runAsNonRoot: true`
  - `readOnlyRootFilesystem: true`
  - `allowPrivilegeEscalation: false`
  - `capabilities: drop: ["ALL"]`
  - `seccompProfile: type: RuntimeDefault`
- **Network Policies**:
  - `default-deny-all`: Zero-trust network segmentation.
  - Dedicated ingress/egress policies restricting traffic strictly between authorized services.
  - Internal stock endpoints (`/api/v1/internal/**`) are strictly reachable only by `order-service` pods, never by external ingress or `api-gateway`.
- **Zero Real Secrets**: All manifests use placeholder secrets (`PLACEHOLDER_REPLACE_WITH_VAULT_OR_SEALED_SECRET`). Integrate with HashiCorp Vault, AWS Secrets Manager, or Sealed Secrets before production rollout.
- **High Availability**: Configured `HorizontalPodAutoscaler` (HPA) and `PodDisruptionBudget` (PDB: `minAvailable: 1`) for zero-downtime rolling deploys.

## 2. Manifest Structure
```
deploy/k8s/
├── namespace.yaml           # Dedicated namespace with PodSecurity restricted
├── configmaps.yaml          # Non-sensitive runtime configurations
├── secrets.yaml             # Placeholder secrets (MUST substitute in production)
├── catalog-service.yaml     # Catalog Service deployment & ClusterIP service
├── order-service.yaml       # Order Service deployment & ClusterIP service
├── api-gateway.yaml         # API Gateway deployment & ClusterIP service
├── shop-ui.yaml             # Angular SPA unprivileged Nginx deployment
├── ingress.yaml             # Ingress routing rules
├── hpa.yaml                 # Autoscaling policies
├── pdb.yaml                 # Disruption budgets
└── network-policies.yaml    # Zero-trust microservice isolation
```

## 3. Deploying to Local Cluster (Kind / Minikube)

### 3.1 Create Cluster with Ingress & Port Mapping
```bash
cat <<EOF | kind create cluster --name shop-cluster --config=-
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
- role: control-plane
  kubeadmConfigPatches:
  - |
    kind: InitConfiguration
    nodeRegistration:
      kubeletExtraArgs:
        node-labels: "ingress-ready=true"
  extraPortMappings:
  - containerPort: 80
    hostPort: 80
    protocol: TCP
  - containerPort: 443
    hostPort: 443
    protocol: TCP
EOF
```

### 3.2 Install Ingress NGINX Controller
```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=90s
```

### 3.3 Apply Manifests in Sequence
```bash
kubectl apply -f deploy/k8s/namespace.yaml
kubectl apply -f deploy/k8s/configmaps.yaml
kubectl apply -f deploy/k8s/secrets.yaml
kubectl apply -f deploy/k8s/network-policies.yaml
kubectl apply -f deploy/k8s/catalog-service.yaml
kubectl apply -f deploy/k8s/order-service.yaml
kubectl apply -f deploy/k8s/api-gateway.yaml
kubectl apply -f deploy/k8s/shop-ui.yaml
kubectl apply -f deploy/k8s/hpa.yaml
kubectl apply -f deploy/k8s/pdb.yaml
kubectl apply -f deploy/k8s/ingress.yaml
```

### 3.4 Verify Deployment Status
```bash
kubectl get pods -n shop -o wide
kubectl get hpa -n shop
kubectl get pdb -n shop
kubectl get ingress -n shop
```
