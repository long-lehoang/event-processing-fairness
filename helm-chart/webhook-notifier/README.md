# Event Processing Fairness System Helm Chart

This README provides guidelines to install Kubernetes, Ingress Controller, and Helm for deploying the event processing fairness microservices.

## Prerequisites

- Kubernetes cluster
- Helm installed

## Step 1: Install Kubernetes

Follow the official Kubernetes documentation to set up a Kubernetes cluster: [Kubernetes Setup](https://kubernetes.io/docs/setup/)

## Step 2: Install Ingress Controller

Install an NGINX Ingress Controller using kubectl:

```sh
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/cloud/deploy.yaml
```

Verify the installation:

```sh
kubectl get pods -n ingress-nginx -l app.kubernetes.io/name=ingress-nginx
```

You have successfully installed Kubernetes, Ingress Controller, Helm, and ArgoCD. You can now proceed to deploy your microservices using Helm charts.
## Alternative Step 2: Install Ingress Controller on Minikube

If you are using Minikube, you can enable the Ingress addon instead of installing the NGINX Ingress Controller manually.

Enable the Ingress addon:

```sh
minikube addons enable ingress
```

Verify the installation:

```sh
kubectl get pods -n kube-system -l k8s-app=ingress-nginx
```

Start ingress:

```sh
minikube tunnel
```


## Step 3: Install Helm

Follow the official Helm documentation to install Helm: [Helm Installation](https://helm.sh/docs/intro/install/)

```shell
brew install helm
```

## Step 4: Deploy Kafka and Event Processing Components

To deploy the Kafka and event processing fairness components, use the following commands:

```sh
# Create Kafka namespace and deploy Kafka with Strimzi operator
kubectl create namespace kafka
kubectl create -f 'https://strimzi.io/install/latest?namespace=kafka' -n kafka
kubectl apply -f https://strimzi.io/examples/latest/kafka/kraft/kafka-single-node.yaml -n kafka 

# Deploy the webhook-notifier and fairness components
cd helm-chart
helm install webhook ./webhook-notifier -n kafka
```

Verify the deployment:

```sh
kubectl get pods -n kafka
```

## Components Overview

The helm chart includes the following components:

- **Kafka**: Message broker for event streaming
- **Redis**: In-memory data store for caching and message queuing
- **PostgreSQL**: Relational database for persistent storage
- **Producer Service**: Generates and publishes events to Kafka
- **Notifier Service**: Processes events and sends notifications

You have now deployed the event processing fairness system using the Helm chart.