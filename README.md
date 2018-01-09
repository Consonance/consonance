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

## Dependencies

I'm showing how to install dependencies on a Mac so adapt to your system/OS.

### PostgreSQL

You need a PostgreSQL server to run the tests, on the Mac try [Postgres.app](http://postgresapp.com/).

Once you have this setup (using whatever technique is appropriate for your system) create the `queue_status` database.

    $bash> "/Applications/Postgres.app/Contents/Versions/10/bin/psql" -p5432 -d "postgres"
    postgres=# create database queue_status;
    postgres=# create user queue_user;
    postgres=# grant all privileges  on database queue_status to queue_user;

Now load the schema:

    $bash> cat ~/gitroot/Consonance/consonance/consonance-arch/sql/schema.sql | "/Applications/Postgres.app/Contents/Versions/10/bin/psql" -p5432 -d "queue_status"

After starting the web service you may have to manually insert the test admin creds:

    postgres=# insert into consonance_user(user_id, admin, hashed_password, name) VALUES (1,true,'8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918','admin@admin.com');

### RabbitMQ

The integration tests require RabbitMQ, here's how to install
via home brew:

    $bash> brew install rabbitmq
    $bash> /usr/local/sbin/rabbitmq-server

### Consonance Config File

By default the client looks for `~/.consonance/config`. You need this file for
tests to pass:

    $bash> mkdir ~/.consonance/ && cp ./consonance-client/src/test/resources/config ~/.consonance/config

### Dockstore CLI (recommended)

This is no longer needed since the Dockstore CLI is now included at build time.  But it's worth having on your local machine.
See http://dockstore.org for how to install, you will want the `dockstore` command dependencies in your path and properly configured in order to test run things from Dockstore manually.  Consonance is using the Dockstore CLI library so you only need to install cwltool in your path if you want to test CWL workflows.  Cromwell for WDL workflows is baked into the Dockstore CLI library.

### Docker

Related to the Dockstore CLI above, one of the WDL workflows used for testing requires Docker.  Make sure the system you are using for testing has Docker installed if you run the full integration tests.

## Building & Testing

The build uses maven (3.2.3, look at using [MVNVM](http://mvnvm.org/)), just run:

    $bash> mvn clean install

 To avoid tests (probably a bad idea!):

    $bash> mvn -Dmaven.test.skip=true clean install

Skip tests and use parallel build (see more info [here](https://zeroturnaround.com/rebellabs/your-maven-build-is-slow-speed-it-up/)).
You should really read up on these params, below I'm using `-T` for threads, `-pl` to skip a bunch of sub-projects, etc.

    $bash> mvn -Dmaven.test.skip=true -T 1C install -pl consonance-integration-testing -am

This gives me a build time of 36 seconds vs. 1:21 min for `mvn clean install`

Now run the full integration tests (assumes you have RabbitMQ and PostgreSQL installed):

    $bash> cd consonance-integration-testing
    # run a particular test class
    $bash> mvn -Dtest=SystemMainIT test
    # or even a speicifc test method!
    $bash> mvn -Dtest=SystemMainIT#testScheduleAndRunWdlLocally test
    # this is an extended AWS-based integration test, see below for more info
    $bash> mvn -Dtest=SystemMainAwsIT#testScheduleAndRunWdlOnAws test
    # or all ITs
    $bash> cd ..
    $bash> mvn -B clean install -DskipITs=false

### Extended Cloud Integration Tests

We have some ITs that are used only when directly called.  They require AWS configs that we currently don't want to submit to travis.

#### Dependencies

Make sure you setup the dependencies above as well as Ansible.

    $> brew install ansible
    $> cat /usr/local/etc/rabbitmq/rabbitmq.config
    [{rabbit, [{loopback_users, []}]}].
    $> cat /usr/local/etc/rabbitmq/rabbitmq-env.conf
    CONFIG_FILE=/usr/local/etc/rabbitmq/rabbitmq
    #NODE_IP_ADDRESS=10.0.0.23
    NODENAME=rabbit@localhost
    loopback_users=none
    $> cat ~/.youxia/example_tags.json
    {
      "Owner": "broconno@ucsc.edu",
      "PrincipalId": "<MASKED>"
    }
    $> # youxia expected
    ~/.aws/config

## Monitoring Integration Tests

### RabbitMQ

See http://localhost:15672/#/queues for your queues, username and password is guest by default.

## Viewing Swagger Web Service API Documentation

See the consonance-webservice [README](consonance-webservice/README.md) for how to setup and view the Swagger API documentation/client.

## Releasing

See the [developer page](https://github.com/Consonance/consonance/wiki/developers) on our wiki.

## Installation

See the container-admin [README](container-admin/README.md) for information on quickly setting up Consonance via Docker-compose and an interactive bootstrap configuration script.

## Using

See the [quickstart guide](https://github.com/Consonance/consonance/wiki/quickstart) and the [non-quickstart guide](https://github.com/Consonance/consonance/wiki/non-quickstart-users) on our wiki.

## TODO

* ~~review and merge the pull request~~
* test container-admin with pre-release locally with provisioning on AWS, document
* perform release
* test container-admin with release on AWS, should be a safe harbor
* then review/try WES support branch from Abraham
* WES support issues
    * it looks like Consonance doesn't have the equivalent of key-values like WES requires. Need to add support for tagging orders with
key values in order to support this.  Right now lookup with key-values isn't supported (it's ignored)
    * caps and defaults for page size should be implemented (see Ga4ghAPI.java)
    * double-check the way I do token offsets in Ga4ghApiServiceImpl.java, specifically listWorkflows.  The value right now is an offset not a page. E.g. when you put in a page_token of 1 it starts at the second result not the second page of results!
    * related to this, if the page_token is empty it should go to page 1
    * `GET /ga4gh/wes/v1/workflows/{workflow_id}`'s response is actually complex. I only modeled a few fields.  Since Consonance is tied to Dockstore CLI (what the worker daemon launches) we should coordinate with the Dockstore CLI developers to make sure this info is reported back from the Dockstore CLI regardless if WDL or CWL workflows is run.  And the Consonance worker daemon then needs to report back this info.
    * the status endpoint needs:
        * location e.g. geo coordinates
        * URL for user login
        * description, more information URL
* TES support issues
    * I think all the TES endpoints are really out of date, we could get TES to work in Consonance but I'm most interested in WES
* New features
    * I want a local worker mode, this will come in handy if the workflow autoscales so the resource requirements are extremely modest
    *
LEFT OFF WITH:

* I need to setup dockstore config file
* I'm left with the error `java.lang.RuntimeException: java.lang.ClassNotFoundException: com.sun.ws.rs.ext.RuntimeDelegateImpl` which is linked to jersey client... using different ones for Dockstore and Consonance?

```
18:09:47.128 [pool-5-thread-2] INFO  i.c.arch.worker.WorkflowRunner - command: dockstore workflow launch --local-entry /datastore/image-descriptor.wdl --json /datastore/run-descriptor.json
java.lang.RuntimeException: java.lang.ClassNotFoundException: com.sun.ws.rs.ext.RuntimeDelegateImpl
	at javax.ws.rs.ext.RuntimeDelegate.findDelegate(RuntimeDelegate.java:122)
	at javax.ws.rs.ext.RuntimeDelegate.getInstance(RuntimeDelegate.java:91)
	at javax.ws.rs.core.UriBuilder.newInstance(UriBuilder.java:69)
	at javax.ws.rs.core.UriBuilder.fromUri(UriBuilder.java:80)

▽
	at javax.ws.rs.core.UriBuilder.fromUri(UriBuilder.java:99)
	at org.glassfish.jersey.client.JerseyWebTarget.<init>(JerseyWebTarget.java:71)
	at org.glassfish.jersey.client.JerseyClient.target(JerseyClient.java:290)
	at org.glassfish.jersey.client.JerseyClient.target(JerseyClient.java:76)
	at io.swagger.client.ApiClient.invokeAPI(ApiClient.java:426)
	at io.swagger.client.api.UsersApi.getUser(UsersApi.java:458)
	at io.dockstore.client.cli.Client.setupClientEnvironment(Client.java:785)
	at io.dockstore.client.cli.Client.run(Client.java:673)
	at io.dockstore.client.cli.Client.main(Client.java:618)
	at io.consonance.arch.worker.WorkflowRunner.call(WorkflowRunner.java:101)
	at io.consonance.arch.worker.WorkflowRunner.call(WorkflowRunner.java:40)
	at java.util.concurrent.FutureTask.run(FutureTask.java:266)
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
	at java.lang.Thread.run(Thread.java:748)
Caused by: java.lang.ClassNotFoundException: com.sun.ws.rs.ext.RuntimeDelegateImpl
	at java.net.URLClassLoader.findClass(URLClassLoader.java:381)
	at java.lang.ClassLoader.loadClass(ClassLoader.java:424)
	at sun.misc.Launcher$AppClassLoader.loadClass(Launcher.java:335)
	at java.lang.ClassLoader.loadClass(ClassLoader.java:357)
```

* now a bunch of errors with dependencies when I updated the Dockstore version.  Why is none of this an issue when running locally?!?!
* I'm in the process of updating to the latest dockstore but that's causing a huge list of dependency updates and all the complexities that come with that.  

Here's where I left off, it looks like something critical was excluded:
```
INFO  [2018-01-08 00:08:49,372] org.eclipse.jetty.server.handler.ContextHandler: Started i.d.j.MutableServletContextHandler@3220c28{/admin,null,AVAILABLE}
INFO  [2018-01-08 00:08:49,372] org.eclipse.jetty.server.AbstractConnector: Started FakeApplication@1dbff71c{HTTP/1.1,[http/1.1]}{0.0.0.0:59229}
INFO  [2018-01-08 00:08:49,373] org.eclipse.jetty.server.Server: Started @2969ms
Tests run: 3, Failures: 0, Errors: 3, Skipped: 0, Time elapsed: 0.272 sec <<< FAILURE! - in io.consonance.client.cli.MainTest
testQuietGetConfiguration(io.consonance.client.cli.MainTest)  Time elapsed: 0.021 sec  <<< ERROR!
java.lang.NoSuchMethodError: io.swagger.client.ApiClient.invokeAPI(Ljava/lang/String;Ljava/lang/String;Ljava/util/List;Ljava/lang/Object;Ljava/util/Map;Ljava/util/Map;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;Lio/swagger/client/TypeRef;)Ljava/lang/Object;
	at io.consonance.client.cli.MainTest.testQuietGetConfiguration(MainTest.java:77)

testGetConfiguration(io.consonance.client.cli.MainTest)  Time elapsed: 0.01 sec  <<< ERROR!
java.lang.NoSuchMethodError: io.swagger.client.ApiClient.invokeAPI(Ljava/lang/String;Ljava/lang/String;Ljava/util/List;Ljava/lang/Object;Ljava/util/Map;Ljava/util/Map;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;Lio/swagger/client/TypeRef;)Ljava/lang/Object;
	at io.consonance.client.cli.MainTest.testGetConfiguration(MainTest.java:62)

testDebugGetConfiguration(io.consonance.client.cli.MainTest)  Time elapsed: 0.008 sec  <<< ERROR!
java.lang.NoSuchMethodError: io.swagger.client.ApiClient.invokeAPI(Ljava/lang/String;Ljava/lang/String;Ljava/util/List;Ljava/lang/Object;Ljava/util/Map;Ljava/util/Map;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;Lio/swagger/client/TypeRef;)Ljava/lang/Object;
	at io.consonance.client.cli.MainTest.testDebugGetConfiguration(MainTest.java:94)


Results :

Tests in error:
  WebClientTest.testListUsers:74 » NoSuchMethod io.swagger.client.ApiClient.invo...
  MainTest.testDebugGetConfiguration:94 » NoSuchMethod io.swagger.client.ApiClie...
  MainTest.testGetConfiguration:62 » NoSuchMethod io.swagger.client.ApiClient.in...
  MainTest.testQuietGetConfiguration:77 » NoSuchMethod io.swagger.client.ApiClie...

Tests run: 4, Failures: 0, Errors: 4, Skipped: 0

[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary:
[INFO]
[INFO] consonance ......................................... SUCCESS [  3.020 s]
[INFO] consonance-common .................................. SUCCESS [  6.180 s]
[INFO] consonance-server-common ........................... SUCCESS [  6.243 s]
[INFO] consonance-arch .................................... SUCCESS [ 25.380 s]
[INFO] consonance-reporting ............................... SUCCESS [ 12.634 s]
[INFO] consonance-webservice .............................. SUCCESS [ 15.949 s]
[INFO] swagger-java-client ................................ SUCCESS [  5.411 s]
[INFO] consonance-client .................................. FAILURE [  5.422 s]
[INFO] consonance-integration-testing ..................... SKIPPED
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
```

Also consonance-arch/src/test/java/io/consonance/arch/test/TestWorkerWithMocking.java was failing with similar errors.  Need to fix the problem and bring it back
