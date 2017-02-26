package org.openmole.plugin.task.gama

import java.io.File

import org.openmole.core.context._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.dsl._
import org.openmole.core.tools.service._
import org.openmole.core.exception._
import org.openmole.plugin.task.external._
import org.openmole.core.tools.io.Prettifier._
import monocle.Lens
import monocle.macros.Lenses
import msi.gama.headless.openmole.MoleSimulationLoader
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.builder.{ InputOutputBuilder, InputOutputConfig }
import org.openmole.core.workflow.validation.ValidateTask
import org.openmole.tool.random._

import scala.util.Try

object GamaTask {

  trait GAMABuilder[T] {
    def gamaInputs: Lens[T, Vector[(Val[_], String)]]
    def gamaOutputs: Lens[T, Vector[(String, Val[_])]]
    def gamaVariableOutputs: Lens[T, Vector[(String, Val[_])]]
    def seed: Lens[T, Option[Val[Int]]]
  }

  implicit def isIO: InputOutputBuilder[GamaTask] = InputOutputBuilder(GamaTask._config)
  implicit def isExternal: ExternalBuilder[GamaTask] = ExternalBuilder(GamaTask.external)

  implicit def isGAMA: GAMABuilder[GamaTask] = new GAMABuilder[GamaTask] {
    override def gamaInputs = GamaTask.gamaInputs
    override def gamaOutputs = GamaTask.gamaOutputs
    override def seed = GamaTask.seed
    override def gamaVariableOutputs = GamaTask.gamaVariableOutputs
  }

  def apply(
    gaml: File,
    experimentName: FromContext[String],
    stopCondition: OptionalArgument[FromContext[String]] = None,
    maxStep: OptionalArgument[FromContext[Int]] = None,
    embedWorkspace: Boolean = false
  )(implicit name: sourcecode.Name) = {
    val gamaTask =
      new GamaTask(
        gaml.getName,
        experimentName,
        stopCondition = stopCondition,
        maxStep = maxStep,
        gamaInputs = Vector.empty,
        gamaOutputs = Vector.empty,
        gamaVariableOutputs = Vector.empty,
        seed = None,
        _config = InputOutputConfig(),
        external = External()
      )

    if (!embedWorkspace) gamaTask set (resources += gaml)
    else gamaTask set (gaml.getParentFileSafe.listFiles().map(resources += _))
  }

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
    experimentName: FromContext[String],
    stopCondition: OptionalArgument[FromContext[String]],
    maxStep: OptionalArgument[FromContext[Int]],
    gamaInputs: Vector[(Val[_], String)],
    gamaOutputs: Vector[(String, Val[_])],
    gamaVariableOutputs: Vector[(String, Val[_])],
    seed: Option[Val[Int]],
    _config: InputOutputConfig,
    external: External
) extends Task with ValidateTask {

  override def validate: Seq[Throwable] = {
    def stopError = if (!stopCondition.isDefined && !maxStep.isDefined) List(new UserBadDataError("At least one of the parameters stopCondition or maxStep should be defined")) else List.empty

    experimentName.validate(inputs.toList) ++
      stopCondition.toList.flatMap(_.validate(inputs.toList)) ++
      maxStep.toList.flatMap(_.validate(inputs.toList)) ++
      stopError
  }

  def config =
    InputOutputConfig.inputs.modify(_ ++ seed)(_config)

  override protected def process(context: Context, executionContext: TaskExecutionContext)(implicit rng: RandomProvider): Context = External.withWorkDir(executionContext) { workDir ⇒
    try {
      GamaTask.preload

      val preparedContext = external.prepareInputFiles(context, external.relativeResolver(workDir))

      GamaTask.withDisposable(MoleSimulationLoader.loadModel(workDir / gaml)) { model =>
        GamaTask.withDisposable(MoleSimulationLoader.newExperiment(model)) { experiment =>

          for ((p, n) <- gamaInputs) experiment.setParameter(n, context(p))
          experiment.setup(experimentName.from(context), seed.map(context(_)).getOrElse(rng().nextInt).toDouble)

          try experiment.play(
            stopCondition.map(_.from(context)).getOrElse(null),
            maxStep.map(_.from(context)).getOrElse(-1)
          )
          catch {
            case t: Throwable ⇒
              throw new UserBadDataError(
                s"""Gama raised an exception while running the simulation:
                    |""".stripMargin + t.stackStringWithMargin
              )
          }

          def gamaOutputVariables = gamaOutputs.map { case (n, p) => Variable.unsecure(p, experiment.getOutput(n)) }
          def gamaVOutputVariables = gamaVariableOutputs.map { case (n, p) => Variable.unsecure(p, experiment.getVariableOutput(n)) }

          external.fetchOutputFiles(preparedContext, external.relativeResolver(workDir)) ++ gamaVOutputVariables ++ gamaOutputVariables
        }
      }

    } catch {
      case u: UserBadDataError => throw u
      case t: Throwable ⇒
        // Don't chain exceptions to avoid deserialisation issue
        throw new UserBadDataError(
          s"""Gama raised the exception:
              |""".stripMargin + t.stackStringWithMargin
        )
    }
  }

}

