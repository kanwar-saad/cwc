

class Node(object):

    def __init__(self, *args, **kwargs):

        data = kwargs.get('data')
        if (data is None):
            print "No data received for Node Creation"
            raise BaseException

        self.ip = data.get('ip')
        self.port = data.get('port')
        self.id = data.get('id')
        self.peers = data.get('peers')
        role = data.get('role')
        self.role = role if (role is not None) else "CN"
        self.energy = data.get('energy')
        self.cluster_id = None

 
    def __str__(self):
        obj_str = self.ip + ":" + str(self.port) + "--" + self.id + "--" + self.role
        return obj_str

    def printData(self):
        print "== Node Data =="
        print "IP       = " + self.ip
        print "Port     = " + str(self.port)
        print "ID       = " + self.id
        print "Energy % = " + str(self.energy)
        print "Role     = " + self.role
        print "Peers    = " + str(self.peers)
        print "==============="
