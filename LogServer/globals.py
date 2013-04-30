import Queue

## Configuraton
ServerPort = 36963
AnnouncePort = 36964
AnnounceTimeout = 10
logFileName = "Log.out"
screenOutput = True



# Internal
sysExit = False
AQueue = Queue.Queue()
AQueue_reply = Queue.Queue()
lSock = None
logfile = None
