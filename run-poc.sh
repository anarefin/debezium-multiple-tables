#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Debezium Multiple Tables POC${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Step 1: Start Docker services
echo -e "${YELLOW}Step 1: Starting Docker services...${NC}"
docker-compose up -d

if [ $? -ne 0 ]; then
    echo -e "${RED}Failed to start Docker services${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Docker services started${NC}"
echo ""

# Step 2: Wait for services to be healthy
echo -e "${YELLOW}Step 2: Waiting for services to be healthy (this may take 30-60 seconds)...${NC}"
sleep 10

max_attempts=30
attempt=0

while [ $attempt -lt $max_attempts ]; do
    postgres_health=$(docker inspect --format='{{.State.Health.Status}}' postgres 2>/dev/null)
    kafka_health=$(docker inspect --format='{{.State.Health.Status}}' kafka 2>/dev/null)
    connect_health=$(docker inspect --format='{{.State.Health.Status}}' kafka-connect 2>/dev/null)
    
    if [ "$postgres_health" = "healthy" ] && [ "$kafka_health" = "healthy" ] && [ "$connect_health" = "healthy" ]; then
        echo -e "${GREEN}✓ All services are healthy${NC}"
        break
    fi
    
    echo "Waiting... (attempt $((attempt+1))/$max_attempts) - Postgres: $postgres_health, Kafka: $kafka_health, Connect: $connect_health"
    sleep 3
    attempt=$((attempt+1))
done

if [ $attempt -eq $max_attempts ]; then
    echo -e "${RED}Services did not become healthy in time. Check logs with 'docker-compose logs'${NC}"
    exit 1
fi

echo ""

# Step 3: Register Debezium connector
echo -e "${YELLOW}Step 3: Registering Debezium connector...${NC}"
chmod +x debezium-connector/register-connectors.sh
./debezium-connector/register-connectors.sh

if [ $? -ne 0 ]; then
    echo -e "${RED}Failed to register Debezium connector${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Debezium connector registered${NC}"
echo ""

# Step 4: Instructions
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}Setup Complete!${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "Next steps:"
echo -e "  1. Run the Spring Boot application:"
echo -e "     ${GREEN}./gradlew bootRun${NC}"
echo ""
echo -e "  2. In another terminal, test by inserting data:"
echo -e "     ${GREEN}docker exec -it postgres psql -U postgres -d debezium_db${NC}"
echo -e "     Then run:"
echo -e "     ${GREEN}INSERT INTO customers (name, email, created_at) VALUES ('Test User', 'test@example.com', NOW());${NC}"
echo ""
echo -e "  3. Watch the Spring Boot console for change events!"
echo ""
echo -e "Useful commands:"
echo -e "  - View connector status: ${GREEN}curl http://localhost:8083/connectors/postgres-connector/status | jq .${NC}"
echo -e "  - View Kafka topics: ${GREEN}docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 --list${NC}"
echo -e "  - Stop services: ${GREEN}docker-compose down${NC}"
echo ""
