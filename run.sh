#!/bin/sh

cat kubernetes/hogwild.yaml | \
    sed "s/__REPLICAS__/$2/g" | \
    sed "s/__MODE__/$1}/g" | \
    kubectl create -f -