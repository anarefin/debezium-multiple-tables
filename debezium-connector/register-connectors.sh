#!/bin/bash

echo "Waiting for Kafka Connect to be ready..."
until curl -s -o /dev/null -w "%{http_code}" http://localhost:8083/ | grep -q "200"; do
  echo "Kafka Connect is not ready yet. Waiting 5 seconds..."
  sleep 5
done

echo "Kafka Connect is ready!"
echo ""

echo "Registering Debezium PostgreSQL Connector..."
curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" \
  http://localhost:8083/connectors/ -d '{
    "name": "postgres-connector",
    "config": {
      "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
      "database.hostname": "postgres",
      "database.port": "5432",
      "database.user": "postgres",
      "database.password": "postgres",
      "database.dbname": "debezium_db",
      "database.server.name": "dbserver1",
      "table.include.list": "public.customers,public.orders",
      "plugin.name": "pgoutput",
      "slot.name": "debezium",
      "publication.name": "dbz_publication",
      "publication.autocreate.mode": "filtered",
      "topic.prefix": "dbserver1",
      "key.converter": "org.apache.kafka.connect.json.JsonConverter",
      "value.converter": "org.apache.kafka.connect.json.JsonConverter",
      "key.converter.schemas.enable": "false",
      "value.converter.schemas.enable": "false",
      "transforms": "unwrap",
      "transforms.unwrap.type": "io.debezium.transforms.ExtractNewRecordState",
      "transforms.unwrap.drop.tombstones": "false",
      "transforms.unwrap.delete.handling.mode": "rewrite",
      "transforms.unwrap.add.fields": "op,db,table,ts_ms",
      "snapshot.mode": "initial",
      "max.queue.size": "100000",
      "max.batch.size": "10000",
      "poll.interval.ms": "100",
      "errors.tolerance": "none",
      "errors.log.enable": "true",
      "decimal.handling.mode": "string"
    }
  }'

echo ""
echo ""
echo "Checking connector status..."
sleep 3
curl -s http://localhost:8083/connectors/postgres-connector/status | jq .

echo ""
echo "List of all connectors:"
curl -s http://localhost:8083/connectors | jq .

echo ""
echo "Connector registration complete!"
echo ""
echo "Topics should be created automatically:"
echo "  - dbserver1.public.customers"
echo "  - dbserver1.public.orders"
