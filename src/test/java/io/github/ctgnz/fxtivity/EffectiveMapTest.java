package io.github.ctgnz.fxtivity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;

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

}
