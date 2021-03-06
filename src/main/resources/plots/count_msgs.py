import numpy as np
import pandas as pd
import matplotlib as mpl
import matplotlib.pyplot as plt
import io
import base64
import sys
import argparse

# See https://matplotlib.org/3.1.0/users/dflt_style_changes.html
plt.style.use('seaborn-ticks')
mpl.rcParams['grid.color'] = 'grey'
mpl.rcParams['grid.linestyle'] = ':'
mpl.rcParams['grid.linewidth'] = 0.5
mpl.rcParams['axes.spines.right'] = False
mpl.rcParams['axes.spines.top'] = False
mpl.rcParams['font.size'] = 12
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


def compute_graph(df):
    fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(16, 12), sharex=False)

    ax1.fill_between(x=df['time'], y1=df['control_msgs'], y2=0, alpha=0.75, label="Control Plane")
    ax1.plot(df['time'], df['control_msgs'], marker='D', markersize=2, alpha=1, linewidth=1)

    ax1.fill_between(x=df['time'], y1=df['sr_read'], y2=0, alpha=0.75, label="SharedR READ")
    ax1.plot(df['time'], df['sr_read'], marker='^', markersize=2, alpha=1, linewidth=1)

    ax1.fill_between(x=df['time'], y1=df['sr_write'], y2=0, alpha=0.75, label="SharedR WRITE")
    ax1.plot(df['time'], df['sr_write'], marker='v', markersize=2, alpha=1, linewidth=1)

    ax1.fill_between(x=df['time'], y1=df['health_checks'], y2=0, alpha=0.75, label="Health Checks")
    ax1.plot(df['time'], df['health_checks'], marker='s', markersize=2, alpha=1, linewidth=1)

    ax1.fill_between(x=df['time'], y1=df['data_msgs'], y2=0, alpha=0.65, label="Data Plane")
    ax1.plot(df['time'], df['data_msgs'], marker='o', markersize=2, alpha=1, linewidth=1)

    ax1.legend()
    ax1.set_title('Messages Transmitted')
    ax1.set_ylabel('# Msgs')
    ax1.grid()

    ax2.plot(df['time'], df['control_msgs'].cumsum(), alpha=1, label="Control Plane")
    ax2.plot(df['time'], df['sr_read'].cumsum(), alpha=1, label="SharedR Read")
    ax2.plot(df['time'], df['sr_write'].cumsum(), alpha=1, label="SharedR Write")
    ax2.plot(df['time'], df['health_checks'].cumsum(), alpha=1, label="Health Checks")
    ax2.plot(df['time'], df['data_msgs'].cumsum(), alpha=1, label="Data Plane")
    ax2.legend()
    ax2.grid()
    ax2.set_title('CDF of messages transmitted')
    ax2.set_xlabel('Simulation Step')
    return fig


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument("--export", help="Export the plot", default=None)
    args = parser.parse_args()

    df = pd.read_csv(sys.stdin)

    fig = compute_graph(df)

    build_graph(fig, export=args.export)
