#!/bin/bash

ELB_URL="http://a1f00513e182e47728ec716886a17ba0-236213954.ap-northeast-2.elb.amazonaws.com"
AUTH_HEADER="Authorization: Basic dXNlcjpwYXNzd29yZA=="
TOPIC="input-topic"

echo "Starting load generation to $ELB_URL..."
echo "Press [CTRL+C] to stop."

while true; do
  # Send random value between 50 and 200
  VALUE=$((50 + $RANDOM % 151))
  
  # Construct JSON payload
  PAYLOAD="{\"value\": $VALUE}"
  
  response=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$ELB_URL/api/data/send?topic=$TOPIC" \
       -H "Content-Type: application/json" \
       -H "$AUTH_HEADER" \
       -d "$PAYLOAD")
       
  if [ "$response" -eq 200 ]; then
     echo "Sent: $PAYLOAD (Status: $response)"
  else
     echo "Failed to send: $PAYLOAD (Status: $response)"
  fi
  
  # Sleep 0.5s to generate ~120 records/min
  sleep 0.5
done
