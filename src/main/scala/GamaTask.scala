package org.openmole.plugin.task.gama

import msi.gama._
import msi.gama.headless.core.HeadlessSimulationLoader
import org.openmole.core.implementation.task._
import org.openmole.core.model.data.{ Prototype, Context }
import org.openmole.core.model.task._
import java.io.File
import msi.gama.headless.runtime.GamaSimulator
import msi.gama.kernel.experiment.ParametersSet

object GamaTask {
  def apply(name: String, gaml: File, experimentName: String, steps: Int)(implicit plugins: PluginSet) = new TaskBuilder { builder ⇒
    def toTask = new GamaTask(name, gaml, experimentName, steps) with builder.Built
  }

  lazy val preload = HeadlessSimulationLoader.preloadGAMA
}

abstract class GamaTask(val name: String, val gaml: File, val experimentName: String, val steps: Int) extends Task {

  override protected def process(context: Context): Context = {
    GamaTask.preload

    val model = HeadlessSimulationLoader.loadModel(gaml.getAbsolutePath)
    val simulator = HeadlessSimulationLoader.newHeadlessSimulation(model, experimentName, new ParametersSet())
    for {
      s <- 0 until steps
    } {
      val scope = simulator.getCurrentSimulation.getScope
      simulator.getCurrentSimulation.step(scope)
    }
    context
  }

  /*def call = {
    HeadlessSimulationLoader.preloadGAMA();
    Map<String, String[]> mm = context.getArguments();
    String[] args = mm.get("application.args");
    if ( !checkParameters(args) ) {
      System.exit(-1);
    }
    Reader in = new Reader(args[0]);
    in.parseXmlFile();
    int numSim = 1;
    if (args.length>2 && args[2] != null) {
      numSim = Cast.asInt(null, args[2]);
    }
    Iterator<Simulation> it = in.getSimulation().iterator();
    FakeApplication fa[] = new FakeApplication[50];
    int n = 0;
    while (it.hasNext()) {
      Simulation sim = it.next();
      for (int i = 0; i < numSim; i++) {
        Simulation si = new Simulation(sim);
        try {
          XMLWriter ou = new XMLWriter(Globals.OUTPUT_PATH + "/"
            + Globals.OUTPUT_FILENAME + i + ".xml");
          si.setBufferedWriter(ou);
          si.loadAndBuild();
        } catch (Exception e) {
          e.printStackTrace();
          System.exit(-1);
        }
        fa[i] = new FakeApplication(si);
        fa[i].start();
        n++;
      }
    }
    boolean done = false;
    while (!done) {
      done = true;
      for (int i = 0; i < n; i++) {

        if (fa[i].isAlive()) {
          done = false;
        }
      }
    }
    return null;
  }*/

}

