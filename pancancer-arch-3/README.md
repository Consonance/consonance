# PanCancer Architecture 3.0 Prototype

## About

The idea behind this architecture is described in more detail elsewhere but the basic idea is a lightweight
framework to run Docker containers for the PanCancer project.

## Building

## Dependencies

### RabbitMQ

Just use Docker, see https://registry.hub.docker.com/_/rabbitmq/

    docker pull rabbitmq
    docker run -d -e RABBITMQ_NODENAME=my-rabbit --name some-rabbit rabbitmq:3

## Testing

