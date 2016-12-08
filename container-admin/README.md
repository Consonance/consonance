## Prereqs

Install [docker-compose](https://docs.docker.com/compose/install/) on a Ubuntu 16.04+ VM and it's dependencies.

**NOTE:** this isn't production ready, there is some manual config you need to do. Read all the directions below before running.

**NOTE:** I'm testing on AWS currently

## Usage

To run the webservice and command-line tools (still a work in progress, the ip address for swagger and other app level issues persist)

    bash install_bootstrap

You can exit and re-enter via:

    exit
    docker-compose run client

**NOTE:** The Bash install_bootstrap script depends on Ubuntu 16.04 but sets up the templates required to run `docker-compose up` if needed.

## Developing

The following files are created from templates by the install script:

* youxia_config -> need to update aws key and various settings
* config -> not much to do
* key.pem -> need your AWS SSH key
* aws.config -> need your AWS API keys
* *the above files won’t be checked in due to the .gitignore policy*

Now, you should have your webservice running on port 8080, you can monitor rabbitmq on port 15672.

You are now ready to submit some work (from within the admin docker container).

    consonance run  --flavour m1.xlarge --image-descriptor Dockstore.cwl --run-descriptor sample_configs.json

Note that you will also need to configure your security group to allow for SSH access between nodes in the security group on public IP addresses.

Check status:

    consonance status --job_uuid 37180f53-e8e1-4079-bf39-89c9bfc8d79c

NOTE: We make the simplfying assumption that the ip address at eth0 of the launcher is reachable from the children. If it is different (i.e. a public ip address is preferred, modify sample_params.json in /container-host-bag in the provisioner container before launching jobs)

Take a look at `/consonance_logs` for daemon and webservice logs in any container

When developing on the Dockerfile, since there is no way to inherit or inject environment variables, replace the Consonance version with:

    sed -i 's/2.0-alpha.9/2.0-alpha.10/g' {} \;

## TODO

* can the indivdual images be hosted on Quay.io so the install_boostrap doesn't need to spend time building them?
* there are a ton of settings that the bootstrapper should expose to end users.  Regions, AMI, etc
* need to setup the previous job hash and expose that through as a callable parameter to the consonance command line
* should generate new token for admin on each deployment, should also use better passwords assigned at build time for rabbitmq and postgres
* it seems like the instance type is hard-coded for Youxia yet it's a param for Consonance.  It really should be a param otherwise a given deployment will only work for a particular AMI/instance type.
