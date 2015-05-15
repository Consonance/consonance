#! /bin/bash

pkill -SIGTERM -f 'java -Dpidfile.*pancancer-arch-3.*Worker.*'
