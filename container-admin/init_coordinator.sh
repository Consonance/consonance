#!/bin/bash

java -cp consonance-arch-*.jar io.consonance.arch.coordinator.Coordinator --config config --endless | tee /consonance_logs/coordinator_nohup.out
