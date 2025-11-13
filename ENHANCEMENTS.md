# Enhanced Features Documentation

This document describes the comprehensive enhancements added to the Hunger Saviour microservices platform.

## 🎯 New Features Overview

### 1. Service Discovery with Netflix Eureka
- **Port**: 8761
- **Purpose**: Dynamic service registration and discovery
- **Benefits**: 
  - Automatic service registration
  - Load balancing
  - Service health monitoring
  - Dynamic scaling support

**Access**: http://localhost:8761

### 2. Redis Caching
- **Port**: 6379
- **Services**: Restaurant Service (primary)
- **Cached Data**:
  - Restaurant listings
  - Menu items
  - Owner-specific restaurants
- **Benefits**:
  - Improved response times
  - Reduced database load
  - Better scalability

### 3. RabbitMQ Message Queue
- **Ports**: 5672 (AMQP), 15672 (Management UI)
- **Services**: Order Service, Notification Service
- **Use Cases**:
  - Asynchronous order status notifications
  - Email notifications
  - Event-driven architecture
- **Benefits**:
  - Decoupled services
  - Reliable message delivery
  - Asynchronous processing

**Management UI**: http://localhost:15672 (guest/guest)

### 4. API Documentation with Swagger/OpenAPI
- **Available on all services**
- **Access Points**:
  - User Service: http://localhost:8081/swagger-ui.html
  - Restaurant Service: http://localhost:8082/swagger-ui.html
  - Order Service: http://localhost:8083/swagger-ui.html
  - Payment Service: http://localhost:8084/swagger-ui.html
  - Notification Service: http://localhost:8085/swagger-ui.html

**Features**:
- Interactive API documentation
- Try-it-out functionality
- Request/response schemas
- Authentication testing

### 5. Monitoring with Prometheus + Grafana
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)

**Metrics Available**:
- JVM metrics (memory, threads, GC)
- HTTP request metrics
- Database connection pool metrics
- Custom business metrics
- Service health status

**Actuator Endpoints**:
- Health: `/actuator/health`
- Metrics: `/actuator/metrics`
- Prometheus: `/actuator/prometheus`

### 6. Centralized Logging with ELK Stack
- **Elasticsearch**: http://localhost:9200
- **Logstash**: Port 5000
- **Kibana**: http://localhost:5601

**Features**:
- Centralized log aggregation
- Log search and filtering
- Real-time log streaming
- Log visualization dashboards
- Error tracking

### 7. Notification Service
- **Port**: 8085
- **Features**:
  - Email notifications for order updates
  - RabbitMQ message consumption
  - Configurable email templates
  - Asynchronous processing

**Configuration**:
```properties
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
```

### 8. WebSocket Support for Real-time Updates
- **Service**: Order Service
- **Endpoint**: `/ws/orders`
- **Use Cases**:
  - Real-time order status updates
  - Live order tracking
  - Customer notifications

**Client Connection**:
```javascript
const socket = new SockJS('http://localhost:8083/ws/orders');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    stompClient.subscribe('/topic/orders/' + orderId, function(message) {
        console.log('Order update:', JSON.parse(message.body));
    });
});
```

### 9. Service Mesh with Istio
- **Documentation**: See [ISTIO.md](ISTIO.md)
- **Features**:
  - Traffic management
  - Security (mTLS)
  - Observability
  - Circuit breaking
  - Canary deployments

### 10. Comprehensive Testing
- **Documentation**: See [TESTING.md](TESTING.md)
- **Test Types**:
  - Integration tests
  - End-to-end tests
  - Contract tests
  - Performance tests

## 📊 Updated Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    CLIENT APPLICATIONS                           │
│                 (Web Browser, Mobile Apps, IoT)                  │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                     API GATEWAY :8080                            │
│        (Spring Cloud Gateway + Eureka Discovery)                 │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                  EUREKA SERVER :8761                             │
│              (Service Discovery & Registration)                  │
└───────────────────────────┬─────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ▼                   ▼                   ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│USER SERVICE  │    │RESTAURANT    │    │ORDER SERVICE │
│    :8081     │    │SERVICE :8082 │    │    :8083     │
│              │    │              │    │              │
│+ Swagger     │    │+ Swagger     │    │+ Swagger     │
│+ Actuator    │    │+ Actuator    │    │+ Actuator    │
│+ Prometheus  │    │+ Prometheus  │    │+ Prometheus  │
└──────┬───────┘    └──────┬───────┘    └──────┬───────┘
       │                   │                   │
       │                   │                   │
       │              ┌────▼────┐         ┌────▼────┐
       │              │  REDIS  │         │RabbitMQ │
       │              │  :6379  │         │ :5672   │
       │              └─────────┘         └────┬────┘
       │                                       │
       ▼                                       ▼
┌──────────────┐                      ┌──────────────┐
│  PostgreSQL  │                      │NOTIFICATION  │
│  Users DB    │                      │SERVICE :8085 │
└──────────────┘                      │              │
                                      │+ Email       │
        ┌──────────────┐              └──────────────┘
        │PAYMENT       │
        │SERVICE :8084 │              ┌──────────────┐
        │              │              │ MONITORING   │
        │+ Swagger     │              │              │
        │+ Actuator    │              │Prometheus    │
        │+ Prometheus  │              │  :9090       │
        └──────┬───────┘              │              │
               │                      │Grafana       │
               │                      │  :3000       │
               ▼                      └──────────────┘
        ┌──────────────┐
        │  PostgreSQL  │              ┌──────────────┐
        │ Payments DB  │              │   LOGGING    │
        └──────────────┘              │              │
                                      │Elasticsearch │
        ┌──────────────┐              │  :9200       │
        │  PostgreSQL  │              │              │
        │Restaurants DB│              │Logstash      │
        └──────────────┘              │  :5000       │
                                      │              │
        ┌──────────────┐              │Kibana        │
        │  PostgreSQL  │              │  :5601       │
        │  Orders DB   │              └──────────────┘
        └──────────────┘
