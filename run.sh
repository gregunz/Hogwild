#!/bin/sh

MODE=${1:-async}
REPLICAS=${2:-10}

cat kubernetes/hogwild.yaml | \
    sed "s/__MODE__/"${MODE}"/g" | \
    sed "s/__REPLICAS__/"${REPLICAS}"/g" | \
    sed "s/__LOG__/"${REPLICAS}"/g" | \
    kubectl create -f -