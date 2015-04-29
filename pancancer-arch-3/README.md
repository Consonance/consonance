# PanCancer Architecture 3.0 Prototype

## About

The idea behind this architecture is described in more detail elsewhere but the basic idea is a lightweight
framework to run Docker containers for the PanCancer project.

## Building

Just a Java maven project so do the following:

    mvn clean install

## Dependencies

Eventually we will want everything in a single (or series) of Docker containers. This
will make it much easy to redistribute but, for the time being, these can be installed
on your development host directly.

I'm focused on development on a Mac using HomeBrew, you will need to setup
the dependencies using whatever system is appropriate for your environment.

### Ubuntu

For RabbitMQ see: https://www.rabbitmq.com/install-debian.html

You need to setup the management plugin: https://www.rabbitmq.com/management.html

You will also need `/usr/local/sbin/rabbitmqadmin` installed, see https://www.rabbitmq.com/management-cli.html

    wget -O - -q http://localhost:15672/cli/rabbitmqadmin > /usr/local/sbin/rabbitmqadmin

For Postgres see:  https://www.digitalocean.com/community/tutorials/how-to-install-and-use-postgresql-on-ubuntu-14-04

### Log4J + Logstash

I'm trying to follow this guide for using Log4J so I can easily incorprate with LogStash in the future: [guide](https://blog.dylants.com/2013/08/27/java-logging-creating-indexing-monitoring/).

### RabbitMQ

