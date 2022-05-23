dir="target"
jarName="server.jar"
cluster="trading"
serverIPs="127.0.0.1:8081,127.0.0.1:8082,127.0.0.1:8083"

logDir="log_server"
mkdir -p $logDir

for ((i=1; i<=3; i+=1)); do
    outputFile="tmp/server$i"
    IP="127.0.0.1:808$i"
    logFile="$logDir/log$i"
    echo "Starting Server $i. IP = $IP"
    java -jar $dir/$jarName $outputFile $cluster $IP $serverIPs > $logFile 2>&1 &
done
wait