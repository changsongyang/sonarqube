/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.process.monitor2;

import org.junit.Test;
import org.sonar.process.ProcessId;

import static org.assertj.core.api.Assertions.assertThat;

public class ChangeEventTest {
  @Test
  public void equals_and_hashcode() {
    ChangeEvent changeEvent = new ChangeEvent(ProcessId.APP, ChangeEventType.STARTED);
    for (ChangeEventType type : ChangeEventType.values()) {
      ChangeEvent temp = new ChangeEvent(ProcessId.APP, type);
      assertThat(changeEvent.equals(temp)).isEqualTo(type.equals(ChangeEventType.STARTED));
      if (type.equals(ChangeEventType.STARTED)) {
        assertThat(changeEvent.hashCode()).isEqualTo(temp.hashCode());
      } else {
        assertThat(changeEvent.hashCode()).isNotEqualTo(temp.hashCode());
      }
    }
    for (ProcessId processId : ProcessId.values()) {
      ChangeEvent temp = new ChangeEvent(processId, ChangeEventType.STARTED);
      assertThat(changeEvent.equals(temp)).isEqualTo(processId.equals(ProcessId.APP));
      if (processId.equals(ProcessId.APP)) {
        assertThat(changeEvent.hashCode()).isEqualTo(temp.hashCode());
      } else {
        assertThat(changeEvent.hashCode()).isNotEqualTo(temp.hashCode());
      }
    }

    assertThat(changeEvent.equals(changeEvent)).isTrue();
    assertThat(changeEvent.equals("STARTED")).isFalse();
    assertThat(changeEvent.toString())
      .isEqualTo(
        String.format("ChangeEvent{type=%s, processId=%s}",
          changeEvent.getType(), changeEvent.getProcessId()
        )
      );
  }
}
