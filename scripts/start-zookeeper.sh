#!/usr/bin/env bash
cd ~/Repositories/kafka_2.12-2.3.0/ || exit

bin/zookeeper-server-start.sh config/zookeeper.properties