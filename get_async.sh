#!/bin/sh

cat async2.yaml | \
sed 's/999999'"/$1/g" | \
kubectl create -f - && \
echo "Setup in place for $1 pods" && sleep 5 && \
kubectl logs hogwild-pod-0 hogwild -f > async_$1.log && \
echo "Calculation finished for $1 pods" && sleep 5 && \
sh delete_pods.sh && \
echo "Deletion finished for $1 pods" && sleep 5
