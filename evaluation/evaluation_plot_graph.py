NODES = [4, 8, 16]
FONT = 8
import matplotlib.pyplot as plt

def plot_throughput(x, y, z):
  plt.title("Throughput Evaluation Result")
  plt.xlabel(r"Nodes")
  plt.ylabel(r"Throughput per second")

  plt.plot(NODES, x, label = "Create Account")
  plt.plot(NODES, y, label = "Send Payment")
  plt.plot(NODES, z, label = "Query Account")
  
  plt.legend(fontsize = FONT)
  plt.savefig("throughput_graph")
  plt.clf()

def plot_latency(x, y, z):
  plt.title("Latency Evaluation Result")
  plt.xlabel(r"Nodes")
  plt.ylabel(r"Latency ms")

  plt.plot(NODES, x, label = "Create Account")
  plt.plot(NODES, y, label = "Send Payment")
  plt.plot(NODES, z, label = "Query Account")
  
  plt.legend(fontsize = FONT)
  plt.savefig("latency_graph")
  plt.clf()

if __name__ == '__main__':
  myFile = open('evaluation_result.txt', 'r')

  results = []

  lines = myFile.readlines()

  for line in lines:
    if ("Average Latency" in line):
      words = line.split("Average Latency = ")
      text = words[1]
      index = text.find(" ms")
      latency = text[0:index]
      results.append(float(latency))
    elif ("Average Throughput" in line):
      words = line.split("Average Throughput = ")
      text = words[1]
      index = text.find(" operations")
      throughput = text[0:index]
      results.append(float(throughput))

  NonStopCreateAccount_lat = [results[0], results[1], results[2]]
  NonStopSendPayment_lat = [results[3], results[4], results[5]]
  NonStopQueryAccount_lat = [results[6], results[7], results[8]]

  plot_latency(NonStopCreateAccount_lat, NonStopSendPayment_lat, NonStopQueryAccount_lat)

  NonStopCreateAccount_thr = [results[9], results[10], results[11]]
  NonStopSendPayment_thr = [results[12], results[13], results[14]]
  NonStopQueryAccount_thr = [results[15], results[16], results[17]]

  plot_throughput(NonStopCreateAccount_thr, NonStopSendPayment_thr, NonStopQueryAccount_thr)

  myFile.close()