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
import msi.gama.kernel.experiment.IParameter
import msi.gama.util.{ GamaList, GamaListFactory }
import msi.gama.util.matrix.GamaMatrix
import msi.gaml.types.{ IType, Types }
import org.openmole.core.expansion._
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.validation._
import org.openmole.tool.random._
import org.openmole.tool.types._

import scala.util.Try

object GamaTask {

  trait GAMABuilder[T] {
    def gamaInputs: Lens[T, Vector[(FromContext[_], String)]]
    def gamaOutputs: Lens[T, Vector[(String, Val[_])]]
    def seed: Lens[T, Option[Val[Int]]]
  }

  implicit def isIO: InputOutputBuilder[GamaTask] = InputOutputBuilder(GamaTask._config)
  implicit def isExternal: ExternalBuilder[GamaTask] = ExternalBuilder(GamaTask.external)

  implicit def isGAMA: GAMABuilder[GamaTask] = new GAMABuilder[GamaTask] {
    override def gamaInputs = GamaTask.gamaInputs
    override def gamaOutputs = GamaTask.gamaOutputs
    override def seed = GamaTask.seed
  }

  def apply(
    gaml: File,
    experimentName: FromContext[String],
    stopCondition: OptionalArgument[FromContext[String]] = None,
    maxStep: OptionalArgument[FromContext[Int]] = None,
    workspace: OptionalArgument[File] = None
  )(implicit name: sourcecode.Name) = {
    val gamlName =
      workspace.option match {
        case None => gaml.getName
        case Some(ws) => gaml.getPath
      }

    val gamaTask =
      new GamaTask(
        gamlName,
        experimentName,
        stopCondition = stopCondition,
        maxStep = maxStep,
        gamaInputs = Vector.empty,
        gamaOutputs = Vector.empty,
        seed = None,
        _config = InputOutputConfig(),
        external = External()
      )

    workspace.option match {
      case None => gamaTask set (resources += gaml)
      case Some(w) => gamaTask set (w.listFiles().map(resources += _))
    }
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
    gamaInputs: Vector[(FromContext[_], String)],
    gamaOutputs: Vector[(String, Val[_])],
    seed: Option[Val[Int]],
    _config: InputOutputConfig,
    external: External
) extends Task with ValidateTask {

  override def validate: Seq[Throwable] = {
    val allInputs = External.PWD :: inputs.toList
    def stopError = if (!stopCondition.isDefined && !maxStep.isDefined) List(new UserBadDataError("At least one of the parameters stopCondition or maxStep should be defined")) else List.empty

    experimentName.validate(allInputs) ++
      stopCondition.toList.flatMap(_.validate(allInputs)) ++
      maxStep.toList.flatMap(_.validate(allInputs)) ++
      stopError ++
      External.validate(external, allInputs)
  }

  def config =
    InputOutputConfig.inputs.modify(_ ++ seed)(_config)

  override protected def process(executionContext: TaskExecutionContext) = FromContext[Context] { parameters ⇒
    External.withWorkDir(executionContext) { workDir ⇒
      try {
        GamaTask.preload
        import parameters._
        import executionContext._

        val context = parameters.context + (External.PWD -> workDir.getAbsolutePath)

        val preparedContext = external.prepareInputFiles(context, external.relativeResolver(workDir))

        GamaTask.withDisposable(MoleSimulationLoader.loadModel(workDir / gaml)) { model =>
          GamaTask.withDisposable(MoleSimulationLoader.newExperiment(model)) { experiment =>

            val gamaParameters = model.getExperiment(experimentName.from(context)).getParameters

            for ((p, n) <- gamaInputs) {
              val parameter = gamaParameters.get(n)
              experiment.setParameter(n, toGAMAObject(p.from(context), parameter, parameter.getType))
            }

            experiment.setup(experimentName.from(context), seed.map(context(_)).getOrElse(random().nextInt).toDouble)

            try experiment.play(
              stopCondition.map(_.from(context)).getOrElse(null),
              maxStep.map(_.from(context)).getOrElse(-1)
            )
            catch {
              case t: Throwable ⇒
                throw new UserBadDataError(
                  """Gama raised an exception while running the simulation:
                      |""".stripMargin + t.
                    stackStringWithMargin
                )
            }

            def gamaOutputVariables =
              gamaOutputs.map {
                case (n, p) =>
                  val gamaValue =
                    experiment.evaluateExpression(n)
                  Variable.unsecure(p, fromGAMAObject(

                    gamaValue, p.`type`.manifest.runtimeClass, p, n
                  ))
              }

            val resultContext = external.fetchOutputFiles(this, preparedContext, external.relativeResolver(workDir), workDir)
            external.cleanWorkDirectory(this, resultContext, workDir)

            resultContext ++ gamaOutputVariables
          }
        }

      } catch {
        case u: UserBadDataError =>
          throw u
        case t: Throwable ⇒
          // Don't chain exceptions to avoid deserialisation issue
          throw new UserBadDataError(
            s"""Gama raised the exception:
                |""".stripMargin + t.stackStringWithMargin
          )
      }
    }
  }

  def toGAMAObject(v: Any, p: IParameter, t: IType[_]): Any = {
    def error = new UserBadDataError(s"Parameter ${p.getName} cannot be set from an input of type ${v.getClass}")
    t.id match {
      case IType.LIST =>
        v match {
          case vs: Array[_] =>
            GamaListFactory.createWithoutCasting(Types.NO_TYPE, vs.map(v => toGAMAObject(v, p, t.getContentType)): _*)
          case _ => throw error
        }
      case _ => v
    }
  }

  def fromGAMAObject(v: Any, m: Class[_], p: Val[_], exp: String): Any = {
    import collection.JavaConverters._
    v match {
      case x: java.util.List[_] =>
        val componentType = m.getComponentType
        if (componentType == null) throw new UserBadDataError(s"Output variable $p cannot be set from gama value $v (expression $exp)")
        val array = java.lang.reflect.Array.newInstance(componentType, x.size)
        for ((xe, i) <- x.asScala.zipWithIndex) java.lang.reflect.Array.set(array, i, fromGAMAObject(xe, componentType, p, exp))
        array
      case x => x
    }
  }

}

