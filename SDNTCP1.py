#!/usr/bin/python

"""
Python script that will monitor the input from sflowtools and calculate rough buffer use within the switches.
The script will then add and remove the flows needed to adjust the window size of a specific TCP Connection
down to half it's current size.
"""

import sys
import datetime
from os import system

sw = 's1'
InputBuffer = 0
OutputBuffer = 0
writeOut = False
writeIn = False
windowAdj = []
prevValue = [0]*15
curPort = 0
firstRun = True
counter = 0
modFlows = []
maxBuffer = 5000

print "Running"
for line in iter(sys.stdin.readline, ""):
    ifName = line.split()[0]
    ifValue = line.split()[1]
    #sys.stdout.write(line)
    if (writeIn and ifName == 'ifInUcastPkts' and curPort != 13 and curPort != 12):
        if (prevValue[curPort] != int(ifValue) and int(ifValue) != 0):
            #print 'Current inValue: ' +str(prevValue[curPort])+'New Value: '+ str(ifValue)
            InputBuffer = InputBuffer + (int(ifValue)-prevValue[curPort])
            prevValue[curPort] = int(ifValue)
            curPort = 0
            writeIn = False
    elif (writeOut and ifName == 'ifOutUcastPkts' and curPort == 13):
        if (prevValue[curPort] != int(ifValue) and int(ifValue) != 0):
            #print 'Current OutValue: ' + str(prevValue[curPort])+'New Value: '+ str(ifValue)
            OutputBuffer = OutputBuffer + (int(ifValue)-prevValue[curPort])
            prevValue[curPort] = int(ifValue)
            curPort = 0
            writeOut = False
    elif (ifValue == sw):
        None
    elif (ifValue == 's1-eth13'):
        curPort = int(ifValue[6:])
        writeOut = True
    elif (ifValue != 's1-eth13' and ifValue[:6] == 's1-eth'):
        curPort = int(ifValue[6:])
        writeIn = True

    if (firstRun and counter == 100):
        InputBuffer = 0
        OutputBuffer = 0
        firstRun = False
    elif (firstRun):
        counter = counter + 1
    print str(datetime.datetime.now().time()) + " InputBuffer =  " + str(InputBuffer) + " || OutputBuffer = " + str(OutputBuffer)
    print "Percent Buffer: " + str(((InputBuffer-OutputBuffer)/maxBuffer) * 100)

    #Reach Critical Threshold and Change the Flow
    if (InputBuffer-OutputBuffer > maxBuffer):
        file = open('/TCPFlows.txt', 'r')
        for lines in file.readlines():
            if lines not in windowAdj:
                windowAdj.append(lines)
                ipsrc = lines.split('-')[0].split(':')[0]
                portsrc = lines.split('-')[0].split(':')[1]
                ipdst = lines.split('-')[1].split(':')[0]
                portdst = lines.split('-')[1].split(':')[1]
                cmd = 'sudo ovs-ofctl add-flow s1 \"priority=62000,tcp,tcp_src='+portsrc+',tcp_dst='+portdst+',ip,ip_src='+ipsrc+',ip_dst='+ipdst+',actions=controller\"'
                modFlows.append('sudo ovs-ofctl del-flow s1 \"tcp,tcp_src='+portsrc+',tcp_dst='+portdst+',ip,ip_src='+ipsrc+',ip_dst='+ipdst+'\"')
                system(cmd)
                break
    elif (InputBuffer-OutputBuffer < maxBuffer):
        for i in modFlows:
            system(i)
            modFlows.remove(i)
