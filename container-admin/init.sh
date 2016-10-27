#!/bin/bash

nohup java -cp consonance-arch-*.jar io.consonance.arch.coordinator.Coordinator --config config --endless | tee /consonance_logs/coordinator_nohup.out &

nohup java -cp consonance-arch-*.jar io.consonance.arch.containerProvisioner.ContainerProvisionerThreads --config config --endless | tee /consonance_logs/container_provisioner_nohup.out &

java -jar consonance-webservice-*.jar server web.yml | tee /consonance_logs/webservice.out
