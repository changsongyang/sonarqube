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

import org.sonar.process.ProcessId;

/**
 * The change event from a SQProcess
 */
public class ChangeEvent {
  private final ChangeEventType type;
  private final ProcessId processId;

  /**
   * Instantiates a new Change event.
   */
  ChangeEvent(ProcessId processId, ChangeEventType type) {
    this.processId = processId;
    this.type = type;
  }

  /**
   * Gets type of change
   *
   * @return {@link ChangeEventType}
   */
  public ChangeEventType getType() {
    return type;
  }

  public ProcessId getProcessId() {
    return processId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ChangeEvent)) {
      return false;
    }

    ChangeEvent that = (ChangeEvent) o;

    if (type != that.type) {
      return false;
    }
    return processId == that.processId;
  }

  @Override
  public int hashCode() {
    int result = type != null ? type.hashCode() : 0;
    result = 31 * result + (processId != null ? processId.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "ChangeEvent{" +
      "type=" + type +
      ", processId=" + processId +
      '}';
  }
}
