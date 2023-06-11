package net.sourceforge.kolmafia.moods;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ExtraManaBurnTest {

  @Test
  public void when_burning_is_over_it_must_be_restored() {
    boolean beforeExecuting = MoodManager.isExecuting;
    ManaBurnManager.burnExtraMana(false);
    assertEquals(MoodManager.isExecuting, beforeExecuting);
  }
}
