
nodePort=$1

# Start DLT Node
timeout 50 scripts/startDLTNode.sh $nodePort

sleep 8

numClients=2
interval="50" # send a transaction per 50ms

# Start DLT Clients
timeout 30 scripts/startDLTClient.sh $numClients $nodePort $interval