package net.sourceforge.kolmafia.moods;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafiaCLI;

public class NormalManaBurn extends ManaBurnStatement {

  @Override
  public boolean beforeManaBurn(Object o) {
    return !KoLCharacter.inZombiecore();
  }

  @Override
  public void afterManaBurn(Object minimumNumber) {
    int minimum = Math.max(0, ((Long) minimumNumber).intValue());
    String nextBurnCast;
    long currentMP = -1;

    while (currentMP != KoLCharacter.getCurrentMP()
        && (nextBurnCast = ManaBurnManager.getNextBurnCast(minimum)) != null) {
      currentMP = KoLCharacter.getCurrentMP();
      KoLmafiaCLI.DEFAULT_SHELL.executeLine(nextBurnCast);
    }
  }
}
