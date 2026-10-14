package ek.rules;

import ek.expansion.BaseGame;
import ek.expansion.Expansion;
import ek.expansion.OneKittenLessExpansion;
import ek.expansion.PlayerRange;
import ek.expansion.TestOnlyExpansion;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GameConfigTest {

    @Test
    void baseGame_onlyHasTheBaseGame() {
        GameConfig config = GameConfig.baseGame(2, 1);
        assertEquals(2, config.humans());
        assertEquals(1, config.bots());
        assertEquals(1, config.expansions().size());
        assertInstanceOf(BaseGame.class, config.expansions().getFirst());
        assertTrue(config.variants().isEmpty());
    }

    @Test
    void rule01_twoToFivePlayersInTotal_accepted() {
        assertDoesNotThrow(() -> GameConfig.baseGame(1, 1));
        assertDoesNotThrow(() -> GameConfig.baseGame(2, 3));
        assertDoesNotThrow(() -> GameConfig.baseGame(5, 0));
    }

    @Test
    void rule01_oneOrSixPlayersInTotal_rejected() {
        assertThrows(IllegalArgumentException.class, () -> GameConfig.baseGame(1, 0));
        assertThrows(IllegalArgumentException.class, () -> GameConfig.baseGame(5, 1));
        assertThrows(IllegalArgumentException.class, () -> GameConfig.baseGame(6, 0));
    }

    @Test
    void noHumans_rejected() {
        // the host plays at the console, so there is always at least one human
        assertThrows(IllegalArgumentException.class, () -> GameConfig.baseGame(0, 2));
    }

    @Test
    void negativeBots_rejected() {
        assertThrows(IllegalArgumentException.class, () -> GameConfig.baseGame(3, -1));
    }

    @Test
    void rejection_explainsTheValidRange() {
        var e = assertThrows(IllegalArgumentException.class, () -> GameConfig.baseGame(4, 2));
        assertTrue(e.getMessage().contains("2–5"), e.getMessage());
    }

    @Test
    void of_putsBaseGameFirst_thenSortsByOrder_whateverTheSelectionOrder() {
        Expansion first = new OneKittenLessExpansion();   // order 10
        Expansion second = new TestOnlyExpansion();       // order 20
        for (List<Expansion> selected : List.of(List.of(first, second), List.of(second, first))) {
            List<Expansion> expansions = GameConfig.of(2, 0, selected, List.of()).expansions();
            assertInstanceOf(BaseGame.class, expansions.get(0));
            assertSame(first, expansions.get(1));
            assertSame(second, expansions.get(2));
        }
    }

    @Test
    void playerRange_forTheSelectedExpansions() {
        assertEquals(new PlayerRange(2, 5), GameConfig.playerRange(List.of()));
        assertEquals(new PlayerRange(2, 5), GameConfig.playerRange(List.of(new TestOnlyExpansion())));
    }

    @Test
    void withoutBaseGameFirst_rejected() {
        Expansion other = players -> Map.of();
        assertThrows(IllegalArgumentException.class,
                () -> new GameConfig(2, 0, List.of(other, new BaseGame()), List.of()));
    }

    @Test
    void lists_areCopied() {
        var config = GameConfig.of(2, 0, new java.util.ArrayList<>(List.of(new TestOnlyExpansion())), List.of());
        assertThrows(UnsupportedOperationException.class, () -> config.expansions().clear());
    }
}
