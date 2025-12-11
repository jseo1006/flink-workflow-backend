#!/bin/bash
set -e

# Configuration
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
REGION="ap-northeast-2"
CLUSTER_NAME="flink-backend-cluster"
ECR_URL="${AWS_ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com"

echo "Deploying to AWS Account: $AWS_ACCOUNT_ID, Region: $REGION"

# 1. Create ECR Repositories
echo "1. Checking/Creating ECR Repositories..."
aws ecr describe-repositories --repository-names flink-manager || aws ecr create-repository --repository-name flink-manager
aws ecr describe-repositories --repository-names api-gateway || aws ecr create-repository --repository-name api-gateway

# 2. Login to ECR
echo "2. Logging in to ECR..."
aws ecr get-login-password --region $REGION | docker login --username AWS --password-stdin $ECR_URL

# 3. Build and Push Docker Images
echo "3. Building and Pushing Docker Images..."

# Flink Manager
echo "Building flink-manager..."
docker build --platform linux/amd64 -t flink-manager -f flink-manager/Dockerfile .
docker tag flink-manager:latest $ECR_URL/flink-manager:latest
docker push $ECR_URL/flink-manager:latest

# API Gateway
echo "Building api-gateway..."
docker build --platform linux/amd64 -t api-gateway -f api-gateway/Dockerfile .
docker tag api-gateway:latest $ECR_URL/api-gateway:latest
docker push $ECR_URL/api-gateway:latest

# 4. Create EKS Cluster (This takes time!)
echo "4. Checking/Creating EKS Cluster..."
if eksctl get cluster --name $CLUSTER_NAME --region $REGION; then
    echo "Cluster $CLUSTER_NAME already exists."
else
    echo "Creating cluster $CLUSTER_NAME (This may take 15-20 minutes)..."
    eksctl create cluster --name $CLUSTER_NAME --version 1.29 --region $REGION --nodes 2 --node-type t3.medium --managed
fi

# 5. Configure Kubectl
echo "5. Updating kubeconfig..."
aws eks update-kubeconfig --region $REGION --name $CLUSTER_NAME

# 6. Apply Kubernetes Manifests
echo "6. Deploying Manifests..."

# Replace placeholders in manifests
sed "s/<AWS_ACCOUNT_ID>/$AWS_ACCOUNT_ID/g; s/<REGION>/$REGION/g" kubernetes/flink-manager.yaml > kubernetes/flink-manager-gen.yaml
sed "s/<AWS_ACCOUNT_ID>/$AWS_ACCOUNT_ID/g; s/<REGION>/$REGION/g" kubernetes/api-gateway.yaml > kubernetes/api-gateway-gen.yaml

kubectl apply -f kubernetes/postgres.yaml
kubectl apply -f kubernetes/kafka.yaml
kubectl apply -f kubernetes/flink-manager-gen.yaml
kubectl apply -f kubernetes/api-gateway-gen.yaml

echo "Deployment Complete!"
echo "Get Service URL with: kubectl get svc -n flink-backend"
