# Deployment Guide - Hunger Saviour

This guide provides detailed instructions for deploying the Hunger Saviour platform in different environments.

## Table of Contents
1. [Local Development](#local-development)
2. [Production Deployment](#production-deployment)
3. [Environment Variables](#environment-variables)
4. [Database Setup](#database-setup)
5. [Monitoring and Logging](#monitoring-and-logging)

## Local Development

### Prerequisites
- Docker Desktop or Docker Engine + Docker Compose
- Java 17 (if running without Docker)
- Maven 3.6+ (if running without Docker)
- PostgreSQL 15 (if running without Docker)

### Quick Start with Docker Compose

1. Clone the repository:
```bash
git clone https://github.com/prashantgaware/hunger-saviour.git
cd hunger-saviour
```

2. Start all services:
```bash
docker-compose up --build
```

3. Access the services:
- API Gateway: http://localhost:8080
- User Service: http://localhost:8081
- Restaurant Service: http://localhost:8082
- Order Service: http://localhost:8083
- Payment Service: http://localhost:8084

4. Run the integration test:
```bash
chmod +x test-api.sh
./test-api.sh
```

### Running Individual Services (Without Docker)

1. Start PostgreSQL and create databases:
```sql
CREATE DATABASE hunger_saviour_users;
CREATE DATABASE hunger_saviour_restaurants;
CREATE DATABASE hunger_saviour_orders;
CREATE DATABASE hunger_saviour_payments;
```

2. Build all services:
```bash
mvn clean package -DskipTests
```

3. Run each service in a separate terminal:

**User Service:**
```bash
cd user-service
mvn spring-boot:run
```

**Restaurant Service:**
```bash
cd restaurant-service
mvn spring-boot:run
```

**Order Service:**
```bash
cd order-service
mvn spring-boot:run
```

**Payment Service:**
```bash
cd payment-service
export STRIPE_API_KEY=your_stripe_key
mvn spring-boot:run
```

**API Gateway:**
```bash
cd api-gateway
mvn spring-boot:run
```

## Production Deployment

### Docker Swarm Deployment

1. Initialize Docker Swarm:
```bash
docker swarm init
```

2. Deploy the stack:
```bash
docker stack deploy -c docker-compose.yml hunger-saviour
```

3. Check service status:
```bash
docker stack services hunger-saviour
```

### Kubernetes Deployment

1. Create namespace:
```bash
kubectl create namespace hunger-saviour
```

2. Apply database configurations:
```bash
kubectl apply -f k8s/postgres-deployments.yml -n hunger-saviour
```

3. Apply service deployments:
```bash
kubectl apply -f k8s/services.yml -n hunger-saviour
```

4. Expose API Gateway:
```bash
kubectl expose deployment api-gateway --type=LoadBalancer --port=8080 -n hunger-saviour
```

### Cloud Deployment (AWS, Azure, GCP)

#### AWS ECS/Fargate

1. Build and push Docker images to ECR:
```bash
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com

# Build and tag images
docker-compose build
docker tag hunger-saviour-api-gateway:latest <account-id>.dkr.ecr.us-east-1.amazonaws.com/hunger-saviour-api-gateway:latest
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/hunger-saviour-api-gateway:latest
# Repeat for all services
```

2. Create ECS Task Definitions for each service

3. Create ECS Services

4. Set up Application Load Balancer for API Gateway

#### Azure Container Instances

```bash
# Create resource group
az group create --name hunger-saviour-rg --location eastus

# Create container instances
az container create \
  --resource-group hunger-saviour-rg \
  --name api-gateway \
  --image hunger-saviour-api-gateway:latest \
  --dns-name-label hunger-saviour-gateway \
  --ports 8080
```

## Environment Variables

### User Service
```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/hunger_saviour_users
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=<secure-password>
JWT_SECRET=<your-jwt-secret-key>
JWT_EXPIRATION=86400000
```

### Restaurant Service
```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/hunger_saviour_restaurants
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=<secure-password>
```

### Order Service
```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/hunger_saviour_orders
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=<secure-password>
```

### Payment Service
```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/hunger_saviour_payments
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=<secure-password>
STRIPE_API_KEY=<your-stripe-secret-key>
```

### API Gateway
```bash
SPRING_CLOUD_GATEWAY_ROUTES_0_URI=http://user-service:8081
SPRING_CLOUD_GATEWAY_ROUTES_1_URI=http://restaurant-service:8082
SPRING_CLOUD_GATEWAY_ROUTES_2_URI=http://order-service:8083
SPRING_CLOUD_GATEWAY_ROUTES_3_URI=http://payment-service:8084
```

## Database Setup

### Production PostgreSQL Configuration

1. Use managed database services:
   - AWS RDS PostgreSQL
   - Azure Database for PostgreSQL
   - Google Cloud SQL for PostgreSQL

2. Create separate databases for each service:
```sql
CREATE DATABASE hunger_saviour_users;
CREATE DATABASE hunger_saviour_restaurants;
CREATE DATABASE hunger_saviour_orders;
CREATE DATABASE hunger_saviour_payments;

-- Create dedicated users
CREATE USER user_service WITH PASSWORD '<secure-password>';
GRANT ALL PRIVILEGES ON DATABASE hunger_saviour_users TO user_service;

CREATE USER restaurant_service WITH PASSWORD '<secure-password>';
GRANT ALL PRIVILEGES ON DATABASE hunger_saviour_restaurants TO restaurant_service;

CREATE USER order_service WITH PASSWORD '<secure-password>';
GRANT ALL PRIVILEGES ON DATABASE hunger_saviour_orders TO order_service;

CREATE USER payment_service WITH PASSWORD '<secure-password>';
GRANT ALL PRIVILEGES ON DATABASE hunger_saviour_payments TO payment_service;
```

3. Configure connection pooling in application.properties:
```properties
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
```

### Database Backup

Set up automated backups:
```bash
# Create backup script
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
pg_dump -h localhost -U postgres hunger_saviour_users > backup_users_$DATE.sql
pg_dump -h localhost -U postgres hunger_saviour_restaurants > backup_restaurants_$DATE.sql
pg_dump -h localhost -U postgres hunger_saviour_orders > backup_orders_$DATE.sql
pg_dump -h localhost -U postgres hunger_saviour_payments > backup_payments_$DATE.sql
```

## Monitoring and Logging

### Application Metrics

Enable Spring Boot Actuator endpoints:
```properties
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.metrics.export.prometheus.enabled=true
```

### Logging Configuration

Use centralized logging with ELK Stack:

1. Add Logstash dependency to each service
2. Configure log shipping to Elasticsearch
3. Visualize logs in Kibana

Example logback-spring.xml:
```xml
<configuration>
    <appender name="LOGSTASH" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
        <destination>logstash:5000</destination>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder" />
    </appender>
    
    <root level="INFO">
        <appender-ref ref="LOGSTASH" />
    </root>
</configuration>
```

### Health Checks

All services expose health check endpoints:
- User Service: http://localhost:8081/api/auth/health
- Restaurant Service: http://localhost:8082/api/restaurants/health
- Order Service: http://localhost:8083/api/orders/health
- Payment Service: http://localhost:8084/api/payments/health

Configure load balancer health checks to these endpoints.

## Security Best Practices

1. **Never commit secrets**: Use environment variables or secret management services
2. **Use HTTPS**: Configure SSL/TLS certificates in production
3. **Database Security**: Use strong passwords and restrict network access
4. **JWT Secret**: Use a strong, unique secret key (minimum 256 bits)
5. **Stripe API Keys**: Use test keys in development, live keys in production only
6. **Rate Limiting**: Implement rate limiting at API Gateway level
7. **CORS**: Configure appropriate CORS policies for production domains

## Scaling Considerations

### Horizontal Scaling

Scale individual services based on load:
```bash
# Docker Swarm
docker service scale hunger-saviour_user-service=3
docker service scale hunger-saviour_order-service=5

# Kubernetes
kubectl scale deployment user-service --replicas=3 -n hunger-saviour
kubectl scale deployment order-service --replicas=5 -n hunger-saviour
```

### Database Scaling

1. Enable read replicas for heavy read operations
2. Implement database connection pooling
3. Use caching (Redis) for frequently accessed data

## Troubleshooting

### Service Won't Start
```bash
# Check logs
docker-compose logs <service-name>

# Check service status
docker-compose ps

# Restart specific service
docker-compose restart <service-name>
```

### Database Connection Issues
```bash
# Test database connectivity
docker exec -it <postgres-container> psql -U postgres -l

# Check connection from service
docker exec -it <service-container> ping postgres
```

### JWT Authentication Issues
- Verify JWT_SECRET is properly set
- Check token expiration settings
- Ensure Authorization header format: "Bearer <token>"

## Support

For issues and questions:
- GitHub Issues: https://github.com/prashantgaware/hunger-saviour/issues
- Documentation: README.md
