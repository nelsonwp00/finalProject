dir="target"
jarName="client.jar"
cluster="trading"
serverIPs="127.0.0.1:8081,127.0.0.1:8082,127.0.0.1:8083"

operation=$1

if [ $operation == "CreateAccount" ]; then
  accountID=$2
  balance=$3
  java -jar $dir/$jarName $cluster $serverIPs $operation $accountID $balance
fi

if [ $operation == "SendPayment" ]; then
  fromAccountID=$2
  toAccountID=$3
  payment=$4
  java -jar $dir/$jarName $cluster $serverIPs $operation $fromAccountID $toAccountID $payment
fi

if [ $operation == "QueryAccount" ]; then
  accountID=$2
  java -jar $dir/$jarName $cluster $serverIPs $operation $accountID
fi
