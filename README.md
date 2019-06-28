# load-tests-sim

The purpose of this project is to create/assemble the basic tools to be able to test the Niomon-Eventlog loop

The project is comprised of two kinds of groups:

1. The Data Generation
2. The Simulations

The general workflow is:

1. Create and register your device on Cumulocity.

Follow the instructions here https://github.com/ubirch/ubirch-client-java and here https://github.com/ubirch/ubirch-client-java/blob/master/HOWTO.md

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

An important place where to find info is, of course, https://gatling.io/docs/2.3/



