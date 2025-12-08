#!/bin/bash

# Sample Workflow JSON
PAYLOAD=$(cat sample_job.json)

# Encode basic auth (user:password -> dXNlcjpwYXNzd29yZA==)
AUTH_HEADER="Authorization: Basic dXNlcjpwYXNzd29yZA=="

echo "Submitting sample job..."
curl -X POST http://localhost:8080/api/workflows/deploy \
     -H "Content-Type: application/json" \
     -H "$AUTH_HEADER" \
     -d "$PAYLOAD"

echo -e "\n\nNote: This request requires AWS credentials to be configured in flink-manager for full deployment."
