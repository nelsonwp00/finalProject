dir="target"
jarName="dlt_node.jar"
cluster="trading"
serverIPs="127.0.0.1:8081,127.0.0.1:8082,127.0.0.1:8083"
port=$1

logDir="log_dlt_node"
mkdir -p $logDir

logFile="$logDir/log"


echo "Starting DLT Node ... "
java -jar $dir/$jarName $cluster $serverIPs $port > $logFile 2>&1 &