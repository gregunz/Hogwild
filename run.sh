#!/usr/bin/env bash

if [ ! "$1" ]; then
	echo "Please give a mode as the first parameter"
	exit 1
fi
if [ ! "$2" ]; then
	echo "Please give a number of workers as the second parameter"
	exit 1
fi

docker rm coordinator
docker rm worker

# Run a coordinator and a worker node
docker run -t -i -d --name coordinator -e mode=$1 -e type="coord" hogwild
docker run -t -i -d --name worker -e mode=$1 -e type="worker" hogwild


#for i in `seq 1 $2`;
#do
#	docker run -t -i -d --name worker -e mode=$1 -e type="worker" hogwild
#done

xterm -title "Coordinator" -e docker attach coordinator
xterm -title "Worker" -e docker attach worker
