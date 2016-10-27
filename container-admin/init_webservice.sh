#!/bin/bash

java -jar consonance-webservice-*.jar server web.yml | tee /consonance_logs/webservice.out
