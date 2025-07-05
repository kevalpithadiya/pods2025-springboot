#/bin/bash

# Kill port forwarding processes
pkill -f "minikube kubectl -- port-forward svc/keval"
pkill -f "kubectl port-forward svc/keval"

# Delete Kubernetes deployments and services
minikube kubectl -- delete service keval-users-service --ignore-not-found=true
minikube kubectl -- delete deployment keval-users-deployment --ignore-not-found=true

minikube kubectl -- delete service keval-marketplace-service --ignore-not-found=true
minikube kubectl -- delete deployment keval-marketplace-deployment --ignore-not-found=true

minikube kubectl -- delete service keval-wallets-service --ignore-not-found=true
minikube kubectl -- delete deployment keval-wallets-deployment --ignore-not-found=true

minikube kubectl -- delete service keval-marketplace-db-service --ignore-not-found=true
minikube kubectl -- delete deployment keval-marketplace-db-deployment --ignore-not-found=true

# Delete docker images
# docker image rm keval/users keval/marketplace-db keval/marketplace keval/wallets

# Terminate minikube instance
minikube stop

