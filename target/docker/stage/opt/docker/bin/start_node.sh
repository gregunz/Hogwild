#!/bin/sh

n=`echo $MY_POD_NAME | awk -F- '{print $NF}'`
first_ip="hogwild-0"
early_stop=""
min-loss=""
ip_port=hogwild-pod-0.statefulset-service:${PORT_TO_OPEN}

if [ "$n" == "0" ];then
  ip_port=hogwild-pod-1.statefulset-service:${PORT_TO_OPEN}
  early_stop=${EARLY_STOPPING}
  loss=${EARLY_STOPPING}
fi

sh hogwild \
    mode=${MODE} \
    port=${PORT_TO_OPEN} \
    interval=${INTERVAL} \
    ip:port=${ip_port} \
    data-path=${DATASET_PATH} \
    lambda=${LAMBDA} \
    step-size=${STEP_SIZE} \
    early-stopping=early_stop \
    min-loss=loss
