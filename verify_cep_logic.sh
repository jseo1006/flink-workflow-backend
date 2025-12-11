#!/bin/bash

# # AWS LoadBalancer URL
AWS_URL="http://a1f00513e182e47728ec716886a17ba0-236213954.ap-northeast-2.elb.amazonaws.com"
AUTH_HEADER="Authorization: Basic dXNlcjpwYXNzd29yZA=="

echo "Testing CEP Logic on AWS at $AWS_URL..."

# # 1. Deploy the Workflow
# echo "1. Deploying SampleCepJob..."
# cat <<EOF > sample_cep_job.json
# {
#     "id": "SampleCepJob",
#     "name": "SampleCepJob",
#     "nodes": [
#         { "id": "source1", "type": "SOURCE", "properties": { "topic": "input-topic" } },
#         { "id": "cep1", "type": "CEP", "properties": {} },
#         { "id": "sink1", "type": "SINK", "properties": { "type": "kafka", "topic": "output-topic" } }
#     ],
#     "edges": [
#         { "source": "source1", "target": "cep1" },
#         { "source": "cep1", "target": "sink1" }
#     ]
# }
# EOF

# RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${AWS_URL}/api/workflows/deploy" \
#      -H "Content-Type: application/json" \
#      -H "${AUTH_HEADER}" \
#      -d @sample_cep_job.json)
# HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
# BODY=$(echo "$RESPONSE" | sed '$d')
# echo "Deploy Status: $HTTP_CODE"
# echo "Response Body: $BODY"

# if [ "$HTTP_CODE" -ne 200 ]; then
#     echo "Deployment failed."
#     exit 1
# fi

# sleep 10 # Wait for job to be ready

# 2. Send a Rule
echo "2. Sending CEP Rule..."
# Rule: value > 100
RULE_JSON='{"field": "value", "operator": ">", "threshold": 100}'

curl -X POST "$AWS_URL/api/cep/rules/SampleCepJob" \
     -H "Content-Type: application/json" \
     -H "$AUTH_HEADER" \
     -d "$RULE_JSON"

# 3. Send Data
echo "3. Sending Data..."
# Should Match
echo "Sending 150 (Should Match)..."
curl -X POST "$AWS_URL/api/data/send?topic=input-topic" \
     -H "Content-Type: application/json" \
     -H "$AUTH_HEADER" \
     -d '{"value": 200}'

sleep 2

# Should Not Match
echo "Sending 50 (Should NOT Match)..."
curl -X POST "$AWS_URL/api/data/send?topic=input-topic" \
     -H "Content-Type: application/json" \
     -H "$AUTH_HEADER" \
     -d '{"value": 50}'

echo -e "\nDone. Check Kafka Output Topic for results."
