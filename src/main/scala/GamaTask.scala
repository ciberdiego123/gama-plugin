package org.openmole.plugin.task.gama

import msi.gama._
import msi.gama.headless.core.HeadlessSimulationLoader
import org.openmole.core.implementation.task._
import org.openmole.core.model.data._
import org.openmole.core.implementation.data._
import org.openmole.core.model.task._
import java.io.File
import msi.gama.headless.runtime.GamaSimulator
import msi.gama.kernel.experiment.ParametersSet
import scala.collection.mutable.ListBuffer
import org.openmole.core.implementation.tools.InputOutputBuilder
import msi.gama.outputs.{ AbstractOutputManager, MonitorOutput }
import org.openmole.misc.workspace.Workspace
import org.openmole.plugin.task.external.{ ExternalTask, ExternalTaskBuilder }
import org.openmole.misc.tools.io.FileUtil._

object GamaTask {
  def apply(name: String, gaml: File, experimentName: String, steps: Int, seed: Prototype[Long] = Task.openMOLESeed)(implicit plugins: PluginSet) = new ExternalTaskBuilder { builder ⇒

    addResource(gaml)

    private var _gamaInputs = new ListBuffer[(Prototype[_], String)]
    private var _gamaVariableOutputs = new ListBuffer[(String, Prototype[_])]
    private var _gamaOutputs = new ListBuffer[(String, Prototype[_])]

    def addGamaInput(prototype: Prototype[_], name: String): this.type = {
      this addInput prototype
      _gamaInputs += prototype -> name
      this
    }

    def addGamaInput(prototype: Prototype[_]): this.type = addGamaInput(prototype, prototype.name)

    def addGamaOutput(name: String, prototype: Prototype[_]): this.type = {
      this addOutput prototype
      _gamaOutputs += name -> prototype
      this
    }

    def addGamaOutput(prototype: Prototype[_]): this.type = addGamaOutput(prototype.name, prototype)

    def addGamaVariableOutput(name: String, prototype: Prototype[_]): this.type = {
      this addOutput prototype
      _gamaVariableOutputs += name -> prototype
      this
    }

    def addGamaVariableOutput(prototype: Prototype[_]): this.type = addGamaVariableOutput(prototype.name, prototype)

    def toTask = new GamaTask(name, gaml, experimentName, steps, _gamaInputs, _gamaOutputs, _gamaVariableOutputs, seed) with builder.Built
  }

  lazy val preload = {
    HeadlessSimulationLoader.preloadGAMA
  }
}

abstract class GamaTask(
    val name: String,
    val gaml: File,
    val experimentName: String,
    val steps: Int,
    val gamaInputs: Iterable[(Prototype[_], String)],
    val gamaOutputs: Iterable[(String, Prototype[_])],
    val gamaVariableOutputs: Iterable[(String, Prototype[_])],
    val seed: Prototype[Long]) extends ExternalTask {

  override protected def process(context: Context): Context = withWorkDir { tmpDir ⇒
    GamaTask.preload
    val links = prepareInputFiles(context, tmpDir.getCanonicalFile)
    val model = HeadlessSimulationLoader.loadModel(tmpDir.child(gaml.getName).getAbsolutePath)

    val parameterSet = new ParametersSet()
    for ((p, n) <- gamaInputs) parameterSet.put(n, context(p))
    val experimentSpecies = HeadlessSimulationLoader.newHeadlessSimulation(model, experimentName, parameterSet)
    experimentSpecies.getAgent.setSeed(context(seed).toDouble)
    val scope = experimentSpecies.getCurrentSimulation.getScope

    try {
      for {
        s <- 0 until steps
      } experimentSpecies.getCurrentSimulation.step(scope)

      experimentSpecies.getSimulationOutputs.step(scope)

      val returnContext =
        Context(
          gamaVariableOutputs.map {
            case (n, p) =>
              Variable.unsecure(
                p,
                experimentSpecies.getCurrentSimulation.getDirectVarValue(scope, n)
              )
          } ++
            gamaOutputs.map {
              case (n, p) =>
                // FIXME test monitor cast
                Variable.unsecure(
                  p,
                  experimentSpecies.getSimulationOutputs.asInstanceOf[AbstractOutputManager].getOutputWithName(n).asInstanceOf[MonitorOutput].getLastValue
                )
            }

        )

      fetchOutputFiles(returnContext, tmpDir.getCanonicalFile, links)
    } finally experimentSpecies.dispose
  }

}

