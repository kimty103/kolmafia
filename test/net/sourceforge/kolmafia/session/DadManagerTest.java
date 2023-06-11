package net.sourceforge.kolmafia.session;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.DadManager.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DadManagerTest {

  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("DadManagerTest");
    Preferences.reset("DadManagerTest");
  }

  @Test
  public void canSearchElementName() {
    Element validElement = Element.SLEAZE;
    String elementName = DadManager.elementToName(validElement);
    assertEquals("sleaze", elementName);
  }

  @Test
  public void canReturnUnknownWhenCharacterHasNoSkill() {
    Element validElement = Element.SLEAZE;
    String elementSpell = DadManager.elementToSpell(validElement);
    assertEquals("Unknown", elementSpell);
  }

}
