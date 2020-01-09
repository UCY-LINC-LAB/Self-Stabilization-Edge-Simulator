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
|mode| Options for the execution of the simulator. See **mode**|
|processes| See **processes**|
|network| See **network**|
|properties| Specific properties of the algorithm. See **properties**|
|events| A list of events. See **events**|

#### mode
|Key|Description|
|---|---|
|gui| Enable/disable the graphical user interface|
|steps| How many steps should be executed.|
|progress_every| Report the simulator progress every X steps|

#### processes
TODO

#### network
TODO

#### properties
TODO

#### events
TODO


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
TODO ...

## Licence
The framework is open-sourced under the Apache 2.0 License base. The codebase of the framework is maintained by the authors for academic research and is therefore provided "as is".
