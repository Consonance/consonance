#!/bin/bash

sudo rabbitmq-plugins enable rabbitmq_management
sudo wget -O - -q http://localhost:15672/cli/rabbitmqadmin > /usr/local/sbin/rabbitmqadmin
sudo chmod a+x /usr/local/sbin/rabbitmqadmin
for i in `/usr/local/sbin/rabbitmqadmin list queues name | grep -v name | awk '{print $2}'`;   do echo $i;   /usr/local/sbin/rabbitmqadmin delete queue name="$i";   done;

sudo -u postgres dropdb queue_status
sudo -u postgres createdb queue_status
psql -h 127.0.0.1 -U queue_user queue_status < sql/schema.sql

psql -h 127.0.0.1 -U queue_user queue_status -c 'delete from job; delete from provision;'

