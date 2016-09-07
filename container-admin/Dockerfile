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
ENV consonance_version=2.0-alpha.4

RUN wget https://seqwaremaven.oicr.on.ca/artifactory/seqware-release/io/consonance/consonance-arch/${consonance_version}/consonance-arch-${consonance_version}.jar \
    && wget https://seqwaremaven.oicr.on.ca/artifactory/seqware-release/io/consonance/consonance-webservice/${consonance_version}/consonance-webservice-${consonance_version}.jar

# install dockerize 
ENV DOCKERIZE_VERSION v0.2.0

RUN wget https://github.com/jwilder/dockerize/releases/download/$DOCKERIZE_VERSION/dockerize-linux-amd64-$DOCKERIZE_VERSION.tar.gz \
    && tar -C /usr/local/bin -xzvf dockerize-linux-amd64-$DOCKERIZE_VERSION.tar.gz

ADD config .
ADD web.yml .
ADD init.sh .

RUN chmod u+x init.sh

EXPOSE 8080 8081

# Waiting for postgres and rabbitmq services 
CMD ["dockerize", "-wait", "tcp://postgres:5432", "-wait", "http://rabbitmq:15672", "-timeout", "10s", "./init.sh"]
