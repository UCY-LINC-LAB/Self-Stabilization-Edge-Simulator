import numpy as np
import pandas as pd
import matplotlib as mpl
import matplotlib.pyplot as plt
import io
import base64
import os
import sys
import argparse

# See https://matplotlib.org/3.1.0/users/dflt_style_changes.html
plt.style.use('seaborn-ticks')
mpl.rcParams['grid.color'] = 'grey'
mpl.rcParams['grid.linestyle'] = ':'
mpl.rcParams['grid.linewidth'] = 0.5
mpl.rcParams['axes.spines.right'] = False
mpl.rcParams['axes.spines.top'] = False
mpl.rcParams['font.size'] = 15
mpl.rcParams['legend.fontsize'] = 'medium'
mpl.rcParams['figure.titlesize'] = 'large'


def build_graph(f, export):
    if export:
        f.savefig(export, format='png')
        return
    img = io.BytesIO()
    f.set_size_inches(11.7, 8.27)
    f.savefig(img, format='png')
    img.seek(0)
    graph_url = base64.b64encode(img.getvalue()).decode()
    return graph_url
    # return 'data:image/png;base64,{}'.format(graph_url)


def load_data(file, period):
    data = []
    last_time = 0
    partial = [0., 0., 0., 0.]
    with open(file, 'r') as fp:
        for line in fp:
            line = line.strip()
            if len(line) == 0:
                continue
            if line.startswith('time'):
                continue
            toks = line.split(',')
            t = int(toks[0])
            control_count = int(toks[1])
            control_size = int(toks[2])
            data_count = int(toks[5])
            data_size = int(toks[6])

            control_size *=(1000/period)
            data_size *=(1000/period)

            partial[0] += control_count
            partial[1] += control_size
            partial[2] += data_count
            partial[3] += data_size

            if t - last_time > period:
                last_time = t
                data.append([t, partial[0], partial[1]/1024, partial[2], partial[3]/1024])
                partial = [0., 0., 0., 0.]
    return np.array(data)

def compute_graph2(data):
    fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(16, 12), sharex=False)
    controlColor = 'xkcd:bright blue'
    dataColor = 'xkcd:light orange'
    # Time is in ms...
    df_time = data[:,0]/1000
    df_control_msgs_count = data[:,1]
    df_control_msgs_size = data[:,2]/1024
    df_data_msgs_count = data[:,3]
    df_data_msgs_size = data[:,4]/1024

    ax1.fill_between(x=df_time, y1=df_data_msgs_size, y2=0, color=dataColor, alpha=1, label="Data Plane")
    ax1.plot(df_time ,df_data_msgs_size, color=dataColor, marker='o', markersize=2, alpha=1, linewidth=1)

    ax1.fill_between(x=df_time, y1=df_control_msgs_size,y2=0, color=controlColor, alpha=0.55, label="Control Plane")
    ax1.plot(df_time,df_control_msgs_size, color=controlColor, marker='D', markersize=2, alpha=0.85, linewidth=1)

    ax1.legend()
    # ax1.set_title('Traffic Transmitted')
    ax1.set_ylabel('Network Traffic (MB/s)')
    ax1.set_xlabel('Time (s)')
    ax1.grid()
    # Now to MBs
    #df['control_msgs_sz'] /= 1024
    #df['data_msgs_sz'] /= 1024

    ax2.plot(df_time, df_data_msgs_size.cumsum(), color=dataColor, alpha=1, label="Data Plane")
    ax2.plot(df_time, df_control_msgs_size.cumsum(), color=controlColor, alpha=1, label="Control Plane")
    ax2.legend()
    ax2.grid()
    ax2.set_ylabel('Total Network Traffic (MB)')
    ax2.set_xlabel('Time (s)')
    return fig

if __name__ == '__main__':

    root = os.getenv('RESULTS_ROOT',"../results/small")
    scenario=os.getenv('SCENARIO',"all_failures")

    experiments = os.listdir(os.path.join(os.path.abspath(root),scenario))
    print("Existing experiments: "+str(experiments))
    experiment= experiments[0]
    print("Using experiment: "+str(experiment))
    file = "stats/network/msgs.csv"

    # In ms
    period = 200
    path = os.path.join(root,scenario,experiment, file)
    data = load_data(path, period)
    fig = compute_graph2(data)
    plt.show()

    #build_graph(fig, export=None)
