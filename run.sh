#!/bin/sh

MODE=${1:-async}
REPLICAS=${2:-10}
LOG=${3:-2}

cat kubernetes/hogwild.yaml | \
    sed "s/__MODE__/"${MODE}"/g" | \
    sed "s/__REPLICAS__/"${REPLICAS}"/g" | \
    sed "s/__LOG__/"${LOG}"/g" | \
    kubectl create -f -