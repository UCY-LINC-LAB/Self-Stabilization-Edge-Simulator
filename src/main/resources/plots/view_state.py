import numpy as np
import pandas as pd
import matplotlib as mpl
import matplotlib.pyplot as plt
import io
import base64
import sys
import argparse
#See https://matplotlib.org/3.1.0/users/dflt_style_changes.html
plt.style.use('seaborn-ticks')
mpl.rcParams['grid.color'] = 'grey'
mpl.rcParams['grid.linestyle'] = ':'
mpl.rcParams['grid.linewidth'] = 0.5
mpl.rcParams['axes.spines.right'] = False
mpl.rcParams['axes.spines.top'] = False
mpl.rcParams['font.size'] = 14
mpl.rcParams['legend.fontsize'] = 'medium'
mpl.rcParams['figure.titlesize'] = 'large'


def plot_line(df,ax):
    g = sns.relplot(x="time", y="value", kind="line", data=df, ax=ax)
    plt.close(g.fig)
    #g.set_axis_labels("Time (s)", "Value")
    #g.fig.autofmt_xdate()
    #return g.fig


def build_graph(f,export):
    if export:
        f.savefig(export, format='png')
        return
    img = io.BytesIO()
    f.set_size_inches(11.7, 8.27)
    f.savefig(img, format='png')
    img.seek(0)
    graph_url = base64.b64encode(img.getvalue()).decode()
    return graph_url
if __name__ == '__main__':

    parser = argparse.ArgumentParser()
    parser.add_argument("--export", help="Export the plot", default=None)
    args = parser.parse_args()

    df = pd.read_csv(sys.stdin)


        # Create a figure instance, and the two subplots
    fig,ax1 = plt.subplots(1,1,figsize=(16,12),sharex=False)
    df['time'] /= 1000
    ax1.plot(df['time'],df['value'],marker='o', linestyle = 'None', markersize=6, markerfacecolor='none',alpha=0.75,label="Calculated")
    ax1.plot(df['time'],df['real'] ,marker='x', linestyle = 'None', color="k",markersize=6,alpha=0.5,label="Real")
    ax1.vlines(df['time'], ymin=df['value'], ymax=df['real'],colors='r')
    #ax1.plot(df['time'],df['data_msgs'],marker='o',markersize=5,alpha=1,label="Data Plane")
    #ax1.stackplot(df['time'],df['control_msgs'],df['data_msgs'],alpha=0.85,labels=("Control Plane","Data PLane"))
    #ax1.fill_between(x=df['time'],y1=df['msgs'],y2=0,alpha=0.75,label="Control Plane")
    #ax1.plot(df['time'],df['control_msgs'],marker='o',markersize=3,alpha=1)
    #ax1.fill_between(x=df['time'],y1=df['data_msgs'],y2=0,alpha=0.65,label="Data Plane")
    #ax1.plot(df['time'],df['data_msgs'],marker='o',markersize=3,alpha=1)
    #ax1.legend()
    ax1.set_title('Accuracy over time')
    ax1.set_ylabel('Value')
    ax1.set_xlabel('Time (s)')
    ax1.legend(loc='best')
    ax1.grid()

    print(build_graph(fig,export=args.export))

