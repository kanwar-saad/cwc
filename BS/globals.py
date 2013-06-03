ServerPort = 36965
minCLEnergyLevel = 30
CLSelectionPeriod = 60
CLSelectTimeout = 30


bsSock = None
cnMap = {}
activeCL = None
CLSelectionInProgress = False
dataTxInProgress = False

CLSelectState = {"IDLE":1, "WAITING":2}
CLSelState = CLSelectState['IDLE']
