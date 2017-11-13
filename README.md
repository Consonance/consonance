# Consonance

[![Build Status](https://travis-ci.org/Consonance/consonance.svg?branch=develop)](https://travis-ci.org/Consonance/Consonance)
[![Coverage Status](https://coveralls.io/repos/Consonance/consonance/badge.svg?branch=develop)](https://coveralls.io/r/Consonance/consonance?branch=develop)

## About

Consonance is a cloud orchestration tool for running Docker-based tools and CWL/WDL workflows available at [Dockstore](https://dockstore.org) on fleets of VMs running in clouds.  It allows you to schedule a set of Dockstore job orders, spinning up the necessary VMs on AWS, Microsoft Azure, or OpenStack via the Youxia library for provisioning cloud-based VMs, and then tearing them down after all work is complete.

We are currently at work on Consonance 2.0 which supports anything from Dockstore and allows users to submit a variety of jobs intended for a variety of instance types.

The latest unstable releases on develop support running tools/workflows from  Dockstore.

Consonance 1.0 is currently in maintenance mode and has also been integrated into a simplified workflow launcher Pancancer Workflow Launcher for AWS for external users and a more flexible but less well documented Pancancer Workflow Launcher for users internal to OICR.

The latest stable releases on master support the pancancer project https://icgc.org/working-pancancer-data-aws

See the Consonance [wiki](https://github.com/Consonance/consonance/wiki) for more information on this project.

## Dependencies

I'm showing how to install dependencies on a Mac so adapt to your system/OS.

### PostgreSQL

You need a PostgreSQL server to run the tests, on the Mac try [Postgres.app](http://postgresapp.com/).

Once you have this setup (using whatever technique is appropriate for your system) create the `queue_status` database.

    $bash> "/Applications/Postgres.app/Contents/Versions/10/bin/psql" -p5432 -d "postgres"
    postgres=# create database queue_status;
    postgres=# create user queue_user;
    postgres=# grant all privileges  on database queue_status to queue_user;

Now load the schema:

    $bash> cat ~/gitroot/Consonance/consonance/consonance-arch/sql/schema.sql | "/Applications/Postgres.app/Contents/Versions/10/bin/psql" -p5432 -d "queue_status"

### RabbitMQ

The integration tests require RabbitMQ, here's how to install
via home brew:

    $bash> brew install rabbitmq
    $bash> /usr/local/sbin/rabbitmq-server

### Consonance Config File

By default the client looks for `~/.consonance/config`. You need this file for
tests to pass:

    $bash> mkdir ~/.consonance/ && cp ./consonance-client/src/test/resources/config ~/.consonance/config

### Dockstore CLI

The integration tests (see below for how to trigger) will actually simulate the full lifecycle of a WDL workflow run using the Dockstore CLI.  See http://dockstore.org for how to install, you need the `dockstore` command dependencies in your path and properly configured in order for the full integration tests to pass.  Consonance is using the Dockstore CLI library so you only need to install cwltool in your path if you want to test CWL workflows.  Cromwell for WDL workflows is baked into the Dockstore CLI library.

### Docker

Related to the Dockstore CLI above, one of the WDL workflows used for testing requires Docker.  Make sure the system you are using for testing has Docker installed if you run the full integration tests.

## Building & Testing

The build uses maven (3.2.3, look at using [MVNVM](http://mvnvm.org/)), just run:

    $bash> mvn clean install

 To avoid tests (probably a bad idea!):

    $bash> mvn -Dmaven.test.skip=true clean install

Skip tests and use parallel build (see more info [here](https://zeroturnaround.com/rebellabs/your-maven-build-is-slow-speed-it-up/)):

    $bash> mvn -Dmaven.test.skip=true -T 1C install -pl consonance-integration-testing -am

This gives me a build time of 36 seconds vs. 1:21 min for `mvn clean install`

Now run the full integration tests (assumes you have RabbitMQ and PostgreSQL installed):

    $bash> cd consonance-integration-testing
    # run a particular test class
    $bash> mvn -Dtest=SystemMainIT test
    # or even a speicifc test method!
    $bash> mvn -Dtest=SystemMainIT#testGetConfiguration test
    # or all ITs
    $bash> cd ..
    $bash> mvn -B clean install -DskipITs=false

## Monitoring Integration Tests

### RabbitMQ

See http://localhost:15672/#/queues for your queues, username and password is guest by default.

## Releasing

See the [developer page](https://github.com/Consonance/consonance/wiki/developers) on our wiki.

## Installation

See the container-admin [README](container-admin/README.md) for information on quickly setting up Consonance via Docker-compose and an interactive bootstrap configuration script.

## Using

See the [quickstart guide](https://github.com/Consonance/consonance/wiki/quickstart) and the [non-quickstart guide](https://github.com/Consonance/consonance/wiki/non-quickstart-users) on our wiki.
