#!/bin/sh

N=`echo ${MY_POD_NAME} | awk -F- '{print $NF}'`

if [ ${N} == "0" ];then
    IP_PORT=hogwild-pod-1.statefulset-service:${PORT_TO_OPEN}
else
    IP_PORT=hogwild-pod-0.statefulset-service:${PORT_TO_OPEN}
    EARLY_STOPPING=""
    MIN_LOSS=""
    LOSS_INTERVAL=""
    LOSS_INTERVAL_IN_SECOND=""
fi

sh hogwild \
    mode=${MODE} \
    port=${PORT_TO_OPEN} \
    ip:port=${IP_PORT} \
    data-path=${DATASET_PATH} \
    lambda=${LAMBDA} \
    step-size=${STEP_SIZE} \
    early-stopping=${EARLY_STOPPING} \
    min-loss=${MIN_LOSS} \
    loss-interval=${LOSS_INTERVAL} \
    loss-interval-in-second=${LOSS_INTERVAL_IN_SECOND} \
    broadcast-interval=${BROADCAST_INTERVAL} \
    broadcast-interval-in-second=${BROADCAST_INTERVAL_IN_SECOND}

