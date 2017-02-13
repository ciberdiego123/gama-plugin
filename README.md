OpenMOLE GAMA plugin
====================

This repository contains the sources for the intergation of [GAMA](https://code.google.com/p/gama-platform/) in [OpenMOLE](http://www.openmole.org).

Licence
-------

This plugin is distributed under the GNU Affero GPLv3 software licence. 


Compilation
-----------

You must compile and install dev version of openmole first

You must install a version of [sbt](http://www.scala-sbt.org/) superior to 0.13. Then go to the repository and execute:

    sbt generateTask

Usage
-----

This plugin works with the developpement version of OpenMOLE 4.0. 

1. Due to a bug in the JVM you should first remove the -XX:+UseG1GC option in the launching script of OpenMOLE (unless you are using java 8).
2. Launch OpenMOLE with the gama plugin loaded: 

    ```./openmole -p path/to/your/plugin/bundles```
    
Change `path/to/your/plugin/` to the path where gama plugin `bundles/` is located

3. Execute a workflow launching a Gama model in OpenMOLE:

    ```scala
    // Declare the variable
    val number_of_preys = Var[Int]
    val nb_preys_init = Var[Int]
    
    // Gama task
    // The third argument of the GamaTask is the gama experiment name
    // and the fourth argument is the number of steps
    val gama = 
      GamaTask("/path/to/predatorPrey.gaml", "preyPred", 10) set (
        gamaInputs += nb_preys_init,
        gamaOutputs += number_of_preys 
      )
    
    val exploration = 
      ExplorationTask(
        nb_preys_init in (0 to 200 by 10)
      )
    
    exploration -< (gama hook ToStringHook())
    ```

