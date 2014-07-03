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
import org.openmole.ide.core.implementation.data.TaskDataUI
import org.openmole.ide.core.implementation.panelsettings.TaskPanelUI
import org.openmole.ide.core.implementation.dataproxy.{Proxies, PrototypeDataProxyUI}
import org.openmole.ide.misc.widget.multirow.MultiChooseFileTextField
import org.openmole.ide.misc.widget.multirow.MultiChooseFileTextField._
import org.openmole.ide.misc.widget.multirow.MultiTwoCombos
import org.openmole.ide.misc.widget.multirow.MultiTwoCombos._
import org.openmole.ide.core.implementation.dataproxy._
import org.openmole.ide.core.implementation.data.EmptyDataUIs._
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.misc.widget.ChooseFileTextField
import org.openmole.ide.misc.tools.util.Converters
import org.openmole.ide.misc.tools.util.Converters._
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import scala.swing.FileChooser._
import scala.swing._


class GamaTaskPanelUI(gdu: GamaTaskDataUI)(implicit val i18n: ResourceBundle = ResourceBundle.getBundle("help", new Locale("en", "EN"))) extends TaskPanelUI {


  //val (gaml, workspaceEmbedded) = Workspace.fromWorkspace(workspace)

  val gamlTextField = new ChooseFileTextField(gdu.gaml.getAbsolutePath,
    "Select a gaml file",
    SelectionMode.FilesOnly,
    Some("Gaml files" -> Seq("gaml")))

  val experimentNameTextField = new TextField(gdu.experimentName)

  val stepTextField = new TextField(gdu.steps.toString)

  val seedComboBox = new ComboBox(comboContent)
  seedComboBox.selection.item = gdu.seed.getOrElse(emptyPrototypeProxy)

  var multiProtoString = new MultiTwoCombos[PrototypeDataProxyUI, String]("", List(), List(), "with", Seq())
  var multiStringProto = new MultiTwoCombos[String, PrototypeDataProxyUI]("", List(), List(), "with", Seq())

  val resourcesMultiTextField = new MultiChooseFileTextField("",
    gdu.resources.map { r ⇒
      new ChooseFileTextFieldPanel(new ChooseFileTextFieldData(r.getAbsolutePath))
    },
    selectionMode = SelectionMode.FilesAndDirectories,
    minus = CLOSE_IF_EMPTY)

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
    ), ("Input mapping", multiProtoString),
    ("Output mapping", multiStringProto),
    ("Resources", new PluginPanel(""))
  )

  def comboContent: List[PrototypeDataProxyUI] = emptyPrototypeProxy :: Proxies.instance.prototypes.toList

  override def saveContent(name: String): TaskDataUI = {
    new GamaTaskDataUI(name,
      new File(gamlTextField.text),
      experimentNameTextField.text,
      stepTextField.text.toInt,
      if (seedComboBox.selection.item == emptyPrototypeProxy) None else Some(seedComboBox.selection.item),
      Converters.flattenTuple2Options(multiProtoString.content.map { c ⇒ (c.comboValue1, c.comboValue2)}).filter { case (p, s) ⇒ Proxies.check(p)},
      Converters.flattenTuple2Options(multiStringProto.content.map { c ⇒ (c.comboValue1, c.comboValue2)}).filter { case (s, p) ⇒ Proxies.check(p)},
      resources = resourcesMultiTextField.content.map { data ⇒ new File(data.content)}
        )
      }

  }
