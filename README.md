# DistMonitor
Application written within NPR subject.

 Project realization is in progress...
## Introduction
Using MPJ (MPI for Java) and Gradle build tool.

Algorithm used for model distributed mutex: Suzuki-Kasami.

## Usage
Create object of class InvalidMonitor by ```InvalidMonitor monitor = new InvalidMonitor(args);```

```args``` is needed for MPJ tool purpouse like number of machines/processes which you can specify in argument line.

You can call ```monitor.lock();``` and ```monitor.unlock();``` on monitor object.

You can also extend ```Data``` class which let you implement your own ```serialize``` and ```deserialize``` methods.
 Both of them are working on byte arrays.

## Author
Mateusz Byczkowski




