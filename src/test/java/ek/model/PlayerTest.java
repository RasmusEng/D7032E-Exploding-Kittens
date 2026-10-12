package ek.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static ek.model.BaseCardType.*;
import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {

    @Test
    void newPlayer_isAliveWithEmptyHand() {
        Player player = new Player("Test");
        assertTrue(player.isAlive());
        assertTrue(player.hand().isEmpty());
    }

    @Test
    void explode_marksDeadAndReturnsWholeHand() {
        Player player = new Player("Test");
        player.hand().add(new Card(ATTACK));
        player.hand().add(new Card(SKIP));

        List<Card> discarded = player.explode();

        assertFalse(player.isAlive());
        assertTrue(player.hand().isEmpty());
        assertEquals(List.of(new Card(ATTACK), new Card(SKIP)), discarded);
    }
}
