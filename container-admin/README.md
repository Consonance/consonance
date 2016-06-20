## Prereqs

Install [docker-compose](https://docs.docker.com/compose/install/) on a Ubuntu 14.04+ VM and it's dependencies.

**NOTE:** this isn't production ready, there is some manual config you need to do. Read all the directions below before running.

**NOTE:** I'm testing on AWS currently

## Usage

To run the webservice and command-line tools (still a work in progress, the ip address for swagger and other app level issues persist)

    docker-compose build
    docker-compose up
    
**NOTE:** this `docker-compose up` currently isn't working since you need to do some manual configuration before you can start the daemons. See the next section.
 
## Developing 

Create these files from templates:

* youxia_config -> need to update aws key and various settings
* config -> not much to do
* key.pem -> need my AWS key
* *the above files won’t be checked in due to the .gitignore policy*

Build with

    docker-compose build

Start with 

    docker-compose run admin bash

Now, inside the admin container you just launched:

Start the webservice in the container 

    nohup java -jar consonance-webservice-*.jar server web.yml &> web.log &

You need to create the database, password is postgres (unless you changed it)

    psql -h postgres -U postgres -W postgres
    > insert into consonance_user(user_id, admin, hashed_password, name) VALUES (1,true,'8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918','admin@admin.com');

Customize `/container-host-bag/example_params.json`, specifically, you need to add your aws keys.  You might also want to customize `/container-host-bag/example_tags.json`.

Start the two daemons

    nohup java -cp consonance-arch-*.jar io.consonance.arch.coordinator.Coordinator --config config --endless &> coordinator.log &
    nohup java -cp consonance-arch-*.jar io.consonance.arch.containerProvisioner.ContainerProvisionerThreads --config config --endless &> provisioner.log &

Now, you should have your webservice running on port 8081, you can monitor rabbitmq on port 15673.

You are now ready to submit some work (from within the admin docker container).

    # get a sample CWL and JSON param file
    wget https://raw.githubusercontent.com/briandoconnor/dockstore-tool-bamstats/develop/Dockstore.cwl
    wget https://raw.githubusercontent.com/briandoconnor/dockstore-tool-bamstats/develop/sample_configs.json
    consonance run  --flavour m1.xlarge \
        --image-descriptor Dockstore.cwl \
        --run-descriptor sample_configs.json 

    #    --extra-file /root/.aws/config=/home/ubuntu/.aws/config=false
    
    
TODO: there's some sort of problem with the admin user getting wiped, need to disable in web.yml

TODO: I need a /root/.aws/config file after all

TODO: chmod the key.pem