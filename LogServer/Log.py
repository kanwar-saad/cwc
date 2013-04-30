import sys
import json
import globals
from socket import *
import datetime

def LogListner():

    # Open Socket
    #try:

    globals.lSock = socket(AF_INET, SOCK_DGRAM)
    globals.lSock.bind(("0.0.0.0", globals.ServerPort))
    #except:
    #    print "Error in opening log Server Socket"
    #    raise BaseException()

    # OPen Log File
    if (globals.logFileName != ""):
        try:
            globals.logfile = open(globals.logFileName, 'w', 0)
            if not globals.logfile:
                print "Error in opening output log file :", logfileName
                raise BaseException()

        except:
            print "Error in opening output file :", logfileName
            raise BaseException()


    while True:
        jsonData, addr = globals.lSock.recvfrom(1024)
        data = json.loads(jsonData)
        log_str = getLogStr(data)
        if globals.logFileName != "":
            globals.logfile.write(log_str)

        if globals.screenOutput is True:
            print log_str



def getLogStr(msg):
    node_id = msg.get("id") if not None else "None"
    facility = msg.get("facility") if not None else "None"
    severity = msg.get("severity") if not None else "None"
    message = msg.get("message") if not None else "None"
    ts =  datetime.datetime.now()
    ts_str = ts.strftime("%H:%M:%S %d/%m/%Y")
    log_str = ""
    log_str += "Timestamp:  "+ ts_str + "\n"
    log_str += "ID:  " + node_id + "  |  Facility:  "+ facility + "  |  Severity:  "+ severity+ "\n"
    log_str += "Message :=  "+ message+ "\n"
    log_str += "========"+ "\n"

    return log_str
