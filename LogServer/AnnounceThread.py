import threading
import json
import time
import globals
from socket import *
import Queue


class AnnounceThread (threading.Thread):
    def __init__(self):
        threading.Thread.__init__(self)
        self.daemon = True
    
    def run(self):
        print "Starting Announce Thread"
        self.s = socket(AF_INET, SOCK_DGRAM)
        self.s.bind(('', 0))
        self.s.setsockopt(SOL_SOCKET, SO_BROADCAST, 1)
        # Create Announce Packet
        data = {}
        data["type"] = "LogAnnounce"
        data["port"] = globals.ServerPort
        self.packet = json.dumps(data)
        # Starting Never Ending Loop
        exit = False
        while (exit == False):
            self.announce()

            # Sleep and check for shutdown from main thread
            for i in range(0, globals.AnnounceTimeout):
                time.sleep(1)
                try:
                    if (not globals.AQueue.empty()):
                        item = globals.AQueue.get(block=False)
                        if (item == "shutdown"):
                            exit = True
                            break
                except Queue.Empty, qe:
                    continue

        self.s.close()
        globals.AQueue_reply.put("shutdown_ok")


    def announce(self):
        self.s.sendto(self.packet, ('<broadcast>', globals.AnnouncePort))
