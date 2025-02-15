package net.sourceforge.kolmafia.maximizer;

import static internal.helpers.Maximizer.commandStartsWith;
import static internal.helpers.Maximizer.getBoosts;
import static internal.helpers.Maximizer.getSlot;
import static internal.helpers.Maximizer.maximize;
import static internal.helpers.Maximizer.modFor;
import static internal.helpers.Maximizer.recommendedSlotIs;
import static internal.helpers.Maximizer.recommendedSlotIsUnchanged;
import static internal.helpers.Maximizer.recommends;
import static internal.helpers.Maximizer.someBoostIs;
import static internal.helpers.Player.withCampgroundItem;
import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withEffect;
import static internal.helpers.Player.withEquippableItem;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withFamiliarInTerrarium;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withLocation;
import static internal.helpers.Player.withMeat;
import static internal.helpers.Player.withNotAllowedInStandard;
import static internal.helpers.Player.withOverrideModifiers;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withRestricted;
import static internal.helpers.Player.withSkill;
import static internal.helpers.Player.withStats;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import java.util.Optional;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RestrictedItemType;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.modifiers.BitmapModifier;
import net.sourceforge.kolmafia.modifiers.DerivedModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.AdventureDatabase.Environment;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class MaximizerTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("MaximizerTest");
    Preferences.reset("MaximizerTest");
  }
  // basic

  @Test
  public void changesGear() {
    final var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
    try (cleanups) {
      assertTrue(maximize("mus"));
      assertEquals(1, modFor(DerivedModifier.BUFFED_MUS), 0.01);
    }
  }

  @Test
  public void equipsItemsOnlyIfHasStats() {
    final var cleanups =
        new Cleanups(withEquippableItem("helmet turtle"), withItem("wreath of laurels"));
    try (cleanups) {
      assertTrue(maximize("mus"));
      assertEquals(1, modFor(DerivedModifier.BUFFED_MUS), 0.01);
      recommendedSlotIs(Slot.HAT, "helmet turtle");
    }
  }

  @Test
  public void nothingBetterThanSomething() {
    final var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
    try (cleanups) {
      assertTrue(maximize("-mus"));
      assertEquals(0, modFor(DerivedModifier.BUFFED_MUS), 0.01);
    }
  }

  @Test
  public void exactMatchFindsModifier() {
    var cleanups =
        new Cleanups(
            withEquippableItem("hemlock helm"), withEquippableItem("government-issued slacks"));

    try (cleanups) {
      assertTrue(maximize("Muscle Experience Percent, -tie"));
      recommendedSlotIsUnchanged(Slot.HAT);
      recommendedSlotIs(Slot.PANTS, "government-issued slacks");
      assertEquals(10, modFor(DoubleModifier.MUS_EXPERIENCE_PCT), 0.01);
    }
  }

  @Nested
  class Max {
    @Test
    public void maxKeywordStopsCountingBeyondTarget() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("hardened slime hat"),
              withEquippableItem("bounty-hunting helmet"),
              withSkill("Refusal to Freeze"));
      try (cleanups) {
        assertTrue(maximize("cold res 3 max, 0.1 item drop"));

        assertEquals(3, modFor(DoubleModifier.COLD_RESISTANCE), 0.01);
        assertEquals(20, modFor(DoubleModifier.ITEMDROP), 0.01);

        recommendedSlotIs(Slot.HAT, "bounty-hunting helmet");
      }
    }

    @Test
    public void startingMaxKeywordTerminatesEarlyIfConditionMet() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("hardened slime hat"),
              withEquippableItem("bounty-hunting helmet"),
              withSkill("Refusal to Freeze"));
      try (cleanups) {
        maximize("3 max, cold res");

        assertTrue(
            Maximizer.boosts.stream()
                .anyMatch(
                    b ->
                        b.toString()
                            .contains("(maximum achieved, no further combinations checked)")));
      }
    }
  }

  @Nested
  class Min {
    @Test
    public void minKeywordFailsMaximizationIfNotHit() {
      final var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertFalse(maximize("mus 2 min"));
        // still provides equipment
        recommendedSlotIs(Slot.HAT, "helmet turtle");
      }
    }

    @Test
    public void minKeywordPassesMaximizationIfHit() {
      final var cleanups = new Cleanups(withEquippableItem("wreath of laurels"));
      try (cleanups) {
        assertTrue(maximize("mus 2 min"));
      }
    }

    @Test
    public void startingMinKeywordFailsMaximizationIfNotHit() {
      final var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertFalse(maximize("2 min, mus"));
        // still provides equipment
        recommendedSlotIs(Slot.HAT, "helmet turtle");
      }
    }

    @Test
    public void startingMinKeywordPassesMaximizationIfHit() {
      final var cleanups = new Cleanups(withEquippableItem("wreath of laurels"));
      try (cleanups) {
        assertTrue(maximize("2 min, mus"));
      }
    }
  }

  @Nested
  class Effective {
    @Test
    public void useRangedWeaponWhenMoxieHigh() {
      final var cleanups =
          new Cleanups(
              withStats(100, 100, 150),
              withEquippableItem("disco ball"),
              withEquippableItem("two-handed depthsword"));

      try (cleanups) {
        assertTrue(maximize("weapon dmg, effective"));
        recommendedSlotIs(Slot.WEAPON, "disco ball");
      }
    }

    @Test
    public void useMeleeWeaponWhenMuscleHigh() {
      final var cleanups =
          new Cleanups(
              withStats(150, 100, 100),
              withEquippableItem("automatic catapult"),
              withEquippableItem("seal-clubbing club"));

      try (cleanups) {
        assertTrue(maximize("weapon dmg, effective"));
        recommendedSlotIs(Slot.WEAPON, "seal-clubbing club");
      }
    }

    @Test
    public void useJuneCleaverWhenMoxieHigh() {
      final var cleanups =
          new Cleanups(
              withStats(100, 100, 150),
              withEquippableItem("disco ball"),
              withEquippableItem("June cleaver"));

      try (cleanups) {
        assertTrue(maximize("weapon dmg, effective"));
        recommendedSlotIs(Slot.WEAPON, "June cleaver");
      }
    }

    @Test
    public void useCosplaySaberWhenMoxieHigh() {
      final var cleanups =
          new Cleanups(
              withStats(100, 100, 150),
              withEquippableItem("disco ball"),
              withEquippableItem("Fourth of May Cosplay Saber"));

      try (cleanups) {
        assertTrue(maximize("weapon dmg, effective"));
        recommendedSlotIs(Slot.WEAPON, "Fourth of May Cosplay Saber");
      }
    }
  }

  @Nested
  class Clownosity {
    @Test
    public void clownosityTriesClownEquipment() {
      final var cleanups = new Cleanups(withEquippableItem("clown wig"));
      try (cleanups) {
        assertFalse(maximize("clownosity -tie"));
        // still provides equipment
        recommendedSlotIs(Slot.HAT, "clown wig");
        assertEquals(50, modFor(BitmapModifier.CLOWNINESS), 0.01);
      }
    }

    @Test
    public void clownositySucceedsWithEnoughEquipment() {
      final var cleanups =
          new Cleanups(withEquippableItem("clown wig"), withEquippableItem("polka-dot bow tie"));
      try (cleanups) {
        assertTrue(maximize("clownosity -tie"));
        recommendedSlotIs(Slot.HAT, "clown wig");
        recommendedSlotIs(Slot.ACCESSORY1, "polka-dot bow tie");
        assertEquals(125, modFor(BitmapModifier.CLOWNINESS), 0.01);
      }
    }

    @Test
    public void clownosityItemsDontStack() {
      var cleanups = withEquippableItem("clownskin belt", 3);

      try (cleanups) {
        maximize("clownosity, -tie");
        assertEquals(50, modFor(BitmapModifier.CLOWNINESS), 0.01);
        assertThat(
            getBoosts().stream()
                .filter(x -> x.isEquipment() && "clownskin belt".equals(x.getItem().getName()))
                .count(),
            equalTo(1L));
      }
    }
  }

  @Nested
  class Raveosity {
    @Test
    public void raveosityTriesRaveEquipment() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("rave visor"),
              withEquippableItem("baggy rave pants"),
              withEquippableItem("rave whistle"));
      try (cleanups) {
        assertFalse(maximize("raveosity -tie"));
        // still provides equipment
        recommendedSlotIs(Slot.HAT, "rave visor");
        recommendedSlotIs(Slot.PANTS, "baggy rave pants");
        recommendedSlotIs(Slot.WEAPON, "rave whistle");
        assertEquals(5, modFor(BitmapModifier.RAVEOSITY), 0.01);
      }
    }

    @Test
    public void raveositySucceedsWithEnoughEquipment() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("blue glowstick"),
              withEquippableItem("glowstick on a string"),
              withEquippableItem("teddybear backpack"),
              withEquippableItem("rave visor"),
              withEquippableItem("baggy rave pants"));
      try (cleanups) {
        assertTrue(maximize("raveosity -tie"));
        recommendedSlotIs(Slot.HAT, "rave visor");
        recommendedSlotIs(Slot.PANTS, "baggy rave pants");
        recommendedSlotIs(Slot.CONTAINER, "teddybear backpack");
        recommendedSlotIs(Slot.OFFHAND, "glowstick on a string");
        assertEquals(7, modFor(BitmapModifier.RAVEOSITY), 0.01);
      }
    }
  }

  @Nested
  class Surgeonosity {
    @Test
    public void surgeonosityTriesSurgeonEquipment() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("head mirror"),
              withEquippableItem("bloodied surgical dungarees"),
              withEquippableItem("surgical apron"),
              withEquippableItem("surgical mask"),
              withEquippableItem("half-size scalpel"),
              withSkill("Torso Awareness"));
      try (cleanups) {
        assertTrue(maximize("surgeonosity -tie"));
        recommendedSlotIs(Slot.PANTS, "bloodied surgical dungarees");
        recommends("head mirror");
        recommends("surgical mask");
        recommends("half-size scalpel");
        recommendedSlotIs(Slot.SHIRT, "surgical apron");
        assertEquals(5, modFor(BitmapModifier.SURGEONOSITY), 0.01);
      }
    }

    @Test
    public void surgeonosityItemsDontStack() {
      var cleanups = withEquippableItem("surgical mask", 3);

      try (cleanups) {
        maximize("surgeonosity, -tie");
        assertEquals(1, modFor(BitmapModifier.SURGEONOSITY), 0.01);
        assertThat(
            getBoosts().stream()
                .filter(x -> x.isEquipment() && "surgical mask".equals(x.getItem().getName()))
                .count(),
            equalTo(1L));
      }
    }
  }

  @Nested
  class Beecore {

    @Test
    public void itemsCanHaveAtMostTwoBeesByDefault() {
      final var cleanups =
          new Cleanups(
              withPath(Path.BEES_HATE_YOU), withEquippableItem("bubblewrap bottlecap turtleban"));
      try (cleanups) {
        maximize("mys");
        recommendedSlotIsUnchanged(Slot.HAT);
      }
    }

    @Test
    public void itemsCanHaveAtMostBeeosityBees() {
      final var cleanups =
          new Cleanups(
              withPath(Path.BEES_HATE_YOU), withEquippableItem("bubblewrap bottlecap turtleban"));
      try (cleanups) {
        maximize("mys, 5beeosity");
        recommendedSlotIs(Slot.HAT, "bubblewrap bottlecap turtleban");
      }
    }

    @Test
    public void beeosityDoesntApplyOutsideBeePath() {
      final var cleanups = new Cleanups(withEquippableItem("bubblewrap bottlecap turtleban"));
      try (cleanups) {
        maximize("mys");
        recommendedSlotIs(Slot.HAT, "bubblewrap bottlecap turtleban");
      }
    }

    @Nested
    class Crown {
      @Test
      public void canCrownFamiliarsWithBeesOutsideBeecore() {
        final var cleanups =
            new Cleanups(
                withEquippableItem("Crown of Thrones"),
                withFamiliarInTerrarium(FamiliarPool.LOBSTER), // 15% spell damage
                withFamiliarInTerrarium(FamiliarPool.GALLOPING_GRILL)); // 10% spell damage

        try (cleanups) {
          maximize("spell dmg");

          // used the lobster in the throne.
          assertTrue(someBoostIs(x -> commandStartsWith(x, "enthrone Rock Lobster")));
        }
      }

      @Test
      public void cannotCrownFamiliarsWithBeesInBeecore() {
        final var cleanups =
            new Cleanups(
                withPath(Path.BEES_HATE_YOU),
                withEquippableItem("Crown of Thrones"),
                withFamiliarInTerrarium(FamiliarPool.LOBSTER), // 15% spell damage
                withFamiliarInTerrarium(FamiliarPool.GALLOPING_GRILL)); // 10% spell damage

        try (cleanups) {
          maximize("spell dmg");

          // used the grill in the throne.
          assertTrue(someBoostIs(x -> commandStartsWith(x, "enthrone Galloping Grill")));
        }
      }
    }

    @Nested
    class Potions {
      @Test
      public void canUsePotionsWithBeesOutsideBeecore() {
        final var cleanups = new Cleanups(withItem("baggie of powdered sugar"));

        try (cleanups) {
          maximize("meat drop");

          assertTrue(someBoostIs(x -> commandStartsWith(x, "use 1 baggie of powdered sugar")));
        }
      }

      @Test
      public void cannotUsePotionsWithBeesInBeecore() {
        final var cleanups =
            new Cleanups(withPath(Path.BEES_HATE_YOU), withItem("baggie of powdered sugar"));

        try (cleanups) {
          maximize("meat drop");

          assertFalse(someBoostIs(x -> commandStartsWith(x, "use 1 baggie of powdered sugar")));
        }
      }
    }
  }

  @Nested
  class Plumber {
    @Test
    public void plumberCommandsErrorOutsidePlumber() {
      final var cleanups = new Cleanups(withPath(Path.AVATAR_OF_BORIS));

      try (cleanups) {
        assertFalse(maximize("plumber"));
        assertFalse(maximize("cold plumber"));
      }
    }

    @Test
    public void plumberCommandForcesSomePlumberItem() {
      final var cleanups =
          new Cleanups(
              withPath(Path.PATH_OF_THE_PLUMBER),
              withEquippableItem("work boots"),
              withEquippableItem("shiny ring", 3));

      try (cleanups) {
        assertTrue(maximize("plumber, mox"));
        recommends("work boots");
      }
    }

    @Test
    public void coldPlumberCommandForcesFlowerAndFrostyButton() {
      final var cleanups =
          new Cleanups(
              withPath(Path.PATH_OF_THE_PLUMBER),
              withEquippableItem("work boots"),
              withEquippableItem("bonfire flower"),
              withEquippableItem("frosty button"),
              withEquippableItem("shiny ring", 3));

      try (cleanups) {
        assertTrue(maximize("cold plumber, mox"));
        recommends("bonfire flower");
        recommends("frosty button");
      }
    }
  }

  @Nested
  class GelatinousNoob {
    @Test
    public void canAbsorbItemsForSkills() {
      final var cleanups =
          new Cleanups(
              withPath(Path.GELATINOUS_NOOB),
              withItem("Knob mushroom"),
              withItem("beer lens"),
              withItem("crossbow string"));

      try (cleanups) {
        assertTrue(maximize("meat"));
        assertTrue(someBoostIs(x -> commandStartsWith(x, "absorb ¶303"))); // Knob mushroom
        assertTrue(someBoostIs(x -> commandStartsWith(x, "absorb ¶443"))); // beer lens
        assertTrue(someBoostIs(x -> commandStartsWith(x, "absorb ¶109"))); // crossbow string
      }
    }

    @Test
    public void canAbsorbEquipmentForEnchants() {
      final var cleanups = new Cleanups(withPath(Path.GELATINOUS_NOOB), withItem("disco mask"));

      try (cleanups) {
        assertTrue(maximize("moxie -tie"));
        recommendedSlotIsUnchanged(Slot.HAT);
        assertTrue(someBoostIs(x -> commandStartsWith(x, "absorb ¶9"))); // disco mask
      }
    }

    @Test
    public void canAbsorbHelmetTurtleForEnchants() {
      final var cleanups = new Cleanups(withPath(Path.GELATINOUS_NOOB), withItem("helmet turtle"));

      try (cleanups) {
        assertTrue(maximize("muscle -tie"));
        recommendedSlotIsUnchanged(Slot.HAT);
        assertTrue(someBoostIs(x -> commandStartsWith(x, "absorb ¶3"))); // helmet turtle
      }
    }

    @Test
    public void canBenefitFromOutfits() {
      final var cleanups =
          new Cleanups(
              withPath(Path.GELATINOUS_NOOB),
              withEquippableItem("bugbear beanie"),
              withEquippableItem("bugbear bungguard"));

      try (cleanups) {
        assertTrue(maximize("spell dmg -tie"));
        recommendedSlotIs(Slot.HAT, "bugbear beanie");
        recommendedSlotIs(Slot.PANTS, "bugbear bungguard");
      }
    }

    @Test
    public void canBenefitFromOutfitsWithWeapons() {
      final var cleanups =
          new Cleanups(
              withPath(Path.GELATINOUS_NOOB),
              withEquippableItem("The Jokester's wig"),
              withEquippableItem("The Jokester's gun"),
              withEquippableItem("The Jokester's pants"));

      try (cleanups) {
        assertTrue(maximize("meat -tie"));
        recommendedSlotIs(Slot.HAT, "The Jokester's wig");
        recommendedSlotIs(Slot.WEAPON, "The Jokester's gun");
        recommendedSlotIs(Slot.PANTS, "The Jokester's pants");
      }
    }
  }

  @Nested
  class Letter {
    @Test
    public void equipLongestItems() {
      final var cleanups =
          new Cleanups(withEquippableItem("spiked femur"), withEquippableItem("sweet ninja sword"));

      try (cleanups) {
        maximize("letter");

        recommendedSlotIs(Slot.WEAPON, "sweet ninja sword");
      }
    }

    @Test
    public void equipMostLetterItems() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("asparagus knife"),
              withEquippableItem("sweet ninja sword"),
              withEquippableItem("Fourth of May Cosplay Saber"),
              withEquippableItem("old sweatpants"));

      try (cleanups) {
        maximize("letter n");

        recommendedSlotIs(Slot.WEAPON, "sweet ninja sword");
        recommendedSlotIs(Slot.PANTS, "old sweatpants");
      }
    }

    @Test
    public void equipMostNumberItems() {
      final var cleanups =
          new Cleanups(withEquippableItem("X-37 gun"), withEquippableItem("sweet ninja sword"));

      try (cleanups) {
        maximize("number");

        recommendedSlotIs(Slot.WEAPON, "X-37 gun");
      }
    }
  }

  @Nested
  class WeaponModifiers {
    @Test
    public void clubModifierDoesntAffectOffhand() {
      final var cleanups =
          new Cleanups(
              withSkill("Double-Fisted Skull Smashing"),
              withEquippableItem("flaming crutch", 2),
              withEquippableItem("white sword", 2),
              withEquippableItem("dense meat sword"));

      try (cleanups) {
        assertTrue(EquipmentManager.canEquip("white sword"), "Can equip white sword");
        assertTrue(EquipmentManager.canEquip("flaming crutch"), "Can equip flaming crutch");
        assertTrue(maximize("mus, club"));
        // Should equip 1 flaming crutch, 1 white sword.
        recommendedSlotIs(Slot.WEAPON, "flaming crutch");
        recommendedSlotIs(Slot.OFFHAND, "white sword");
      }
    }

    @Test
    public void swordModifierFavorsSword() {
      final var cleanups =
          new Cleanups(withEquippableItem("sweet ninja sword"), withEquippableItem("spiked femur"));

      try (cleanups) {
        assertTrue(maximize("spooky dmg, sword"));
        recommendedSlotIs(Slot.WEAPON, "sweet ninja sword");
      }
    }
  }

  // effect limits

  @Test
  public void maximizeGiveBestScoreWithEffectsAtNoncombatLimit() {
    final var cleanups =
        new Cleanups(
            withEquippableItem("Space Trip safety headphones"),
            withEquippableItem("Krampus Horn"),
            // get ourselves to -25 combat
            withEffect("Shelter of Shed"),
            withEffect("Smooth Movements"));

    try (cleanups) {
      assertTrue(
          EquipmentManager.canEquip("Space Trip safety headphones"),
          "Cannot equip Space Trip safety headphones");
      assertTrue(EquipmentManager.canEquip("Krampus Horn"), "Cannot equip Krampus Horn");
      assertTrue(
          maximize(
              "cold res,-combat -hat -weapon -offhand -back -shirt -pants -familiar -acc1 -acc2 -acc3"));
      assertEquals(
          25,
          modFor(DoubleModifier.COLD_RESISTANCE) - modFor(DoubleModifier.COMBAT_RATE),
          0.01,
          "Base score is 25");
      assertTrue(maximize("cold res,-combat -acc2 -acc3"));
      assertEquals(
          27,
          modFor(DoubleModifier.COLD_RESISTANCE) - modFor(DoubleModifier.COMBAT_RATE),
          0.01,
          "Maximizing one slot should reach 27");

      recommendedSlotIs(Slot.ACCESSORY1, "Krampus Horn");
    }
  }

  @Nested
  class Underwater {
    @Test
    public void aboveWaterZonesDoNotCheckUnderwaterNegativeCombat() {
      final var cleanups =
          new Cleanups(withLocation("Noob Cave"), withEquippableItem("Mer-kin sneakmask"));

      try (cleanups) {
        assertTrue(maximize("-combat -tie"));
        assertEquals(0, modFor(DoubleModifier.COMBAT_RATE), 0.01);

        recommendedSlotIsUnchanged(Slot.HAT);
      }
    }

    @Test
    public void underwaterZonesCheckUnderwaterNegativeCombat() {
      final var cleanups =
          new Cleanups(withLocation("The Ice Hole"), withEquippableItem("Mer-kin sneakmask"));

      try (cleanups) {
        assertEquals(
            AdventureDatabase.getEnvironment(Modifiers.currentLocation), Environment.UNDERWATER);
        assertTrue(maximize("-combat -tie"));

        recommendedSlotIs(Slot.HAT, "Mer-kin sneakmask");
      }
    }
  }

  @Nested
  class Outfits {
    @Test
    public void considersOutfitsIfHelpful() {
      final var cleanups =
          new Cleanups(withEquippableItem("eldritch hat"), withEquippableItem("eldritch pants"));

      try (cleanups) {
        assertTrue(maximize("item -tie"));

        assertEquals(50, modFor(DoubleModifier.ITEMDROP), 0.01);
        recommendedSlotIs(Slot.HAT, "eldritch hat");
        recommendedSlotIs(Slot.PANTS, "eldritch pants");
      }
    }

    @Test
    public void avoidsOutfitsIfOtherItemsBetter() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("eldritch hat"),
              withEquippableItem("eldritch pants"),
              withEquippableItem("Team Avarice cap"));

      try (cleanups) {
        assertTrue(maximize("item -tie"));

        assertEquals(100, modFor(DoubleModifier.ITEMDROP), 0.01);
        recommendedSlotIs(Slot.HAT, "Team Avarice cap");
      }
    }

    @Test
    public void forcingOutfitRequiresThatOutfit() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("bounty-hunting helmet"),
              withEquippableItem("bounty-hunting rifle"),
              withEquippableItem("bounty-hunting pants"),
              withEquippableItem("eldritch hat"),
              withEquippableItem("eldritch pants"));

      try (cleanups) {
        assertTrue(maximize("item -tie"));

        assertEquals(70, modFor(DoubleModifier.ITEMDROP), 0.01);
        recommendedSlotIs(Slot.HAT, "bounty-hunting helmet");
        recommendedSlotIs(Slot.WEAPON, "bounty-hunting rifle");
        recommendedSlotIs(Slot.PANTS, "bounty-hunting pants");

        assertTrue(maximize("item, +outfit Eldritch Equipage -tie"));
        assertEquals(65, modFor(DoubleModifier.ITEMDROP), 0.01);
        recommendedSlotIs(Slot.HAT, "eldritch hat");
        recommendedSlotIs(Slot.WEAPON, "bounty-hunting rifle");
        recommendedSlotIs(Slot.PANTS, "eldritch pants");

        assertTrue(maximize("item, -outfit Bounty-Hunting Rig -tie"));
        assertEquals(65, modFor(DoubleModifier.ITEMDROP), 0.01);
        recommendedSlotIs(Slot.HAT, "eldritch hat");
        recommendedSlotIs(Slot.WEAPON, "bounty-hunting rifle");
        recommendedSlotIs(Slot.PANTS, "eldritch pants");
      }
    }
  }

  @Nested
  class Synergy {
    @Test
    public void considersBrimstoneIfHelpful() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("Brimstone Beret"), withEquippableItem("Brimstone Boxers"));

      try (cleanups) {
        assertTrue(maximize("ml -tie"));
        assertEquals(4, modFor(DoubleModifier.MONSTER_LEVEL), 0.01);
        recommendedSlotIs(Slot.HAT, "Brimstone Beret");
        recommendedSlotIs(Slot.PANTS, "Brimstone Boxers");
      }
    }

    @Nested
    class Smithsness {
      @Test
      public void considersSmithsnessIfHelpful() {
        final var cleanups =
            new Cleanups(
                withEquippableItem("Half a Purse"), withEquippableItem("Hairpiece On Fire"));

        try (cleanups) {
          assertTrue(maximize("meat -tie"));
          recommendedSlotIs(Slot.OFFHAND, "Half a Purse");
          recommendedSlotIs(Slot.HAT, "Hairpiece On Fire");
        }
      }

      @Test
      public void usesFlaskfullOfHollowWithSmithsness() {
        final var cleanups =
            new Cleanups(
                withItem("Flaskfull of Hollow"),
                withStats(100, 100, 100),
                withEquipped(Slot.PANTS, "Vicar's Tutu"));

        try (cleanups) {
          assertTrue(maximize("muscle -tie"));
          assertTrue(someBoostIs(x -> commandStartsWith(x, "use 1 Flaskfull of Hollow")));
        }
      }

      @Test
      public void usesFlaskfullOfHollow() {
        final var cleanups =
            new Cleanups(withItem("Flaskfull of Hollow"), withStats(100, 100, 100));

        try (cleanups) {
          assertTrue(maximize("muscle -tie"));
          assertTrue(someBoostIs(x -> commandStartsWith(x, "use 1 Flaskfull of Hollow")));
        }
      }
    }

    @Test
    public void considersCloathingIfHelpful() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("Goggles of Loathing"), withEquippableItem("Jeans of Loathing"));

      try (cleanups) {
        assertTrue(maximize("item -tie"));
        assertEquals(2, modFor(DoubleModifier.ITEMDROP), 0.01);
        recommendedSlotIs(Slot.HAT, "Goggles of Loathing");
        recommendedSlotIs(Slot.PANTS, "Jeans of Loathing");
      }
    }

    @Nested
    class SlimeHatesIt {
      @Test
      public void considersInSlimeTube() {
        final var cleanups =
            new Cleanups(
                withLocation("The Slime Tube"),
                withEquippableItem("pernicious cudgel"),
                withEquippableItem("grisly shield"),
                withEquippableItem("shield of the Skeleton Lord"),
                withItem("bitter pill"));

        try (cleanups) {
          assertTrue(maximize("ml -tie"));

          recommendedSlotIs(Slot.WEAPON, "pernicious cudgel");
          recommendedSlotIs(Slot.OFFHAND, "grisly shield");
          assertTrue(someBoostIs(x -> commandStartsWith(x, "use 1 bitter pill")));
        }
      }

      @Test
      public void doesntCountIfNotInSlimeTube() {
        final var cleanups =
            new Cleanups(
                withLocation("Noob Cave"),
                withEquippableItem("pernicious cudgel"),
                withEquippableItem("grisly shield"),
                withEquippableItem("shield of the Skeleton Lord"));

        try (cleanups) {
          assertTrue(maximize("ml -tie"));

          recommendedSlotIs(Slot.OFFHAND, "shield of the Skeleton Lord");
        }
      }
    }

    @Nested
    class HoboPower {
      @Test
      public void usesHoboPowerIfPossible() {
        final var cleanups =
            new Cleanups(
                withEquippableItem("Hodgman's garbage sticker"),
                withEquippableItem("Hodgman's bow tie"),
                withEquippableItem("Hodgman's lobsterskin pants"),
                withEquippableItem("Hodgman's porkpie hat"),
                withEquippableItem("silver cow creamer"));

        try (cleanups) {
          assertTrue(maximize("meat -tie"));

          recommendedSlotIs(Slot.HAT, "Hodgman's porkpie hat");
          recommendedSlotIs(Slot.PANTS, "Hodgman's lobsterskin pants");
          recommendedSlotIs(Slot.OFFHAND, "Hodgman's garbage sticker");
          recommends("Hodgman's bow tie");
        }
      }

      @Test
      public void hoboPowerDoesntCountWithoutOffhand() {
        final var cleanups =
            new Cleanups(
                withEquippableItem("Hodgman's bow tie"), withEquippableItem("silver cow creamer"));

        try (cleanups) {
          assertTrue(maximize("meat -tie"));

          assertEquals(30, modFor(DoubleModifier.MEATDROP), 0.01);
          recommendedSlotIs(Slot.OFFHAND, "silver cow creamer");
          recommendedSlotIsUnchanged(Slot.ACCESSORY1);
          recommendedSlotIsUnchanged(Slot.ACCESSORY2);
          recommendedSlotIsUnchanged(Slot.ACCESSORY3);
        }
      }
    }
  }

  @Nested
  class Mutex {
    @Test
    public void equipAtMostOneHalo() {
      var cleanups =
          new Cleanups(
              withEquippableItem(ItemPool.SHINING_HALO),
              withEquippableItem(ItemPool.TIME_HALO),
              withEquippableItem(ItemPool.TIME_SWORD));

      try (cleanups) {
        assertTrue(maximize("adv, exp"));

        assertEquals(5, modFor(DoubleModifier.ADVENTURES), 0.01);
        recommendedSlotIsUnchanged(Slot.WEAPON);
        recommendedSlotIs(Slot.ACCESSORY1, "time halo");
        recommendedSlotIsUnchanged(Slot.ACCESSORY2);
        recommendedSlotIsUnchanged(Slot.ACCESSORY3);
      }
    }
  }

  @Nested
  class Modeables {
    @Test
    public void canFoldUmbrella() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("unbreakable umbrella"), withProperty("umbrellaState", "cocoon"));

      try (cleanups) {
        assertTrue(maximize("Monster Level Percent"));
        assertTrue(someBoostIs(b -> commandStartsWith(b, "umbrella broken")));
        recommendedSlotIs(Slot.OFFHAND, "unbreakable umbrella");
      }
    }

    @Test
    public void expShouldSuggestUmbrella() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("unbreakable umbrella"),
              withEquipped(Slot.PANTS, "old patched suit-pants"),
              withEquippableItem("Microplushie: Hipsterine"),
              withProperty("umbrellaState", "cocoon"));

      try (cleanups) {
        assertTrue(maximize("exp"));
        assertTrue(someBoostIs(b -> commandStartsWith(b, "umbrella broken")));
        recommendedSlotIs(Slot.OFFHAND, "unbreakable umbrella");
      }
    }

    @Test
    public void expShouldNotSuggestUmbrellaIfBetterInSlot() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("unbreakable umbrella"),
              withEquipped(Slot.PANTS, "old patched suit-pants"),
              withEquippableItem("vinyl shield"),
              withProperty("umbrellaState", "cocoon"));

      try (cleanups) {
        assertTrue(maximize("exp"));
        recommendedSlotIs(Slot.OFFHAND, "vinyl shield");
      }
    }

    @Test
    public void chooseForwardFacingUmbrellaToSatisfyShield() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("unbreakable umbrella"),
              withEquippableItem("tip jar"),
              withEquipped(Slot.PANTS, "old sweatpants"));

      try (cleanups) {
        assertTrue(maximize("meat, shield"));
        assertTrue(someBoostIs(b -> commandStartsWith(b, "umbrella forward-facing")));
        recommendedSlotIs(Slot.OFFHAND, "unbreakable umbrella");
      }
    }

    @Test
    public void edPieceChoosesFishWithSea() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("The Crown of Ed the Undying"),
              withEquippableItem("star shirt"),
              withEquipped(Slot.PANTS, "old sweatpants"),
              withProperty("edPiece", "puma"));

      try (cleanups) {
        assertTrue(maximize("muscle, sea"));
        assertTrue(someBoostIs(b -> commandStartsWith(b, "edpiece fish")));
        recommendedSlotIs(Slot.HAT, "The Crown of Ed the Undying");
      }
    }

    @Test
    public void edPieceChoosesBasedOnModesWithoutSea() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("The Crown of Ed the Undying"),
              withEquippableItem("star shirt"),
              withEquipped(Slot.PANTS, "old sweatpants"),
              withProperty("edPiece", "puma"));

      try (cleanups) {
        assertTrue(maximize("muscle"));
        assertTrue(someBoostIs(b -> commandStartsWith(b, "edpiece bear")));
        recommendedSlotIs(Slot.HAT, "The Crown of Ed the Undying");
      }
    }

    @Test
    public void multipleModeables() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("backup camera"),
              withEquippableItem("unbreakable umbrella"),
              withEquipped(Slot.PANTS, "old sweatpants"));
      try (cleanups) {
        assertTrue(maximize("ml, -combat, equip backup camera, equip unbreakable umbrella"));
        assertTrue(someBoostIs(b -> commandStartsWith(b, "umbrella cocoon")));
        assertTrue(someBoostIs(b -> commandStartsWith(b, "backupcamera ml")));
      }
    }

    @Test
    public void doesNotSelectUmbrellaIfNegativeToOurGoal() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("unbreakable umbrella"),
              withEquippableItem("old sweatpants"),
              withEquippableItem("star boomerang"));

      try (cleanups) {
        assertTrue(maximize("-hp"));
        recommendedSlotIs(Slot.WEAPON, "star boomerang");
        assertThat(getSlot(Slot.OFFHAND), equalTo(Optional.empty()));
      }
    }

    @Test
    public void equipUmbrellaOnLeftHandMan() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("unbreakable umbrella"),
              withFamiliar(FamiliarPool.LEFT_HAND),
              withEquipped(Slot.PANTS, "old patched suit-pants"),
              withEquippableItem("Microplushie: Hipsterine"),
              withProperty("umbrellaState", "cocoon"));

      try (cleanups) {
        assertTrue(maximize("exp, -offhand"));
        recommendedSlotIs(Slot.FAMILIAR, "unbreakable umbrella");
        assertThat(getSlot(Slot.OFFHAND), equalTo(Optional.empty()));
      }
    }

    @Test
    public void suggestEquippingUmbrellaOnLeftHandMan() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("unbreakable umbrella"),
              withFamiliarInTerrarium(FamiliarPool.LEFT_HAND),
              withEquipped(Slot.PANTS, "old patched suit-pants"),
              withEquippableItem("Microplushie: Hipsterine"),
              withProperty("umbrellaState", "cocoon"));

      try (cleanups) {
        assertTrue(maximize("exp, -offhand, switch left-hand man"));
        assertThat(someBoostIs(b -> commandStartsWith(b, "familiar Left-Hand Man")), equalTo(true));
        assertThat(
            someBoostIs(b -> commandStartsWith(b, "umbrella broken; equip familiar ¶10899")),
            equalTo(true));
      }
    }

    @Test
    public void suggestEquippingSomethingBetterThanUmbrellaOnLeftHandMan() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("unbreakable umbrella"),
              withFamiliarInTerrarium(FamiliarPool.LEFT_HAND),
              withEquipped(Slot.PANTS, "old patched suit-pants"),
              withEquippableItem("shield of the Skeleton Lord"),
              withProperty("umbrellaState", "cocoon"));

      try (cleanups) {
        assertTrue(maximize("exp, -offhand, switch left-hand man"));
        assertThat(someBoostIs(b -> commandStartsWith(b, "familiar Left-Hand Man")), equalTo(true));
        assertThat(someBoostIs(b -> commandStartsWith(b, "equip familiar ¶9890")), equalTo(true));
      }
    }

    @Test
    public void shouldSuggestTunedRetrocape() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("unwrapped knock-off retro superhero cape"),
              withEquippableItem("palm-frond cloak"),
              withProperty("retroCapeSuperhero", "vampire"),
              withProperty("retroCapeWashingInstructions", "thrill"));

      try (cleanups) {
        assertTrue(maximize("hot res"));
        recommendedSlotIs(Slot.CONTAINER, "unwrapped knock-off retro superhero cape");
        assertTrue(someBoostIs(x -> commandStartsWith(x, "retrocape vampire hold")));
      }
    }

    @Test
    public void shouldSuggestTunedSnowsuit() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("Snow Suit"),
              withEquippableItem("wax lips"),
              withFamiliar(FamiliarPool.BLOOD_FACED_VOLLEYBALL));

      try (cleanups) {
        assertTrue(maximize("exp, hp regen"));
        recommendedSlotIs(Slot.FAMILIAR, "Snow Suit");
        assertTrue(someBoostIs(x -> commandStartsWith(x, "snowsuit goatee")));
      }
    }

    @Test
    public void shouldSuggestCameraIfSecondBest() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("backup camera"),
              withEquippableItem("incredibly dense meat gem"),
              withProperty("backupCameraMode", "ml"));

      try (cleanups) {
        assertTrue(maximize("meat"));
        recommends(ItemPool.BACKUP_CAMERA);
        recommends("incredibly dense meat gem");
        assertTrue(someBoostIs(x -> commandStartsWith(x, "backupcamera meat")));
      }
    }

    @Test
    public void shouldSuggestCameraIfSlotsExcluded() {
      final var cleanups =
          new Cleanups(withEquippableItem("backup camera"), withProperty("backupCameraMode", "ml"));

      try (cleanups) {
        assertTrue(maximize("meat -acc1 -acc2"));
        recommendedSlotIs(Slot.ACCESSORY3, "backup camera");
        assertTrue(someBoostIs(x -> commandStartsWith(x, "backupcamera meat")));
      }
    }
  }

  @Nested
  class Horsery {
    @Test
    public void suggestsHorseryIfAvailable() {
      var cleanups = withProperty("horseryAvailable", true);

      try (cleanups) {
        assertTrue(maximize("-combat"));
        assertTrue(someBoostIs(x -> commandStartsWith(x, "horsery dark")));
      }
    }

    @Test
    public void doesNotSuggestHorseryIfUnaffordable() {
      var cleanups =
          new Cleanups(
              withProperty("horseryAvailable", true),
              withProperty("_horsery", "normal horse"),
              withMeat(0));

      try (cleanups) {
        assertTrue(maximize("-combat"));
        assertFalse(someBoostIs(x -> commandStartsWith(x, "horsery dark")));
      }
    }

    @Test
    public void doesNotSuggestHorseryIfNotAllowedInStandard() {
      var cleanups =
          new Cleanups(
              withProperty("horseryAvailable", true),
              withRestricted(true),
              withNotAllowedInStandard(RestrictedItemType.ITEMS, "Horsery contract"));

      try (cleanups) {
        assertTrue(maximize("-combat"));
        assertFalse(someBoostIs(x -> commandStartsWith(x, "horsery dark")));
      }
    }
  }

  @Nested
  public class Familiars {
    @Test
    public void leftHandManEquipsItem() {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.LEFT_HAND),
              withEquippableItem(ItemPool.WICKER_SHIELD, 2),
              withItem(ItemPool.STUFFED_CHEST) // equipment with no enchant to test modifiers crash
              );

      try (cleanups) {
        assertTrue(maximize("moxie"));
        recommendedSlotIs(Slot.OFFHAND, "wicker shield");
        recommendedSlotIs(Slot.FAMILIAR, "wicker shield");
      }
    }

    @Test
    public void switchFamiliarConsidersGenericItems() {
      var cleanups =
          new Cleanups(
              withFamiliarInTerrarium(FamiliarPool.MOSQUITO),
              withItem(ItemPool.SOLID_SHIFTING_TIME_WEIRDNESS) // 4 adv with any familiar
              );

      try (cleanups) {
        assertTrue(maximize("adv -tie +switch mosquito"));
        recommendedSlotIs(Slot.FAMILIAR, "solid shifting time weirdness");
        assertThat(someBoostIs(b -> commandStartsWith(b, "familiar Mosquito")), equalTo(true));
      }
    }

    @Test
    public void switchMultipleFamiliarsConsidersMultipleItems() {
      var cleanups =
          new Cleanups(
              withFamiliarInTerrarium(FamiliarPool.TRICK_TOT),
              withFamiliarInTerrarium(FamiliarPool.HAND),
              withFamiliarInTerrarium(FamiliarPool.MOSQUITO),
              withItem(ItemPool.TRICK_TOT_UNICORN), // 5 adv with tot
              withItem(ItemPool.TRICK_TOT_CANDY), // 0 adv
              withItem(ItemPool.TIME_SWORD), // 3 adv with hand
              withItem(ItemPool.SOLID_SHIFTING_TIME_WEIRDNESS) // 4 adv with any familiar
              );

      try (cleanups) {
        assertTrue(
            maximize(
                "adv -weapon -offhand -tie +switch tot +switch disembodied hand +switch mosquito"));
        recommendedSlotIs(Slot.FAMILIAR, "li'l unicorn costume");
        assertThat(
            someBoostIs(b -> commandStartsWith(b, "familiar Trick-or-Treating Tot")),
            equalTo(true));
      }
    }

    @Test
    public void switchMultipleFamiliarsWithFoldable() {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.MOSQUITO),
              withFamiliarInTerrarium(FamiliarPool.BADGER),
              withFamiliarInTerrarium(FamiliarPool.PURSE_RAT, 400),
              withItem(ItemPool.LIARS_PANTS));

      try (cleanups) {
        assertTrue(maximize("ml +switch badger +switch purse rat"));
        recommendedSlotIs(Slot.FAMILIAR, "flaming familiar doppelgänger");
        assertThat(someBoostIs(b -> commandStartsWith(b, "familiar Purse Rat")), equalTo(true));
      }
    }
  }

  @Nested
  public class Uniques {
    @Test
    public void suggestsBestNonStackingWatchForAdventures() {
      var cleanups =
          new Cleanups(
              withEquippableItem("Counterclockwise Watch"), // 10, watch
              withEquippableItem("grandfather watch"), // 6, also a watch
              withEquippableItem("plexiglass pocketwatch"), // 3, stacks
              withEquippableItem("gold wedding ring") // 1
              );

      try (cleanups) {
        assertTrue(maximize("adv"));
        assertEquals(14, modFor(DoubleModifier.ADVENTURES), 0.01);
        recommends("Counterclockwise Watch");
        recommends("plexiglass pocketwatch");
        recommends("gold wedding ring");
      }
    }

    @Test
    public void watchesDontStackOutsideAdventuresEither() {
      var cleanups =
          new Cleanups(
              withEquippableItem(ItemPool.SASQ_WATCH), // 3, watch
              withEquippableItem("Crimbolex watch") // 5, also a watch
              );

      try (cleanups) {
        assertTrue(maximize("fites"));
        assertEquals(5, modFor(DoubleModifier.PVP_FIGHTS), 0.01);
        recommends("Crimbolex watch");
        assertFalse(
            someBoostIs(x -> x.isEquipment() && ItemPool.SASQ_WATCH == x.getItem().getItemId()));
      }
    }

    @Test
    public void surgeonosityItemsStackOutsideSurgeonosity() {
      var cleanups = withEquippableItem("surgical mask", 3);

      try (cleanups) {
        assertTrue(maximize("mp, -tie"));
        assertEquals(120, modFor(DoubleModifier.MP), 0.01);
        assertThat(
            getBoosts().stream()
                .filter(x -> x.isEquipment() && "surgical mask".equals(x.getItem().getName()))
                .count(),
            equalTo(3L));
        assertEquals(1, modFor(BitmapModifier.SURGEONOSITY), 0.01);
      }
    }

    @Test
    public void clownosityItemsStackOutsideClownosity() {
      var cleanups = withEquippableItem("clownskin belt", 3);

      try (cleanups) {
        assertTrue(maximize("mp, -tie"));
        assertEquals(45, modFor(DoubleModifier.MP), 0.01);
        assertThat(
            getBoosts().stream()
                .filter(x -> x.isEquipment() && "clownskin belt".equals(x.getItem().getName()))
                .count(),
            equalTo(3L));
        assertEquals(50, modFor(BitmapModifier.CLOWNINESS), 0.01);
      }
    }

    @Test
    public void raveosityItemsStackOutsideRaveosity() {
      var cleanups = withEquippableItem("blue glowstick", 3);

      try (cleanups) {
        assertTrue(maximize("mp, -tie"));
        assertEquals(15, modFor(DoubleModifier.MP), 0.01);
        assertThat(
            getBoosts().stream()
                .filter(x -> x.isEquipment() && "blue glowstick".equals(x.getItem().getName()))
                .count(),
            equalTo(3L));
        assertEquals(1, modFor(BitmapModifier.RAVEOSITY), 0.01);
      }
    }

    @Test
    public void brimstoneItemsStackOutsideBrimstone() {
      var cleanups =
          new Cleanups(
              withEquippableItem("Brimstone Bludgeon", 3),
              withSkill(SkillPool.DOUBLE_FISTED_SKULL_SMASHING));

      try (cleanups) {
        assertTrue(maximize("muscle, -tie"));
        assertEquals(100, modFor(DoubleModifier.MUS_PCT), 0.01);
        recommendedSlotIs(Slot.WEAPON, "Brimstone Bludgeon");
        recommendedSlotIs(Slot.OFFHAND, "Brimstone Bludgeon");
        assertEquals(1, modFor(BitmapModifier.BRIMSTONE), 0.01);
      }
    }

    @Test
    public void cloathingItemsStackOutsideCloathing() {
      var cleanups =
          new Cleanups(
              withEquippableItem("Stick-Knife of Loathing", 3),
              withSkill(SkillPool.DOUBLE_FISTED_SKULL_SMASHING));

      try (cleanups) {
        assertTrue(maximize("spell dmg, -tie"));
        assertEquals(400, modFor(DoubleModifier.SPELL_DAMAGE_PCT), 0.01);
        recommendedSlotIs(Slot.WEAPON, "Stick-Knife of Loathing");
        recommendedSlotIs(Slot.OFFHAND, "Stick-Knife of Loathing");
        assertEquals(1, modFor(BitmapModifier.CLOATHING), 0.01);
      }
    }
  }

  @Nested
  public class Foldables {
    @Test
    public void forcedFoldablePreventsOtherSlots() {
      var cleanups = withEquippableItem(ItemPool.ICE_SICKLE);

      try (cleanups) {
        assertTrue(maximize("ml, +equip ice baby"));
        recommendedSlotIsUnchanged(Slot.WEAPON);
        recommendedSlotIs(Slot.OFFHAND, "ice baby");
      }
    }

    @Test
    public void prefersFoldableInSlotWithHigherScore() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.ORIGAMI_MAGAZINE),
              withSkill(SkillPool.TORSO),
              withFamiliar(FamiliarPool.GHUOL_WHELP));

      try (cleanups) {
        assertTrue(maximize("meat, sleaze dmg, -tie"));
        recommendedSlotIsUnchanged(Slot.WEAPON);
        recommendedSlotIs(Slot.SHIRT, "origami pasties");
        recommendedSlotIsUnchanged(Slot.FAMILIAR);
      }
    }
  }

  @Nested
  public class Booleans {
    @Nested
    public class NeverFumble {
      @Test
      public void unableToNeverFumbleMarksAsFailed() {
        assertFalse(maximize("never fumble"));
      }

      @Test
      public void requiresConditionToSucceed() {
        var cleanups =
            new Cleanups(
                withEquippableItem("aerogel anvil"),
                withEquippableItem("Baron von Ratsworth's monocle"),
                withEquippableItem("observational glasses"),
                withEquippableItem("ring of the Skeleton Lord"));

        try (cleanups) {
          assertTrue(maximize("never fumble"));
          recommends("aerogel anvil");
          recommends("ring of the Skeleton Lord");
          recommends("Baron von Ratsworth's monocle");
        }
      }
    }

    @Nested
    public class Pirate {
      @Test
      public void unableToPirateMarksAsFailed() {
        assertFalse(maximize("pirate"));
      }

      @Test
      public void fledgesOrOutfitBothCount() {
        var cleanups =
            new Cleanups(
                withEquippableItem("eyepatch"),
                withEquippableItem("swashbuckling pants"),
                withEquippableItem("Pantsgiving"),
                withEquippableItem("stuffed shoulder parrot"),
                withEquippableItem("pirate fledges"),
                withEquippableItem("moustache sock"),
                withEquippableItem("tube sock"),
                withEquippableItem("mirrored aviator shades"));

        try (cleanups) {
          assertTrue(maximize("pirate, meat, -tie"));
          recommends("pirate fledges");
          recommends("Pantsgiving");

          assertTrue(maximize("pirate, moxie, -tie"));
          recommendedSlotIs(Slot.HAT, "eyepatch");
          recommendedSlotIs(Slot.PANTS, "swashbuckling pants");
          recommends("stuffed shoulder parrot");
          recommends("moustache sock");
          recommends("tube sock");
        }
      }
    }

    @Nested
    public class Sea {
      @Test
      public void unableToAdventureInSeaMarksAsFailed() {
        assertFalse(maximize("sea"));
      }

      @Test
      public void prefersSeaGearToHigherScore() {
        var cleanups =
            new Cleanups(
                withFamiliar(FamiliarPool.MOSQUITO),
                withEquippableItem(ItemPool.DAS_BOOT),
                withEquippableItem("Mer-kin scholar mask"),
                withEquippableItem("Lens of Violence"),
                withEquippableItem("old SCUBA tank"));

        try (cleanups) {
          assertTrue(maximize("item, sea"));
          recommendedSlotIs(Slot.HAT, "Mer-kin scholar mask");
          recommendedSlotIs(Slot.FAMILIAR, "das boot");
          recommendedSlotIsUnchanged(Slot.CONTAINER);
        }
      }
    }
  }

  @Nested
  public class Chefstaves {
    @Test
    public void cantEquipCheffstaffsOnLeftHandMan() {
      var cleanups =
          new Cleanups(
              withEquippableItem("Staff of Kitchen Royalty"),
              withFamiliar(FamiliarPool.LEFT_HAND),
              withSkill(SkillPool.SPIRIT_OF_RIGATONI));

      try (cleanups) {
        assertTrue(maximize("spell dmg, -weapon"));
        recommendedSlotIsUnchanged(Slot.FAMILIAR);
      }
    }

    @Test
    public void mustEquipSauceGloveForChefstaff() {
      var cleanups =
          new Cleanups(
              withEquippableItem("Staff of Kitchen Royalty"),
              withEquippableItem("special sauce glove"),
              withClass(AscensionClass.SAUCEROR));

      try (cleanups) {
        assertTrue(maximize("spell dmg, -tie"));
        recommendedSlotIs(Slot.WEAPON, "Staff of Kitchen Royalty");
        recommends("special sauce glove");
      }
    }

    @Test
    public void cannotUseSauceGloveIfNotSauceror() {
      var cleanups =
          new Cleanups(
              withEquippableItem("Staff of Kitchen Royalty"),
              withEquippableItem("special sauce glove"),
              withClass(AscensionClass.SEAL_CLUBBER));

      try (cleanups) {
        assertTrue(maximize("spell dmg, -tie"));
        recommendedSlotIsUnchanged(Slot.WEAPON);
      }
    }

    @Test
    public void canUseSkillToEquipChefstaves() {
      var cleanups =
          new Cleanups(
              withEquippableItem("Staff of Kitchen Royalty"),
              withSkill(SkillPool.SPIRIT_OF_RIGATONI));

      try (cleanups) {
        assertTrue(maximize("spell dmg, -tie"));
        recommendedSlotIs(Slot.WEAPON, "Staff of Kitchen Royalty");
      }
    }
  }

  @Nested
  public class VampireVintnerWine {
    @Test
    public void doesNotSuggestVintnerWineIfUnavailable() {
      var cleanups = new Cleanups(withItem(ItemPool.VAMPIRE_VINTNER_WINE, 0));

      try (cleanups) {
        assertTrue(maximize("Item Drop"));
        assertFalse(someBoostIs(x -> commandStartsWith(x, "drink 1 1950 Vampire Vintner wine")));
      }
    }

    @Test
    public void doesSuggestVintnerWineIfAvailableWithCorrectEffect() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.VAMPIRE_VINTNER_WINE, 1),
              withProperty("vintnerWineEffect", "Wine-Hot"),
              withProperty("vintnerWineLevel", 12));

      try (cleanups) {
        assertTrue(maximize("Item Drop"));
        assertTrue(someBoostIs(x -> commandStartsWith(x, "drink 1 1950 Vampire Vintner wine")));
      }
    }

    @Test
    public void doesNotSuggestVintnerWineIfAvailableWithWrongEffect() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.VAMPIRE_VINTNER_WINE, 1),
              withProperty("vintnerWineEffect", "Wine-Hot"),
              withProperty("vintnerWineLevel", 12));

      try (cleanups) {
        assertTrue(maximize("Monster Level"));
        assertFalse(someBoostIs(x -> commandStartsWith(x, "drink 1 1950 Vampire Vintner wine")));
      }
    }
  }

  @Nested
  class Skills {
    @Test
    public void suggestsSkillsIfRelevant() {
      var cleanups = new Cleanups(withSkill(SkillPool.SCARYSAUCE));

      try (cleanups) {
        assertTrue(maximize("cold res"));
        assertTrue(someBoostIs(x -> commandStartsWith(x, "cast 1 Scarysauce")));
      }
    }

    @Nested
    class BirdOfTheDay {
      @Test
      public void suggestsBirdIfRelevant() {
        var cleanups =
            new Cleanups(
                withProperty("_canSeekBirds", true),
                withProperty("_birdOfTheDay", "Filthy Smiling Pine Parrot"),
                withProperty(
                    "_birdOfTheDayMods",
                    "Mysticality Percent: +75, Stench Resistance: +2, Experience: +2, MP Regen Min: 10, MP Regen Max: 20"),
                withProperty("yourFavoriteBird", "Southern Clandestine Fig Chachalaca"),
                withProperty(
                    "yourFavoriteBirdMods",
                    "Stench Resistance: +2, Combat Rate: -9, MP Regen Min: 10, MP Regen Max: 20"),
                withSkill(SkillPool.SEEK_OUT_A_BIRD),
                withSkill(SkillPool.VISIT_YOUR_FAVORITE_BIRD),
                withOverrideModifiers(
                    ModifierType.EFFECT,
                    2551,
                    "Mysticality Percent: +75, Stench Resistance: +2, Experience: +2, MP Regen Min: 10, MP Regen Max: 20"),
                withOverrideModifiers(
                    ModifierType.EFFECT,
                    2552,
                    "Stench Resistance: +2, Combat Rate: -9, MP Regen Min: 10, MP Regen Max: 20"));

        try (cleanups) {
          assertTrue(maximize("mp regen"));
          assertTrue(someBoostIs(x -> commandStartsWith(x, "cast 1 Seek out a Bird")));
          assertTrue(someBoostIs(x -> commandStartsWith(x, "cast 1 Visit your Favorite Bird")));
        }
      }
    }
  }

  @Nested
  class PassiveDamage {
    @Test
    public void suggestsPassiveDamage() {
      var cleanups =
          new Cleanups(
              withEquippableItem(ItemPool.HIPPY_PROTEST_BUTTON),
              withEquippableItem(ItemPool.BOTTLE_OPENER_BELT_BUCKLE),
              withEquippableItem(ItemPool.HOT_PLATE),
              withEquippableItem(ItemPool.SHINY_RING),
              withEquippableItem(ItemPool.BEJEWELED_PLEDGE_PIN),
              withEquippableItem(ItemPool.GROLL_DOLL),
              withEquippableItem(ItemPool.ANT_RAKE),
              withEquippableItem(ItemPool.SERRATED_PROBOSCIS_EXTENSION),
              withSkill(SkillPool.JALAPENO_SAUCESPHERE),
              withItem(ItemPool.CHEAP_CIGAR_BUTT),
              withFamiliar(FamiliarPool.MOSQUITO));

      try (cleanups) {
        assertTrue(maximize("passive dmg"));
        recommendedSlotIs(Slot.OFFHAND, "hot plate");
        recommendedSlotIs(Slot.FAMILIAR, "ant rake");
        recommends(ItemPool.HIPPY_PROTEST_BUTTON);
        recommends(ItemPool.BOTTLE_OPENER_BELT_BUCKLE);
        recommendedSlotIs(Slot.ACCESSORY3, "Groll doll");
        assertTrue(someBoostIs(x -> commandStartsWith(x, "cast 1 Jalapeño Saucesphere")));
        assertTrue(someBoostIs(x -> commandStartsWith(x, "use 1 cheap cigar butt")));
      }
    }

    @Test
    public void suggestsUnderwaterPassiveDamageUnderwater() {
      var cleanups =
          new Cleanups(
              withEquippableItem(ItemPool.EELSKIN_HAT),
              withEquippableItem(ItemPool.EELSKIN_PANTS),
              withEquippableItem(ItemPool.EELSKIN_SHIELD),
              withLocation("The Ice Hole"));

      try (cleanups) {
        assertTrue(maximize("passive dmg -tie"));
        recommendedSlotIs(Slot.HAT, "eelskin hat");
        recommendedSlotIs(Slot.PANTS, "eelskin pants");
        recommendedSlotIs(Slot.OFFHAND, "eelskin shield");
      }
    }

    @Test
    public void doesNotSuggestUnderwaterPassiveDamageIfNotUnderwater() {
      var cleanups =
          new Cleanups(
              withEquippableItem(ItemPool.EELSKIN_HAT),
              withEquippableItem(ItemPool.EELSKIN_PANTS),
              withEquippableItem(ItemPool.EELSKIN_SHIELD),
              withLocation("Noob Cave"));

      try (cleanups) {
        assertTrue(maximize("passive dmg -tie"));
        recommendedSlotIsUnchanged(Slot.HAT);
        recommendedSlotIsUnchanged(Slot.PANTS);
        recommendedSlotIsUnchanged(Slot.OFFHAND);
      }
    }
  }

  @Nested
  class Standard {
    private final Cleanups withWitchess =
        new Cleanups(
            withCampgroundItem(ItemPool.WITCHESS_SET), withProperty("puzzleChampBonus", 20));

    @Test
    public void suggestsWitchessIfOwned() {
      var cleanups = new Cleanups(withWitchess);

      try (cleanups) {
        assertTrue(maximize("familiar weight"));
        assertTrue(someBoostIs(b -> commandStartsWith(b, "witchess")));
      }
    }

    @Test
    public void doesNotSuggestWitchessWhenOutOfStandard() {
      var cleanups =
          new Cleanups(
              withPath(Path.STANDARD),
              withRestricted(true),
              withNotAllowedInStandard(RestrictedItemType.ITEMS, "Witchess Set"),
              withWitchess);

      try (cleanups) {
        assertTrue(maximize("familiar weight"));
        assertFalse(someBoostIs(b -> commandStartsWith(b, "witchess")));
      }
    }

    @Test
    public void suggestsWitchessWhenOutOfStandardForLegacyOfLoathing() {
      var cleanups =
          new Cleanups(
              withPath(Path.LEGACY_OF_LOATHING),
              withRestricted(true),
              withProperty("replicaWitchessSetAvailable", true),
              withNotAllowedInStandard(RestrictedItemType.ITEMS, "Witchess Set"),
              withWitchess);

      try (cleanups) {
        assertTrue(maximize("familiar weight"));
        assertTrue(someBoostIs(b -> commandStartsWith(b, "witchess")));
      }
    }
  }
}
