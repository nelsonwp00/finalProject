nodesExpansion=('4' '8' '16')
operations=('NonStopSendPayment' 'NonStopQueryAccount' 'NonStopCreateAccount')
operationDuration='10' #second
sleepDuration='5' #second
script='scripts/startClient.sh'

# set up 16 accounts for NonStopSendPayment test
timeout $operationDuration scripts/createAccounts.sh '16'; sleep $sleepDuration;

for operation in ${operations[@]}; do
  for numNode in ${nodesExpansion[@]}; do
    timeout $operationDuration scripts/startClient.sh $operation $numNode; sleep $sleepDuration;
  done
done