#! /bin/bash

LOG_FILE=/var/log/arch3_worker.log
PID_FILE=/var/run/arch3_worker.pid
USER=ubuntu

cd /home/$USER/

if [ -f $PID_FILE ]; then
  echo "Existing PID file found at $PID_FILE"
  echo "Maybe kill the Worker daemon (sudo bash kill_worker_daemon.sh) before trying to start it?"
  exit 0
fi
set -e
sudo touch $PID_FILE 
sudo chown $USER $PID_FILE
# the worker now detects its own id from openstack or AWS metadata (cloud-init)
sudo -u $USER nohup java -cp pancancer-arch-3-*.jar info.pancancer.arch3.worker.Worker --config workerConfig.ini --pidFile $PID_FILE  </dev/null > $LOG_FILE 2>&1 &
PID=$!
echo $PID > $PID_FILE
echo "PID of worker daemon is $PID"
