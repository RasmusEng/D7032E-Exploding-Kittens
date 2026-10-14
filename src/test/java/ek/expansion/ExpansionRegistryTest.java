package ek.expansion;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Uses the providers registered in {@code src/test/resources/META-INF/services}, which the
 * test run puts on the classpath next to the production ones.
 */
class ExpansionRegistryTest {

    private final ExpansionRegistry registry = new ExpansionRegistry();

    private static List<Class<?>> classes(List<?> options) {
        return options.stream().<Class<?>>map(Object::getClass).toList();
    }

    @Test
    void findsAnExpansionRegisteredOnlyInTestResources_withoutCodeChanges() {
        assertTrue(classes(registry.optionalExpansions()).contains(TestOnlyExpansion.class));
    }

    @Test
    void sortsByOrder_notByTheOrderTheyAreRegistered() {
        // registered TestOnly (order 20) before OneKittenLess (order 10)
        assertEquals(List.of(OneKittenLessExpansion.class, TestOnlyExpansion.class),
                classes(registry.optionalExpansions()));
    }

    @Test
    void sameResultEveryTime() {
        assertEquals(classes(registry.optionalExpansions()),
                classes(new ExpansionRegistry().optionalExpansions()));
    }

    @Test
    void baseGame_isNotAnOptionalExpansion() {
        assertFalse(classes(registry.optionalExpansions()).contains(BaseGame.class));
    }

    @Test
    void brokenProviders_areSkippedAndReportedByName() {
        List<String> problems = registry.problems();
        assertEquals(2, problems.size(), problems.toString());
        assertTrue(problems.get(0).contains("BrokenExpansion"), problems.get(0));
        assertTrue(problems.get(0).contains("broken on purpose"), problems.get(0));
        assertTrue(problems.get(1).contains("MissingExpansion"), problems.get(1));
    }

    @Test
    void findsTheFasterVariant() {
        assertEquals(List.of(FasterGame.class), classes(registry.variants()));
    }
}