See [install guide](https://www.rabbitmq.com/install-homebrew.html)

Basically you do:

    brew update
    brew install rabbitmq
    /usr/local/sbin/rabbitmq-server

And at that point the service is running.

### PostgreSQL

Install with Homebrew

    brew install postgresql

Now launch it:

    postgres -D /usr/local/var/postgres

Now create a user:

    # using 'queue' as the password by default
    boconnor@odm-boconnor ~$ createuser -P -s -e queue_user
    Enter password for new role:
    Enter it again:
    CREATE ROLE queue PASSWORD 'md5f8ceabb22d9297bd28382151f35a2252' SUPERUSER CREATEDB CREATEROLE INHERIT LOGIN;

Now create a DB:

    createdb queue_status

Setup a schema for the DB:

    psql -h 127.0.0.1 -U queue_user -W queue_status < sql/schema.sql

Connect to the DB if you need to:

    psql -h 127.0.0.1 -U queue_user -W queue_status

Delete the contents if you want to reset:

    delete from job; delete from provision;

Drop the DB if you need to clear it out:

    dropdb queue_status
    createdb queue_status

## Testing Locally

The following will let you test on a local box. This simulates a multiple machine/VM
setup on a single box just using Java and RabbitMQ.  Eventually, this will just
be one of multiple possible backends configured by the settings file. This single-machine,
pure Java running example will be used for integration and other tests.

### Job Generator

This generates job orders, 5 in this case. If you leave off the `--total-jobs` option it will submit jobs on an infinite loop.

    java -cp target/pancancer-arch-3-1.0.0-SNAPSHOT.jar info.pancancer.arch3.jobGenerator.JobGenerator --config conf/config.json --total-jobs 5

### Coordinator

This consumes the jobs and prepares messages for the VM and Job Queues.

It then monitors the results queue to see when jobs fail or finish.

Finally, for failed or finished workflows, it informs the Container provisioner about finished
VMs that can be terminated.

    java -cp target/pancancer-arch-3-1.0.0-SNAPSHOT.jar info.pancancer.arch3.coordinator.Coordinator --config conf/config.json

### Container Provisioner

This will spin up (fake) containers that will launch Workers.

    java -cp target/pancancer-arch-3-1.0.0-SNAPSHOT.jar info.pancancer.arch3.containerProvisioner.ContainerProvisionerThreads --config conf/config.json

### Worker

    java -cp target/pancancer-arch-3-1.0.0-SNAPSHOT.jar info.pancancer.arch3.worker.Worker --config conf/config.json

### Checking Results

Log into the DB and do:

    queue_status=# select * from provision; select job_id, status, job_uuid, provision_uuid, job_hash from job;

## Testing on AWS

WORK IN PROGRESS

In this test I will create a single node for running this framework and associated daemons and a single worker node that actually runs the worker thread and performs some docker workflow run.

### Job Generator

This generates actual job orders using INI files provided by Adam's centralized decider and some command line options.

The first step is to use Adam's command line tool to generate one or more INI files.  See https://github.com/ICGC-TCGA-PanCancer/central-decider-client for details on how to use this.  It's not difficult but you need to follow these steps to have INI files for the next step below.

Now that you have INI files, the next step is to run this command line tool.  It will parse the INI files and generate a job with them and other information it takes from the command line.  It then submits the job "order" to the order message queue.

    java -cp target/pancancer-arch-3-1.0.0-SNAPSHOT.jar info.pancancer.arch3.jobGenerator.JobGeneratorDEWorkflow --config conf/config.json --ini-dir <directories_with_ini_files> --workflow-name <workflow_name> --workflow-version <workflow_version> --workflow-path <workflow_path> 
    
    # for example:
    java -cp target/pancancer-arch-3-1.0.0-SNAPSHOT.jar info.pancancer.arch3.jobGenerator.JobGeneratorDEWorkflow --config conf/config.json --ini-dir ini --workflow-name DEWrapper --workflow-version 1.0.0 --workflow-path /workflow/Workflow_Bundle_DEWrapperWorkflow_1.0.0_SeqWare_1.1.0
    # alternatively for hello world
    java -cp target/pancancer-arch-3-1.0.0-SNAPSHOT.jar info.pancancer.arch3.jobGenerator.JobGeneratorDEWorkflow --config conf/config.json --ini-dir /home/ubuntu/gitroot/central-decider-client/ini --workflow-name HelloWorld --workflow-version 1.0-SNAPSHOT --workflow-path /workflow/Workflow_Bundle_HelloWorld_1.0-SNAPSHOT_SeqWare_1.1.0
    

### Coordinator

This consumes the jobs and prepares messages for the VM and Job Queues.

It then monitors the results queue to see when jobs fail or finish.

Finally, for failed or finished workflows, it informs the Container provisioner about finished
VMs that can be terminated.

    java -cp target/pancancer-arch-3-1.0.0-SNAPSHOT.jar info.pancancer.arch3.coordinator.Coordinator --config conf/config.json

### Container Provisioner

This will spin up (fake) containers that will launch Workers.

    java -cp target/pancancer-arch-3-1.0.0-SNAPSHOT.jar info.pancancer.arch3.containerProvisioner.ContainerProvisionerThreads --config conf/config.json

### Worker

LEFT OFF WITH: need to test standalone, multiple workers

    java -cp target/pancancer-arch-3-1.0.0-SNAPSHOT.jar info.pancancer.arch3.worker.Worker --config conf/config.json

### Checking Results

Log into the DB and do:

    queue_status=# select * from provision; select job_id, status, job_uuid, provision_uuid, job_hash from job;



## Cleanup

To cleanup and delete all queues and DB tables:

    bash scripts/cleanup.sh

You can use this in your testing to reset the system but keep in mind the danger of using this in production systems where you want to save your DB.  Backup accordingly!

## Diagrams

![Alt text](img/arch.png)
![Alt text](img/error.png)
![Alt text](img/flow.png)
![Alt text](img/state.png)

## TODO

### Soon

* implement heartbeat
    * stderr/stdout
* try to model complex/non-standard events in the standalone daemons
    * job fails, 20% of the time
    * vm disappears... need to update the DB then re-enqueue the VM/Job request
    * longer-running jobs... longer than 10s
* Solomon wants a "workflow_path" added to the order
* figure out impl/extends class strategy for the various components so they can be  swapped out with different implementations -- TODO, Solomon?
    * worker threads
    * workers that fail, are successful, etc
    * flesh out worker to run docker and provide heartbeat, resources, etc
* finalize the message format between the layers, serializers -- DONE
* pick a storage mechanism for state used by the VMProvisioner and Coordinator -- DONE
* lifecycle of jobs -- DONE
    * enqueue, monitor, launch VMs, status, etc
    * see diagram
* need to add
    * error checking
    * improve logging
    * cleanup of messaging and DB handles
    * reporting tool that shows a summary of the DB contents including Donor/Project

### Future

* utilities for clearing the status persistence storage and the message queues if you need to "start over"
* really great logging/reporting that's human readable
* ability to turn off the VMProvisioner in case a human makes the worker nodes
* log files loaded into the ELK stack for visualization
* Docker container for the system, integration with Architecture Setup 3.0
* need job queues with different names based on the workflow and version they target, this will make it easier to run multiple workflow types at the same time
