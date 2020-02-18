About
-----
This Java implementation solves the [production planning problem](../problem_description.pdf) via a graph algorithmic approach based on enumerating directed cycles in a residual graph which represents all feasible schedules.

An outline of the approach can be found in [doc/graph-optimisation.pdf](doc/graph-optimisation.pdf).

Compilation & Execution
-----------------------

1. `cd graph-optimisation`
2. `mvn package`
3. `java -jar runner/target/graph-opt-jar-with-dependencies.jar --edges=value --iterations=value2 problem_file.txt`

To see available command line arguments execute: `java -jar runner/target/graph-opt-jar-with-dependencies.jar -h`