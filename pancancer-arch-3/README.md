# PanCancer Architecture 3.0 Prototype

## About

The idea behind this architecture is described in more detail elsewhere but the basic idea is a lightweight
framework to run Docker containers for the PanCancer project.

## Building

## Dependencies

### Log4J + Logstash

I'm trying to follow this guide for using Log4J so I can easily incorprate with LogStash in the future: [guide](https://blog.dylants.com/2013/08/27/java-logging-creating-indexing-monitoring/).

### RabbitMQ

#### Option 1 - Mac

See [install guide](https://www.rabbitmq.com/install-homebrew.html)

Basically you do:

    brew update
    brew install rabbitmq
    /usr/local/sbin/rabbitmq-server
    
And at that point the service is running.

#### Option 2 - Docker

Use Docker, see https://registry.hub.docker.com/_/rabbitmq/

    docker pull rabbitmq
    docker run -d -e RABBITMQ_NODENAME=my-rabbit --name some-rabbit rabbitmq:3

TODO: need to figure out how to connect this to my code e.g. what ports to connect to localhost on.

## Testing

### Generating Jobs

    java -cp target/PanCancerArch3-1.0.0-SNAPSHOT.jar info.pancancer.arch3.jobGenerator.JobGenerator --config conf/config.json

### Watching Queues

