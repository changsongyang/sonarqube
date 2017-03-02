package org.sonar.process.monitor2;

import org.sonar.process.ProcessId;

public class SQProcess {

  enum State {
    INIT, STARTING, UP, OPERATIONAL, STOPPING, STOPPED
  }

  public ProcessId getId() {
    // TODO
    throw new UnsupportedOperationException();
  }

  public State getState() {
    throw new UnsupportedOperationException();
  }
}
