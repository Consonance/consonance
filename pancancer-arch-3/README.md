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

### Log4J + Logstash

I'm trying to follow this guide for using Log4J so I can easily incorprate with LogStash in the future: [guide](https://blog.dylants.com/2013/08/27/java-logging-creating-indexing-monitoring/).

### RabbitMQ

See [install guide](https://www.rabbitmq.com/install-homebrew.html)

Basically you do:

    brew update
    brew install rabbitmq
    /usr/local/sbin/rabbitmq-server

And at that point the service is running.

###SQLlite

Used to store state for the Coordinator and the VM Provisioner.

* http://www.tutorialspoint.com/sqlite/sqlite_java.htm
* https://bitbucket.org/xerial/sqlite-jdbc

## Testing

The following will let you test on a local box. This simulates a multiple machine/VM
setup on a single box just using Java and RabbitMQ.  Eventually, this will just
be one of multiple possible backends configured by the settings file. This single-machine,
pure Java running example will be used for integration and other tests.

### Job Generator

This generates job orders on an infinite loop.

    java -cp target/PanCancerArch3-1.0.0-SNAPSHOT.jar info.pancancer.arch3.jobGenerator.JobGenerator --config conf/config.json

### Coordinator

This consumes the jobs and prepares messages for the VM and Job Queues.

It then monitors the results queue to see when jobs fail or finish.

Finally, for failed or finished workflows, it informats the VM about finished
VMs that can be terminated.

    java -cp target/PanCancerArch3-1.0.0-SNAPSHOT.jar info.pancancer.arch3.coordinator.Coordinator --config conf/config.json

### Container Provisioner

    java -cp target/PanCancerArch3-1.0.0-SNAPSHOT.jar info.pancancer.arch3.containerProvisioner.ContainerProvisioner --config conf/config.json

### Worker

    java -cp target/PanCancerArch3-1.0.0-SNAPSHOT.jar info.pancancer.arch3.worker.Worker --config conf/config.json

### Checking Results

Temp object for helping with debugging.

    java -cp target/PanCancerArch3-1.0.0-SNAPSHOT.jar info.pancancer.arch3.coordinator.CoordinatorResult --config conf/config.json

## Cleanup

To cleanup and delete all queues:

    for i in `/usr/local/sbin/rabbitmqadmin list queues name | grep -v name | awk '{print $2}'`; \
      do echo $i; \
      /usr/local/sbin/rabbitmqadmin delete queue name="$i"; \
      done;
    /usr/local/sbin/rabbitmqadmin list queues name


## TODO

* multi-client reading of queue seems problematic, not sure why, see https://www.rabbitmq.com/tutorials/tutorial-three-java.html
* need job queues with different names based on the workflow and version they target, this will make it easier to run multiple workflow types at the same time
