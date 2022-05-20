echo "Operation = CreateAccount. Number of Accounts = $1"

dir="target"
jarName="client.jar"
cluster="trading"
serverIPs="127.0.0.1:8081,127.0.0.1:8082,127.0.0.1:8083"
operation="CreateAccount"
numAccount=$1
accountBalance="1000"

logDir="log_client/createAccounts"
mkdir -p $logDir

for ((i=1; i<=$numAccount; i+=1)); do
    logFile="$logDir/log${i}"
    accountID="acc$i"

    echo "Creating Account $accountID"

    java -jar $dir/$jarName $cluster $serverIPs $operation $accountID $accountBalance > $logFile 2>&1 &
done
wait