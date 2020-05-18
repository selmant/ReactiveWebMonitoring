#!/usr/bin/env bash
cd ~/IdeaProjects/ReactiveWebMonitoring/ || exit

sbt "runMain akka.Main tr.edu.ege.Main"