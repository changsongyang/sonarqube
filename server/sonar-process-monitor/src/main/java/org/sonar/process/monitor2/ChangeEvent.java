package org.sonar.process.monitor2;

import java.util.Optional;


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

  private final Optional<SQProcess> sqProcess;
  private final Type type;

  /**
   * Instantiates a new Change event.
   *
   * @param sqProcess the sq process
   * @param type      the type
   */
  public ChangeEvent(SQProcess sqProcess, Type type) {
    this.sqProcess = Optional.ofNullable(sqProcess);
    this.type = type;
  }

  /**
   * Gets process ref.
   *
   * @return the process ref
   */
  public Optional<SQProcess> getProcessRef() {
    return sqProcess;
  }

  /**
   * Gets type of change
   *
   * @return {@link Type}
   */
  public Type getType() {
    return type;
  }
}
