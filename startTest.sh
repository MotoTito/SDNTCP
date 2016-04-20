#!/bin/bash

#This script will start sflowtools and feed the right information to the SDNTCP python script.

sflowtool -p 6343 | egrep 'ifName|ifInUcastPkts|ifOutUcastPkts' | ./SDNTCP1.py
