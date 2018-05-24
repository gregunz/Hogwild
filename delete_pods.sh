#!/bin/sh

kubectl delete service statefulset-service
kubectl delete statefulset hogwild-pod --cascade=false
kubectl delete pods --all
