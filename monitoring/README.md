## Deploy Helm Chart for Prometheus Grafana monitoring

To deploy kube-prometheus-stack Helm chart, use the following commands:

```sh
kubectl create ns monitoring
helm -n monitoring install prometheus-grafana-stack -f values-prometheus.yaml kube-prometheus-stack
```
If you have issue on Node Export: "Error: failed to start container "node-exporter": Error response from daemon: path / is mounted on / but it is not a shared or slave mount".
Let run this command:
```sh
kubectl patch ds prometheus-grafana-stack-prometheus-node-exporter -n monitoring --type "json" -p '[{"op": "remove", "path" : "/spec/template/spec/containers/0/volumeMounts/2/mountPropagation"}]'
```

To deploy loki-promtail Helm chart, use the following commands:

```sh
helm -n monitoring install loki -f values-loki.yaml loki-stack
```