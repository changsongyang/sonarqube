package org.sonar.process.monitor2;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.process.monitor2.SQProcessTransitions.State.INIT;
import static org.sonar.process.monitor2.SQProcessTransitions.State.STARTED;
import static org.sonar.process.monitor2.SQProcessTransitions.State.STARTING;
import static org.sonar.process.monitor2.SQProcessTransitions.State.STOPPED;
import static org.sonar.process.monitor2.SQProcessTransitions.State.STOPPING;

public class SQProcessTransitionsTest {

  @Test
  public void equals_and_hashcode() {
    SQProcessTransitions init = new SQProcessTransitions();
    assertThat(init.getState()).isEqualTo(INIT);
    assertThat(init.equals(init)).isTrue();
    assertThat(init.equals(null)).isFalse();
    assertThat(init.equals("INIT")).isFalse();
    assertThat(init.hashCode()).isEqualTo(new SQProcessTransitions().hashCode());

    SQProcessTransitions starting = new SQProcessTransitions();
    assertThat(starting.tryToMoveTo(STARTING)).isTrue();
    assertThat(starting.equals(init)).isFalse();
    assertThat(starting.equals(starting)).isTrue();
    assertThat(starting.getState()).isEqualTo(STARTING);
  }


  @Test
  public void can_move_to_STOPPING_from_STARTING_STARTED_only() {
    for (SQProcessTransitions.State state : SQProcessTransitions.State.values()) {
      boolean tryToMoveTo = newTransition(state).tryToMoveTo(STOPPING);
      if (state == STARTING || state == STARTED) {
        assertThat(tryToMoveTo).describedAs("from state " + state).isTrue();
      } else {
        assertThat(tryToMoveTo).describedAs("from state " + state).isFalse();
      }
    }
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
        transition.tryToMoveTo(STARTING);
        transition.tryToMoveTo(STARTED);
        return transition;
      case STOPPED:
        transition.tryToMoveTo(STARTING);
        transition.tryToMoveTo(STOPPING);
        transition.tryToMoveTo(STOPPED);
        return transition;
      case STARTING:
        transition.tryToMoveTo(STARTING);
        return transition;
      case STOPPING:
        transition.tryToMoveTo(STARTING);
        transition.tryToMoveTo(STOPPING);
        return transition;
      default:
          throw new IllegalArgumentException("Unsupported state " + state);
    }
  }
}
