FROM ubuntu:14.04.3

# Update the APT cache
RUN apt-get update && apt-get upgrade -y

#prepare for Java download
RUN apt-get update && apt-get install -y python-software-properties software-properties-common git

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
ADD config .
ADD web.yml .

# the Ansible-based setup
# TODO this is just being used for testing
RUN git clone https://github.com/ICGC-TCGA-PanCancer/container-host-bag.git && cd container-host-bag && git checkout 'feature/#113-feature-brian-testing-con-2'


# set default webservice to run

CMD sleep 5000; \
    java -cp consonance-arch-*.jar io.consonance.arch.coordinator.Coordinator --config config --endless; \ 
    java -cp consonance-arch-*.jar io.consonance.arch.containerProvisioner.ContainerProvisionerThreads --config config --endless ; \ 
    java -jar consonance-webservice-*.jar server web.yml
