#!/usr/bin/env bash

docker-compose -f docker_poet.yaml down

sleep 5

docker-compose -f docker_poet.yaml up --build