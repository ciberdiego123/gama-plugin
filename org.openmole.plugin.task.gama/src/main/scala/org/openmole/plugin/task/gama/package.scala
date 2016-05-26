/*
 * Copyright (C) 2015 Romain Reuillon
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.task

import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.data._
import org.openmole.plugin.task.gama.GamaTask.GAMABuilder

package object gama {

  lazy val gamaInputs = new {
    def +=[T: GAMABuilder: InputOutputBuilder](p: Prototype[_]): T => T = this.+=[T](p, p.name)
    def +=[T: GAMABuilder: InputOutputBuilder](p: Prototype[_], name: String): T => T =
      (implicitly[GAMABuilder[T]].gamaInputs add p -> name) andThen
        (inputs += p)
  }

  lazy val gamaOutputs = new {
    def +=[T: GAMABuilder: InputOutputBuilder](name: String, prototype: Prototype[_]): T => T =
      (implicitly[GAMABuilder[T]].gamaOutputs add name -> prototype) andThen (outputs += prototype)
    def +=[T: GAMABuilder: InputOutputBuilder](prototype: Prototype[_]): T => T = this.+=[T](prototype.name, prototype)
  }

  lazy val gamaVariableOutputs = new {
    def +=[T: GAMABuilder: InputOutputBuilder](name: String, prototype: Prototype[_]): T => T =
      (implicitly[GAMABuilder[T]].gamaVariableOutputs add name -> prototype) andThen
        (outputs += prototype)
    def +=[T: GAMABuilder: InputOutputBuilder](p: Prototype[_]): T => T = this.+=[T](p.name, p)
  }

  lazy val gamaSeed = new {
    def :=[T: GAMABuilder](seed: Prototype[Long]): T => T = implicitly[GAMABuilder[T]].seed.set(Some(seed))
  }

}
