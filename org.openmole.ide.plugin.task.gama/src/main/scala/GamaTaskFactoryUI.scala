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

import org.openmole.core.model.task.ITask
import org.openmole.ide.core.implementation.builder.{ PuzzleUIMap, SceneFactory }
import org.openmole.plugin.task.gama.GamaTask
import org.openmole.ide.core.implementation.factory.TaskFactoryUI
import org.openmole.ide.misc.tools.util.Converters._
import java.io.File

class GamaTaskFactoryUI extends TaskFactoryUI {

  override def toString = "Gama"

  def buildDataUI = new GamaTaskDataUI

  def buildDataProxyUI(task: ITask, uiMap: PuzzleUIMap) = {

    val t = SceneFactory.as[GamaTask](task)

    uiMap.task(t, x ⇒ new GamaTaskDataUI(t.name,
      t.gaml.getCanonicalPath,
      t.experimentName,
      t.steps,
      uiMap.prototypeUI(t.seed),
      t.gamaInputs.toList.map { p ⇒ (uiMap.prototypeUI(p._1).get, p._2) },
      t.gamaOutputs.toList.map { p ⇒ (p._1, uiMap.prototypeUI(p._2).get) }))
  }

  override def category = List("ABM")
}