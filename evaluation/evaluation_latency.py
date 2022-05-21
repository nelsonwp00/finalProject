import sys
from operator import add

from pyspark.sql import SparkSession

def lineMapper(line):
    words = line.split('=')

    if (len(words) > 1 and "ms" in words[1]):
        latency = words[1][1:]
        words = [latency]
    else:
        words = ["notUsed"]
    
    #print("words = ", words)

    return words   

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
        totalTimeUsed = 0
        averageLatency = 0

        for i in range(1, int(n) + 1):
            fileName = "log_client/n" + n + "-" + operation + "/log" + str(i)
            lines = spark.read.text(fileName).rdd.map(lambda r: r[0])
            counts = lines.flatMap(lambda x: lineMapper(x)).map(lambda x: (x, 1)).reduceByKey(add)
            output = counts.collect()
            output = sorted(output, key=lambda tup: tup[1], reverse=True)

            for (word, count) in output:
                #print("%s: %i" % (word, count))
                if (word == "notUsed"): continue

                #outputFile.write('(' + word + ', ' + str(count) + ')\n')
                index = word.find("ms")

                totalTimeUsed += int(word[0:index]) * count
                numOperations += count
            
            #print ("finished " + fileName)
        
        averageLatency = str(round(totalTimeUsed / numOperations, 2))
        message = operation + " with "+ n + " Nodes : Average Latency = " + averageLatency + " ms\n"
        outputFile.write(message)

    outputFile.close()
    spark.stop()