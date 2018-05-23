#!/bin/sh

#n=`echo $MY_POD_NAME | awk -F- '{print $NF}'`
#last_number=`echo $MY_POD_IP | cut -d . -f 4`
#difference=$((last_number-n))
#first_ip=`echo $MY_POD_IP | cut -d"." -f1-3`.$difference

ip_port=dns-service:${PORT_TO_OPEN}

if [ "$ROLE" == "master" ];then
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
