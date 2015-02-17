package org.openmole.plugin.task.gama

import java.util.logging.{Level, Logger}

import org.openmole.core.dsl._
import java.io.File
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import org.openmole.misc.exception.UserBadDataError

import scala.collection.mutable.ListBuffer
import org.openmole.plugin.task.external._
import org.openmole.misc.tools.io.FileUtil._
import msi.gama.headless.openmole.MoleSimulationLoader
import org.openmole.misc.tools.io.Prettifier._

object GamaTask {

  private def builder(gamlPath: String, experimentName: String, steps: Int, seed: Prototype[Long] = Task.openMOLESeed) = new ExternalTaskBuilder { builder ⇒
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

    def toTask = new GamaTask(gamlPath, experimentName, steps, gamaInputs, gamaOutputs, gamaVariableOutputs, seed) with builder.Built
  }


  def apply(gaml: File, experimentName: String, steps: Int, seed: Prototype[Long] = Task.openMOLESeed)(implicit plugins: PluginSet) = {
    val b = builder(gaml.getName, experimentName, steps, seed)
    b addResource gaml
    b
  }

  def withWorkspace(workspace: File, gamlPath: String, experimentName: String, steps: Int, seed: Prototype[Long] = Task.openMOLESeed)(implicit plugins: PluginSet) = {
    val b = builder(gamlPath, experimentName, steps, seed)
    workspace.listFiles.foreach (f => b addResource f)
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

