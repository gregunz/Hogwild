#!/bin/sh

kubectl delete service hogwild
kubectl delete statefulset hogwild --cascade=false
kubectl delete pods --all
