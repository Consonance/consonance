#!/bin/bash

for i in `rabbitmqadmin list queues name | grep -v name | awk '{print $2}'`;   do echo $i;   rabbitmqadmin delete queue name="$i";   done;

dropdb queue_status
createdb queue_status
psql -h 127.0.0.1 -U queue_user queue_status < sql/schema.sql

psql -h 127.0.0.1 -U queue_user queue_status -c 'delete from job; delete from provision;'

