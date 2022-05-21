import sys
from operator import add

from pyspark.sql import SparkSession

def lineMapper(line):
    if "Success" in line:
        return ["Success"]
    
    return ['notUsed']   

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: wordcount <file>", file=sys.stderr)
        sys.exit(-1)

    spark = SparkSession\
        .builder\
        .appName("PythonWordCount")\
        .getOrCreate()
    
    operation = sys.argv[1]
    outputFile = open("evaluation_result.txt", "a")
    nodesExpansion = ['4', '8', '16']

    for n in nodesExpansion:
        numOperations = 0
        operationDuration = 10 #second

        for i in range(1, int(n) + 1):
            fileName = "log_client/n" + n + "-" + operation + "/log" + str(i)
            lines = spark.read.text(fileName).rdd.map(lambda r: r[0])
            counts = lines.flatMap(lambda x: lineMapper(x)).map(lambda x: (x, 1)).reduceByKey(add)
            output = counts.collect()
            output = sorted(output, key=lambda tup: tup[1], reverse=True)

            for (word, count) in output:
                if (word != "Success"): continue
                #print("%s: %i" % (word, count))
                numOperations += count

                #outputFile.write('(' + word + ', ' + str(count) + ')\n')

        averageThroughput = str(round(numOperations / operationDuration, 2))
        message = operation + " with "+ n + " Nodes : Average Throughput = " + averageThroughput + " operations / second\n"
        outputFile.write(message)

    outputFile.close()
    spark.stop()