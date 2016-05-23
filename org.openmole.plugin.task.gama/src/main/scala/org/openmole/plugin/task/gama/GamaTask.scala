package org.openmole.plugin.task.gama

import java.io.File

import msi.gama.headless.openmole.MoleSimulationLoader
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import org.openmole.core.exception._
import org.openmole.tool.file._
import org.openmole.plugin.task.external._
import org.openmole.core.tools.io.Prettifier._

import scala.collection.mutable.ListBuffer
import scala.util.Try

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

    var gamaSeed: Option[Prototype[Long]] = None
    def setSeed(seed: Prototype[Long]): this.type = {
      builder.gamaSeed = Some(seed)
      this
    }

    def toTask =
      new GamaTask(gamlPath, experimentName, steps, gamaInputs, gamaOutputs, gamaVariableOutputs, gamaSeed) with builder.Built {
        override val inputs = super.inputs ++ seed
      }

  }

  def apply(gaml: File, experimentName: String, steps: Int) = {
    val b = new Builder(gaml.getName, experimentName, steps)
    b addResource gaml
    b
  }

  def apply(workspace: File, model: String, experimentName: String, steps: Int) = {
    val b = new Builder(model, experimentName, steps)
    workspace.listFiles.foreach(f => b addResource (f))
    b
  }

  lazy val preload = {
    MoleSimulationLoader.loadGAMA()
  }

  private def withDisposable[T, D <: { def dispose() }](d: => D)(f: D => T): T = {
    val disposable = d
    try f(disposable)
    finally Try(disposable.dispose())
  }

  def toExcep

}

abstract class GamaTask(
    val gaml: String,
    val experimentName: String,
    val steps: Int,
    val gamaInputs: Iterable[(Prototype[_], String)],
    val gamaOutputs: Iterable[(String, Prototype[_])],
    val gamaVariableOutputs: Iterable[(String, Prototype[_])],
    val seed: Option[Prototype[Long]]
) extends ExternalTask {

  override protected def process(context: Context, executionContext: TaskExecutionContext)(implicit rng: RandomProvider): Context = withWorkDir(executionContext) { workDir ⇒
    try {
      GamaTask.preload
      
      prepareInputFiles(context, relativeResolver(workDir))

      GamaTask.withDisposable(MoleSimulationLoader.loadModel(workDir / gaml)) { model =>
        GamaTask.withDisposable(MoleSimulationLoader.newExperiment(model)) { experiment =>

          for ((p, n) <- gamaInputs) experiment.setParameter(n, context(p))
          experiment.setup(experimentName, seed.map(context(_)).getOrElse(rng().nextLong))

          for { s <- 0 until steps }
            try experiment.step
            catch {
              case t: Throwable ⇒
               throw new UserBadDataError(t, s"Gama raised an exception while running the simulation (after $s steps)")
            }

          def gamaOutputVariables = gamaOutputs.map { case (n, p) => Variable.unsecure(p, experiment.getOutput(n)) }
          def gamaVOutputVariables = gamaVariableOutputs.map { case (n, p) => Variable.unsecure(p, experiment.getVariableOutput(n)) }
          def returnContext = Context(gamaVOutputVariables ++ gamaOutputVariables)

          fetchOutputFiles(returnContext, relativeResolver(workDir))
        }
      }

    } catch {
      case u: UserBadDataError => throw u
      case t: Throwable ⇒ throw new UserBadDataError(t, "Gama raised an exception")
    }
  }

}

