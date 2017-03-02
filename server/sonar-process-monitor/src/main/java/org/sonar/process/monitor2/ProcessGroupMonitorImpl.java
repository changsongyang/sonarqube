/*
 *
 *  * SonarQube
 *  * Copyright (C) 2009-2017 SonarSource SA
 *  * mailto:info AT sonarsource DOT com
 *  *
 *  * This program is free software; you can redistribute it and/or
 *  * modify it under the terms of the GNU Lesser General Public
 *  * License as published by the Free Software Foundation; either
 *  * version 3 of the License, or (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  * Lesser General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public License
 *  * along with this program; if not, write to the Free Software Foundation,
 *  * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */

package org.sonar.process.monitor2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessGroupMonitorImpl implements ProcessGroupMonitor {
  private static final Logger LOG = LoggerFactory.getLogger(ProcessGroupMonitorImpl.class);
  private static final long WATCH_DELAY_MS = 500L;

  private final List<SQProcess> processes = new ArrayList<>();
  private final List<Consumer<ChangeEvent>> listeners = new ArrayList<>();
  private final FileSystem fileSystem;
  private final List<WatcherThread> watcherThreads = new CopyOnWriteArrayList<>();
  private final StateWatcherThread stateWatcherThread = new StateWatcherThread(Collections.unmodifiableList(processes));

  @CheckForNull
  private JavaProcessLauncher launcher;
  private static final Timeouts TIMEOUTS = new Timeouts();

  public ProcessGroupMonitorImpl(FileSystem fileSystem) {
    this.fileSystem = fileSystem;
    this.launcher = new JavaProcessLauncher(TIMEOUTS, fileSystem.getTempDir());
  }

  @Override
  public List<SQProcess> getProcesses() {
    return Collections.unmodifiableList(processes);
  }

  @Override
  public SQProcess start(@Nonnull JavaCommand javaCommand) {
    processes.forEach(sqProcess -> {
      if (sqProcess.getId().equals(javaCommand.getProcessId())) {
        throw new IllegalStateException(
          String.format("Can not start multiple times %s", sqProcess.getId().getKey())
        );
      }
    });

    SQProcess sqProcess = null;
    try {
      sqProcess = launcher.launch(javaCommand);
      monitor(sqProcess);
    } catch (InterruptedException | RuntimeException e) {
      LOG.error(
        String.format("%s failed to start", sqProcess),
        e
      );
      if (sqProcess == null) {
        sqProcess = new SQProcess(javaCommand, null, null,null);
      }
      sendChangeEvent(new ChangeEvent(sqProcess, ChangeEvent.Type.PROCESS_STATE_CHANGE));
    }

    return sqProcess;
  }

  @Override
  public void stop(SQProcess sqProcess) {
    if (!processes.contains(sqProcess)) {
      throw new IllegalStateException(
        String.format("Can not stop %s since it has not been started", sqProcess.getId().getKey())
      );
    }
    sqProcess.stop();
  }

  @Override
  public void register(@Nonnull Consumer<ChangeEvent> listener) {
    listeners.add(listener);
  }

  @Override
  public void close() {
    stateWatcherThread.finish();
    launcher.close();
  }

  @Override
  public void stopAll() {
    stateWatcherThread.finish();
    launcher.close();
    // TODO
    throw new UnsupportedOperationException();
  }

  /**
   * Watches for state change in processes
   */
  private class StateWatcherThread extends Thread {
    private final List<SQProcess> sqProcesses;
    private final Map<SQProcess, SQProcess.State> previousStates = new HashMap<>();
    private boolean stopRequested = false;

    private StateWatcherThread(@Nonnull List<SQProcess> sqProcesses) {
      super("State watcher");
      this.sqProcesses = sqProcesses;
    }

    @Override
    public void run() {
      while (!stopRequested) {
        detectStateChanges();
        try {
          Thread.sleep(WATCH_DELAY_MS);
        } catch (InterruptedException ignored) {
          // keep watching
        }
      }
    }

    private void finish() {
      stopRequested = true;
    }

    private void detectStateChanges() {
      sqProcesses.forEach(sqProcess -> {
        SQProcess.State previousState = previousStates.get(sqProcess);
        if (sqProcess.getState() != previousState) {
          // TODO send a bean instead of sqProcess.
          sendChangeEvent(new ChangeEvent(sqProcess, ChangeEvent.Type.PROCESS_STATE_CHANGE));
          previousStates.put(sqProcess, sqProcess.getState());
        }
      });
    }
  }

  private void monitor(SQProcess sqProcess) throws InterruptedException {
    // physically watch if process is alive
    WatcherThread watcherThread = new WatcherThread(sqProcess);
    watcherThread.start();
    watcherThreads.add(watcherThread);
  }

  private void sendChangeEvent(ChangeEvent changeEvent) {
    listeners.forEach(listener -> listener.accept(changeEvent));
  }
}
