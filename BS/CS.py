#!/usr/bin/python

import sys
import time
import json
import globals
from threading import *
from socket import *

from node import *
from handler import *


cs_port = 8089 
csSock = None
def start():

    print "Starting Content Server"
    try:
        csSock = socket(AF_INET, SOCK_DGRAM)
        csSock.bind(('', cs_port)) 
    except:
        print "Error in Initializing CS Socket"
        return

    print "Initialization complete"
    
    try:
        msg = {}
        msg['msgType'] = 'DATA_TRIGGER'
        csSock.sendto 
    except BaseException, e:
        print e
        print "\nException Occurred ... Exiting"
        globals.bsSock.close()




if __name__ == "__main__":
    start()
