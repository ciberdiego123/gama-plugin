/*
 * Copyright (C) 30/06/14 mathieu
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
import org.openmole.ide.core.implementation.data.TaskDataUI
import org.openmole.ide.core.implementation.dataproxy.{Proxies, PrototypeDataProxyUI}
import org.openmole.plugin.task.gama.GamaTask
import org.openmole.core.model.data._
import org.openmole.core.model.task._

class GamaTaskDataUI(val name: String = "",
                     val gaml: File = new File(""),
                     val experimentName: String = "",
                     val steps: Int = 1,
                     val seed: Option[PrototypeDataProxyUI] = None,
                     val gamaInputs: Seq[(PrototypeDataProxyUI, String, Int)] = Seq.empty,
                     val gamaOutputs: Seq[(String, PrototypeDataProxyUI, Int)] = Seq.empty,
                     val gamaVariableOutputs: Seq[(String, PrototypeDataProxyUI)] = Seq.empty,
                     val resources: List[File] = List.empty,
                     val inputs: Seq[PrototypeDataProxyUI] = Seq.empty,
                     val outputs: Seq[PrototypeDataProxyUI] = Seq.empty,
                     val inputParameters: Map[PrototypeDataProxyUI, String] = Map.empty) extends TaskDataUI {

def coreObject (plugins: PluginSet) = util.Try {
val gt = GamaTask (name, gaml, experimentName, steps, seed.get.dataUI.coreObject.get.asInstanceOf[Prototype[Long]] ) (plugins)

gamaInputs.foreach {
case (p, n, _) ⇒ gt addGamaInput (p.dataUI.coreObject.get, n)
}

gamaOutputs.foreach {
case (n, p, _) ⇒ gt addGamaOutput (n, p.dataUI.coreObject.get)
}

gamaVariableOutputs foreach {
case (vost, vop) =>
gt addGamaVariableOutput (vost, vop.dataUI.coreObject.get)
}

initialise (gt)

resources.foreach {
rfile ⇒
gt addResource rfile
}

gt.toTask
}

def coreClass = classOf[GamaTask]

override def imagePath = "img/gamaTask.png"

def fatImagePath = "img/gamaTask_fat.png"

def buildPanelUI = new GamaTaskPanelUI (this)

def doClone (ins: Seq[PrototypeDataProxyUI],
outs: Seq[PrototypeDataProxyUI],
params: Map[PrototypeDataProxyUI, String] ) = new GamaTaskDataUI (name,
gaml,
experimentName,
steps,
seed,
Proxies.instance.filterListTupleIn (gamaInputs.toList),
Proxies.instance.filterListTupleOut (gamaOutputs.toList),
gamaVariableOutputs,
resources,
ins,
outs,
params)


}
