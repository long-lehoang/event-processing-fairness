# Event Processing Fairness

## Overview
This project implements an event processing system for webhook applications that ensures fairness when processing events across users/account IDs. The system is designed for scalability, reliability, and equitable resource allocation.

## Technical Stack

### Infrastructure
- Kubernetes (K8s with Docker/Minikube)

### Monitoring
- Loki + Promtail
- Grafana
- Prometheus
- AlertManager

### Application
- Spring Boot
- Apache Kafka
- Redis
- PostgreSQL

## Development

### Local Development
To run the application locally for debugging:
```shell
# Navigate to the infra directory
cd infra

# Start services using docker-compose
docker-compose up
```

### Kubernetes Deployment

#### Build Service Images

Build the Notifier service:
```shell
cd notifier
./build.sh <version>
```

Build the Producer service:
```shell
cd producer
./build.sh <version>
```

Note: The `webhook` service is a mock implementation of a webhook server (client).

#### Deploy Services
- Deploy our main services using the configuration in the `helm-chart` folder
- Deploy monitoring services using the configuration in the `monitoring` folder

## Documentation
For detailed information about the system architecture, refer to the documentation in the `docs` folder.

## Testing
SQL scripts for test data insertion are available in the `scripts` folder.

## Performance Testing
Benchmark tests are available in the `benchmarks` folder.