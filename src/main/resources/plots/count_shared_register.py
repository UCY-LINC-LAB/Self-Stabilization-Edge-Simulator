import numpy as np
import pandas as pd
import matplotlib as mpl
import matplotlib.pyplot as plt
import io
import base64
import sys
#See https://matplotlib.org/3.1.0/users/dflt_style_changes.html
plt.style.use('seaborn-ticks')
mpl.rcParams['grid.color'] = 'grey'
mpl.rcParams['grid.linestyle'] = ':'
mpl.rcParams['grid.linewidth'] = 0.5
mpl.rcParams['axes.spines.right'] = False
mpl.rcParams['axes.spines.top'] = False
mpl.rcParams['font.size'] = 12
mpl.rcParams['legend.fontsize'] = 'medium'
mpl.rcParams['figure.titlesize'] = 'large'


def plot_line(df,ax):
    g = sns.relplot(x="time", y="value", kind="line", data=df, ax=ax)
    plt.close(g.fig)
    #g.set_axis_labels("Time (s)", "Value")
    #g.fig.autofmt_xdate()
    #return g.fig


def build_graph(f):
    img = io.BytesIO()
    f.set_size_inches(11.7, 8.27)
    f.savefig(img, format='png')
    img.seek(0)
    graph_url = base64.b64encode(img.getvalue()).decode()
    return graph_url
    #return 'data:image/png;base64,{}'.format(graph_url)

if __name__ == '__main__':

    df = pd.read_csv(sys.stdin)


        # Create a figure instance, and the two subplots
    fig,(ax1,ax2) = plt.subplots(2,1,figsize=(16,12),sharex=False)
    ax1.plot(df['time'],df['msgs'],marker='o',markersize=5,alpha=1,label="")
    #ax1.plot(df['time'],df['data_msgs'],marker='o',markersize=5,alpha=1,label="Data Plane")
    #ax1.stackplot(df['time'],df['control_msgs'],df['data_msgs'],alpha=0.85,labels=("Control Plane","Data PLane"))
    #ax1.fill_between(x=df['time'],y1=df['msgs'],y2=0,alpha=0.75,label="Control Plane")
    #ax1.plot(df['time'],df['control_msgs'],marker='o',markersize=3,alpha=1)
    #ax1.fill_between(x=df['time'],y1=df['data_msgs'],y2=0,alpha=0.65,label="Data Plane")
    #ax1.plot(df['time'],df['data_msgs'],marker='o',markersize=3,alpha=1)
    #ax1.legend()
    ax1.set_title('Shared Register: Messages Transmitted')
    ax1.set_ylabel('# Msgs')
    ax1.grid()

    ax2.plot(df['time'],df['msgs'].cumsum(),alpha=1,label="")
    #ax2.plot(df['time'],df['data_msgs'].cumsum(),alpha=1,label="Data Plane")
    #ax2.legend()
    ax2.grid()
    ax2.set_title('CDF of messages transmitted')
    ax2.set_xlabel('Simulation Step')
    print(build_graph(fig))

