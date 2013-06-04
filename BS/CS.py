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
cs_data_port = 4446 
csSock = None
data_chunk_size = 1024

def get_next_chunk(src, size):
            data = src.read(size)
            return data 

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
        src_file = open('image.jpg', 'rb')
        

        msg = {}
        msg['msgType'] = 'DATA_TRIGGER'
        msg['chunk_size'] = data_chunk_size
        msg['data_port'] = cs_data_port
        msg['metadata'] = {}
        msg['metadata']['name'] = 'Stream1.jpg'
        msg['metadata']['mime'] = 'file'


        json_msg = json.dumps(msg)

        # Start TCp server
        
        host="127.0.0.1"                # Set the server address to variable host
        s=socket(AF_INET, SOCK_STREAM)
        s.bind((host,cs_data_port))                 # Binds the socket. Note that the input to 
                                                    # the bind function is a tuple
        csSock.sendto(json_msg, ('127.0.0.1', globals.ServerPort))

        s.listen(1)

        print "Waiting for connection"

        q, addr = s.accept()
        print "Connection Established"

        chunk = ""
        iterations = 0
        while True:
            iterations += 1
            chunk = get_next_chunk(src_file, data_chunk_size)
            q.send(chunk) 
            if (len(chunk) < data_chunk_size):
                break

        src_file.close()

        print "Iterations = ", str(iterations)

        #jsondata, addr = csSock.recvfrom(1024)

        #print jsondata

        #data = json.loads(jsondata)
        
        #if data['msgType'] == 'DATA_TRIGGER_OK':
        #    print "Permission to start Data TX"

    except BaseException, e:
        print e
        print "\nException Occurred ... Exiting"
        csSock.close()

    csSock.close()


if __name__ == "__main__":
    start()
