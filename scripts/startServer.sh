dir="target"
jarName="server.jar"
cluster="trading"
serverIPs="127.0.0.1:8081,127.0.0.1:8082,127.0.0.1:8083"

#java -jar $dir/$jar_name "trading" "127.0.0.1:8081,127.0.0.1:8082,127.0.0.1:8083" "NonStopCreateAccount" "500"
#/tmp/server1 trading 127.0.0.1:8081 127.0.0.1:8081,127.0.0.1:8082,127.0.0.1:8083
for ((i=1; i<=3; i+=1)); do
    outputFile="tmp/server$i"
    IP="127.0.0.1:808$i"
    logFile="log_server/log$i"
    echo "Starting Server $i. IP = $IP"
    java -jar $dir/$jarName $outputFile $cluster $IP $serverIPs > $logFile 2>&1 &
done
wait