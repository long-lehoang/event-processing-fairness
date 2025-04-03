### Local Environment Setup with Docker Compose

To set up your local development environment with PostgreSQL, Kafka, and Redis, run the following command:

```shell
docker-compose up -d
```

## Service Ports
- Kafka: `localhost:9092`
- Redis: `localhost:6379`
- PostgreSQL: `localhost:5432`

You can access these services using the default connection strings in your application or by connecting directly to these ports.