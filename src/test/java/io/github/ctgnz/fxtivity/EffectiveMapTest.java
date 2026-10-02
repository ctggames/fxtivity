package io.github.ctgnz.fxtivity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;

import javafx.collections.transformation.FilteredList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Successions kept by key: every record across all keys following the effective date, one key's own history, and a record added after the combined view was first taken.
 * <p>
 * Eleven keys and twelve records - one key, G, has two in succession. Where records start and end on the same dates, the combined view holds them in key order, which is what the
 * positional assertions in {@link #testMap()} rely on:
 *
 * <pre>
 *  A 1920 ----------------- 1948
 *  B 1920 ---------------------------------- 1973
 *  C                        1948 ----------- 1973
 *  D 1920 ---------------------------------- 1973
 *  E 1920 ---------------------------------- 1973
 *  F 1920 ---------------------------------- 1973
 *  G           1937 - 1943 | 1943 ---------- 1973
 *  H 1920 ---------------------------------- 1973
 *  I                        1948 ----------- 1973
 *  J       1933 ---------------------------- 1973
 *  K                   1945 ---------------- 1973
 * </pre>
 */
class EffectiveMapTest {

    private EffectiveMap<String, Term> candidate;

    @BeforeEach
    void setup() {
        Effectivity.forDates(LocalDate.of(1920, 1, 1), LocalDate.of(1900, 1, 1), Effectivity.FOREVER);
        candidate = new EffectiveMap<>(true);
        put("A", "A", 1920, 1948);
        put("B", "B", 1920, 1973);
        put("C", "C", 1948, 1973);
        put("D", "D", 1920, 1973);
        put("E", "E", 1920, 1973);
        put("F", "F", 1920, 1973);
        put("G", "G1", 1937, 1943);
        put("G", "G2", 1943, 1973);
        put("H", "H", 1920, 1973);
        put("I", "I", 1948, 1973);
        put("J", "J", 1933, 1973);
        put("K", "K", 1945, 1973);
    }

    private void put(String key, String id, int fromYear, int toYear) {
        candidate.put(key, Term.of(id, fromYear, toYear));
    }

    @Test
    void testMap() {
        assertThat(candidate.keySet(), hasSize(11));

        Effectivity.forDate(LocalDate.of(1920, 1, 1));
        MultiEffectiveList<Term> records = candidate.getEffectiveRecords();
        assertThat(records, hasSize(12));

        assertThat(records.effectiveSize(), is(6));
        FilteredList<Term> filtered = records.effective();
        assertThat(filtered.get(0).id(), is("A"));
        assertThat(filtered.get(4).id(), is("F"));

        Effectivity.forDate(LocalDate.of(1935, 1, 1));
        assertThat(records.effectiveSize(), is(7));
        assertThat(filtered.get(0).id(), is("A"));
        assertThat(filtered.get(4).id(), is("F"));
        assertThat(filtered.get(6).id(), is("J"));

        Effectivity.forDate(LocalDate.of(1938, 1, 1));
        assertThat(records.effectiveSize(), is(8));
        assertThat(filtered.get(0).id(), is("A"));
        assertThat(filtered.get(4).id(), is("F"));
        assertThat(filtered.get(6).id(), is("J"));
        assertThat(filtered.get(7).id(), is("G1"));

        Effectivity.forDate(LocalDate.of(1947, 1, 1));
        assertThat(records.effectiveSize(), is(9));
        assertThat(filtered.get(0).id(), is("A"));
        assertThat(filtered.get(4).id(), is("F"));
        assertThat(filtered.get(7).id(), is("G2"));
        assertThat(filtered.get(8).id(), is("K"));

        Effectivity.forDate(LocalDate.of(1949, 1, 1));
        assertThat(records.effectiveSize(), is(10));
        assertThat(filtered.get(3).id(), is("F"));
        assertThat(filtered.get(6).id(), is("G2"));
        assertThat(filtered.get(7).id(), is("K"));
        assertThat(filtered.get(8).id(), is("C"));
        assertThat(filtered.get(9).id(), is("I"));
    }

    @Test
    void testHistory() {
        SingleEffectiveList<Term> records = candidate.getRecords("G");
        assertThat(records.size(), is(2));
        Effectivity.forDate(LocalDate.of(1938, 1, 1));
        assertThat(records.getEffectiveRecord().get().id(), is("G1"));
        Effectivity.forDate(LocalDate.of(1948, 1, 1));
        assertThat(records.getEffectiveRecord().get().id(), is("G2"));
    }

    /** A record put after the combined view was first taken appears in it. */
    @Test
    void testAddAfterGetEffectiveRecords() {
        Effectivity.forDate(LocalDate.of(1928, 1, 1));
        MultiEffectiveList<Term> records = candidate.getEffectiveRecords();
        assertThat(records, hasSize(12));
        assertThat(records.effectiveSize(), is(6));
        candidate.put("L", Term.of("L", 1930, 1942));
        records = candidate.getEffectiveRecords();
        assertThat(records.effectiveSize(), is(6));
        Effectivity.forDate(LocalDate.of(1938, 1, 1));
        assertThat(records.effectiveSize(), is(9));
        Effectivity.forDate(LocalDate.of(1943, 1, 1));
        assertThat(records.effectiveSize(), is(8));
    }

    private Term only(String key) {
        return candidate.getRecords(key).getFirst();
    }

    /** {@code putAll} works on a map whose combined view has never been asked for. */
    @Test
    void testPutAllFirst() {
        EffectiveMap<String, Term> map = new EffectiveMap<>(true);
        assertDoesNotThrow(() -> map.putAll("A", List.of(Term.of("A1", 1920, 1930), Term.of("A2", 1930, 1940))));
        assertThat(map.size(), is(2));
        assertThat(map.getEffectiveRecords(), hasSize(2));
    }

    /** One key is a succession: a record overlapping another under the same key is refused, and the map is unchanged. */
    @Test
    void testOverlapUnderOneKeyIsRefused() {
        assertThat(candidate.put("A", Term.of("A2", 1940, 1960)), is(false));
        assertThat(candidate.get("A"), hasSize(1));
        assertThat(candidate.size(), is(12));
    }

    /** A different record with the same dates under one key overlaps the first, and is refused as such. */
    @Test
    void testSameDatesUnderOneKeyIsRefused() {
        Term a = only("A");
        assertThat(candidate.put("A", Term.of("A2", 1920, 1948)), is(false));
        assertThat(candidate.get("A"), contains(a));
    }

    /** A key's history is the map's own, live: what an editor changes through it shows in the map and in the combined view. */
    @Test
    void testEditingThroughAKeysHistory() {
        SingleEffectiveList<Term> history = candidate.getRecords("G");
        assertThat(candidate.getRecords("G"), is(sameInstance(history)));
        Term g1 = history.getFirst();
        Term g2 = history.getLast();
        assertThat(history.remove(g1), is(true));
        assertThat(candidate.get("G"), contains(g2));
        assertThat(candidate.size(), is(11));
        assertThat(candidate.getEffectiveRecords(), not(hasItem(g1)));

        assertThat(history.reschedule(g2, LocalDate.of(1937, 1, 1), LocalDate.of(1973, 1, 1)), is(true));
        Effectivity.forDate(LocalDate.of(1938, 1, 1));
        assertThat(candidate.getEffectiveRecords().effective(), hasItem(g2));
    }

    /** A key with no history yet can be given one through {@code getRecords}, as an editor adding the first record would. */
    @Test
    void testANewKeysHistory() {
        Term l = Term.of("L", 1930, 1942);
        assertThat(candidate.containsKey("L"), is(false));
        assertThat(candidate.getRecords("L").add(l), is(true));
        assertThat(candidate.containsKey("L"), is(true));
        assertThat(candidate.getEffectiveRecords(), hasItem(l));
    }

    @Test
    void testRemove() {
        Term a = only("A");
        assertThat(candidate.remove("A", a), is(true));
        assertThat(candidate.containsKey("A"), is(false));
        assertThat(candidate.keySet(), hasSize(10));
        assertThat(candidate.getEffectiveRecords(), not(hasItem(a)));
        assertThat(candidate.remove("A", a), is(false));
    }

    /** The combined view is derived from the keys' histories, so it cannot be written to - a record added there would belong to no key. */
    @Test
    void testTheCombinedViewIsReadOnly() {
        MultiEffectiveList<Term> records = candidate.getEffectiveRecords();
        Term stray = Term.of("stray", 1920, 1930);
        assertThrows(UnsupportedOperationException.class, () -> records.add(stray));
        assertThrows(UnsupportedOperationException.class, () -> records.remove(0));
        assertThrows(UnsupportedOperationException.class, () -> records.clear());
        assertThat(records, hasSize(12));
    }

    /** The whole map, read-only: every key with its history, in key order. */
    @Test
    void testTheSourceMap() {
        var source = candidate.getSourceMap();
        assertThat(source.keySet(), hasSize(11));
        assertThat(source.firstKey(), is("A"));
        assertThat(source.get("G"), hasSize(2));
        assertThrows(UnsupportedOperationException.class, () -> source.remove("A"));
        assertThrows(UnsupportedOperationException.class, () -> source.get("G").clear());
    }

}
