echo "Operation = $1. Number of Clients = $2. Interval = $3ms"

dir="target"
jarName="client.jar"
cluster="trading"
serverIPs="127.0.0.1:8081,127.0.0.1:8082,127.0.0.1:8083"
operation=$1
numClients=$2
interval=$3

logDir="log_client/n${numClients}-${1}"
mkdir -p $logDir

#java -jar $dir/$jar_name "trading" "127.0.0.1:8081,127.0.0.1:8082,127.0.0.1:8083" "NonStopCreateAccount" "500"

for ((i=1; i<=$numClients; i+=1)); do
    echo "Starting Client $i"
    logFile="$logDir/log${i}"
    java -jar $dir/$jarName $cluster $serverIPs $operation $interval > $logFile 2>&1 &
done
wait