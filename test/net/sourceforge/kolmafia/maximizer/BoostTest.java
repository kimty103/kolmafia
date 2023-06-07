package net.sourceforge.kolmafia.maximizer;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.Modeable;
import net.sourceforge.kolmafia.equipment.Slot;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BoostTest {
    @Test
    void BoostTest() {
        Boost boost1 = new Boost("", "&", (AdventureResult) null, 0.f);
        assertEquals("<html><font color=gray>&amp;</font></html>", boost1.toString());
        assertEquals(0.0f, boost1.getBoost());

        Boost boost = new Boost("", "&", (AdventureResult) null, 0.f);
        assertEquals(null, boost.getItem());
    }

    @Test
    void compareTo() {
        Boost boostThis = new Boost("", "&", null, true, null, 0.f, false);
        assertEquals(-1, boostThis.compareTo(null));

        Boost boostOther = new Boost("", "&", Slot.ACCESSORY2, null, 0.f);
        assertEquals(true, boostThis.isEquipment() != boostOther.isEquipment());
        assertEquals(1, boostThis.compareTo(boostOther));
        assertEquals(-1, boostOther.compareTo(boostThis));

        boostOther = new Boost("", "&", null, true, null, 0.f, true);
        assertEquals(true, boostThis.isEquipment() == boostOther.isEquipment());
        assertEquals(1, boostThis.compareTo(boostOther));
        assertEquals(-1, boostOther.compareTo(boostThis));

        boostOther = new Boost("", "&", null, true, null, 0.f, false);
        assertEquals(true, boostThis.isEquipment() == boostOther.isEquipment());
        assertEquals(false, boostThis.isEquipment());
        assertEquals(0.f, boostThis.compareTo(boostOther));

        Boost boostEquipThis = new Boost("", "&", Slot.ACCESSORY2, null, 0.f);
        Boost boostEquipOther = new Boost("", "&", Slot.ACCESSORY2, null, 0.f);
        assertEquals(true, boostEquipThis.isEquipment() == boostEquipOther.isEquipment());
        assertEquals(true, boostEquipThis.isEquipment());
        assertEquals(0, boostEquipThis.compareTo(boostEquipOther));
    }

    @Test
    void executeTest() {
        Boost boost = new Boost("", "&", null, true, null, 0.f, false);
        assertEquals(true, !boost.isEquipment());
        assertEquals(false, boost.execute(true));

        assertEquals(0, boost.getCmd().length());
        assertEquals(false, boost.execute(false));

        boost = new Boost("abort", "&", null, true, null, 0.f, false);
        assertNotEquals(0, boost.getCmd().length());
        assertEquals(true, boost.execute(false));
    }

    @Test
    void addToIsEquipmentTest() {
        Boost boost = new Boost("", "&", FamiliarData.NO_FAMILIAR, 0.f);
        assertEquals(true, boost.isEquipment());

        MaximizerSpeculation maximizerSpeculation = new MaximizerSpeculation();
        boost.addTo(maximizerSpeculation);
        assertEquals(FamiliarData.NO_FAMILIAR, maximizerSpeculation.getFamiliar());

        boost = new Boost("", "&", Slot.ACCESSORY2,
                new AdventureResult("Light!", 0,false), 0.f,
                FamiliarData.NO_FAMILIAR, FamiliarData.NO_FAMILIAR, new HashMap<>());
        assertEquals(Slot.ACCESSORY2, boost.getSlot());
        boost.addTo(maximizerSpeculation);
        assertEquals(FamiliarData.NO_FAMILIAR, maximizerSpeculation.getEnthroned());
        assertEquals(FamiliarData.NO_FAMILIAR, maximizerSpeculation.getBjorned());
    }

    @Test
    void addToIsEffectTest() {
        AdventureResult effect = new AdventureResult("Light!", 0,true);
        Boost boost = new Boost("", "&", effect, true, null, 0.0f, false);
        assertEquals(effect, boost.getItem());

        MaximizerSpeculation maximizerSpeculation = new MaximizerSpeculation();
        boost.addTo(maximizerSpeculation);
        assertNotEquals(true, maximizerSpeculation.hasEffect(effect));

        boost = new Boost("", "&", effect, false, null, 0.0f, false);
        boost.addTo(maximizerSpeculation);
        assertEquals(true, maximizerSpeculation.hasEffect(effect));
    }

    @Test
    void addToHorseTest() {
        Boost boost = new Boost("", "&", "horse", 0.0f);
        MaximizerSpeculation maximizerSpeculation = new MaximizerSpeculation();
        boost.addTo(maximizerSpeculation);

        assertEquals("horse", maximizerSpeculation.getHorsery());
    }
}