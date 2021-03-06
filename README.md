# Load Test Driver

## Purpose

The purpose of this project is to create/assemble the basic tools to be able to test the Niomon-Eventlog loop.

In general the tools has two options for sending data, 1) based on a data previously created and 2) data generated on the fly.

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
* **SendUPPAproxRateSecondsSimulation**: It is a simulation that simulates a number of requests per second over a fixed duration.
* **VerifyUPPAtOnceUserSimulation**: It is a simulation that verifies the data that was sent injecting a fixed number of users all at once.  
* **VerifyUPPRampUsersSimulation**: It is a simulation that verifies the data that was sent injecting a growing number of users over a period of time.
* **SendUPPAproxRateSecondsSimulationContinuous**: It is a simulation that sends data based on steps. Very good for gradual injection.
* **VerifyUPPAproxRateSecondsSimulationContinuous**: It is a simulation that verifies the data that was sent injecting data in steps.


## How to run the workflow.

0. It is important that you select the proper values for your tools:

You can do this by looking at: *src/main/resources/application.base.conf*

It is also important that the anchoring systems have sufficient funds, so that a valid verification can be performed. 

1. Compile and package the project.

First of all, compile the project, by running the following command.

```shell
mvn clean compile test-compile
```

This command will clean all possible existing compiled  resources and compile both normal classes and tests -simulations-.

The previous command helps in making sure the system compiles, now we need to package it:

```shell
mvn package
```

If all goes well, we should be able to start our tools.

2. Create and register your device(s) on Cumulocity.

    2.1 You need to be logged in on <https://ubirch.cumulocity.com/>. Once logged in, you should visit 
     <https://ubirch.cumulocity.com/apps/devicemanagement/index.html#/deviceregistration>
     
    2.2 Run "com.ubirch.DeviceGenerator" by running the following command:
    
    ```shell
        java -cp target/ubirch-load-test-1.0.0.jar com.ubirch.DeviceGenerator 
    ```
    
    This will start a process to guide you through the device registration on Cumulocity.
    
    **Note**: There is a config key *deviceGenerator.runKeyRegistration* that allows you to run the key registration step when creating the devices themselves.
    This is very useful for when you just want to register the keys for you device as part of the same device generation process.
    
3. Register the public keys for your brand-new devices. To do so, run the following command:

    ```shell
        java -cp target/ubirch-load-test-1.0.0.jar com.ubirch.KeyRegistration 
    ```
    
    If the registration for a particular key succeeds, you should see a log statement like this:
    
    "Status Response: 200"

3. To generate the data -UPPS- you will use to run your simulations. To run this generation, run the following command:

    ```shell
        java -cp target/ubirch-load-test-1.0.0.jar com.ubirch.DataGenerator 
    ```

4. Run your simulations 

At the time of this writing, there are three simulations:

* **SendUPPAtOnceUserSimulation**: It is a simulation that injects a fixed number of users all at once. 
* **SendUPPConstantUsersWithThrottleSimulation**: It is a simulation that sets a constant number of users to be inserted during a fixed period of time.
Additionally, the throughput is possible to be configured. 
* **SendUPPRampUsersSimulation**: It is a simulation that injects a growing number of users over a period of time.
* **SendUPPAproxRateSecondsSimulation**: It is a simulation that simulates a number of requests per second over a fixed duration.
* **VerifyUPPAtOnceUserSimulation**: It is a simulation that verifies the data that was sent injecting a fixed number of users all at once.  
* **VerifyUPPRampUsersSimulation**: It is a simulation that verifies the data that was sent injecting a growing number of users over a period of time.
* **SendUPPAproxRateSecondsSimulationContinuous**: It is a simulation that sends data based on steps. Very good for gradual injection.
* **VerifyUPPAproxRateSecondsSimulationContinuous**: It is a simulation that verifies the data that was sent injecting data in steps.

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

## Notes

1. It is recommended that if you modify your simulations, you also run mvn clean compile, as it is sometime possible that 
some old version be there. 

2. You can play with the principal values in the application.conf.

3. An important place where to find info is, of course, <https://gatling.io/docs/2.3/>

4. Be aware that if data is not overwritten but appended if files already exist

5. Note that if you sent data in a random/shuffled way, it is likely that the verification will fail, it is recommended in this case to either: 
Shuffle/randomize the data files but having them previously stored or keep "queue" as sending ordering.
   
## Go-Client Certify Test

### Device Registration

We want to register device on the console only and not create keys. Please note the
deviceGenerator.runKeyRegistration key in /src/main/resources/application.go.conf
be false. If true, it will create keys and interrupt the Go-Client Warmup

```shell
java -cp target/ubirch-load-test-1.0.0.jar com.ubirch.DeviceGenerator 
```

### Data Transformation

We want to transform the format the DeviceGenerator creates and create a file that will be loaded in the Go-Client/Certifier

TODO

### Run Test

```shell
mvn gatling:test -Dgatling.simulationClass=com.ubirch.simulations.SendHashAproxRateSecondsSimulationContinuous  -Dconfig.file=./src/main/resources/application.go.conf
```
