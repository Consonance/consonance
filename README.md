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

## Installation

See the container-admin [README](container-admin/README.md) for information on quickly setting up Consonance via Docker-compose and an interactive bootstrap configuration script.

## TODO

Consonance is a work in progress, there are many features and bugs that need to be fleshed out. Here are the most imporant issues.

* can the indivdual images be hosted on Quay.io so the install_boostrap doesn't need to spend time building them?
* there are a ton of settings that the bootstrapper should expose to end users.  Regions, AMI, etc
* need to setup the previous job hash and expose that through as a callable parameter to the consonance command line
* should generate new token for admin on each deployment, should also use better passwords assigned at build time for rabbitmq and postgres
* it seems like the instance type is hard-coded for Youxia yet it's a param for Consonance.  It really should be a param otherwise a given deployment will only work for a particular AMI/instance type.
* the initial job submitted seem to go into the START state and never get processed
* re-enqueing work for lost jobs ultimately fails
* we need to improve the worker deamon, it fails to launch properly on occasion which menas we have workers that never process jobs
