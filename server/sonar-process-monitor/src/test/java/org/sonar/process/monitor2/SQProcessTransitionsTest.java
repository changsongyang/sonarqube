package org.sonar.process.monitor2;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SQProcessTransitionsTest {

  @Test
  public void equals_and_hashcode() {
    SQProcessTransitions init = new SQProcessTransitions();
    assertThat(init.equals(init)).isTrue();
    assertThat(init.equals(null)).isFalse();
    assertThat(init.equals("INIT")).isFalse();
    assertThat(init.hashCode()).isEqualTo(new SQProcessTransitions().hashCode());

    SQProcessTransitions starting = new SQProcessTransitions();
    starting.tryToMoveTo(SQProcessTransitions.State.STARTING);
    assertThat(init.equals(starting)).isFalse();
  }

  @Test
  public void no_state_can_not_move_to_itself() {
    for (SQProcessTransitions.State state : SQProcessTransitions.State.values()) {
      assertThat(newTransition(state).tryToMoveTo(state)).isFalse();
    }
  }

  private SQProcessTransitions newTransition(SQProcessTransitions.State state) {
    SQProcessTransitions transition = new SQProcessTransitions();
    switch (state) {
      case INIT:
        return transition;
      case STARTED:
        transition.tryToMoveTo(SQProcessTransitions.State.STARTING);
        transition.tryToMoveTo(SQProcessTransitions.State.STARTED);
        return transition;
      case STOPPED:
        transition.tryToMoveTo(SQProcessTransitions.State.STARTING);
        transition.tryToMoveTo(SQProcessTransitions.State.STOPPING);
        transition.tryToMoveTo(SQProcessTransitions.State.STOPPED);
        return transition;
      case STARTING:
        transition.tryToMoveTo(SQProcessTransitions.State.STARTING);
        return transition;
      case STOPPING:
        transition.tryToMoveTo(SQProcessTransitions.State.STARTING);
        transition.tryToMoveTo(SQProcessTransitions.State.STOPPING);
        return transition;
      default:
          throw new IllegalArgumentException("Unsupported state " + state);
    }
  }
}
