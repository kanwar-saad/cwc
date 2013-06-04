
import sys
import time
import json
import string
import random
import globals
from threading import *
from socket import *

from node import *
from handler import *



def msgHandler(sock, data, address):
    ret = True
    print "Inside Message Handler"
    print data
    msgType = data.get('msgType')

    handler = { "CN_INFO": cn_info_handler,
            'BS_SELECT_CL_RESP': select_cl_resp_handler, 
            'CL_EXPIRE_BS': cl_expire_handler, 
            'DATA_TRIGGER': data_trigger_handler, 
            'CL_DATA_INIT_REJ': data_init_rej_handler, 
            'CL_DATA_INIT_HOLD': data_init_hold_handler, 
            'CL_DATA_INIT_OK': data_init_ok_handler, 
            'CL_DATA_TX_SUMMARY': data_tx_summary_handler, 
            'CL_DATA_TX': data_tx_handler} 


    try:
        handler[msgType](sock, data, address)
    except KeyError, e:
        print "Invalid msgType received : "+msgType
    except BaseException, e:
        print "====ERROR===="
        print e


    return ret



def cn_info_handler(sock, rxdata, address):
    data = {}
    data['id'] = rxdata.get('cnid')
    if (data.get('id') is None):
        print "Received Data has no ID"
        return

    data['peers'] = rxdata.get('neighbors')
    data['ip'] = address[0]
    data['port'] = address[1]
    
    battery_level = rxdata.get('battery_level')
    is_charging = rxdata.get('is_charging')
    #print battery_level
    #print is_charging
    if ((battery_level is not None) and (is_charging is not None)):
        energy = int(battery_level) + (int(is_charging) * 10)
    else:
        energy = 0
    data['energy'] = energy



    node = globals.cnMap.get(data['id'])
    if (node is None):
        data['role'] = "CN"
        node = Node(data=data)
        globals.cnMap[data['id']] = node
    else:
        print "Node data found.. updating."
        if (node.id == globals.activeCL):
            node.role = "CL"
        else:
            node.role = "CN"

        node.peers = data['peers']
        node.ip = data['ip']
        node.port = data['port']
        node.energy = data['energy']
    
    print "*** Updated List ***"
    for key, val in globals.cnMap.items():
        print val
    print "***              ***"


def cl_reliable_data_tx_thread(dsock, metadata, cs_address, cl_address, src_port, chunk_size):
    # Start TCP server
    t=socket(AF_INET, SOCK_STREAM)
    t.bind(("0.0.0.0",src_port))                 # Binds the socket. Note that the input to 
                                                    # the bind function is a tuple
    t.listen(1)
    
    # Send BS_DATA_INIT_REQ to CL
    cl_msg = {}
    cl_msg['msgType'] = 'BS_DATA_INIT_REQ'
    cl_msg['chunk_size'] = globals.data_chunk_size
    cl_msg['data_port'] = src_port
    cl_msg['metadata'] = metadata
    
    cl_msg_json = json.dumps(cl_msg)
    dsock.sendto(cl_msg_json, cl_address)

    print "Waiting for CL to connect" 
    q, addr = t.accept()
    print "Connection Established"

    # Receive file from CS and send to CL
    dst_file = open(metadata['name'], 'wb')
    tbytes = 0
    s=socket(AF_INET, SOCK_STREAM)      # Creates a socket
    s.connect(cs_address)          # Connect to server address
    while True:   
        chunk = s.recv(chunk_size)
        dst_file.write(chunk)
        q.send(chunk)
        tbytes += len(chunk)
        if len(chunk) == 0:
            break;

    s.close()
    t.close()
    dst_file.close()
    
    print "bytes received =", str(tbytes) 
         
         

def data_trigger_handler(sock, rxdata, address):
    print "DATA_TRIGGER received"
    globals.data_chunk_size = rxdata['chunk_size']
    metadata = rxdata['metadata']
    cs_data_port = rxdata['data_port']
    if metadata['mime'] == 'file':
        # Start a thread for CL
        t = Thread(target=cl_reliable_data_tx_thread, args=(sock, metadata, (address[0], cs_data_port), (globals.activeCL.ip, globals.activeCL.port), globals.ServerDataPort , globals.data_chunk_size))
        t.start()
        # Connect to CS and receive file
         
    else:
        print "MIME not supported"


    # Send BS_DATA_INIT_REQ to CL
    #cl_msg = {}
    #cl_msg['msgType'] = 'BS_DATA_INIT_REQ'
    #cl_msg['chunk_size'] = globals.data_chunk_size
    #cl_msg['metadata'] = metadata
    
    #cl_msg_json = json.dumps(cl_msg)

    #if (globals.activeCL is not None):
    #    sock.sendto(cl_msg_json, (globals.activeCL.ip, globals.activeCL.port))
    #    # dummy response back to CS
    #    resp = {}
    #    resp['msgType'] = 'DATA_TRIGGER_OK'
    #else:
    #    print 'Data Trigger Failed - No Active CL'
    #    resp = {}
    #    resp['msgType'] = 'DATA_TRIGGER_REJECT'
    #
    #
    #    resp_json = json.dumps(resp)
    #    sock.sendto(resp_json, address) 

