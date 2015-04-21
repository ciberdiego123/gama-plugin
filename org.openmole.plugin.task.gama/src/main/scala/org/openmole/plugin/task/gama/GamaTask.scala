package org.openmole.plugin.task.gama

import java.io.File

import msi.gama.headless.openmole.MoleSimulationLoader
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import org.openmole.core.exception.UserBadDataError
import org.openmole.tool.file._
import org.openmole.core.tools.io.Prettifier._
import org.openmole.plugin.task.external._

import scala.collection.mutable.ListBuffer

object GamaTask {

  class Builder(gamlPath: String, experimentName: String, steps: Int) extends ExternalTaskBuilder { builder ⇒
    private var gamaInputs = new ListBuffer[(Prototype[_], String)]
    private var gamaOutputs = new ListBuffer[(String, Prototype[_])]
    private var gamaVariableOutputs = new ListBuffer[(String, Prototype[_])]

    def addGamaInput(prototype: Prototype[_], name: String): this.type = {
      this addInput prototype
      gamaInputs += prototype -> name
      this
    }

    def addGamaInput(prototype: Prototype[_]*): this.type = {
      prototype.foreach(p => addGamaInput(p, p.name))
      this
    }

    def addGamaOutput(name: String, prototype: Prototype[_]): this.type = {
      this addOutput prototype
      gamaOutputs += name -> prototype
      this
    }

    def addGamaOutput(prototype: Prototype[_]*): this.type = {
      prototype.foreach(p => addGamaOutput(p.name, p))
      this
    }

    def addGamaVariableOutput(name: String, prototype: Prototype[_]): this.type = {
      this addOutput prototype
      gamaVariableOutputs += name -> prototype
      this
    }

    def addGamaVariableOutput(prototype: Prototype[_]): this.type = addGamaVariableOutput(prototype.name, prototype)

    var gamaSeed: Prototype[Long] = Task.openMOLESeed
    def setSeed(seed: Prototype[Long]): this.type = {
      builder.gamaSeed = seed
      this
    }

    def toTask =
      new GamaTask(gamlPath, experimentName, steps, gamaInputs, gamaOutputs, gamaVariableOutputs, gamaSeed) with builder.Built {
        override val inputs = super.inputs + seed
      }

  }


  def apply(gaml: File, experimentName: String, steps: Int)(implicit plugins: PluginSet) = {
    val b = new Builder(gaml.getName, experimentName, steps)
    b addResource gaml
    b
  }

  def apply(workspace: File, model: String, experimentName: String, steps: Int) = {
    val b = new Builder(model, experimentName, steps)
    workspace.listFiles.foreach (f => b addResource (f))
    b
  }

  lazy val preload = {
    MoleSimulationLoader.loadGAMA()
  }

}

abstract class GamaTask(
    val gaml: String,
    val experimentName: String,
    val steps: Int,
    val gamaInputs: Iterable[(Prototype[_], String)],
    val gamaOutputs: Iterable[(String, Prototype[_])],
    val gamaVariableOutputs: Iterable[(String, Prototype[_])],
    val seed: Prototype[Long]) extends ExternalTask {


  override protected def process(context: Context): Context = withWorkDir { tmpDir ⇒
    try {

      GamaTask.preload

      prepareInputFiles(context, tmpDir.getCanonicalFile, "")
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

        fetchOutputFiles(returnContext, tmpDir.getCanonicalFile, "")
      } finally experiment.dispose
    }catch {
      case t: Throwable ⇒
        throw new UserBadDataError(
          s"""Gama raised the exception:
          |""".stripMargin + t.stackStringWithMargin
        )
    }
  }

}

