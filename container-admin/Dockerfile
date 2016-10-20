FROM ubuntu:14.04.3

# Update the APT cache
# prepare for Java download
RUN apt-get update \
    && apt-get upgrade -y \
    && apt-get install -y \
    python-software-properties \
    software-properties-common \
    && apt-get clean

# grab oracle java (auto accept licence)
RUN add-apt-repository -y ppa:webupd8team/java \
    && apt-get update \
    && echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections \
    && apt-get install -y oracle-java8-installer

# install Consonance services
ENV consonance_version=2.0-alpha.9

RUN wget https://seqwaremaven.oicr.on.ca/artifactory/seqware-release/io/consonance/consonance-arch/${consonance_version}/consonance-arch-${consonance_version}.jar \
    && wget https://seqwaremaven.oicr.on.ca/artifactory/seqware-release/io/consonance/consonance-webservice/${consonance_version}/consonance-webservice-${consonance_version}.jar

# install dockerize 
ENV DOCKERIZE_VERSION v0.2.0

RUN wget https://github.com/jwilder/dockerize/releases/download/$DOCKERIZE_VERSION/dockerize-linux-amd64-$DOCKERIZE_VERSION.tar.gz \
    && tar -C /usr/local/bin -xzvf dockerize-linux-amd64-$DOCKERIZE_VERSION.tar.gz

# the web and Consonance config
COPY config .
COPY web.yml .
ADD init.sh .

RUN chmod u+x init.sh
# TODO: make sure you create these from the .template files and customize them
RUN mkdir -p /root/.youxia /root/.consonance /root/.consonance/self-installs /root/.ssh
COPY youxia_config /root/.youxia/config
COPY config /root/.consonance/config
COPY key.pem /root/.ssh/key.pem
RUN chmod 600 /root/.ssh/key.pem
COPY aws.config /root/.aws/config

# for youxia and the consonance command line on the main box 
EXPOSE 8080 8081
RUN wget https://github.com/Consonance/consonance/releases/download/${consonance_version}/consonance && mv consonance /bin && chmod a+x /bin/consonance
RUN wget --no-check-certificate http://wrench.res.oicr.on.ca/artifactory/seqware-release/io/consonance/consonance-client/${consonance_version}/consonance-client-${consonance_version}.jar && mv consonance-client-${consonance_version}.jar /root/.consonance/self-installs/

# the Ansible-based setup
COPY bag_params.json /container-host-bag/example_params.json

# TODO: 1) update the above to have my AWS creds in it and 2) create the admin user in postgres db
# Waiting for postgres and rabbitmq services 
CMD ["dockerize", "-wait", "tcp://postgres:5432", "-wait", "tcp://rabbitmq:5672", "-timeout", "10s", "./init.sh"]
# now get a sample CWL and test JSON
RUN wget https://raw.githubusercontent.com/briandoconnor/dockstore-tool-bamstats/develop/Dockstore.cwl
RUN wget https://raw.githubusercontent.com/briandoconnor/dockstore-tool-bamstats/develop/sample_configs.json
