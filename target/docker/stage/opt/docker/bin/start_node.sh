#!/bin/sh

n=`echo $MY_POD_NAME | awk -F- '{print $NF}'`
last_number=`echo $MY_POD_IP | cut -d . -f 4`
difference=$((last_number-n))
first_ip=`echo $MY_POD_IP | cut -d"." -f1-3`.$difference

ip_port=${first_ip}:${PORT_TO_OPEN}

if [ "$n" == "0" ];then
  ip_port=""
fi

sh /opt/docker/bin/hogwild \
    mode=${MODE} \
    port=${PORT_TO_OPEN} \
    interval=${INTERVAL} \
    worker-ip:worker-port=${ip_port} \
    data-path=${DATASET_PATH}