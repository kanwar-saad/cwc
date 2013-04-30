#!/usr/bin/python

import sys
import time
import threading
import socket
import json
import globals
from AnnounceThread import *
from Log import *

def start():

    try:
        AThread = AnnounceThread()
        # Start Threads
        AThread.start()

        while (True):
            LogListner()
            continue
    except KeyboardInterrupt, e:
        globals.AQueue.put("shutdown")
        globals.lSock.close()
        if (globals.logfile):
            globals.logfile.close()

        while (True):
            if not globals.AQueue_reply.empty():
                rep = globals.AQueue_reply.get(block=False)
                if (rep == "shutdown_ok"):
                    break


if __name__ == "__main__":
    start()
