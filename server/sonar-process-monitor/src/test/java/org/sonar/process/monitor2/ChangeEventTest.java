package org.sonar.process.monitor2;

import org.junit.Test;
import org.sonar.process.ProcessId;

import static org.assertj.core.api.Assertions.assertThat;

public class ChangeEventTest {
  @Test
  public void equals_and_hashcode() {
    ChangeEvent changeEvent = new ChangeEvent(ProcessId.APP, ChangeEventType.STARTED);
    for (ChangeEventType type : ChangeEventType.values()) {
      ChangeEvent temp = new ChangeEvent(ProcessId.APP, type);
      assertThat(changeEvent.equals(temp)).isEqualTo(type.equals(ChangeEventType.STARTED));
      if (type.equals(ChangeEventType.STARTED)) {
        assertThat(changeEvent.hashCode()).isEqualTo(temp.hashCode());
      } else {
        assertThat(changeEvent.hashCode()).isNotEqualTo(temp.hashCode());
      }
    }
    for (ProcessId processId : ProcessId.values()) {
      ChangeEvent temp = new ChangeEvent(processId, ChangeEventType.STARTED);
      assertThat(changeEvent.equals(temp)).isEqualTo(processId.equals(ProcessId.APP));
      if (processId.equals(ProcessId.APP)) {
        assertThat(changeEvent.hashCode()).isEqualTo(temp.hashCode());
      } else {
        assertThat(changeEvent.hashCode()).isNotEqualTo(temp.hashCode());
      }
    }

    assertThat(changeEvent.equals(changeEvent)).isTrue();
    assertThat(changeEvent.equals("STARTED")).isFalse();
    assertThat(changeEvent.toString())
      .isEqualTo(
        String.format("ChangeEvent{type=%s, processId=%s}",
          changeEvent.getType(), changeEvent.getProcessId()
        )
      );
  }
}
