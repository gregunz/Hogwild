#!/bin/bash

#This will be set elsewhere later on
interval=500
#This will be set elsewhere later on
port=80


n=`echo $1 | awk -F- '{print $NF}'`
last_number=`echo $2 | cut -d . -f 4`
difference=$((last_number-n))
first_ip=`echo $2 | cut -d"." -f1-3`.$difference

if [$n -eq 0 ];then
  if [$MODE -eq "async" ];then
    sbt "run async ${port} ${interval}"
  else
    sbt "run sync coord ${port}"
  fi
else
  if [$MODE -eq "async" ];then
    sbt "run async ${port} ${first_ip} ${port} ${interval}"
  else
    sbt "run sync worker ${first_ip} ${port}"
  fi
fi
