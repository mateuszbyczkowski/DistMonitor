# Distributed Monitor
Application written within NPR subject.

## Introduction
This application simlates standard Monitor object for distributed environment.
Using MPJ (MPI for Java) as communication tool and Gradle.

Algorithm used for model distributed mutex: Suzuki-Kasami.

## Description
The general idea of this project was to implement Monitor abstraction which lets any developer implement it and use for his own purpouse.

The concept is simple. Developer extends InvalidMonitor class and extends Data class which lets him adjust existing mechanisms for his needs. Model example is producer and consument problem without active waiting and with endless buffer. 

There are implemented few usefull functions like distributed wait and notify/notifyAll, acquireCriticalSection, releaseCriticalSection and few methods for Suzuki-Kasami token passing needs. 

Token starts in process 0, the only process which has token is able to enter to critical section. Token is passing to processes which are requesting it, but after current token owner critical section exit. If current owner exit critical section he passes token to process which has highest request number (requested for token the most times) if there are few equals request numbers token is passed to first process in array which has biggest request number.

Project is not finished which is caused by problems with algorithm and MPJ usage.

## Usage
Create object of class InvalidMonitor by ```InvalidMonitor monitor = new InvalidMonitor(args);``` (or extend it).

```args``` is needed for MPJ tool purpouse like number of machines/processes which you can specify in argument line.

You can call ```monitor.lock();``` and ```monitor.unlock();``` on monitor object.

You can also extend ```Data``` class which let you implement your own ```serialize``` and ```deserialize``` methods.
 Both of them are working on byte arrays.

## Author
Mateusz Byczkowski




