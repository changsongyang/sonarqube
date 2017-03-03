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

  /**
   * Type of the change
   */
  public enum Type {
    /**
     * A restart was requested by a process
     */
    RESTART_REQUESTED,
    /**
     * Process has change its state
     */
    PROCESS_STATE_CHANGE,
    /**
     * A stop was requested by a process
     */
    STOP_REQUESTED,
  }

  private final Type type;
  private final ProcessId processId;
  private final SQProcess.State currentState;
  private final SQProcess.State previousState;

  /**
   * Instantiates a new Change event.
   *
   * @param previousState the previous state of the processId
   * @param type      the type
   */
  ChangeEvent(ProcessId processId, SQProcess.State currentState, SQProcess.State previousState, Type type) {
    this.processId = processId;
    this.currentState = currentState;
    this.previousState = previousState;
    this.type = type;
  }

  /**
   * Gets type of change
   *
   * @return {@link Type}
   */
  public Type getType() {
    return type;
  }

  public ProcessId getProcessId() {
    return processId;
  }

  public SQProcess.State getCurrentState() {
    return currentState;
  }

  public SQProcess.State getPreviousState() {
    return previousState;
  }
}
