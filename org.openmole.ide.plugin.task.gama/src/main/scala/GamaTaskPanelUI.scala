/*
 * Copyright (C) 01/07/14 mathieu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.ide.plugin.task.gama

import java.io.File
import java.util.{Locale, ResourceBundle}
import msi.gama.headless.openmole.MoleSimulationLoader
import org.openmole.core.model.task.PluginSet
import org.openmole.ide.core.implementation.data.TaskDataUI
import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openmole.ide.core.implementation.panelsettings.TaskPanelUI
import org.openmole.ide.core.implementation.dataproxy.{Proxies, PrototypeDataProxyUI}
import org.openmole.ide.misc.widget.multirow.MultiTextFieldCombo.{TextFieldComboData, TextFieldComboPanel}
import org.openmole.ide.misc.widget.multirow.{MultiComboTextField, MultiChooseFileTextField, MultiTextFieldCombo}
import org.openmole.ide.misc.widget.multirow.MultiChooseFileTextField._
import org.openmole.ide.misc.widget.multirow.MultiComboTextField._
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import org.openmole.ide.misc.widget.multirow.RowWidget._
import org.openmole.ide.core.implementation.dataproxy._
import org.openmole.ide.core.implementation.data.EmptyDataUIs._
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.misc.widget.ChooseFileTextField
import org.openmole.ide.misc.tools.util.Converters
import org.openmole.ide.misc.tools.util.Converters._
import org.openmole.plugin.task.gama.GamaTask
import scala.swing.FileChooser._
import scala.swing._
import scala.concurrent.stm.Ref
import scala.concurrent.stm
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success
import scala.collection.JavaConversions._


class GamaTaskPanelUI(gdu: GamaTaskDataUI)(implicit val i18n: ResourceBundle = ResourceBundle.getBundle("help", new Locale("en", "EN"))) extends PluginPanel("") with TaskPanelUI {


  val gamlTextField = new ChooseFileTextField(gamlPathOrEmpty(gdu.gaml.getAbsolutePath),
    "Select a gaml file",
    SelectionMode.FilesOnly,
    Some("Gaml files" -> Seq("gaml")))

  gamlTextField.minimumSize = new Dimension(500, 15)

  val experimentNameTextField = new TextField(gdu.experimentName)

  val stepTextField = new TextField(gdu.steps.toString)

  val seedComboBox = new ComboBox(Proxies.instance.classPrototypes(classOf[Long]))
  seedComboBox.selection.item = gdu.seed.getOrElse(emptyPrototypeProxy)

  var multiProtoString = new MultiComboTextField[PrototypeDataProxyUI]("", List(), List())
  var multiStringProto = new MultiTextFieldCombo[PrototypeDataProxyUI]("", List(), List())

  val resourcesMultiTextField = new MultiChooseFileTextField("",
    gdu.resources.map { r ⇒
      new ChooseFileTextFieldPanel(new ChooseFileTextFieldData(r.getAbsolutePath))
    },
    selectionMode = SelectionMode.FilesAndDirectories,
    minus = CLOSE_IF_EMPTY)

  private val inputMappingPanel = new PluginPanel("")
  private val outputMappingPanel = new PluginPanel("")

  updateIOPanel

  private def updateIOPanel = {
    StatusBar().inform("Reading the gaml file ...")
    inputMappingPanel.contents += new Label("<html><i>Loading...</i></html>")
    outputMappingPanel.contents += new Label("<html><i>Loading...</i></html>")
    val (i, o) = buildMultis
    if (inputMappingPanel.contents.size > 0) inputMappingPanel.contents.remove(0, 1)
    if (outputMappingPanel.contents.size > 0) outputMappingPanel.contents.remove(0, 1)
    inputMappingPanel.contents += i
    outputMappingPanel.contents += o
    inputMappingPanel.revalidate
    outputMappingPanel.revalidate
    inputMappingPanel.repaint
    outputMappingPanel.repaint
    revalidate
    repaint
  }

  val components = List(("Settings",
    new PluginPanel("", "[left]rel[grow,fill]", "") {
      contents += new Label("Gaml file")
      contents +=(gamlTextField, "growx,wrap")
      contents += new Label("Experiment name")
      contents +=(experimentNameTextField, "growx,wrap")
      contents += new Label("Steps")
      contents +=(stepTextField, "growx,wrap")
      contents += new Label("Seed")
      contents += seedComboBox
    }
    ),
    ("Input mapping", inputMappingPanel),
    ("Output mapping", outputMappingPanel),
    ("Resources", resourcesMultiTextField.panel)
  )

  def gamlPathOrEmpty(path: String) = {
    if (path.endsWith(".gaml")) path
    else ""
  }

  def buildMultis: (Component, Component) = {
    try {
      multiStringProto = new MultiTextFieldCombo[PrototypeDataProxyUI](
        "",
        comboContent,
        gdu.gamaOutputs.map {
          m ⇒ new TextFieldComboPanel(comboContent, new TextFieldComboData(m._1, Some(m._2)))
        }.toList,
        minus = CLOSE_IF_EMPTY)

      multiProtoString = new MultiComboTextField[PrototypeDataProxyUI](
        "",
        comboContent,
        gdu.gamaInputs.map {
          m ⇒ new ComboTextFieldPanel(comboContent, new ComboTextFieldData(Some(m._1), m._2))
        }.toList,
        minus = CLOSE_IF_EMPTY)
      StatusBar().clear
    }
    catch {
      case e: Throwable ⇒
        StatusBar().block(e.getMessage, stack = e.getStackTraceString)
    }
    (multiProtoString.panel, multiStringProto.panel)
  }

  def comboContent: List[PrototypeDataProxyUI] = Proxies.instance.prototypes.toList

  override def saveContent(name: String): TaskDataUI =
    new GamaTaskDataUI(name,
      new File(gamlTextField.text),
      experimentNameTextField.text,
      stepTextField.text.toInt,
      if (seedComboBox.selection.item == emptyPrototypeProxy) None else Some(seedComboBox.selection.item),
      multiProtoString.content.flatMap { c ⇒ c.comboValue match {
        case Some(x: PrototypeDataProxyUI) =>  Some((x, c.textFieldValue, 0))
        case _=> None}
      },
      multiStringProto.content.flatMap { c ⇒ c.comboValue match {
        case Some(x: PrototypeDataProxyUI) => Some(c.textFieldValue, x,0)
        case _=> None}
      },
      resources = resourcesMultiTextField.content.map { data ⇒ new File(data.content)}
    )


}