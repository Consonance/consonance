#! /bin/bash

pkill --signal SIGTERM -f 'java -Dpidfile.*pancancer-arch-3.*Worker.*'
