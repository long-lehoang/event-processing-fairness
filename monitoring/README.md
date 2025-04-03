## Monitoring Stack

This folder contains the Helm charts and configuration files for setting up monitoring for the application using Prometheus, Grafana, Loki, and Promtail.

### Available Components
- **Prometheus & Grafana**: For metrics collection, visualization, and alerting
- **Loki & Promtail**: For log aggregation and querying

### Deploying the Monitoring Stack

#### 1. Deploy Prometheus and Grafana

To deploy the kube-prometheus-stack Helm chart:

```sh
kubectl create ns monitoring
helm -n monitoring install prometheus-grafana-stack -f values-prometheus.yaml kube-prometheus-stack
```

If you encounter an issue with Node Exporter showing: "Error: failed to start container "node-exporter": Error response from daemon: path / is mounted on / but it is not a shared or slave mount", run:

```sh
kubectl patch ds prometheus-grafana-stack-prometheus-node-exporter -n monitoring --type "json" -p '[{"op": "remove", "path" : "/spec/template/spec/containers/0/volumeMounts/2/mountPropagation"}]'
```

#### 2. Deploy Loki and Promtail

To deploy the loki-stack Helm chart for log collection:

```sh
helm -n monitoring install loki -f values-loki.yaml loki-stack
```

The Loki configuration includes specific log scraping for the notifier service.

### Accessing the Dashboards

After deployment, you can access the Grafana dashboard to view metrics and logs.

### Configuration Files

- `values-prometheus.yaml`: Configuration for Prometheus and Grafana
- `values-loki.yaml`: Configuration for Loki and Promtail log collection system