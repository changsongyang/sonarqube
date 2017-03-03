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
  public void receive_events() throws Exception {
    List<ChangeEvent> receivedEvents = new ArrayList<>();

    underTest = newProcessGroupMonitor();
    underTest.register(changeEvent -> receivedEvents.add(changeEvent));

    JavaCommand es = newESJavaCommand();
    SQProcess sqProcess = underTest.start(es);
    Thread.sleep(1_000);
    underTest.stop(sqProcess);
    Thread.sleep(1_000);
    assertThat(receivedEvents).containsExactly(
      new ChangeEvent(es.getProcessId(), ChangeEventType.STARTED),
      new ChangeEvent(es.getProcessId(), ChangeEventType.STOPPED)
    );
    underTest.stopAll();
  }

  private ProcessGroupMonitorImpl newProcessGroupMonitor() throws IOException {
    when(fileSystem.getTempDir()).thenReturn(tempDir);
      return new ProcessGroupMonitorImpl(fileSystem);
  }

  private JavaCommand newESJavaCommand() {
    return new JavaCommand(ProcessId.ELASTICSEARCH)
      .addClasspath(testJar.getAbsolutePath())
      .setClassName("org.sonar.process.test.StandardProcess");
  }
}
