#!/usr/bin/python

#install boto on machine
#sudo apt-get install python-boto
#create AWS credentals file for boto
#install paramiko
#sudo pip install paramiko

import time
import sys
import os
import boto.ec2
from boto.manage.cmdshell import sshclient_from_instance
import paramiko


def get_confi_key_value_pairs(file_name):
    myvars = {}
    with open(file_name) as myfile:
        for line in myfile:
            name, var = line.partition("=")[::2]
            if len(name) > 0 and len(var) > 0:
                myvars[name.strip()] = var.strip()
    return myvars


def main():

    config_key_value_pairs = get_confi_key_value_pairs('/root/.youxia/config')

    region = config_key_value_pairs['region']
    conn = boto.ec2.connect_to_region(region)
    #conn = boto.ec2.connect_to_region("us-west-2")

    reservations =  conn.get_all_instances()
    for res in reservations:
        for inst in res.instances:
            instance_name = 'instance_managed_by_' + config_key_value_pairs['managed_tag']
    #        if ('Name' in inst.tags) and (inst.tags['Name'] == 'instance_managed_by_walt-hca-fleet'):
            if ('Name' in inst.tags) and (inst.tags['Name'] == instance_name):
                #print "%s (%s) [%s]" % (inst.tags['Name'], inst.id, inst.state)
                if inst.state == 'running':
                   print "Checking running instance: %s (%s) %s [%s]" % (inst.tags['Name'], inst.id, inst.ip_address, inst.state)

    #               key = paramiko.RSAKey.from_private_key_file('/home/ubuntu/.ssh/jshands_us_west.pem')
                   aws_ssh_key = config_key_value_pairs['aws_ssh_key']
                   key = paramiko.RSAKey.from_private_key_file(aws_ssh_key)
                   paramiko_client = paramiko.SSHClient()
                   paramiko_client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

                   # Connect/ssh to an instance
                   try:
                       # Here 'ubuntu' is user name and 'instance_ip' is public IP of EC2
                       paramiko_client.connect(hostname=inst.ip_address,
                               username="ubuntu", pkey=key)

                       # set up hosts file so we don't get warning message in stderr 'sudo: unable to resolve host i-xxxx'
                       # Execute a command(cmd) after connecting/ssh to an instance
                       cmd = 'sudo sed -i /etc/hosts -e \"s/^127.0.0.1 localhost$/127.0.0.1 localhost ' + inst.id + '/\"'
                       #print cmd
                       stdin, stdout, stderr = paramiko_client.exec_command(cmd)
                       set_up_hosts_stdout = stdout.read()
                       set_up_hosts_stderr = stderr.read()
                       #print "set up hosts file for sudo: %s stderr: %s" % (set_up_hosts_stdout, set_up_hosts_stderr)
                       if len(set_up_hosts_stdout) == 0: # and len(set_up_hosts_stderr) == 0:
                           print 'Successfully set up hosts file for sudo'


                       # Execute a command(cmd) after connecting/ssh to an instance
                       stdin, stdout, stderr = paramiko_client.exec_command('ls -l /var/log/arch_worker.log')
                       #print "stdout type:%s" % type(stdout)
                       worker_log_exists_stdout = stdout.read()
                       worker_log_exists_stderr = stderr.read()

    #                   print "worker log exists stdout %s" % worker_log_exists_stdout
    #                   print "worker log exists stderr %s" % worker_log_exists_stderr

    #                   print "len worker log exists stdout %s" % len(worker_log_exists_stdout)
    #                   print "len worker log exists stderr %s" % len(worker_log_exists_stderr)


                       if len(worker_log_exists_stdout) > 0 and len(worker_log_exists_stderr) == 0:
                           print 'Found worker log /var/log/arch_worker'
                           # Execute a command(cmd) after connecting/ssh to an instance
                           stdin, stdout, stderr = paramiko_client.exec_command('grep \"io.consonance.arch.worker.Worker - Exiting\" /var/log/arch_worker.log')
                           worker_exited_stdout = stdout.read()
                           worker_exited_stderr = stderr.read()
                           #print "grep for worker exit stdout: %s stderr: %s" % (worker_exited_stdout, worker_exited_stderr)

                           #debug
                           #if len(worker_exited_stdout) >= 0 or len(worker_exited_stderr) >= 0:

                           if len(worker_exited_stdout) > 0 and len(worker_exited_stderr) == 0:
                               print "Worker deamon exited on instance: %s (%s) [%s]" % (inst.tags['Name'], inst.id, inst.state)


                           #DEBUG START!!!
                               '''
                           else:
                               print "!!!!!!Worker deamon still running on instance: %s (%s) [%s]" % (inst.tags['Name'], inst.id, inst.state)
                               # Execute a command(cmd) after connecting/ssh to an instance
                               stdin, stdout, stderr = paramiko_client.exec_command('sudo bash /home/ubuntu/kill_worker_daemon.sh')
                               kill_worker_daemon_stdout = stdout.read()
                               kill_worker_daemon_stderr = stderr.read()
                               #print "kill worker daemon stdout: %s stderr: %s" % (kill_worker_daemon_stdout, kill_worker_daemon_stderr)
                               if len(kill_worker_daemon_stdout) == 0 and len(kill_worker_daemon_stderr) == 0:
                                   print "Killed worker daemon on instance: %s (%s) [%s]" % (inst.tags['Name'], inst.id, inst.state)
                               '''
                           #DEBUG END!!!

    #                           '''
                               # if the pipeline failed capture the worker log file
                               # Execute a command(cmd) after connecting/ssh to an instance
                               stdin, stdout, stderr = paramiko_client.exec_command('grep \"Error while running job\" /var/log/arch_worker.log')
                               worker_error_stdout = stdout.read()
                               worker_error_stderr = stderr.read()
    #                           print "grep for worker error stdout: %s stderr: %s" % (worker_exited_stdout, worker_exited_stderr)
                               if len(worker_error_stdout) > 0 and len(worker_error_stderr) == 0:
                                   print 'The worker daemon reported a failure; capturing arch_worker.log'
                                   stdin, stdout, stderr = paramiko_client.exec_command('cat /var/log/arch_worker.log')
                                   worker_error_stdout = stdout.read()
                                   worker_error_stderr = stderr.read()
                                   error_log_file_name = inst.tags['Name'] + '_' + str(inst.id) + '_' + 'arch_worker.log'

                                   try:
                                       with open(error_log_file_name, 'w') as outfile:
                                           outfile.write(worker_error_stdout)
                                           outfile.write(worker_error_stderr)
                                   except Exception as error:
                                       print('ERROR writing error log')

                                   #outfile = open(error_log_file_name, 'w')
                                   #outfile.write(worker_error_stdout)
                                   #outfile.write(worker_error_stderr)
                                   #outfile.close()
                               '''
                               # Execute a command(cmd) after connecting/ssh to an instance
                               stdin, stdout, stderr = paramiko_client.exec_command('ps aux | grep java | grep consonance-arch-.*.jar | grep -v grep')
                               worker_daemon_running_stdout = stdout.read()
                               worker_daemon_running_stderr = stderr.read()
                               #print "grep for consonance stdout: %s stderr: %s" % (worker_daemon_running_stdout, worker_daemon_running_stderr)
                               if len(worker_daemon_running_stdout) == 0 and len(worker_daemon_running_stderr) == 0:
                                   print 'Verified that consonance-arch-.*.jar is not running'
                                   # Execute a command(cmd) after connecting/ssh to an instance
                                   stdin, stdout, stderr = paramiko_client.exec_command('sudo rm -rf /datastore/*')
                                   rm_datastore_stdout = stdout.read()
                                   rm_datastore_stderr = stderr.read()
    #                               print "remove /datastore/* stdout: %s stderr: %s" % (rm_datastore_stdout, rm_datastore_stderr)
                                   stdin, stdout, stderr = paramiko_client.exec_command('ls /datastore')
                                   ls_datastore_stdout = stdout.read()
                                   ls_datastore_stderr = stderr.read()
    #                               print "ls /datastore/* stdout: %s stderr: %s" % (ls_datastore_stdout, ls_datastore_stderr)
                                   #if the directory is empty
                                   if len(ls_datastore_stdout) == 0  and len(ls_datastore_stderr) == 0:
                                       print "Deleted contents of /datastore  on instance: %s (%s) [%s]" % (inst.tags['Name'], inst.id, inst.state)
                                       # Execute a command(cmd) after connecting/ssh to an instance
                                       stdin, stdout, stderr = paramiko_client.exec_command('sudo bash /home/ubuntu/run_worker_daemon.sh')
                                       run_worker_daemon_stdout = stdout.read()
                                       run_worker_daemon_stderr = stderr.read()
    #                                   print "run worker daemon stdout: %s stderr: %s" % (run_worker_daemon_stdout, run_worker_daemon_stderr)
                                       if len(run_worker_daemon_stdout) > 0: # and len(run_worker_daemon_stderr) == 0:
                                           print "Restarted worker daemon on instance: %s (%s) [%s]" % (inst.tags['Name'], inst.id, inst.state)
                               '''
                           else:
                               print "INFORMATION: Worker log does not show worker exit: %s (%s) [%s]" % (inst.tags['Name'], inst.id, inst.state)
                       else:
                           print "ERROR: Did not find worker log arch_worker.log on: %s (%s) [%s]" % (inst.tags['Name'], inst.id, inst.state)


                       # close the client connection once the job is done
                       paramiko_client.close()

                       #wait 15 seconds to avoid 'SSHException: Error reading SSH protocol banner' error
                       print 'Waiting 5 seconds'
                       print '\n\n'
                       time.sleep(5)

                       #sys.exit(0)

                   except Exception, e:
                       print "ERROR checking workers!!! %s" % e


if __name__ == "__main__":
    main()
