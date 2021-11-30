#!/usr/bin/env bash

docker-compose -f docker_pbf.yaml down

sleep 5

docker-compose -f docker_pbf.yaml up --build