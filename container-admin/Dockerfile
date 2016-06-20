FROM ubuntu:14.04.3

# Update the APT cache
RUN apt-get update && apt-get upgrade -y

#prepare for Java download
RUN apt-get update && apt-get install -y python-software-properties software-properties-common git postgresql-client vim ansible

#grab oracle java (auto accept licence)
RUN add-apt-repository -y ppa:webupd8team/java
RUN apt-get update
RUN echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections
RUN apt-get install -y oracle-java8-installer

# install Consonance services
ENV consonance_version=2.0-alpha.6

RUN wget https://seqwaremaven.oicr.on.ca/artifactory/seqware-release/io/consonance/consonance-arch/${consonance_version}/consonance-arch-${consonance_version}.jar
RUN wget https://seqwaremaven.oicr.on.ca/artifactory/seqware-release/io/consonance/consonance-webservice/${consonance_version}/consonance-webservice-${consonance_version}.jar

# the web and Consonance config
COPY config .
COPY web.yml .

# for youxia and the consonance command line on the main box
# TODO: make sure you create these from the .template files and customize them
RUN mkdir -p /root/.youxia /root/.consonance /root/.consonance/self-installs /root/.ssh
COPY youxia_config /root/.youxia/config
COPY config /root/.consonance/config
COPY key.pem /root/.ssh/key.pem
RUN chmod 600 /root/.ssh/key.pem

# FIXME: need coordinated release to match alpha.6
RUN wget https://github.com/Consonance/consonance/releases/download/2.0-alpha.2/consonance && mv consonance /bin && chmod a+x /bin/consonance
RUN wget --no-check-certificate http://wrench.res.oicr.on.ca/artifactory/seqware-release/io/consonance/consonance-client/2.0-alpha.2/consonance-client-2.0-alpha.2.jar && mv consonance-client-2.0-alpha.2.jar /root/.consonance/self-installs/

# the Ansible-based setup
# TODO this is just being used for testing
RUN git clone https://github.com/ICGC-TCGA-PanCancer/container-host-bag.git && cd container-host-bag && git checkout 'feature/#113-feature-brian-testing-con-2'

# TODO: 1) update the above to have my AWS creds in it and 2) create the admin user in postgres db

# now get a sample CWL and test JSON
RUN wget https://raw.githubusercontent.com/briandoconnor/dockstore-tool-bamstats/develop/Dockstore.cwl
RUN wget https://raw.githubusercontent.com/briandoconnor/dockstore-tool-bamstats/develop/sample_configs.json

# set default webservice to run
CMD sleep 5000; \
    java -cp consonance-arch-*.jar io.consonance.arch.coordinator.Coordinator --config config --endless; \ 
    java -cp consonance-arch-*.jar io.consonance.arch.containerProvisioner.ContainerProvisionerThreads --config config --endless ; \ 
    java -jar consonance-webservice-*.jar server web.yml
