#!/usr/bin/env bash
cd ~/Repositories/kafka_2.12-2.3.0/ || exit

bin/kafka-console-consumer.sh \
    --bootstrap-server localhost:9092 \
    --topic 'dc559691-cfeb-4606-8697-c5b3e9949c28'