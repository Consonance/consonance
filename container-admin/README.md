## Prereqs

Install [docker-compose](https://docs.docker.com/compose/install/) on a Ubuntu 16.04+ VM and it's dependencies.

**NOTE:** this isn't production ready, there is some manual config you need to do. Read all the directions below before running.

**NOTE:** I'm testing on AWS currently. We recommend working through the [EC2 tutorial](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/concepts.html) if you have not already.

## Usage

Before you run the `install_bootstrap` script, you will want to edit a security group that defines the rules for your
launcher and the worker nodes that it creates. Generally, you will need at least three rules. The inbound rule should be 
that hosts in a security group should be able to access other hosts in its own security group. You will also want to be able to access 
all ports from your own ip address for trouble-shooting. Lastly, a quirk with the current version is that the public 
 ip address of the launcher should be whitelisted for access to hosts in the security group. 
 
A screen shot follows with an example of the public ip address of the launcher being ():
 
 

To run the webservice and command-line tools (still a work in progress, the ip address for swagger and other app level issues persist)

    bash install_bootstrap

You can exit and re-enter via:

    exit
    docker-compose run client

**NOTE:** The Bash install\_bootstrap script depends on Ubuntu 16.04 but sets up the templates required to run `docker-compose up` if needed.

## Developing

The following files are created from templates by the install script:

* youxia\_config -> need to update aws key and various settings
* config -> not much to do
* key.pem -> need your AWS SSH key
* aws.config -> need your AWS API keys
* *the above files won’t be checked in due to the .gitignore policy*

Now, you should have your webservice running on port 8080, you can monitor rabbitmq on port 15672.

You are now ready to submit some work (from within the admin docker container).  I suggest you use the `bamstats` Dockstore tool for testing purposes.  See:

* [Dockstore.cwl](https://github.com/briandoconnor/dockstore-tool-bamstats/blob/develop/Dockstore.cwl)
* [sample\_configs.json](https://github.com/briandoconnor/dockstore-tool-bamstats/blob/develop/sample_configs.json)

The following command submits a job and requests a m1.xlarge for it to run on. 

    consonance run  --flavour m1.xlarge --image-descriptor Dockstore.cwl --run-descriptor sample_configs.json

Note that you will also need to configure your security group to allow for SSH access between nodes in the security group on public IP addresses.

Check status:

    consonance status --job_uuid 37180f53-e8e1-4079-bf39-89c9bfc8d79c

NOTE: We make the simplfying assumption that the ip address at eth0 of the launcher is reachable from the children. If it is different (i.e. a public ip address is preferred, modify sample\_params.json in /container-host-bag in the provisioner container before launching jobs)

Take a look at `/consonance_logs` for daemon and webservice logs in any container

When developing on the Dockerfile, since there is no way to inherit or inject environment variables, replace the Consonance version with:

    sed -i 's/2.0-alpha.9/2.0-alpha.12/g' {} \;

