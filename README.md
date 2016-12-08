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
