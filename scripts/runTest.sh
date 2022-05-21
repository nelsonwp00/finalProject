nodesExpansion=('4' '8' '16')
operations=('NonStopSendPayment' 'NonStopQueryAccount' 'NonStopCreateAccount')
operationDuration='10' #second
sleepDuration='5' #second
script='scripts/startClient.sh'

# set up 16 accounts for NonStopSendPayment test
timeout $operationDuration scripts/createAccounts.sh '16'; sleep $sleepDuration;

# the 5th argument is the time interval (ms) to call NonStopSendPayment API
# reason : back-to-back call
#timeout $operationDuration $script 'NonStopSendPayment' '4' '5'; sleep $sleepDuration;
#timeout $operationDuration $script 'NonStopSendPayment' '8' '10'; sleep $sleepDuration;
#timeout $operationDuration $script 'NonStopSendPayment' '16' '20'; sleep $sleepDuration;

for operation in ${operations[@]}; do
  for numNode in ${nodesExpansion[@]}; do
    timeout $operationDuration scripts/startClient.sh $operation $numNode; sleep $sleepDuration;
  done
done