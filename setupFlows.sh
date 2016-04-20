#!/bin/bash

#This script will generate flows for the setupNetwork python scripts created for the CS6250
#Computer Networks project.

> /TCPFlows.txt

#Create Flow that will allow for ARP and DataLink (DL) Layer broadcast traffic
sudo ovs-ofctl del-flows s1
sudo ovs-ofctl del-flows s2
sudo ovs-ofctl add-flow s1 "priority=5,in_port=*,dl_dst=01:00:00:00:00:00/01:00:00:00:00:00,actions=flood"
sudo ovs-ofctl add-flow s2 "priority=5,in_port=*,dl_dst=01:00:00:00:00:00/01:00:00:00:00:00,actions=flood"
sudo ovs-ofctl add-flow s1 "in_port=*,ip,ip_dst=10.0.0.80/28,actions=13"
sudo ovs-ofctl add-flow s2 "in_port=*,ip,ip_dst=10.0.0.0/28,actions=13"

sudo ovs-ofctl add-flow s1 "priority=60000,tcp,tcp_flags=0x2 actions=normal,output:controller"
sudo ovs-ofctl add-flow s1 "priority=60000,tcp,tcp_flags=+fin actions=normal,output:controller"
sudo ovs-ofctl add-flow s1 "priority=60000,tcp,tcp_flags=+rst actions=normal,output:controller"
sudo ovs-ofctl add-flow s2 "priority=60000,tcp,tcp_flags=0x2 actions=normal,output:controller"
sudo ovs-ofctl add-flow s2 "priority=60000,tcp,tcp_flags=+fin actions=normal,output:controller"
sudo ovs-ofctl add-flow s2 "priority=60000,tcp,tcp_flags=+rst actions=normal,output:controller"

#number of senders
SENDERS=10
#number of receivers
RECEIVERS=10

#Add the flows for the senders
while [ $SENDERS -gt 0 ];
do            
    #echo $SENDERS
    HEX=`echo "obase=16;ibase=10; $SENDERS" | bc`
    #echo $HEX
    #Add the OVS flow for the DL layer and out the appropriate port.
    
    sudo ovs-ofctl add-flow s1 "in_port=*,dl_dst=00:00:00:00:00:0$HEX,dl_type=*,actions=$SENDERS"
    sudo ovs-ofctl add-flow s2 "in_port=*,dl_dst=00:00:00:00:00:0$HEX,dl_type=*,actions=13"
    #Add the OVS Flow for the IP Layer
    sudo ovs-ofctl add-flow s1 "in_port=*,ip,ip_dst=10.0.0.$SENDERS,actions=$SENDERS"

    let SENDERS=SENDERS-1
done
SENDERS=10
#Add the flows for the receivers
while [ $RECEIVERS -gt 0 ];
do            
    #echo $RECEIVERS
    let IP=RECEIVERS+80
    let MAC=RECEIVERS+SENDERS
    HEX=`echo "obase=16;ibase=10; $MAC" | bc`
    #echo $HEX

    #Add the OVS flow for the DL layer and out the appropriate port.
    echo "in_port=*,dl_dst=00:00:00:00:00:0$HEX,dl_type=*,actions=$TEMP"
    sudo ovs-ofctl add-flow s2 "in_port=*,dl_dst=00:00:00:00:00:0$HEX,dl_type=*,actions=$RECEIVERS"
    sudo ovs-ofctl add-flow s1 "in_port=*,dl_dst=00:00:00:00:00:0$HEX,dl_type=*,actions=13"    
    #Add the OVS Flow for the IP Layer
    sudo ovs-ofctl add-flow s2 "in_port=*,ip,ip_dst=10.0.0.$IP,actions=$RECEIVERS"

    let RECEIVERS=RECEIVERS-1
done


#Open up a feed to the SflowTools
sudo ovs-vsctl -- --id=@netflow create sflow agent=s1  target=\"127.0.0.1:6343\" sampling=1 polling=1 -- -- set bridge s1 sflow=@netflow
sudo ovs-vsctl -- --id=@netflow create sflow agent=s2  target=\"127.0.0.1:6344\" sampling=1 polling=1 -- -- set bridge s2 sflow=@netflow

