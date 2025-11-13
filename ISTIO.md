# Service Mesh with Istio - Deployment Guide

This guide provides instructions for deploying the Hunger Saviour microservices with Istio service mesh for advanced traffic management, security, and observability.

## Prerequisites

- Kubernetes cluster (1.20+)
- kubectl configured
- Istio 1.19+ installed
- Docker images built and pushed to registry

## Installing Istio

1. Download Istio:
```bash
curl -L https://istio.io/downloadIstio | sh -
cd istio-1.19.0
export PATH=$PWD/bin:$PATH
```

2. Install Istio with demo profile:
```bash
istioctl install --set profile=demo -y
```

3. Enable automatic sidecar injection:
```bash
kubectl label namespace default istio-injection=enabled
```

## Deploying Services

### 1. Deploy PostgreSQL Databases

```yaml
apiVersion: v1
kind: Service
metadata:
  name: postgres-users
spec:
  ports:
  - port: 5432
  selector:
    app: postgres-users
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgres-users
spec:
  selector:
    matchLabels:
      app: postgres-users
  template:
    metadata:
      labels:
        app: postgres-users
    spec:
      containers:
      - name: postgres
        image: postgres:15-alpine
        env:
        - name: POSTGRES_DB
          value: hunger_saviour_users
        - name: POSTGRES_USER
          value: postgres
        - name: POSTGRES_PASSWORD
          value: postgres
        ports:
        - containerPort: 5432
```

### 2. Deploy Microservices

Example for User Service:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: user-service
spec:
  ports:
  - port: 8081
    name: http
  selector:
    app: user-service
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service
spec:
  replicas: 2
  selector:
    matchLabels:
      app: user-service
  template:
    metadata:
      labels:
        app: user-service
        version: v1
    spec:
      containers:
      - name: user-service
        image: your-registry/user-service:latest
        ports:
        - containerPort: 8081
        env:
        - name: SPRING_DATASOURCE_URL
          value: jdbc:postgresql://postgres-users:5432/hunger_saviour_users
        - name: EUREKA_CLIENT_SERVICEURL_DEFAULTZONE
          value: http://eureka-server:8761/eureka/
```

### 3. Configure Istio Gateway

```yaml
apiVersion: networking.istio.io/v1beta1
kind: Gateway
metadata:
  name: hunger-saviour-gateway
spec:
  selector:
    istio: ingressgateway
  servers:
  - port:
      number: 80
      name: http
      protocol: HTTP
    hosts:
    - "*"
```

### 4. Configure Virtual Services

```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: user-service
spec:
  hosts:
  - "*"
  gateways:
  - hunger-saviour-gateway
  http:
  - match:
    - uri:
        prefix: /api/auth
    - uri:
        prefix: /api/users
    route:
    - destination:
        host: user-service
        port:
          number: 8081
```

## Traffic Management

### Canary Deployment

```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: order-service
spec:
  hosts:
  - order-service
  http:
  - match:
    - headers:
        user-type:
          exact: beta
    route:
    - destination:
        host: order-service
        subset: v2
  - route:
    - destination:
        host: order-service
        subset: v1
      weight: 90
    - destination:
        host: order-service
        subset: v2
      weight: 10
```

### Circuit Breaking

```yaml
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: payment-service
spec:
  host: payment-service
  trafficPolicy:
    connectionPool:
      tcp:
        maxConnections: 100
      http:
        http1MaxPendingRequests: 50
        http2MaxRequests: 100
    outlierDetection:
      consecutiveErrors: 5
      interval: 30s
      baseEjectionTime: 30s
      maxEjectionPercent: 50
```

## Security

### mTLS Configuration

Enable mutual TLS for all services:

```yaml
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default
  namespace: default
spec:
  mtls:
    mode: STRICT
```

### Authorization Policies

```yaml
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: payment-service-authz
spec:
  selector:
    matchLabels:
      app: payment-service
  action: ALLOW
  rules:
  - from:
    - source:
        principals: ["cluster.local/ns/default/sa/order-service"]
    to:
    - operation:
        methods: ["POST"]
        paths: ["/api/payments"]
```

## Observability

### Access Istio Dashboard

```bash
# Kiali
istioctl dashboard kiali

# Jaeger (Tracing)
istioctl dashboard jaeger

# Grafana (Metrics)
istioctl dashboard grafana
```

### Custom Metrics

Add custom metrics to services:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: istio-custom-bootstrap-config
data:
  custom_bootstrap.json: |
    {
      "stats_sinks": [
        {
          "name": "envoy.stat_sinks.metrics_service",
          "typed_config": {
            "@type": "type.googleapis.com/envoy.config.metrics.v3.MetricsServiceConfig"
          }
        }
      ]
    }
```

## Best Practices

1. **Use Namespaces**: Separate environments (dev, staging, prod) into different namespaces
2. **Resource Limits**: Set CPU and memory limits for all containers
3. **Health Checks**: Configure liveness and readiness probes
4. **Monitoring**: Enable Prometheus metrics collection
5. **Security**: Enable mTLS and authorization policies
6. **Traffic Management**: Use circuit breakers and timeouts
7. **Observability**: Enable distributed tracing

## Troubleshooting

### Check Istio Proxy Status

```bash
istioctl proxy-status
```

### View Proxy Configuration

```bash
istioctl proxy-config route user-service-xyz.default
```

### Analyze Service Mesh

```bash
istioctl analyze
```

## References

- [Istio Documentation](https://istio.io/latest/docs/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Spring Boot on Kubernetes](https://spring.io/guides/gs/spring-boot-kubernetes/)
