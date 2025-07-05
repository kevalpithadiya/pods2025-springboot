#/bin/bash

# Start minikube instance
minikube start
minikube addons enable metrics-server

# Configure docker client to user minikube docker server
eval $(minikube docker-env)

# Build images
docker build -t keval/users ./users
docker build -t keval/wallets ./wallets
docker build -t keval/marketplace-db ./marketplace-db
docker build -t keval/marketplace ./marketplace

# Deployments and Services

# Install metrics server for Horizontal Pods Autoscaler
minikube kubectl -- apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml

minikube kubectl -- apply -f ./users/users-deployment.yaml
minikube kubectl -- apply -f ./users/users-service.yaml

minikube kubectl -- apply -f ./marketplace-db/marketplace-db-deployment.yaml
minikube kubectl -- apply -f ./marketplace-db/marketplace-db-service.yaml

minikube kubectl -- apply -f ./marketplace/marketplace-deployment.yaml
minikube kubectl -- apply -f ./marketplace/marketplace-service.yaml
minikube kubectl -- apply -f ./marketplace/marketplace-hpa.yaml

minikube kubectl -- apply -f ./wallets/wallets-deployment.yaml
minikube kubectl -- apply -f ./wallets/wallets-service.yaml

echo "Waiting for 10s for services to start before port-forwarding"
sleep 10s
minikube kubectl -- port-forward svc/keval-users-service 8080:8080 &
minikube kubectl -- port-forward svc/keval-marketplace-service 8081:8080 &
minikube kubectl -- port-forward svc/keval-wallets-service 8082:8080 &
