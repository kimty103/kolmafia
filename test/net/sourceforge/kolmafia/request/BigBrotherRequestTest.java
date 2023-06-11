package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withQuestProgress;
import static internal.matchers.Preference.isSetTo;
import static internal.matchers.Quest.isStep;
import static net.sourceforge.kolmafia.request.BigBrotherRequest.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import java.util.List;
import net.sourceforge.kolmafia.*;
import net.sourceforge.kolmafia.KoLConstants.Stat;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BigBrotherRequestTest {

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("Big Brother");
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("Big Brother");

    KoLConstants.inventory.clear();
  }

  /**
   * Purpose: test accessible adding item to inventory
   * Input: add item to inventory each call function
   * Expected:
   *    return "You haven't rescued Big Brother yet."
   *    return "You don't have the right equipment to adventure underwater."
   *    return "Your familiar doesn't have the right equipment to adventure underwater."
   *    return null
   */
  @Test
  @Order(1)
  void accessibleTest() {
    KoLConstants.inventory.clear();

    assertThat(BigBrotherRequest.accessible(), is("You haven't rescued Big Brother yet."));

    KoLConstants.inventory.add(BUBBLIN_STONE);

    assertThat(
        BigBrotherRequest.accessible(),
        is("You don't have the right equipment to adventure underwater."));

    KoLConstants.inventory.add(AERATED_DIVING_HELMET);

    assertThat(
        BigBrotherRequest.accessible(),
        is("Your familiar doesn't have the right equipment to adventure underwater."));

    KoLConstants.inventory.add(DAS_BOOT);

    assertThat(BigBrotherRequest.accessible(), is(nullValue()));
  }

  /**
   * Purpose: test canBuyItem adding item to inventory and change Preferences value
   * Input: add item to inventory and change Preferences value
   * Expected:
   *    return true before change values
   *    return false
   */
  @Test
  @Order(2)
  void canBuyItemTest() {
    CoinmasterData dataForTrueTest = BigBrotherRequest.BIG_BROTHER;

    List<Integer> items =
        List.of(ItemPool.MADNESS_REEF_MAP, ItemPool.DAMP_OLD_BOOT, ItemPool.BLACK_GLASS);

    for (Integer item : items) {
      assertThat(dataForTrueTest.canBuyItem(item), is(true));
    }

    KoLConstants.inventory.add(BLACK_GLASS);
    Preferences.setBoolean("mapToMadnessReefPurchased", true);
    Preferences.setBoolean("dampOldBootPurchased", true);

    CoinmasterData dataForFalseTest = BigBrotherRequest.BIG_BROTHER;

    for (Integer item : items) {
      assertThat(dataForFalseTest.canBuyItem(item), is(false));
    }

    assertThat(dataForFalseTest.canBuyItem(ItemPool.FOLDER_19), is(false));
    assertThat(dataForFalseTest.canBuyItem(ItemPool.HOUSE), is(false));

    Preferences.setBoolean("mapToMadnessReefPurchased", false);
    Preferences.setBoolean("dampOldBootPurchased", false);
  }

  /**
   * Purpose: test equip and update with adding or removing item in inventory
   * Input: list of item adventure request
   * Expected: BigBrotherRequest`s member variable self and familiar are change each adding or removing item
   */
  @Test
  @Order(3)
  void equipTest() {
    BigBrotherRequest bigBrotherRequest = new BigBrotherRequest();

    List<AdventureResult> selfResults =
        List.of(
            AERATED_DIVING_HELMET,
            SCHOLAR_MASK,
            GLADIATOR_MASK,
            CRAPPY_MASK,
            SCUBA_GEAR,
            OLD_SCUBA_TANK);
    List<AdventureResult> familiarResults = List.of(AMPHIBIOUS_TOPHAT, DAS_BOOT, BATHYSPHERE);

    KoLCharacter.setFamiliar(new FamiliarData(FamiliarPool.DANCING_FROG));

    KoLConstants.inventory.add(DAS_BOOT);

    for (AdventureResult result : selfResults) {
      KoLConstants.inventory.add(result);

      bigBrotherRequest.equip();

      KoLConstants.inventory.remove(result);
    }

    KoLConstants.inventory.add(AERATED_DIVING_HELMET);

    for (AdventureResult result : familiarResults) {
      KoLConstants.inventory.add(result);

      bigBrotherRequest.equip();

      KoLConstants.inventory.remove(result);
    }
  }

  @Nested
  class Quests {

    private Cleanups withSeaQuestProgress() {
      return new Cleanups(
          // Minimum to get to The Sea
          withQuestProgress(Quest.SEA_OLD_GUY, QuestDatabase.STARTED),
          // Minimum to talk to Big Brother
          withQuestProgress(Quest.SEA_MONKEES, "step3"),
          withProperty("bigBrotherRescued", true),
          withProperty("dampOldBootPurchased", false),
          withProperty("mapToAnemoneMinePurchased", false),
          withProperty("mapToMadnessReefPurchased", false),
          withProperty("mapToTheDiveBarPurchased", false),
          withProperty("mapToTheMarinaraTrenchPurchased", false),
          withProperty("mapToTheSkateParkPurchased", false));
    }

    @Test
    public void visitingBigBrotherAdvancesOldGuyQuest() {
      var builder = new FakeHttpClientBuilder();
      var cleanups = new Cleanups(withHttpClientBuilder(builder), withSeaQuestProgress());
      try (cleanups) {
        // This response text does not contain a damp old boot
        builder.client.addResponse(200, html("request/test_visit_big_brother.html"));
        String URL = "monkeycastle.php?who=2";
        GenericRequest request = new GenericRequest(URL);
        request.run();
        assertThat("dampOldBootPurchased", isSetTo(true));
        assertThat(Quest.SEA_OLD_GUY, isStep("step1"));
      }
    }

    @Test
    public void visitingBigBrotherDetectsOptionalMapsPurchased() {
      var builder = new FakeHttpClientBuilder();
      var cleanups = new Cleanups(withHttpClientBuilder(builder), withSeaQuestProgress());
      try (cleanups) {
        // This response text does not contain a map to Madness Reef
        // This response text does not contain a map to The Skate Park
        builder.client.addResponse(200, html("request/test_visit_big_brother.html"));
        String URL = "monkeycastle.php?who=2";
        GenericRequest request = new GenericRequest(URL);
        request.run();
        assertThat("mapToMadnessReefPurchased", isSetTo(true));
        assertThat("mapToTheSkateParkPurchased", isSetTo(true));
      }
    }

    @ParameterizedTest
    @EnumSource(
        value = AscensionClass.class,
        names = {"SEAL_CLUBBER", "SAUCEROR", "ACCORDION_THIEF"})
    public void visitingBigBrotherDetectsQuestMapsPurchased(AscensionClass clazz) {
      var builder = new FakeHttpClientBuilder();
      var cleanups =
          new Cleanups(withHttpClientBuilder(builder), withSeaQuestProgress(), withClass(clazz));
      try (cleanups) {
        // This response text does not contain a map to Anemone Mine
        // This response text does not contain a map to The Marinara Trench
        // This response text does not contain a map to The Five Bar
        builder.client.addResponse(200, html("request/test_visit_big_brother.html"));
        String URL = "monkeycastle.php?who=2";
        GenericRequest request = new GenericRequest(URL);
        request.run();
        assertThat("mapToAnemoneMinePurchased", isSetTo(clazz.getMainStat() != Stat.MUSCLE));
        assertThat(
            "mapToTheMarinaraTrenchPurchased", isSetTo(clazz.getMainStat() != Stat.MYSTICALITY));
        assertThat("mapToTheDiveBarPurchased", isSetTo(clazz.getMainStat() != Stat.MOXIE));
      }
    }
  }
}
