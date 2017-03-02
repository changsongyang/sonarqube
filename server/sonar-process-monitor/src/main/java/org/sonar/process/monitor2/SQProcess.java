package org.sonar.process.monitor2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.process.ProcessCommands;
import org.sonar.process.ProcessId;
import org.sonar.process.ProcessUtils;

public class SQProcess {
  enum State {
    INIT, STARTING, UP, OPERATIONAL, STOPPING, STOPPED
  }

  private static final Logger LOG = LoggerFactory.getLogger(SQProcess.class);

  private final JavaCommand javaCommand;
  private final ProcessCommands commands;
  private final Process process;
  private final StreamGobbler gobbler;
  private State state;

  SQProcess(JavaCommand javaCommand, ProcessCommands commands, Process process, StreamGobbler gobbler) {
    this.javaCommand = javaCommand;
    this.commands = commands;
    this.process = process;
    this.gobbler = gobbler;
  }

  /**
   * The {@link Process}
   */
  Process getProcess() {
    return process;
  }

  public ProcessCommands getCommands() {
    return commands;
  }

  /**
   * Sends kill signal and awaits termination. No guarantee that process is gracefully terminated (=shutdown hooks
   * executed). It depends on OS.
   */
  void stop() {
    if (ProcessUtils.isAlive(process)) {
      try {
        ProcessUtils.sendKillSignal(process);
        // signal is sent, waiting for shutdown hooks to be executed (or not... it depends on OS)
        process.waitFor();

      } catch (InterruptedException e) {
        // can't wait for the termination of process. Let's assume it's down.
        LOG.warn(String.format("Interrupted while stopping process %s", javaCommand.getProcessId()), e);
        Thread.currentThread().interrupt();
      }
    }
    ProcessUtils.closeStreams(process);
    StreamGobbler.waitUntilFinish(gobbler);
  }


  public ProcessId getId() {
    return javaCommand.getProcessId();
  }

  public State getState() {
    if (process == null || !ProcessUtils.isAlive(process)) {
      return State.STOPPED;
    }

    if (commands.isUp()) {
      return State.UP;
    }
    if (commands.isOperational()) {
      return State.OPERATIONAL;
    }
    if (commands.askedForStop()) {
      return State.STOPPING;
    }
    // TODO ? STARTING ?
    return State.INIT;
  }

  @Override
  public String toString() {
    return String.format("Process[%s]", javaCommand.getProcessId());
  }
}
