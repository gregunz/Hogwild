#!/bin/sh

MODE=$1
REPLICAS=$2

cat kubernetes/hogwild.yaml | \
    sed "s/__REPLICAS__/"${REPLICAS}"/g" | \
    sed "s/__MODE__/"${MODE}"/g" | \
    kubectl create -f -