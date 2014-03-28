OpenMOLE GAMA plugin
====================

This repository contains the sources for the intergation of [GAMA](https://code.google.com/p/gama-platform/) in [OpenMOLE](http://www.openmole.org).

Licence
-------

This plugin is distributed under the GNU Affero GPLv3 software licence. 


Compilation
-----------

   ```sbt osgi-bundle```

Usage
-----

This plugin works with the developpement version of OpenMOLE, the future 1.0 release. 

1. Due to a bug in the JVM you should first remove the -XX:+UseG1GC option in the launching script of OpenMOLE (unless you are using java 8).
2. Launch OpenMOLE with the gama plugin loaded: 

    ```./openmole -c -p /path/to/openmole/gama/plugin/repo/target/scala-2.10/openmole-gama_2.10-1.0-SNAPSHOT.jar /path/to/openmole/gama/plugin/repo/bundles/```

3. Execute a workflow launching a Gama model in OpenMOLE:

    ```scala
    import org.openmole.plugin.domain.collection._
    import org.openmole.plugin.sampling.combine._
    import org.openmole.plugin.task.gama._
    import org.openmole.plugin.hook.display._
    
    // Declare the variable
    val number_of_preys = Prototype[Int]("number_of_preys")
    val nb_preys_init = Prototype[Int]("nb_preys_init")
    
    // Gama task
    val gama = GamaTask("hello", "/path/to/predatorPrey.gaml", "preyPred", 10)
    
    gama addGamaInput nb_preys_init
    gama addGamaOutput number_of_preys
    
    val exploration = 
      ExplorationTask("explo", Factor(nb_preys_init, 0 to 200 by 10 toDomain))
    
    val ex = exploration -< (gama hook ToStringHook()) toExecution
    
    ex.start 
    ```

