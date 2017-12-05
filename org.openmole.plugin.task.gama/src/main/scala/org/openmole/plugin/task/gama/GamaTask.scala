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
import msi.gama.headless.core.{ GamaHeadlessException, HeadlessSimulationLoader }
import msi.gama.headless.openmole.MoleSimulationLoader
import msi.gama.kernel.experiment.IParameter
import msi.gama.kernel.model.IModel
import msi.gama.precompiler.GamlProperties
import msi.gama.util.{ GamaList, GamaListFactory }
import msi.gama.util.matrix.GamaMatrix
import msi.gaml.compilation.GamlCompilationError
import msi.gaml.types.{ IType, Types }
import org.openmole.core.expansion._
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.validation._
import org.openmole.tool.random._
import org.openmole.tool.types._
import org.openmole.core.fileservice.FileService
import org.openmole.core.pluginmanager._
import org.openmole.core.serializer.plugin.Plugins
import org.openmole.core.workspace.NewFile

import collection.JavaConverters._
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
    workspace: OptionalArgument[File] = None,
    failOnGamlError: Boolean = true
  )(implicit name: sourcecode.Name) = {
    val gamlName =
      workspace.option match {
        case None => gaml.getName
        case Some(ws) => gaml.getPath
      }

    val plugins = extensionPlugins(workspace, gaml, failOnGamlError)
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
        plugins = plugins,
        failOnGamlError = failOnGamlError,
        external = External()
      )

    workspace.option match {
      case None => gamaTask set (resources += gaml)
      case Some(w) => gamaTask set (w.listFiles().map(resources += _))
    }
  }

  private def withDisposable[T, D <: { def dispose() }](d: => D)(f: D => T): T = {
    val disposable = d
    try f(disposable)
    finally Try(disposable.dispose())
  }

  def errorString(compileErrors: Seq[GamlCompilationError]) = {
    def level(error: GamlCompilationError) =
      (error.isInfo, error.isWarning, error.isError) match {
        case (true, _, _) => "INFO"
        case (_, true, _) => "WARNING"
        case (_, _, true) => "ERROR"
        case _ => "UNKNOWN"
      }

    s"""Gaml compilation errors (some errors may be caused by OpenGL function calls, in this case either separate the graphical aspect in another gaml file or set "failOnGamlError = false" on the GAMATask):
    |${compileErrors.map(e => s"  ${level(e)}: $e").mkString("\n")}""".stripMargin
  }

  def checkErrors(failOnError: Boolean, compileErrors: Seq[GamlCompilationError]) = {
    def containsError = compileErrors.exists(_.isError)
    if (failOnError && containsError) throw new UserBadDataError(errorString(compileErrors))
  }

  def extensionPlugins(workspace: Option[File], model: File, failOnError: Boolean) = {
    val modelFile =
      workspace match {
        case None => model
        case Some(w) => w / model.getPath
      }

    val properties = new GamlProperties()
    val errors = new java.util.LinkedList[GamlCompilationError]()

    try GamaTask.withDisposable(MoleSimulationLoader.loadModel(modelFile, errors, properties)) { model => }
    catch {
      case e: GamaHeadlessException => throw new UserBadDataError(e, errorString(errors.asScala))
    }

    checkErrors(failOnError, errors.asScala)

    val allBundles = PluginManager.bundles
    val bundles = properties.get(GamlProperties.PLUGINS).asScala.map {
      plugin =>
        allBundles.find(_.getSymbolicName == plugin).getOrElse(throw new UserBadDataError(s"Missing plugin for extensions $plugin"))
    }

    bundles.flatMap(PluginManager.allPluginDependencies).map(_.file).toSeq
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
  plugins: Seq[File],
  failOnGamlError: Boolean,
  external: External) extends Task with ValidateTask with Plugins {

  override def validate = Validate { p =>
    import p._

    val allInputs = External.PWD :: inputs.toList
    def stopError = if (!stopCondition.isDefined && !maxStep.isDefined) List(new UserBadDataError("At least one of the parameters stopCondition or maxStep should be defined")) else List.empty

    experimentName.validate(allInputs) ++
      stopCondition.toList.flatMap(_.validate(allInputs)) ++
      maxStep.toList.flatMap(_.validate(allInputs)) ++
      stopError ++
      External.validate(external)(allInputs).apply
  }

  def config =
    InputOutputConfig.inputs.modify(_ ++ seed)(_config)

  def compile(model: File) = {
    val properties = new GamlProperties()
    val errors = new java.util.LinkedList[GamlCompilationError]()
    val compiled =
      try MoleSimulationLoader.loadModel(model, errors, properties)
      catch {
        case e: GamaHeadlessException =>
          throw new UserBadDataError(e, GamaTask.errorString(errors.asScala))
      }

    GamaTask.checkErrors(failOnGamlError, errors.asScala)

    compiled
  }

  override protected def process(executionContext: TaskExecutionContext) = FromContext[Context] { parameters =>

    External.withWorkDir(executionContext) { workDir =>
      try {
        import parameters._
        import parameters.newFile

        val context = parameters.context + (External.PWD -> workDir.getAbsolutePath)

        val preparedContext = external.prepareInputFiles(context, external.relativeResolver(workDir))

        GamaTask.withDisposable(compile(workDir / gaml)) { model =>
          GamaTask.withDisposable(MoleSimulationLoader.newExperiment(model)) { experiment =>

            val gamaParameters = model.getExperiment(experimentName.from(context)).getParameters

            for ((p, n) <- gamaInputs) {
              val parameter = gamaParameters.get(n)
              if (parameter == null) throw new UserBadDataError(s"Parameter $n not found in experiment ${experimentName.from(context)}")
              val value = p.from(context)
              try experiment.setParameter(n, toGAMAObject(value, parameter, parameter.getType))
              catch {
                case t: Throwable => throw new InternalError(s"Error while setting experiment parameter $n with value $value", t)
              }
            }

            experiment.setup(experimentName.from(context), seed.map(context(_)).getOrElse(random().nextInt).toDouble)

            //FIXME workaround some wierd gama bug, otherwise output cannot be evaluated
            gamaOutputs.foreach {
              case (n, p) => experiment.evaluateExpression(n)
            }

            try experiment.play(
              stopCondition.map(_.from(context)).getOrElse(null),
              maxStep.map(_.from(context)).getOrElse(-1)
            )
            catch {
              case t: Throwable =>
                throw new UserBadDataError(
                  """Gama raised an exception while running the simulation:
                    |""".stripMargin + t.stackStringWithMargin
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

            val resultContext = external.fetchOutputFiles(outputs, preparedContext, external.relativeResolver(workDir), workDir)
            external.cleanWorkDirectory(outputs, resultContext, workDir)

            resultContext ++ gamaOutputVariables
          }
        }

      } catch {
        case u: UserBadDataError => throw u
        case t: Throwable =>
          // Don't chain exceptions to avoid deserialisation issue
          throw new UserBadDataError(
            "Gama raised an exception:\n" +
              t.stackStringWithMargin + "\n" +
              s"""The content of the working directory was ($workDir)
              |${directoryContentInformation(workDir)}""".stripMargin
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

