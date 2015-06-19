### Reporting

There are two methods of reporting, they should not be used concurrently
Note that both require a valid config file (in these examples, at ~/.arch3/config ). 

### Traditional CLI

A standard CLI utility for Linux is provided

    
    $ java -cp target/pancancer-reporting-*.jar  info.pancancer.arch3.reportcli.ReportCLI --config ~/.arch3/config 
    Available commands are:
    `gather` gathers the last message sent by each worker and displays the last line of it
    `info` retrieves detailed information on provisioned instances
    `jobs` retrieves detailed information on jobs
    `provisioned` retrieves detailed information on provisioned instances
    `status` retrieves configuration and version information on arch3
    $ java -cp pancancer-arch-3/target/pancancer-arch-*.jar  info.pancancer.arch3.reportcli.ReportCLI --config ~/.arch3/config info
    database.postgresDBName: queue_status
    database.postgresHost: 127.0.0.1
    database.postgresUser: queue_user
    rabbit.rabbitMQHost: localhost
    rabbit.rabbitMQQueueName: pancancer_arch_3
    rabbit.rabbitMQUser: queue_user
    report.namespace: flying_snow
    version: 1.1-alpha.2-SNAPSHOT

### SlackBot

You can also communicate with our reporting tools as a SlackBot. This is our recommended approach. 

    java -cp target/pancancer-reporting-*.jar  info.pancancer.arch3.reportbot.SlackReportBot --endless --config ~/.arch3/config
    
