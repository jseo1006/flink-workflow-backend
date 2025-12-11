#!/bin/bash

# AWS LoadBalancer URL
AWS_URL="http://a1f00513e182e47728ec716886a17ba0-236213954.ap-northeast-2.elb.amazonaws.com"
AUTH_HEADER="Authorization: Basic dXNlcjpwYXNzd29yZA=="

echo "Testing AWS Deployment at $AWS_URL..."

echo "2. Deploy Workflow (CEP Enabled)..."
# Create sample CEP job payload
cat <<EOF > sample_cep_job.json
{
    "id": "SampleCepJob",
    "name": "SampleCepJob",
    "nodes": [
        { "id": "source1", "type": "SOURCE", "properties": { "topic": "input-topic" } },
        { "id": "cep1", "type": "CEP", "properties": {} },
        { "id": "sink1", "type": "SINK", "properties": { "type": "kafka", "topic": "output-topic" } }
    ],
    "edges": [
        { "source": "source1", "target": "cep1" },
        { "source": "cep1", "target": "sink1" }
    ]
}
EOF

echo "Submitting sample CEP job to AWS EKS..."
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${AWS_URL}/api/workflows/deploy" \
     -H "Content-Type: application/json" \
     -H "${AUTH_HEADER}" \
     -d @sample_cep_job.json)

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -ne 200 ]; then
    echo "Deployment failed with status $HTTP_CODE"
    echo "Response Body: $BODY"
    exit 1
fi

echo "Job SampleCepJob deployed successfully."

echo "3. Sending Sample Data..."
# Send JSON data
curl -X POST "$AWS_URL/api/data/send?topic=input-topic" \
     -H "Content-Type: application/json" \
     -H "$AUTH_HEADER" \
     -d '{"value": 150}'

echo -e "\n\nDone."
