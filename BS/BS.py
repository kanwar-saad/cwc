#!/usr/bin/python

import sys
import time
import json
import globals
from threading import *
from socket import *

from node import *
from handler import *


def start():

    print "Starting base Station"
    try:
        globals.bsSock = socket(AF_INET, SOCK_DGRAM)
        globals.bsSock.bind(('', globals.ServerPort)) 
    except:
        print "Error in Initializing BS Socket"
        return

    print "Initialization complete"
    
    try:
        Timer(globals.CLSelectionPeriod, CLSelectWrapper).start()
        while (True):
            jsonData, address = globals.bsSock.recvfrom(1024)

            #print jsonData
            try:
                data = json.loads(jsonData)
            except:
                print "Error parsing JSON data"
                continue

            #print data
            ret = msgHandler(globals.bsSock, data, address)
            if (ret == False):
                print "Closing .. "
                raise BaseException()
    except BaseException, e:
        print e
        print "\nException Occurred ... Exiting"
        globals.bsSock.close()




if __name__ == "__main__":
    start()
