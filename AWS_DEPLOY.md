# Deploying Flink Workflow Backend to AWS

This guide outlines how to build, push, and deploy the **API Gateway** and **Flink Manager** to AWS using Docker Containers.

## Prerequisites
- **AWS Account** with permissions for ECR, ECS, or App Runner.
- **AWS CLI** installed and configured (`aws configure`).
- **Docker** installed and running.

## 1. Build Docker Images Locally

First, verify that you can build the images locally.

```bash
docker compose build
```

To run locally for testing:
```bash
docker compose up
```

## 2. Push to AWS ECR (Elastic Container Registry)

You need to create repositories for your images in AWS ECR.

1.  **Create Repositories** (if they don't exist):
    ```bash
    aws ecr create-repository --repository-name flink-workflow/api-gateway
    aws ecr create-repository --repository-name flink-workflow/flink-manager
    ```

2.  **Authenticate Docker to ECR**:
    Replace `your-region` and `your-account-id` with your actual values.
    ```bash
    aws ecr get-login-password --region your-region | docker login --username AWS --password-stdin your-account-id.dkr.ecr.your-region.amazonaws.com
    ```

3.  **Tag and Push Images**:
    ```bash
    # Tag API Gateway
    docker tag flink-workflow-backend-api-gateway:latest your-account-id.dkr.ecr.your-region.amazonaws.com/flink-workflow/api-gateway:latest
    # Push API Gateway
    docker push your-account-id.dkr.ecr.your-region.amazonaws.com/flink-workflow/api-gateway:latest

    # Tag Flink Manager
    docker tag flink-workflow-backend-flink-manager:latest your-account-id.dkr.ecr.your-region.amazonaws.com/flink-workflow/flink-manager:latest
    # Push Flink Manager
    docker push your-account-id.dkr.ecr.your-region.amazonaws.com/flink-workflow/flink-manager:latest
    ```

## 3. Deploy to AWS

### Option A: AWS App Runner (Recommended for API)
AWS App Runner is the easiest way to run containerized web applications.
1.  Go to the [App Runner Console](https://console.aws.amazon.com/apprunner).
2.  **Create Service**.
3.  Source: **Container Registry**.
4.  Provider: **Amazon ECR**.
5.  Browse to select your `flink-workflow/api-gateway` image.
6.  **Configuration**:
    - Port: `8080`
    - Environment Variables:
        - `FLINK_MANAGER_URL`: URL of the Flink Manager service (see below).
7.  Repeat for `flink-manager` (Port `8081`).

### Option B: AWS ECS (Elastic Container Service)
For more control or inter-service communication within a VPC.
1.  Create an **ECS Cluster** (Fargate is recommended).
2.  Create **Task Definitions** for both services, pointing to the ECR images.
3.  Create **Services** from these Task Definitions.
    - Ensure they share a Security Group that allows traffic on ports 8080/8081.
    - Use **Service Discovery** or an **Internal Load Balancer** so API Gateway can reach Flink Manager.

## 4. Configuration
Ensure your `application.yml` or environment variables in AWS match your production settings, especially for:
- `AWS_REGION`
- Database connections (if using RDS)
- Kinesis/S3 bucket names.
