echo "Operation = $1. Number of Clients = $2"

dir="target"
jarName="client.jar"
cluster="trading"
serverIPs="127.0.0.1:8081,127.0.0.1:8082,127.0.0.1:8083"
operation=$1
numClients=$2
interval=$3

logDir="log_client/n${numClients}-${1}"
mkdir -p $logDir

for ((i=1; i<=$numClients; i+=1)); do
    #echo "Starting Client $i"
    logFile="$logDir/log${i}"

    if [ -z "$interval" ]
    then
      java -jar $dir/$jarName $cluster $serverIPs $operation > $logFile 2>&1 &
    else
      java -jar $dir/$jarName $cluster $serverIPs $operation $interval > $logFile 2>&1 &
    fi
done
wait