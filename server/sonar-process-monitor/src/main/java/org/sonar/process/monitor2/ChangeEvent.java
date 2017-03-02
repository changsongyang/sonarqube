package org.sonar.process.monitor2;

import java.util.Optional;

public class ChangeEvent {

  public enum Type {
    RESTART_REQUESTED, PROCESS_STATE_CHANGE, STOP_REQUESTED
  }

  public Optional<SQProcess> getProcessRef() {
    throw new UnsupportedOperationException();
  }

  public Type getType() {
    throw new UnsupportedOperationException();
  }
}
