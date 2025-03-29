## Event Processing Project

### Overview
We deploy event processing for webhook application, ensure fairness when process event for each user/account_id, scalabity, reliability.

### Technical stack
#### - Infra: K8s(docker/minikube)
#### - Monitoring: Loki + Promtail + Grafana + Prometheus + AlertManager
#### - Application: SpringBoot, Kafka, Redis, Postgres

### To run application in local for debug
You can run docker-compose in `infra` folder
### To deploy in k8s
You can build image for `notifier` service and `producer` service, `webhook` is mock service to webhhok server (client).
#### Build Notifier image
```shell
cd notifier
./build.sh <version>
```

#### Build Producer image
```shell
cd producer
./build.sh <version>
```

#### Deploy our services in `helm-chart` folder
#### Deploy monitoring service in `monitoring` folder

### Document about architecture
Refer `docs` folder

### SQL script for insert data for testing
Refer `scripts` folder