def select_cl_resp_handler(sock, rxdata, address):
    print "BS_SELECT_CL_RESP received"
    
    cnid = rxdata.get('cnid')
    cluster_id = rxdata.get('cluster_id')
    status = rxdata.get('status')

    if (cnid is None) or (cluster_id is None) or (status is None):
        print "Invalid BS_SELECT_CL_RESP received"
        return

    if (status != "ACCEPT")  and (status != "REJECT"):
        print "Invalid status in BS_SELECT_CL_RESP"
        return
    
    if (globals.activeCL is None):
        print "BS not waiting for BS_SELECT_CL_RESP"
        return
            
    if (cnid != globals.activeCL.id) or (cluster_id != globals.activeCL.cluster_id):
        print "Invalid values in BS_SELECT_CL_RESP"
        return
        
    if globals.CLSelState != globals.CLSelectState['WAITING']:
        print "BS not waiting for BS_SELECT_CL_RESP"
        return
        
    if (status == "REJECT"):
        print "CL Rejected. Retry CL Selection"
        CLSelect(retry=True)
        return

    
    if (status == "ACCEPT"):
        globals.CLSelState = globals.CLSelectState['IDLE']
        globals.CLSelectionInProgress = False
        return

    print "Unknown Error BS_SELECT_CL_RESP"



def cl_expire_handler(sock, rxdata, address):
    print "TODO"
def data_init_rej_handler(sock, rxdata, address):
    print "TODO"
def data_init_hold_handler(sock, rxdata, address):
    print "TODO"
def data_init_ok_handler(sock, rxdata, address):
    print "TODO"
def data_tx_summary_handler(sock, rxdata, address):
    print "TODO"
def data_tx_handler(sock, rxdata, address):
    print "TODO"


def CLSelectWrapper():
    CLSelect()
    Timer(globals.CLSelectionPeriod, CLSelectWrapper).start()

def CLSelect(retry=False):
    
    try:
        if retry == False:
            if (globals.dataTxInProgress is True):
                print "Data Tx In Progress ... Deferring CL Selection"
                return 
            if (globals.CLSelectionInProgress is True):
                print "CL Selection is in Progress .. returning"
                return
        if (len(globals.cnMap.items()) == 0):
            print "No CN present.. returning"
            return
        
        globals.CLSelectionInProgress = True
     
        selected_cl = None
        for key, val in globals.cnMap.items():
            node = val
            if selected_cl is None:
                selected_cl = node
            node.printData()
            
            if (node.energy > selected_cl.energy):
                selected_cl = node

        if (selected_cl.energy < globals.minCLEnergyLevel):
            globals.activeCL = None
            globals.CLSelectionInProgress = False
            print "No CN eligible for CL .. returning"
            raise BaseException
            return
        
        # reset previous CL *if any(
        if globals.activeCL is not None:
            globals.activeCL.role = "CN"
            globals.activeCL.cluster_id = None
            

        globals.activeCL = selected_cl
        print "Selected Cl detials"
        selected_cl.printData()
        send_cl_select_req()
    except:
        print "Exiting Cl select.. restarting timer"
        globals.CLSelectionInProgress = False
        return
    
    return

def random_str_generator(size=6, chars=string.ascii_uppercase + string.digits + string.ascii_lowercase):
        return ''.join(random.choice(chars) for x in range(size))

def gen_cluster_id():
    return random_str_generator()
    

def send_cl_select_req():
    try:
        data = {}
        data['cluster_id'] = gen_cluster_id()
        data['msgType'] = 'BS_SELECT_CL_REQ'
        data['cnid'] = globals.activeCL.id

        jsonData = json.dumps(data)

        globals.activeCL.cluster_id = data['cluster_id']
        globals.CLSelState = globals.CLSelectState['WAITING']
         
        Timer(globals.CLSelectTimeout, cl_select_callback).start()
        
        globals.bsSock.sendto(jsonData, (globals.activeCL.ip, globals.activeCL.port))
    except BaseException, e:
        print e

def cl_select_callback():
    if (globals.CLSelState ==globals.CLSelectState['WAITING']):
       globals.CLSelState = globals.CLSelectState['IDLE']
       CLSelect(retry=True) 

    else:
        globals.CLSelState = globals.CLSelectState['IDLE']
       

