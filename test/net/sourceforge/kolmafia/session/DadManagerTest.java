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

  private String setResponseText(String[] clueTextList) {
    return "You shake your head and look above the tank, at the window into space. "
        + clueTextList[1]
        + " forms "
        + clueTextList[2]
        + " in the darkness, each more "
        + clueTextList[3]
        + " than the last. "
        + "The "
        + clueTextList[4]
        + " "
        + clueTextList[5]
        + ", "
        + clueTextList[6]
        + " revealing "
        + clueTextList[7]
        + "-dimensional monstrosities.. No. "
        + "Look again. There is nothing. Are your "
        + clueTextList[8]
        + " betraying you? "
        + "As if on cue, "
        + clueTextList[9]
        + "-sided triangles materialize and then disappear. "
        + "So impossible that your "
        + clueTextList[10]
        + " throbs.";
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

  @Test
  public void canSetAllWeaknessWhenValidResponseText() {
    String[] clueTextList = {
      "",
      "horrifying",
      "swim",
      "awful",
      "space",
      "cracks open",
      "suddenly",
      "32",
      "eyes",
      "3",
      "stomach"
    };
    String validResponseText = setResponseText(clueTextList);
    Element[] weaknessList = {
      Element.NONE,
      Element.SPOOKY,
      Element.PHYSICAL,
      Element.COLD,
      Element.PHYSICAL,
      Element.COLD,
      Element.SLEAZE,
      Element.SLEAZE,
      Element.SPOOKY,
      Element.NONE,
      Element.PHYSICAL
    };

    DadManager.solve(validResponseText);

    int index = 0;
    for (DadManager.Element elementalWeakness : DadManager.ElementalWeakness) {
      assertEquals(weaknessList[index++], elementalWeakness);
    }
  }

  @Test
  public void canSetWeakness10BeNone() {
    int targetIndex = 10;
    String[] clueTextList = {
      "",
      "chaotic",
      "shamble",
      "putrescent",
      "darkness",
      "wobbles",
      "suddenly",
      "64",
      "mind",
      "3",
      "head"
    };
    String responseText = setResponseText(clueTextList);

    DadManager.solve(responseText);

    Element noneWeakness = DadManager.weakness(targetIndex);
    assertEquals(Element.NONE, noneWeakness);
  }
}
