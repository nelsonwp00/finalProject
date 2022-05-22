echo "Number of Client = $1"

dir="target"
jarName="dlt_client.jar"
nodeIP="127.0.0.1"

numClients=$1
nodePort=$2
interval=$3

logDir="log_dlt_client"
mkdir -p $logDir

for ((i=1; i<=$numClients; i+=1)); do
    echo "Starting DLT Client $i ... "
    logFile="$logDir/log${i}"

    java -jar $dir/$jarName $nodeIP $nodePort $interval > $logFile 2>&1 &
done
wait