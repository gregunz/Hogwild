#!/bin/sh

kubectl delete service dns-service
kubectl delete service statefulset-service
kubectl delete deployment hogwild-master
kubectl delete statefulset hogwild-slave --cascade=false
kubectl delete pods --all
