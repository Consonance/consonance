# PanCancer Architecture 3.0 Prototype

## About

The idea behind this architecture is described in more detail elsewhere but the basic idea is a lightweight
framework to run Docker containers for the PanCancer project.

## Building

## Dependencies

### Log4J + Logstash

I'm trying to follow this guide for using Log4J so I can easily incorprate with LogStash in the future: [https://blog.dylants.com/2013/08/27/java-logging-creating-indexing-monitoring/](guide).

### RabbitMQ

Just use Docker, see https://registry.hub.docker.com/_/rabbitmq/

    docker pull rabbitmq
    docker run -d -e RABBITMQ_NODENAME=my-rabbit --name some-rabbit rabbitmq:3

## Testing

