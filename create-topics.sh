#!/bin/bash

echo "Waiting for Kafka to be ready..."
sleep 30

echo "Creating Kafka topics..."

# Tạo topic cho notifications
kafka-topics.sh --create \
    --bootstrap-server kafka:9092 \
    --topic notifications \
    --partitions 3 \
    --replication-factor 1 \
    --config retention.ms=604800000 \
    --config cleanup.policy=delete

# Tạo topic cho mail
kafka-topics.sh --create \
    --bootstrap-server kafka:9092 \
    --topic mail-topic \
    --partitions 3 \
    --replication-factor 1 \
    --config retention.ms=604800000 \
    --config cleanup.policy=delete

# Tạo topic cho chat
kafka-topics.sh --create \
    --bootstrap-server kafka:9092 \
    --topic chat-messages \
    --partitions 3 \
    --replication-factor 1 \
    --config retention.ms=604800000 \
    --config cleanup.policy=delete

# Tạo Dead Letter Topics
kafka-topics.sh --create \
    --bootstrap-server kafka:9092 \
    --topic notifications.DLT \
    --partitions 3 \
    --replication-factor 1 \
    --config retention.ms=2592000000 \
    --config cleanup.policy=delete

kafka-topics.sh --create \
    --bootstrap-server kafka:9092 \
    --topic mail-topic.DLT \
    --partitions 3 \
    --replication-factor 1 \
    --config retention.ms=2592000000 \
    --config cleanup.policy=delete

echo "Topics created successfully!"
echo "Listing all topics:"
kafka-topics.sh --list --bootstrap-server kafka:9092

echo "Topic details:"
kafka-topics.sh --describe --bootstrap-server kafka:9092
