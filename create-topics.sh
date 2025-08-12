#!/bin/bash

echo "Creating Kafka topics..."

kafka-topics --create --if-not-exists --topic mail-topic --bootstrap-server kafka:9092 --partitions 1 --replication-factor 1
echo "mail-topic created/verified"

kafka-topics --create --if-not-exists --topic user-topic --bootstrap-server kafka:9092 --partitions 1 --replication-factor 1
echo "user-topic created/verified"

kafka-topics --create --if-not-exists --topic notification-topic --bootstrap-server kafka:9092 --partitions 1 --replication-factor 1
echo "notification-topic created/verified"

echo "All topics created successfully!"
kafka-topics --list --bootstrap-server kafka:9092
