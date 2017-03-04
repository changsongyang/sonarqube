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
import org.sonar.process.ProcessCommands;
import org.sonar.process.ProcessId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SQProcessTest {
  @Test
  public void test_states() {
    Process process = mock(Process.class);
    ProcessCommands processCommands = mock(ProcessCommands.class);
    JavaCommand javaCommand = mock(JavaCommand.class);
    StreamGobbler streamGobbler = mock(StreamGobbler.class);

    SQProcess sqProcess = new SQProcess(javaCommand, processCommands, process, streamGobbler);
    // Mock a process that have finished by returning 0
    when(process.exitValue()).thenReturn(0);
    assertThat(sqProcess.getState()).isEqualTo(SQProcess.State.STOPPED);

    // Mock a process that is still running
    when(process.exitValue()).thenThrow(IllegalThreadStateException.class);

    when(processCommands.askedForRestart()).thenReturn(false);
    when(processCommands.askedForStop()).thenReturn(false);
    when(processCommands.isUp()).thenReturn(false);
    when(processCommands.isOperational()).thenReturn(false);
    assertThat(sqProcess.getState()).isEqualTo(SQProcess.State.INIT);

    when(processCommands.askedForRestart()).thenReturn(true);
    when(processCommands.askedForStop()).thenReturn(false);
    when(processCommands.isUp()).thenReturn(false);
    when(processCommands.isOperational()).thenReturn(false);
    assertThat(sqProcess.getState()).isEqualTo(SQProcess.State.ASKED_FOR_RESTART);

    when(processCommands.askedForRestart()).thenReturn(false);
    when(processCommands.askedForStop()).thenReturn(true);
    when(processCommands.isUp()).thenReturn(false);
    when(processCommands.isOperational()).thenReturn(false);
    assertThat(sqProcess.getState()).isEqualTo(SQProcess.State.ASKED_FOR_SHUTDOWN);

    when(processCommands.askedForRestart()).thenReturn(false);
    when(processCommands.askedForStop()).thenReturn(false);
    when(processCommands.isUp()).thenReturn(true);
    when(processCommands.isOperational()).thenReturn(false);
    assertThat(sqProcess.getState()).isEqualTo(SQProcess.State.UP);

    when(processCommands.askedForRestart()).thenReturn(false);
    when(processCommands.askedForStop()).thenReturn(false);
    when(processCommands.isUp()).thenReturn(false);
    when(processCommands.isOperational()).thenReturn(true);
    assertThat(sqProcess.getState()).isEqualTo(SQProcess.State.OPERATIONAL);
  }

  @Test
  public void test_toString() {
    Process process = mock(Process.class);
    ProcessCommands processCommands = mock(ProcessCommands.class);
    JavaCommand javaCommand = new JavaCommand(ProcessId.ELASTICSEARCH);
    StreamGobbler streamGobbler = mock(StreamGobbler.class);

    SQProcess sqProcess = new SQProcess(javaCommand, processCommands, process, streamGobbler);
    assertThat(sqProcess.toString()).isEqualTo("Process[es]");
  }
}
