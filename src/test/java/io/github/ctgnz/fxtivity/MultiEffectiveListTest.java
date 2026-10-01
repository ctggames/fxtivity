package io.github.ctgnz.fxtivity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Elements free to overlap: how many are in effect on a date, which one when exactly one is, and the span they cover.
 * <p>
 * Every date is moved with {@link Effectivity#forDate(LocalDate)}, the one effective date for the whole application, so each test pins the active range first. Without that a test
 * would depend on whatever range the previous test left behind.
 */
class MultiEffectiveListTest {

    private Term r1;
    private Term r2;
    private Term r3;
    private Term r4;

    @BeforeEach
    void init() {
        Effectivity.forDates(LocalDate.of(1948, 1, 1), LocalDate.of(1920, 1, 1), LocalDate.of(1973, 1, 1));
        r1 = Term.of("r1", 1920, 1973);
        r2 = Term.of("r2", 1920, 1973);
        r3 = Term.of("r3", 1933, 1973);
        r4 = Term.of("r4", 1943, 1973);
    }

    @Test
    void testDefaultDate() {
        Effectivity.forDate(LocalDate.of(1928, 6, 30));
        MultiEffectiveList<Term> candidate = new MultiEffectiveList<>(Arrays.asList(r1, r2, r3, r4));
        assertThat(candidate.size(), is(4));
        assertThat(candidate.effectiveSize(), is(2));
    }

    @Test
    void testEffectiveRecord() {
        Effectivity.forDate(LocalDate.of(1928, 6, 30));
        MultiEffectiveList<Term> candidate = new MultiEffectiveList<>(Arrays.asList(r1, r3, r4));
        assertThat(candidate.size(), is(3));
        assertThat(candidate.effectiveSize(), is(1));
        assertThat(candidate.getEffectiveRecord().get(), is(r1));
        Effectivity.forDate(LocalDate.of(1938, 1, 1));
        assertThat(candidate.getEffectiveRecord().isEmpty(), is(true));
    }

    @Test
    void testFirst() {
        MultiEffectiveList<Term> candidate = new MultiEffectiveList<>(Arrays.asList(r1, r2, r3, r4));
        Effectivity.forDate(LocalDate.of(1920, 1, 1));
        assertThat(candidate.size(), is(4));
        assertThat(candidate.effectiveSize(), is(2));
    }

    @Test
    void testLast() {
        MultiEffectiveList<Term> candidate = new MultiEffectiveList<>(Arrays.asList(r1, r2, r3, r4));
        Effectivity.forDate(LocalDate.of(1972, 12, 31));
        assertThat(candidate.size(), is(4));
        assertThat(candidate.effectiveSize(), is(4));
    }

    @Test
    void testMiddle() {
        MultiEffectiveList<Term> candidate = new MultiEffectiveList<>(Arrays.asList(r1, r2, r3, r4));
        Effectivity.forDate(LocalDate.of(1938, 1, 1));
        assertThat(candidate.size(), is(4));
        assertThat(candidate.effectiveSize(), is(3));
    }

    @Test
    void testNone() {
        MultiEffectiveList<Term> candidate = new MultiEffectiveList<>(Arrays.asList(r3, r4));
        Effectivity.forDate(LocalDate.of(1928, 1, 1));
        assertThat(candidate.size(), is(2));
        assertThat(candidate.effectiveSize(), is(0));
        assertThat(candidate.getEffectiveRecord().isEmpty(), is(true));
    }

    @Test
    void testDateRange() {
        MultiEffectiveList<Term> candidate = new MultiEffectiveList<>(Arrays.asList(r1, r2, r3, r4));
        assertThat(candidate.getDateRange(), is(DateRange.closedOpen(LocalDate.of(1920, 1, 1), LocalDate.of(1973, 1, 1))));
    }

    @Test
    void testOverlapsAllowed() {
        MultiEffectiveList<Term> candidate = new MultiEffectiveList<>(Arrays.asList(r1, r3, r4));
        Effectivity.forDate(LocalDate.of(1928, 1, 1));
        assertThat(candidate.effectiveSize(), is(1));
        assertThat(candidate.add(r2), is(true));
        assertThat(candidate.size(), is(4));
        assertThat(candidate.effectiveSize(), is(2));
    }

}
