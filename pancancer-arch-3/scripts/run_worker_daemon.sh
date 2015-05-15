#! /bin/bash

LOG_FILE=/var/log/arch3_worker.log
PID_FILE=/var/run/arch3_worker.pid

if [ -f $PID_FILE ]; then
  echo "Existing PID file found at $PID_FILE"
  echo "Maybe kill the Worker daemon (sudo bash kill_worker_daemon.sh) before trying to start it?"
  exit 0
fi

#Note: -Dpidfile is not a standard java option; it is custom, for the Worker.
nohup java -Dpidfile=$PID_FILE -cp pancancer-arch-3-1.0.0-SNAPSHOT.jar info.pancancer.arch3.worker.Worker --config workerConfig.json --uuid 50f20496-c221-4c25-b09b-839511e76df4 </dev/null > $LOG_FILE 2>&1 &
PID=$!
echo $PID > $PID_FILE
echo "PID of worker daemon is $PID"

