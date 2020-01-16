# Distributed Systems Simulator for the Edge
This java application is a discrete-event simulator for the edge computing paradigm. It allows
the user to model and evaluate distributed algorithms by describing an edge computing infrastructure along with different 
types of failures. For this, the user defines a scenario that specifies how processes are connected, 
what characteristics they have (e.g., network latency, bandwidth, etc.) and what external events may happen (e.g., fail a process, packet loss).

## Getting Started

### Build the project
The project uses the maven tool to build source files into a binary executable. 
To do so, make sure that Maven and the Java JDK 8 is installed.
To build the source files use the following command:
```bash
mnv package
```
### Run the project
To run the executable type the following command:
```bash
java -jar target/DSSimulator-1.0.jar 
# or run with one parameter: the name of the scenario 
java -jar target/DSSimulator-1.0.jar  small
```
The simulator will run the default scenario (*small* which is located in *scenarios/* directory). 
The default scenario topology contains the following:

* 1 cloud with uplink and downlink bandwidth of 100 msgs per step.
* 4 cloudlets. 2 located in north area and 2 in central area. The links between the cloudlets in the same area have ~10ms latency, while
the latency between two cloudlets in different area is ~100ms.
* 20 iots. 10 located in the north area and 10 in the central area.

The rest scenarios have the same setup, except that they describe different types of failure(e.g., fail cloudlets, fail leader, etc).
For more information about the scenario description see Section [Simulator Model](#Simulator-Model)

### Simulator Model
The input of the simulator is a yaml file. Below we provide a description of the properties a user must define.  

|Key|Description|
|---|---|
|root|The root directory to store the results|
|name|The name of the experiment|
|random_seed| A positive integer for experiment reproducibility|
|mode| Options for the execution of the simulator. See [mode](#mode)|
|processes| See [processes](#processes)|
|network| See [network](#network)|
|properties| Specific properties of the algorithm. See [properties](#properties)|
|events| A list of events. See [events](#events)|

#### mode
This part of the scenario defines the configuration of simulator. Each configuration key is explained in the following table:

|Key|Description|
|---|---|
|gui| Enable/disable the graphical user interface|
|steps| How many steps should be executed.|
|progress_every| Report the simulator progress every X steps|
|statistics_every| Report the monitored statistics progress every X steps|
|statistics_after| Start collecting statistics after X steps|
|plot_every| Plot the default graphs after X steps|
|logsEnabled| Enable/disable the logs from each component|
|trace.links| Enable/disable the logs from the links|
|logs| A list of components to log. *e.g., dsslib.components.selfstabilization.SSIoTModule*|
|trace_events| A list of events to trace. *e.g., dsslib.components.networking.NetworkModule$SendMsg*|
|statistics| A list of statistics to monitor. Currently available: *network*, *aggregateState*, *selfStabilization*|

#### processes
This section allows the user to configure how many cloudlets and iot should be spawned, in which zones,their speed, the network characteristics
 and which modules each process will contain. 

|Key|Description|
|---|---|
|cloud.speed| Describe how fast the cloud is. A value of 1 is the maximum speed. A small value indicates a slower process.|
|cloud.modules| A list of modules within the cloud. (see [module](#module))|
|cloudlets.speed| Describe how fast the cloudlets are.|
|cloudlets.link_to_other_cloudlets| A string id representing the type of network (see [Network](#network))|
|cloudlets.zones| A list of zones that cloudlets will be instantiated (see [zone](#zone))|
|cloudlets.modules| A list of modules within each cloudlet. (see [module](#module))|
|iots.speed| Describe how fast the iots are.|
|cloudlets.zones| A list of zones that iots will be instantiated (see [zone](#zone))|
|cloudlets.modules| A list of modules within each iot. (see [module](#module))|

#### zone
A zone describes an area in which processes are spawned and connected.

|Key|Description|
|---|---|
|zone| A unique name for the zone|
|count| The number of processes that will be spawned|
|links.cloudlets| A string id representing the type of network in which the processes will be connected with other cloudlets (see [Network](#network))|
|links.cloud| A string id representing the type of network in which the processes will be connected with the cloud (see [Network](#network))|

#### module
A module is the interface of a well-defined algorithm. For example the NetworkModule is responsible for transmitting and
receiving messages. This functionality can be implemented with many ways, and therefore, in our example we extend the NetworkModule and 
create a *component* that realises the functionality of the network module. In the following table we describe how this 
mapping can be defined.

|Key|Description|
|---|---|
|module| The name of the interface module (e.g., *dsslib.components.networking.NetworkModule*). |
|implementation| The name of the class that implements the aforementioned  module (e.g., *dsslib.components.networking.NetworkComponent*). |
|params| A key-value map of parameters for the module |

#### network
The following describe the network characteristics. Note, that the user may create as many network types 
as he/she wants, only by specifying a different *network_id*.

|Key|Description|
|---|---|
|*network_id*.speed.type| Currently, the only type of network speed supported is *gaussian*|
|*network_id*.speed.props.mean| The mean of the gaussian distribution| 
|*network_id*.speed.props.sdev| The standard deviation of the gaussian distribution| 
|*network_id*.downstreamBandwidth| The capacity of messages on the downstream link|
|*network_id*.upstreamBandwidth| The capacity of messages on the upstream link|

#### properties
These type of properties are specific to the self-stabilization algorithm.

|Key|Description|
|---|---|
|guards| The number of cloudlets that will be assigned as guards in the scenario |
|aggregate| The analytic function that will be executed in the cloudlets after receiving the raw metrics from IoT devices|
|records| The number of records in each message sent by an IoT device|
|recordSize| The size of each record in bytes| 


#### events
The last part of the scenario is the *events*. Here the user can denote the time that an event should happen.
In the following example we describe the currently supported events.

##### Enable processes
```yaml
- at: 1
  type: ENABLE_ALL_RANDOM
  from: 1
  to: 10000
```
This example enables all the processes(i.e., cloud,iots,links,cloudlets) randomly in within the first 10,000 steps.


##### Fail the leader
```yaml
- at: 15000
  type: FAIL_LEADER
```
This example fail-stop the leader process at the 15,000th step.

##### Fail the guards
```yaml
- at: 30000
  type: FAIL_GUARDS
  count: 2
```
This example randomly fail-stops two guards at the 30,000th step.

##### Fail random cloudlets
```yaml
- at: 45000
  type: FAIL_RANDOM_CLOUDLETS_ONLY
  count: 3
```
This example randomly fail-stops 3 cloudlets (that are not guards or leader) at the 45,000th step.

##### Fail random Iots
```yaml
- at: 50000
  type: FAIL_IOTS_ONLY
  count: 5
```
This example randomly fail-stops 5 iot  at the 50,000th step.


##### Fail random iot links
```yaml
- at: 60000
  type: FAIL_LINKS_IOT_TO_CLOUDLET
  count: 1
  from: 0
  to: 10000
  regions:
    - center
```
This example randomly fail-stops 1 link between an ioT and a cloudlet from the region *center*. The failure will
 happen at random between the 60,000th and 70,000th step.
 

## How it works?
The main component of the simulator is the scheduler. 
The scheduler is responsible for allocating execution steps to all processes. 
Each process has its own notion of time, depending on its speed. 
In this simulator we assume that a single step is 1 millisecond.

![image](https://github.com/UCY-LINC-LAB/Self-Stabilization-Edge-Simulator/blob/master/docs/scheduler.png)

The simulated scenario contains processes. A process consists of a component stack. 
The components are the software. Each process can be connected via links.
In each step a process receives all the messages from its input buffer, executes the state automata of each module, and finally, 
it sends the messages on its local output buffer. Component within the same process interact via events. 

![image](https://github.com/UCY-LINC-LAB/Self-Stabilization-Edge-Simulator/blob/master/docs/overview.png)

## Reference
When using the framework please use the following reference to cite our work: 
TBD

## Licence
The framework is open-sourced under the Apache 2.0 License base. The codebase of the framework is maintained by the authors for academic research and is therefore provided "as is".
