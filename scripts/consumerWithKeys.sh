#!/usr/bin/env bash
cd ~/Repositories/kafka_2.12-2.3.0/ || exit

bin/kafka-console-consumer.sh \
    --bootstrap-server localhost:9092 \
    --topic 'd0ea8228-b03d-4dc6-9e41-aaa9061536ff' \
    --from-beginning \
    --property print.key=true \
    --property key.separator=: