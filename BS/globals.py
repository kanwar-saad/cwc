ServerPort = 36965
ServerDataPort = 36975
minCLEnergyLevel = 30
CLSelectionPeriod = 60
CLSelectTimeout = 30

data_chunk_size = 0
bsSock = None
cnMap = {}
activeCL = None
CLSelectionInProgress = False
dataTxInProgress = False

CLSelectState = {"IDLE":1, "WAITING":2}
CLSelState = CLSelectState['IDLE']
