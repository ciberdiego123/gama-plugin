package org.openmole.plugin.task.gama

import java.io.File

import msi.gama.headless.openmole.MoleSimulationLoader
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.dsl._

import org.openmole.core.exception._
import org.openmole.plugin.task.external._
import org.openmole.core.tools.io.Prettifier._
import monocle.Lens
import monocle.macros.Lenses
import org.openmole.core.workflow.builder.{ InputOutputBuilder, InputOutputConfig }

import scala.collection.mutable.ListBuffer
import scala.util.Try

object GamaTask {

  trait GAMABuilder[T] {
    def gamaInputs: Lens[T, Vector[(Prototype[_], String)]]
    def gamaOutputs: Lens[T, Vector[(String, Prototype[_])]]
    def gamaVariableOutputs: Lens[T, Vector[(String, Prototype[_])]]
    def seed: Lens[T, Option[Prototype[Double]]]
  }

  implicit def isIO: InputOutputBuilder[GamaTask] = InputOutputBuilder(GamaTask._config)
  implicit def isExternal: ExternalBuilder[GamaTask] = ExternalBuilder(GamaTask.external)

  implicit def isGAMA: GAMABuilder[GamaTask] = new GAMABuilder[GamaTask] {
    override def gamaInputs = GamaTask.gamaInputs
    override def gamaOutputs = GamaTask.gamaOutputs
    override def seed = GamaTask.seed
    override def gamaVariableOutputs = GamaTask.gamaVariableOutputs
  }

  def apply(gaml: File, experimentName: String, steps: Int) =
    new GamaTask(
      gaml.getName,
      experimentName,
      steps
    ) set (resources += gaml)

  def apply(workspace: File, model: String, experimentName: String, steps: Int) =
    new GamaTask(
      model,
      experimentName,
      steps
    ) set (
      workspace.listFiles().map(resources += _)
    )

  lazy val preload = {
    MoleSimulationLoader.loadGAMA()
  }

  private def withDisposable[T, D <: { def dispose() }](d: => D)(f: D => T): T = {
    val disposable = d
    try f(disposable)
    finally Try(disposable.dispose())
  }

}

@Lenses case class GamaTask(
    gaml: String,
    experimentName: String,
    steps: Int,
    gamaInputs: Vector[(Prototype[_], String)] = Vector.empty,
    gamaOutputs: Vector[(String, Prototype[_])] = Vector.empty,
    gamaVariableOutputs: Vector[(String, Prototype[_])] = Vector.empty,
    seed: Option[Prototype[Double]] = None,
    _config: InputOutputConfig = InputOutputConfig(),
    external: External = External()
) extends Task {

  def config =
    InputOutputConfig.inputs.modify(_ ++ seed)(_config)

  override protected def process(context: Context, executionContext: TaskExecutionContext)(implicit rng: RandomProvider): Context = external.withWorkDir(executionContext) { workDir ⇒
    try {
      GamaTask.preload

      external.prepareInputFiles(context, external.relativeResolver(workDir))

      GamaTask.withDisposable(MoleSimulationLoader.loadModel(workDir / gaml)) { model =>
        GamaTask.withDisposable(MoleSimulationLoader.newExperiment(model)) { experiment =>

          for ((p, n) <- gamaInputs) experiment.setParameter(n, context(p))
          experiment.setup(experimentName, seed.map(context(_)).getOrElse(rng().nextDouble))

          for { s <- 0 until steps }
            try experiment.step
            catch {
              case t: Throwable ⇒
                throw new UserBadDataError(
                  s"""s"Gama raised an exception while running the simulation (after $s steps)":
                      |""".stripMargin + t.stackStringWithMargin
                )
            }

          def gamaOutputVariables = gamaOutputs.map { case (n, p) => Variable.unsecure(p, experiment.getOutput(n)) }
          def gamaVOutputVariables = gamaVariableOutputs.map { case (n, p) => Variable.unsecure(p, experiment.getVariableOutput(n)) }
          def returnContext = Context(gamaVOutputVariables ++ gamaOutputVariables)

          external.fetchOutputFiles(returnContext, external.relativeResolver(workDir))
        }
      }

    } catch {
      case u: UserBadDataError => throw u
      case t: Throwable ⇒
        // Don't chain exceptions to avoid unserialisation issue
        throw new UserBadDataError(
          s"""Gama raised the exception:
              |""".stripMargin + t.stackStringWithMargin
        )
    }
  }

}

