#!/bin/sh

n=`echo $MY_POD_NAME | awk -F- '{print $NF}'`
first_ip="hogwild-0"

ip_port=hogwild-pod-0.statefulset-service:${PORT_TO_OPEN}

if [ "$n" == "0" ];then
  ip_port=""
fi

sh hogwild \
    mode=${MODE} \
    port=${PORT_TO_OPEN} \
    interval=${INTERVAL} \
    worker-ip:worker-port=${ip_port} \
    data-path=${DATASET_PATH} \
    samples=${SAMPLES} \
    lambda=${LAMBDA} \
    step-size=${STEP_SIZE}
