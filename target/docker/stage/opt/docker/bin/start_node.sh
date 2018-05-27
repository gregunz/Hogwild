#!/bin/sh

N=`echo ${MY_POD_NAME} | awk -F- '{print $NF}'`

if [ ${N} == "0" ];then
    IP_PORT=hogwild-pod-1.statefulset-service:${PORT_TO_OPEN}
else
    IP_PORT=hogwild-pod-0.statefulset-service:${PORT_TO_OPEN}
    EARLY_STOPPING=""
    MIN_LOSS=""
    LOSS_INTERVAL=""
    LOSS_INTERVAL_UNIT=""
fi

sh hogwild \
    mode=${MODE} \
    my-ip=${MY_POD_NAME} \
    port=${PORT_TO_OPEN} \
    ip:port=${IP_PORT} \
    data-path=${DATASET_PATH} \
    lambda=${LAMBDA} \
    step-size=${STEP_SIZE} \
    early-stopping=${EARLY_STOPPING} \
    min-loss=${MIN_LOSS} \
    loss-interval=${LOSS_INTERVAL} \
    loss-interval-unit=${LOSS_INTERVAL_UNIT} \
    broadcast-interval=${BROADCAST_INTERVAL} \
    broadcast-interval-unit=${BROADCAST_INTERVAL_UNIT}
