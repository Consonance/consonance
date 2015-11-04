## Prereqs

Install [docker-compose](https://docs.docker.com/compose/install/) on a Ubuntu 14.04+ VM. 

## Usage

To run the webservice and command-line tools (still a work in progress, the ip address for swagger and other app level issues persist)

    docker-compose build
    docker-compose up
    
### Developing 

Build with

    docker-compose build

Start with 

    docker-compose run admin bash

Start the webservice in the container 

    java -jar consonance-webservice-*.jar server web.yml

Start the two daemons

    java -cp consonance-arch-*.jar io.consonance.arch.coordinator.Coordinator --config config --endless
    java -cp consonance-arch-*.jar io.consonance.arch.containerProvisioner.ContainerProvisionerThreads --config config --endless

Now, you should have your webservice running on port 8081, you can monitor rabbitmq on port 15673.
