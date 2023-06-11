package net.sourceforge.kolmafia.moods;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.preferences.Preferences;

public class ExtraManaBurn extends ManaBurnStatement {

  private static final ExtraManaBurn INSTANCE = new ExtraManaBurn();

  private ExtraManaBurn() {}

  public static ExtraManaBurn getInstance() {
    return INSTANCE;
  }

  @Override
  public boolean beforeManaBurn(Object isManualInvocation) {
    if (KoLmafia.refusesContinue()
        || KoLCharacter.inZombiecore()
        || KoLCharacter.getLimitMode().limitRecovery()) {
      return false;
    }

    float manaBurnTrigger = Preferences.getFloat("manaBurningTrigger");
    return (boolean) isManualInvocation
        || KoLCharacter.getCurrentMP() >= (int) (manaBurnTrigger * KoLCharacter.getMaximumMP());
  }

  @Override
  public void afterManaBurn(Object o) {
    String nextBurnCast;
    long currentMP = -1;

    while (currentMP != KoLCharacter.getCurrentMP()
        && (nextBurnCast = ManaBurnManager.getNextBurnCast()) != null) {
      currentMP = KoLCharacter.getCurrentMP();
      KoLmafiaCLI.DEFAULT_SHELL.executeLine(nextBurnCast);
    }
  }
}
