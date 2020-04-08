#!/usr/bin/env bash
cd ~/Repositories/akka-sample-sharding-scala/ || exit

sbt "runMain akka.Main sample.sharding.Main"