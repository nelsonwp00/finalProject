# Run Latency Test
./bin/spark-submit ./evaluation_latency.py NonStopCreateAccount
./bin/spark-submit ./evaluation_latency.py NonStopSendPayment
./bin/spark-submit ./evaluation_latency.py NonStopQueryAccount

# Run Throughput Test
./bin/spark-submit ./evaluation_throughput.py NonStopCreateAccount
./bin/spark-submit ./evaluation_throughput.py NonStopSendPayment
./bin/spark-submit ./evaluation_throughput.py NonStopQueryAccount