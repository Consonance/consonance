## Prereqs

Install [docker-compose](https://docs.docker.com/compose/install/) on a Ubuntu 16.04+ VM and it's dependencies.

**NOTE:** This project is fairly DIY, read all the directions below before running.

**NOTE:** We currently test on AWS. We recommend working through the [EC2 tutorial](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/concepts.html) if you have not already.

## Usage

Before you run the `install_bootstrap` script, you will want to create a SSH key used to secure communication with the 
launcher. You will also need a security group that defines the rules for your launcher and the worker nodes that it 
creates. Generally, you will need at least three rules. 
 
A screen shot follows with example security group rules:
 
![security groups](images/security_groups.png)

The first row indicates that that hosts in a security group should be able to access other hosts in the group using private 
ip addresses. The second row indicates that all ports are accessible from your own ip address (206.108.127.16 in this example)
for trouble-shooting. Lastly, the third row is due to a quirk with the current version of Consonance. It indicates that 
the public ip address of the launcher is whitelisted for access to hosts in the security group.

To run the webservice and command-line tools

    bash install_bootstrap

You can exit and re-enter via:

    exit
    docker-compose run client

**NOTE:** The Bash install\_bootstrap script is tested on Ubuntu 16.04 but sets up the templates required to run `docker-compose up` on other OSes if needed.

## Developing

After starting Docker compose, you should have your webservice running on port 8080, you can monitor rabbitmq on port 15672.

You are now ready to submit some work (from within the admin docker container).  I suggest you use the `bamstats` Dockstore tool for testing purposes.  See:

* [Dockstore.cwl](https://github.com/briandoconnor/dockstore-tool-bamstats/blob/develop/Dockstore.cwl)
* [sample\_configs.json](https://github.com/briandoconnor/dockstore-tool-bamstats/blob/develop/sample_configs.json)

The following command submits a job and requests a m1.xlarge for it to run on. 

    consonance run  --flavour m1.xlarge --image-descriptor Dockstore.cwl --run-descriptor sample_configs.json

Check status:

    consonance status --job_uuid 37180f53-e8e1-4079-bf39-89c9bfc8d79c

Take a look at `/consonance_logs` for daemon and webservice logs in any container

When developing on the Dockerfile, since there is no way to inherit or inject environment variables, replace the Consonance version with:

    sed -i 's/2.0-alpha.9/2.0-alpha.12/g' {} \;

