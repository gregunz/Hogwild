#!/bin/bash

interval=$INTERVAL

port=$PORT_TO_OPEN


n=`echo $1 | awk -F- '{print $NF}'`
last_number=`echo $2 | cut -d . -f 4`
difference=$((last_number-n))
first_ip=`echo $2 | cut -d"." -f1-3`.$difference

ip_port=${first_ip}:${port}

if [ "$n" == "0" ];then
  ip_port=""
fi

sbt "run mode=${3}
port=${port}
interval=${interval}
worker-ip:worker-port=${ip_port}
data-path=${DATASET_PATH}"
