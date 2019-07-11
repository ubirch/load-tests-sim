# load-tests-sim

## Purpose

The purpose of this project is to create/assemble the basic tools to be able to test the Niomon-Eventlog loop.

## Workflow

The following image provides a suggested workflow for the load-test tools created.

![Load Test Workflow](https://raw.githubusercontent.com/ubirch/load-tests-sim/master/images/load-tests-wf.png "Load Test Workflow") 

## Components

The project is comprised of two kinds of groups:

1. Device Generator: A tool that allows to creates one or more devices. 

* **com.ubirch.DeviceGenerator**
    
2. Key Registration: A tool that allows to register the public keys of the devices. The keys are automatically created with the device generation. 

* **com.ubirch.KeyRegistration**

3. The Data Generation: A tool that allows to generate the data packages for the devices that were created.
    
* **com.ubirch.DataGenerator** 

4. The Simulations: A collection of Gatling Simulations that allow to simulate/test the sending of the data package based on different configuration values.

* **SendUPPAtOnceUserSimulation**: It is a simulation that injects a fixed number of users all at once. 
* **SendUPPConstantUsersWithThrottleSimulation**: It is a simulation that sets a constant number of users to be inserted during a fixed period of time.
Additionally, the throughput is possible to be configured. 
* **SendUPPRampUsersSimulation**: It is a simulation that injects a growing number of users over a period of time.


## How to run the workflow.

The general workflow is:

1. Create and register your device(s) on Cumulocity.

Follow the instructions here <https://github.com/ubirch/ubirch-client-java> and here <https://github.com/ubirch/ubirch-client-java/blob/master/HOWTO.md>

2. To generate the data -UPPS- you will use to run your simulations.

The tool that is in charge of this is:

*com.ubirch.Generator*

3. At the time of this writing, there are three simulations:

* **SendUPPAtOnceUserSimulation**: It is a simulation that injects a fixed number of users all at once. 
* **SendUPPConstantUsersWithThrottleSimulation**: It is a simulation that sets a constant number of users to be inserted during a fixed period of time.
Additionally, the throughput is possible to be configured. 
* **SendUPPRampUsersSimulation**: It is a simulation that injects a growing number of users over a period of time.


In order to star the simulations, you have two options, you either run all the simulations or you select which one you would like to start first.
I would recommend that latter as you are able to observe a little better the results of the simulations.

To run all commands:

```shell

mvn gatling:test

```

To run a specific simulation, run the following command:

```shell

mvn gatling:test -Dgatling.simulationClass=com.ubirch.simulations.SendUPPConstantUsersWithThrottleSimulation

```

At the end of each simulation there's a url you can use to open a report on your browser.

*Notes*: It is recommended that if you modify your simulations, you also run mvn clean compile, as it is sometime possible that 
some old version be there. 

You can play with the principal values in the application.conf.

An important place where to find info is, of course, <https://gatling.io/docs/2.3/>