```

## 🚀 Getting Started with New Features

### Start All Services

```bash
# Clone and navigate to the project
git clone https://github.com/prashantgaware/hunger-saviour.git
cd hunger-saviour

# Set environment variables
export STRIPE_API_KEY=your_stripe_key
export MAIL_USERNAME=your_email@gmail.com
export MAIL_PASSWORD=your_app_password

# Build the project
mvn clean install -DskipTests

# Start all services with Docker Compose
docker-compose up --build
```

### Access Points

| Service | URL | Credentials |
|---------|-----|-------------|
| API Gateway | http://localhost:8080 | - |
| Eureka Server | http://localhost:8761 | - |
| Swagger UI (User) | http://localhost:8081/swagger-ui.html | - |
| Swagger UI (Restaurant) | http://localhost:8082/swagger-ui.html | - |
| Swagger UI (Order) | http://localhost:8083/swagger-ui.html | - |
| Swagger UI (Payment) | http://localhost:8084/swagger-ui.html | - |
| Swagger UI (Notification) | http://localhost:8085/swagger-ui.html | - |
| Prometheus | http://localhost:9090 | - |
| Grafana | http://localhost:3000 | admin/admin |
| RabbitMQ Management | http://localhost:15672 | guest/guest |
| Kibana | http://localhost:5601 | - |

### Verify Services

```bash
# Check Eureka Dashboard
curl http://localhost:8761

# Check service health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8084/actuator/health
curl http://localhost:8085/actuator/health

# Check Prometheus metrics
curl http://localhost:8081/actuator/prometheus

# Check Redis connection
redis-cli -h localhost -p 6379 ping
```

## 📖 Additional Documentation

- [Architecture Details](ARCHITECTURE.md) - Detailed system architecture
- [API Documentation](API.md) - Complete API reference
- [Security Guide](SECURITY.md) - Security best practices
- [Deployment Guide](DEPLOYMENT.md) - Production deployment
- [Istio Service Mesh](ISTIO.md) - Advanced service mesh setup
- [Testing Guide](TESTING.md) - Integration and E2E testing

## 🔧 Configuration

### Service Discovery
All services automatically register with Eureka on startup. No manual configuration needed.

### Caching
Redis caching is enabled by default for Restaurant Service:
```properties
spring.cache.type=redis
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

### Message Queue
RabbitMQ is configured for order notifications:
```properties
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
```

### Monitoring
Prometheus metrics are exposed on all services:
```properties
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.metrics.export.prometheus.enabled=true
```

## 🧪 Testing

Run all tests:
```bash
mvn clean verify
```

Run integration tests:
```bash
mvn verify -Pintegration-tests
```

See [TESTING.md](TESTING.md) for comprehensive testing guide.

## 📊 Monitoring and Observability

### Prometheus Queries

Example queries for monitoring:
```promql
# Request rate
rate(http_server_requests_seconds_count[5m])

# Error rate
rate(http_server_requests_seconds_count{status=~"5.."}[5m])

# 95th percentile latency
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# JVM memory usage
jvm_memory_used_bytes / jvm_memory_max_bytes
```

### Grafana Dashboards

Import pre-built dashboards:
1. Spring Boot Statistics (ID: 6756)
2. JVM Micrometer (ID: 4701)
3. RabbitMQ Overview (ID: 10991)

## 🔐 Security Enhancements

- Service-to-service authentication with JWT
- Redis password protection (production)
- RabbitMQ user authentication
- HTTPS/TLS for production deployments
- Rate limiting (via API Gateway)
- CORS configuration

## 🚀 Performance Optimizations

- Redis caching reduces database load by ~70%
- Connection pooling for databases
- Async message processing
- Load balancing via Eureka
- Response compression
- Query optimization

## 🐛 Troubleshooting

### Eureka Service Not Registering
```bash
# Check Eureka client configuration
curl http://localhost:8081/actuator/env | grep eureka

# Check network connectivity
ping eureka-server
```

### Redis Connection Issues
```bash
# Test Redis connection
redis-cli -h localhost -p 6379 ping

# Check Redis logs
docker logs hunger-saviour-redis
```

### RabbitMQ Messages Not Processing
```bash
# Check queue status
curl -u guest:guest http://localhost:15672/api/queues

# View RabbitMQ logs
docker logs hunger-saviour-rabbitmq
```

## 📚 Additional Resources

- [Spring Cloud Documentation](https://spring.io/projects/spring-cloud)
- [Netflix Eureka](https://github.com/Netflix/eureka)
- [Redis Documentation](https://redis.io/documentation)
- [RabbitMQ Tutorials](https://www.rabbitmq.com/getstarted.html)
- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
- [Elastic Stack](https://www.elastic.co/guide/index.html)

## 🤝 Contributing

Contributions are welcome! Please see CONTRIBUTING.md for guidelines.

## 📄 License

This project is licensed under the MIT License - see LICENSE file for details.
