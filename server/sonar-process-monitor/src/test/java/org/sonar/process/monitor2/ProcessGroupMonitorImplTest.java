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

import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.process.ProcessId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProcessGroupMonitorImplTest {

  private static File testJar;

  private FileSystem fileSystem = mock(FileSystem.class);
  private ProcessGroupMonitorImpl underTest;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  private File tempDir;

  /**
   * Find the JAR file containing the test apps. Classes can't be moved in sonar-process-monitor because
   * they require sonar-process dependencies when executed here (sonar-process, commons-*, ...).
   */
  @BeforeClass
  public static void initTestJar() {
    File targetDir = new File("server/sonar-process/target");
    if (!targetDir.exists() || !targetDir.isDirectory()) {
      targetDir = new File("../sonar-process/target");
    }
    if (!targetDir.exists() || !targetDir.isDirectory()) {
      throw new IllegalStateException("target dir of sonar-process module not found. Please build it.");
    }
    Collection<File> jars = FileUtils.listFiles(targetDir, new String[] {"jar"}, false);
    for (File jar : jars) {
      if (jar.getName().startsWith("sonar-process-") && jar.getName().endsWith("-test-jar-with-dependencies.jar")) {
        testJar = jar;
        return;
      }
    }
    throw new IllegalStateException("No sonar-process-*-test-jar-with-dependencies.jar in " + targetDir);
  }

  @Before
  public void setUp() throws Exception {
    tempDir = temp.newFolder();
    when(fileSystem.getTempDir()).thenReturn(tempDir);
  }

  @Test
  public void fail_to_start_multiple_times() throws Exception {
    underTest = newProcessGroupMonitor();
    JavaCommand es = newESJavaCommand();
    underTest.start(es);
    boolean failed = false;
    try {
      underTest.start(newESJavaCommand());
    } catch (IllegalStateException e) {
      failed = e.getMessage().equals(
        String.format("Can not start multiple times [%s]", es.getProcessId().getKey())
      );
    }
    underTest.stopAll();
    assertThat(failed).isTrue();
  }

  @Test
  public void receive_starting_stopping_events() throws Exception {
    List<ChangeEvent> receivedEvents = new ArrayList<>();
    Object lock = new Object();

    underTest = newProcessGroupMonitor();
    underTest.register(changeEvent -> {
      receivedEvents.add(changeEvent);
      synchronized (lock) {
        lock.notify();
      }
    });

    JavaCommand es = newESJavaCommand();
    SQProcess sqProcess = underTest.start(es);
    synchronized (lock) {
      lock.wait(1_000);
      assertThat(receivedEvents).containsExactly(
        new ChangeEvent(es.getProcessId(), ChangeEventType.STARTED)
      );
    }
    synchronized (lock) {
      underTest.stop(sqProcess);
      lock.wait(1_000);
      assertThat(receivedEvents).containsExactly(
        new ChangeEvent(es.getProcessId(), ChangeEventType.STARTED),
        new ChangeEvent(es.getProcessId(), ChangeEventType.STOPPED)
      );
    }
    underTest.stopAll();
  }

  @Test
  public void receive_all_events() throws Exception {
    List<ChangeEvent> receivedEvents = new ArrayList<>();
    Object lock = new Object();
    SQProcess sqProcess = mock(SQProcess.class);
    when(sqProcess.getProcessId()).thenReturn(ProcessId.ELASTICSEARCH);

    underTest = newProcessGroupMonitor(Lists.newArrayList(sqProcess));
    underTest.register(changeEvent -> {
      receivedEvents.add(changeEvent);
      synchronized (lock) {
        lock.notify();
      }
    });

    for (SQProcess.State state : SQProcess.State.values()) {
      receivedEvents.clear();
      when(sqProcess.getState()).thenReturn(state);

      synchronized (lock) {
        lock.wait(1_000);
        switch (state) {
          case STOPPED:
            assertThat(receivedEvents).containsExactly(
              new ChangeEvent(ProcessId.ELASTICSEARCH, ChangeEventType.STOPPED)
            );
            break;
          case ASKED_FOR_RESTART:
            assertThat(receivedEvents).containsExactly(
              new ChangeEvent(ProcessId.ELASTICSEARCH, ChangeEventType.RESTART_REQUESTED)
            );
            break;
          case ASKED_FOR_SHUTDOWN:
            assertThat(receivedEvents).containsExactly(
              new ChangeEvent(ProcessId.ELASTICSEARCH, ChangeEventType.STOP_REQUESTED)
            );
            break;
          case UP:
            assertThat(receivedEvents).containsExactly(
              new ChangeEvent(ProcessId.ELASTICSEARCH, ChangeEventType.STARTED)
            );
            break;
          case OPERATIONAL:
            assertThat(receivedEvents).containsExactly(
              new ChangeEvent(ProcessId.ELASTICSEARCH, ChangeEventType.OPERATIONAL)
            );
            break;
          case INIT:
            assertThat(receivedEvents).isEmpty();
            break;
          default:
            throw new IllegalStateException(
              String.format("Unknown state [%s]", state)
            );
        }
      }
    }
    underTest.stopAll();
  }

  private ProcessGroupMonitorImpl newProcessGroupMonitor() throws IOException {
    when(fileSystem.getTempDir()).thenReturn(tempDir);
      return new ProcessGroupMonitorImpl(fileSystem);
  }

  private ProcessGroupMonitorImpl newProcessGroupMonitor(List<SQProcess> sqProcesses) throws IOException {
    when(fileSystem.getTempDir()).thenReturn(tempDir);
    return new ProcessGroupMonitorImpl(fileSystem, sqProcesses);
  }

  private JavaCommand newESJavaCommand() {
    return new JavaCommand(ProcessId.ELASTICSEARCH)
      .addClasspath(testJar.getAbsolutePath())
      .setClassName("org.sonar.process.test.StandardProcess");
  }
}
