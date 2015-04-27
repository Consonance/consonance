#!/bin/bash

for i in `/usr/local/sbin/rabbitmqadmin list queues name | grep -v name | awk '{print $2}'`;   do echo $i;   /usr/local/sbin/rabbitmqadmin delete queue name="$i";   done;

psql -h 127.0.0.1 -U queue_user queue_status -c 'delete from job; delete from provision;'

