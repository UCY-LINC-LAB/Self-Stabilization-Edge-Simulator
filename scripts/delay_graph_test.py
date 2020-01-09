import matplotlib as mpl
import matplotlib.pyplot as plt
import numpy as np
import os
import sys

plt.style.use('seaborn-ticks')
mpl.rcParams['grid.color'] = 'grey'
mpl.rcParams['grid.linestyle'] = ':'
mpl.rcParams['grid.linewidth'] = 0.5
mpl.rcParams['axes.spines.right'] = True
mpl.rcParams['axes.spines.top'] = True
mpl.rcParams['font.size'] = 16
mpl.rcParams['legend.fontsize'] = 'medium'
mpl.rcParams['figure.titlesize'] = 'large'


def compute(file, threshold, after=0, tolerance=1):
    start = 0
    prevV = 0
    latencies = []
    matching = True

    with open(file, 'r') as fp:
        for line in fp:
            if line.startswith("#"):
                continue
            if (line.startswith("t")):
                continue
            line = line.strip()
            toks = line.split(",")
            t = int(toks[0])
            if t < after:
                continue
            val = int(float(toks[2]))
            real = int(float(toks[3]))

            if abs(val - real) <= tolerance and not matching:
                matching = True
                latency = t - start
                if latency > threshold:
                    latency = threshold

                latencies.append(latency)
            elif abs(val - real) <= tolerance and abs(prevV - real) <= tolerance:
                ## OK
                pass
            elif abs(val - real) > tolerance and abs(prevV - real) > tolerance and matching:
                start = t
                matching = False
            prevV = real
        latency = t - start
        if not matching and (latency > threshold):
            latency = threshold
        if abs(val - real) > tolerance:
            latencies.append(latency)

    return np.array(latencies)


if __name__ == '__main__':

    root = os.getenv('RESULTS_ROOT',"../results/small")

    configs = {
        "No failure": "baseline",
        "4 IoT fail": "fail_iot",
        "Fail Guards": "fail_guards",
        "Fail Cloudlets": "fail_cloudlets",
    }
    time = "040000"
    after= 10000
    threshold= 5000
    accuracy=1

    all = []
    for key in configs:
        count = 0
        dir = os.path.abspath(os.path.join(root, configs[key]))
        experiments = os.listdir(dir)
        experiment = experiments[0]
        data = []
        for experiment in experiments:
            f = os.path.join(root, dir, experiment, 'stats/state/', time + ".csv")
            count += 1
            if not os.path.exists(f):
                print("\n\t[Run {}]  Not ready yet".format(count) + f + "\n")
                count -= 1
                continue
            print("\n\t[Run {}] ".format(count) + f + "\n")
            l = compute(f, threshold=threshold, after=after, tolerance=accuracy)
            if len(l) == 0:
                print("File: " + f + " has not values...")
            else:
                data.extend(l)
                mean = l.mean()
                std = l.std()
                #print(l, mean)
        all.append(data)

    fig = plt.figure(1, figsize=(16, 9))
    ax = fig.add_subplot(111)
    ax.set_ylabel('Information Delay (ms)')
    # ax.set_ylim([0,10000])
    ax.set_xlabel('Concurrently Failed Guards')
    ax.set_xticklabels(configs.keys())
    ax.grid()
    # fig.tight_layout()
    fig.align_ylabels(ax)

    # Create the boxplot
    bp = ax.boxplot(all)
    plt.show()
