package org.openmole.plugin.task.gama

import java.util.logging.{Level, Logger}

import com.google.common.base.Stopwatch
import com.google.inject.internal.util.$Stopwatch
import msi.gama.headless.core.HeadlessSimulationLoader
import org.openmole.core.implementation.task._
import org.openmole.core.model.data._
import org.openmole.core.implementation.data._
import org.openmole.core.model.task._
import java.io.File
import scala.collection.mutable.ListBuffer
import org.openmole.plugin.task.external.{ ExternalTask, ExternalTaskBuilder }
import org.openmole.misc.tools.io.FileUtil._
import msi.gama.headless.openmole.MoleSimulationLoader

object GamaTask {
  def apply(name: String, gaml: File, experimentName: String, steps: Int, seed: Prototype[Long] = Task.openMOLESeed)(implicit plugins: PluginSet) = new ExternalTaskBuilder { builder ⇒

    addResource(gaml)

    private var gamaInputs = new ListBuffer[(Prototype[_], String)]
    private var gamaOutputs = new ListBuffer[(String, Prototype[_])]
    private var gamaVariableOutputs = new ListBuffer[(String, Prototype[_])]

    def addGamaInput(prototype: Prototype[_], name: String): this.type = {
      this addInput prototype
      gamaInputs += prototype -> name
      this
    }

    def addGamaInput(prototype: Prototype[_]): this.type = addGamaInput(prototype, prototype.name)

    def addGamaOutput(name: String, prototype: Prototype[_]): this.type = {
      this addOutput prototype
      gamaOutputs += name -> prototype
      this
    }

    def addGamaOutput(prototype: Prototype[_]): this.type = addGamaOutput(prototype.name, prototype)

    def addGamaVariableOutput(name: String, prototype: Prototype[_]): this.type = {
      this addOutput prototype
      gamaVariableOutputs += name -> prototype
      this
    }

    def addGamaVariableOutput(prototype: Prototype[_]): this.type = addGamaVariableOutput(prototype.name, prototype)

    def toTask = new GamaTask(name, gaml.getName, experimentName, steps, gamaInputs, gamaOutputs, gamaVariableOutputs, seed) with builder.Built
  }

  lazy val preload = {
    MoleSimulationLoader.loadGAMA()
    Logger.getLogger(classOf[HeadlessSimulationLoader].getCanonicalName).setLevel(Level.INFO)
    Logger.getLogger(classOf[$Stopwatch].getCanonicalName).setLevel(Level.INFO)

  }
}

abstract class GamaTask(
    val name: String,
    val gaml: String,
    val experimentName: String,
    val steps: Int,
    val gamaInputs: Iterable[(Prototype[_], String)],
    val gamaOutputs: Iterable[(String, Prototype[_])],
    val gamaVariableOutputs: Iterable[(String, Prototype[_])],
    val seed: Prototype[Long]) extends ExternalTask {

  override protected def process(context: Context): Context = withWorkDir { tmpDir ⇒
    GamaTask.preload

    val links = prepareInputFiles(context, tmpDir.getCanonicalFile)
    val model = MoleSimulationLoader.loadModel(tmpDir.child(gaml))

    val experiment = MoleSimulationLoader.newExperiment(model)

    try {
      for ((p, n) <- gamaInputs) experiment.setParameter(n, context(p))
      experiment.setup(experimentName, context(seed))

      for {
        s <- 0 until steps
      } experiment.step

      val returnContext =
        Context(
          gamaVariableOutputs.map {
            case (n, p) =>
              Variable.unsecure(p, experiment.getVariableOutput(n))
          } ++
            gamaOutputs.map {
              case (n, p) =>
                Variable.unsecure(p, experiment.getOutput(n))
            }
        )

      fetchOutputFiles(returnContext, tmpDir.getCanonicalFile, links)
    } finally experiment.dispose
  }

}

