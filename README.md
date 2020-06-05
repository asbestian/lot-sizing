[![Build Status](https://travis-ci.org/asbestian/lot-sizing.svg?branch=master)](https://travis-ci.org/asbestian/lot-sizing)

About
-----
We consider the discrete, single-machine, multi-item, single-level [lot sizing problem](./doc/problem\_description.pdf). 
The problem is solved via a graph algorithmic approach based on enumerating directed cycles in a residual graph which 
represents all feasible schedules.

An outline of the approach can be found in [doc/graph-optimisation.pdf](./doc/graph-optimisation.pdf).

Further information is also available at [OptimizationHub](https://opthub.uniud.it/problem/lsp)

Folders
--------
[algorithm](./algorithm) - part of Java implementation

[doc](./doc) - contains further documentation

[graph](./graph) - part of Java implementation

[input](./input) - part of Java implementation

[instances](./instances) - contains instance files

[runner](./runner) - part of Java implementation

[visualisation](./visualisation) - part of Java implementation

Compilation & Execution
-----------------------

1. `mvn package`
2. `java -jar runner/target/graph-opt-jar-with-dependencies.jar problem_instance`

* To see all available command line arguments execute: `java -jar runner/target/graph-opt-jar-with-dependencies.jar -h`
* To change logging level to _debug_ add `-Dorg.slf4j.simpleLogger.defaultLogLevel=debug` to `java` command.
* The default logging output target is `System.err`. To change the target add `-Dorg.slf4j.simpleLogger.logFile=file` to `java` command.


Authors
-------
[Sebastian Schenker](https://github.com/asbestian)
