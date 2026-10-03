package io.github.ctgnz.fxtivity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.collections.ListChangeListener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A succession - one element at a time - and the requirements its operations have to meet.
 * <p>
 * Two of them are easy to mistake for defects and are not. {@code addAll} adds elements one at a time, so when one is refused the ones before it stay; {@code setAll} is
 * all-or-nothing, so one refused element leaves the list exactly as it was. {@link #testGapsNotAllowedAddAll()} and {@link #testGapsNotAllowedSetAll()} pin each.
 * <p>
 * The terms, in date order, with the active range running from 1920 up to 1973 and the effective date starting at 1948:
 *
 * <pre>
 *  r1 1920 ---- 1933
 *  r2           1933 ---- 1945
 *  r3                     1945 -------- 1963
 *  r4                                   1963 ---- 1973
 *  r5 1920 ------------------------ 1953
 *  r6                1942 ----------------------- 1973
 *  r7           1933 ------- 1948
 * </pre>
 *
 * r1 to r4 are a gapless succession; r5, r6 and r7 each overlap it.
 */
class SingleEffectiveListTest {

    private Term r1;
    private Term r2;
    private Term r3;
    private Term r4;
    private Term r5;
    private Term r6;
    private Term r7;

    @BeforeEach
    void init() {
        Effectivity.forDates(LocalDate.of(1948, 1, 1), LocalDate.of(1920, 1, 1), LocalDate.of(1973, 1, 1));
        r1 = Term.of("r1", 1920, 1933);
        r2 = Term.of("r2", 1933, 1945);
        r3 = Term.of("r3", 1945, 1963);
        r4 = Term.of("r4", 1963, 1973);
        r5 = Term.of("r5", 1920, 1953);
        r6 = Term.of("r6", 1942, 1973);
        r7 = Term.of("r7", 1933, 1948);
    }

    @Test
    void testCurrentDate() {
        Effectivity.forDate(LocalDate.of(1928, 1, 1));
        SingleEffectiveList<Term> candidate = new SingleEffectiveList<>(Arrays.asList(r1, r2, r3));
        assertThat(candidate.size(), is(3));
        assertThat(candidate.effectiveSize(), is(1));
        assertThat(candidate.getEffectiveRecord().get(), is(r1));
    }

    @Test
    void testDateRange() {
        SingleEffectiveList<Term> candidate = new SingleEffectiveList<>(Arrays.asList(r1, r2, r3, r4));
        assertThat(candidate.getDateRange(), is(DateRange.closedOpen(LocalDate.of(1920, 1, 1), LocalDate.of(1973, 1, 1))));
    }

    @Test
    void testDefaultDate() {
        SingleEffectiveList<Term> candidate = new SingleEffectiveList<>(Arrays.asList(r1, r2, r3));
        assertThat(candidate.size(), is(3));
        assertThat(candidate.effectiveSize(), is(1));
        assertThat(candidate.getEffectiveRecord().get(), is(r3));
    }

    @Test
    void testFirst() {
        SingleEffectiveList<Term> candidate = new SingleEffectiveList<>(Arrays.asList(r1, r2, r3, r4));
        Effectivity.forDate(LocalDate.of(1920, 1, 1));
        assertThat(candidate.size(), is(4));
        assertThat(candidate.effectiveSize(), is(1));
        assertThat(candidate.getEffectiveRecord().get(), is(r1));
    }

    @Test
    void testGapsAllowed() {
        SingleEffectiveList<Term> candidate = new SingleEffectiveList<>(Arrays.asList(r1, r2), true);
        assertThat(candidate.add(r4), is(true));
        Effectivity.forDate(LocalDate.of(1965, 1, 1));
        assertThat(candidate.getEffectiveRecord().isPresent(), is(true));
    }

    @Test
    void testGapsAllowedAddAll() {
        SingleEffectiveList<Term> candidate = new SingleEffectiveList<>(Arrays.asList(r1), true);
        assertThat(candidate.addAll(r2, r4), is(true));
        Effectivity.forDate(LocalDate.of(1938, 1, 1));
        assertThat(candidate.getEffectiveRecord().isPresent(), is(true));
    }

    @Test
    void testGapsAllowedContructor() {
        SingleEffectiveList<Term> candidate = new SingleEffectiveList<>(Arrays.asList(r1, r2, r4), true);
        assertThat(candidate.getEffectiveRecord().isPresent(), is(false));
        Effectivity.forDate(LocalDate.of(1965, 1, 1));
        assertThat(candidate.getEffectiveRecord().isPresent(), is(true));
        Effectivity.forDate(LocalDate.of(1940, 1, 1));
        assertThat(candidate.getEffectiveRecord().isPresent(), is(true));
    }

    @Test
    void testGapsAllowedSetAll() {
        SingleEffectiveList<Term> candidate = new SingleEffectiveList<>(true);
        assertThat(candidate.addAll(r1, r2, r4), is(true));
        Effectivity.forDate(LocalDate.of(1965, 1, 1));
        assertThat(candidate.getEffectiveRecord().isPresent(), is(true));
    }

    @Test
    void testGapsNotAllowed() {
        SingleEffectiveList<Term> candidate = new SingleEffectiveList<>(Arrays.asList(r1, r2), false);
        assertThat(candidate.add(r4), is(false));
        Effectivity.forDate(LocalDate.of(1965, 1, 1));
        assertThat(candidate.getEffectiveRecord().isPresent(), is(false));
    }

    /** {@code addAll} is not all-or-nothing: elements are added one at a time, so the ones before a refused element stay. */
    @Test
    void testGapsNotAllowedAddAll() {
        SingleEffectiveList<Term> candidate = new SingleEffectiveList<>(Arrays.asList(r1), false);
        assertThat(candidate.addAll(r2, r4), is(false));
        Effectivity.forDate(LocalDate.of(1965, 1, 1));
        assertThat(candidate.getEffectiveRecord().isPresent(), is(false));
        // elements are added individually, so the r2 record should still be in the list
        Effectivity.forDate(LocalDate.of(1940, 1, 1));
        assertThat(candidate.getEffectiveRecord().isPresent(), is(true));
    }

    @Test
    void testGapsNotAllowedContructor() {
        assertThrows(IllegalArgumentException.class, () -> new SingleEffectiveList<>(Arrays.asList(r1, r2, r4), false));
    }

    /** {@code setAll} is all-or-nothing: one refused element and none is set. */
    @Test
    void testGapsNotAllowedSetAll() {
        SingleEffectiveList<Term> candidate = new SingleEffectiveList<>(false);
        assertThat(candidate.setAll(r1, r2, r4), is(false));
        // The whole list must be valid to allow any to be set
        assertThat(candidate.getEffectiveRecord().isPresent(), is(false));
        // Any date shows the list empty. 1965 rather than a date past the active range, which the effective date may not leave.
        Effectivity.forDate(LocalDate.of(1965, 6, 30));
        assertThat(candidate.getEffectiveRecord().isPresent(), is(false));
        Effectivity.forDate(LocalDate.of(1948, 1, 1));
        assertThat(candidate.getEffectiveRecord().isPresent(), is(false));
    }

    /** An element added before the first is checked against the one it precedes, not the last. */
    @Test
    void testGapsNotAllowedAddBeforeFirst() {
        SingleEffectiveList<Term> candidate = new SingleEffectiveList<>(Arrays.asList(r3, r4), false);
        assertThat("r1 ends in 1933, leaving nothing in effect until r3 starts in 1945", candidate.add(r1), is(false));
        assertThat("r2 ends as r3 starts", candidate.add(r2), is(true));
        assertThat(candidate, contains(r2, r3, r4));
    }

    /** An element added between two others is checked against both, and refused if it leaves a gap on either side. */
    @Test
    void testGapsNotAllowedAddBetween() {
        SingleEffectiveList<Term> candidate = new SingleEffectiveList<>(Arrays.asList(r1, r4), true);
        candidate.setGapsAllowed(false);
        assertThat("r2 leaves nothing in effect from 1945 until r4 starts", candidate.add(r2), is(false));
        assertThat("r3 leaves nothing in effect from 1933, when r1 ends, until it starts", candidate.add(r3), is(false));
        Term between = Term.of("between", 1933, 1963);
        assertThat(candidate.add(between), is(true));
        assertThat(candidate, contains(r1, between, r4));
    }

    /** Gaps are found by date, whatever order the elements are given in. */
    @Test
    void testGapsNotAllowedSetAllOutOfOrder() {
        SingleEffectiveList<Term> candidate = new SingleEffectiveList<>(false);
        assertThat(candidate.setAll(r2, r1), is(true));
        assertThat(candidate.setAll(r4, r1), is(false));
        assertThat(candidate, contains(r1, r2));
    }

    @Test
    void testOverlapsNotAllowedConstructor() {
        assertThrows(IllegalArgumentException.class, () -> new SingleEffectiveList<>(Arrays.asList(r1, r5), true));
        assertThrows(IllegalArgumentException.class, () -> new SingleEffectiveList<>(Arrays.asList(r2, r7), false));
    }

    @Test
    void testLast() {
        SingleEffectiveList<Term> candidate = new SingleEffectiveList<>(Arrays.asList(r1, r2, r3, r4));
        Effectivity.forDate(LocalDate.of(1972, 12, 31));
        assertThat(candidate.size(), is(4));
        assertThat(candidate.effectiveSize(), is(1));
        assertThat(candidate.getEffectiveRecord().get(), is(r4));
    }

    @Test
    void testMiddle() {
        SingleEffectiveList<Term> candidate = new SingleEffectiveList<>(Arrays.asList(r1, r2, r3, r4));
        assertThat(candidate.size(), is(4));
        assertThat(candidate.effectiveSize(), is(1));
        assertThat(candidate.getEffectiveRecord().get(), is(r3));
    }

    /** The previous element is found by date, so it is found for an element not in the list as well. */
    @Test
    void testPrevious() {
        SingleEffectiveList<Term> candidate = new SingleEffectiveList<>(Arrays.asList(r1, r2, r3, r4));
        assertThat(candidate.getPrevious(r1).isEmpty(), is(true));
        assertThat(candidate.getPrevious(r2).get(), is(r1));
        assertThat(candidate.getPrevious(r3).get(), is(r2));
        assertThat(candidate.getPrevious(r4).get(), is(r3));
        assertThat(candidate.getPrevious(r5).get(), is(r1));
        assertThat(candidate.getPrevious(r6).get(), is(r2));
        assertThat(candidate.getPrevious(r7).get(), is(r2));
    }

    /** The next element is found by date, so it is found for an element not in the list as well. */
    @Test
    void testNext() {
        SingleEffectiveList<Term> candidate = new SingleEffectiveList<>(Arrays.asList(r1, r2, r3));
        assertThat(candidate.getNext(r1).get(), is(r2));
        assertThat(candidate.getNext(r2).get(), is(r3));
        assertThat(candidate.getNext(r3).isEmpty(), is(true));
        assertThat(candidate.getNext(r4).isEmpty(), is(true));
        assertThat(candidate.getNext(r5).get(), is(r2));
        assertThat(candidate.getNext(r6).get(), is(r3));
        assertThat(candidate.getNext(r7).get(), is(r3));
    }

    @Test
    void testNone() {
        SingleEffectiveList<Term> candidate = new SingleEffectiveList<>(Arrays.asList(r1, r2, r4), true);
        // A date in the gap between r2 and r4 rather than one past the active range, which the effective date may not leave.
        Effectivity.forDate(LocalDate.of(1950, 6, 30));
        assertThat(candidate.size(), is(3));
        assertThat(candidate.effectiveSize(), is(0));
        assertThat(candidate.getEffectiveRecord().isEmpty(), is(true));
    }

    @Test
    void testOverlapsNotAllowed() {
        SingleEffectiveList<Term> candidate = new SingleEffectiveList<>(Arrays.asList(r1, r2, r3, r4));
        assertThat(candidate.size(), is(4));
        assertThat(candidate.add(r5), is(false));
        assertThat(candidate.add(r6), is(false));
        assertThat(candidate.add(r7), is(false));
        assertThat(candidate.size(), is(4));
    }

    @Test
    void testInsertBackwards() {
        SingleEffectiveList<Term> candidate = new SingleEffectiveList<>(Arrays.asList(r1, r2, r4), true);
        assertThat(r2.getEnd(), is(LocalDate.of(1945, 1, 1)));
        assertThat(r4.getStart(), is(LocalDate.of(1963, 1, 1)));
        candidate.insertBackwards(r6);
        assertThat("Previous element end date should be pushed back", r2.getEnd(), is(LocalDate.of(1942, 1, 1)));
        assertThat(r4.getStart(), is(LocalDate.of(1963, 1, 1)));
        assertThat(r6.getStart(), is(LocalDate.of(1942, 1, 1)));
        assertThat("Added element end date should be pushed back", r6.getEnd(), is(LocalDate.of(1963, 1, 1)));
    }

    @Test
    void testInsertBackwardsSingle() {
        SingleEffectiveList<Term> candidate = new SingleEffectiveList<>(Arrays.asList(r1), true);
        assertThat(r1.getEnd(), is(LocalDate.of(1933, 1, 1)));
        r7.setStart(LocalDate.of(1930, 1, 1));
        candidate.insertBackwards(r7);
        assertThat("Previous element end date should be pushed back", r1.getEnd(), is(LocalDate.of(1930, 1, 1)));
        assertThat(r7.getStart(), is(LocalDate.of(1930, 1, 1)));
        assertThat(r7.getEnd(), is(LocalDate.of(1948, 1, 1)));
    }

    @Test
    void testInsertBackwardsGap() {
        SingleEffectiveList<Term> candidate = new SingleEffectiveList<>(Arrays.asList(r1, r2, r4), true);
        assertThat(r2.getEnd(), is(LocalDate.of(1945, 1, 1)));
        assertThat(r4.getStart(), is(LocalDate.of(1963, 1, 1)));
        r6.setEnd(LocalDate.of(1960, 1, 1));
        candidate.insertBackwards(r6);
        assertThat("Previous element end date should be pushed back", r2.getEnd(), is(LocalDate.of(1942, 1, 1)));
        assertThat(r4.getStart(), is(LocalDate.of(1963, 1, 1)));
        assertThat(r6.getStart(), is(LocalDate.of(1942, 1, 1)));
        assertThat(r6.getEnd(), is(LocalDate.of(1960, 1, 1)));
    }

    @Test
    void testInsertBackwardsNoPrevious() {
        SingleEffectiveList<Term> candidate = new SingleEffectiveList<>(Arrays.asList(r2, r3), true);
        assertThat(r2.getStart(), is(LocalDate.of(1933, 1, 1)));
        assertThat(r3.getStart(), is(LocalDate.of(1945, 1, 1)));
        candidate.insertBackwards(r5);
        assertThat(r2.getStart(), is(LocalDate.of(1933, 1, 1)));
        assertThat(r3.getStart(), is(LocalDate.of(1945, 1, 1)));
        assertThat(r5.getStart(), is(LocalDate.of(1920, 1, 1)));
        assertThat(r5.getEnd(), is(LocalDate.of(1933, 1, 1)));
    }

    @Test
    void testInsertBackwardsNoPreviousOverlap() {
        SingleEffectiveList<Term> candidate = new SingleEffectiveList<>(Arrays.asList(r3), true);
        assertThat(r3.getStart(), is(LocalDate.of(1945, 1, 1)));
        assertThat(r7.getEnd(), is(LocalDate.of(1948, 1, 1)));
        candidate.insertBackwards(r7);
        assertThat(r3.getStart(), is(LocalDate.of(1945, 1, 1)));
        assertThat(r7.getStart(), is(LocalDate.of(1933, 1, 1)));
        assertThat("Added element end date should be pushed back", r7.getEnd(), is(LocalDate.of(1945, 1, 1)));
    }

    @Test
    void testInsertBackwardsNoNext() {
        SingleEffectiveList<Term> candidate = new SingleEffectiveList<>(Arrays.asList(r1, r2), true);
        assertThat(r2.getEnd(), is(LocalDate.of(1945, 1, 1)));
        candidate.insertBackwards(r4);
        assertThat(r2.getEnd(), is(LocalDate.of(1945, 1, 1)));
        assertThat(r4.getStart(), is(LocalDate.of(1963, 1, 1)));
        assertThat(r4.getEnd(), is(LocalDate.of(1973, 1, 1)));
    }

    @Test
    void testInsertBackwardsNoNextOverlap() {
        SingleEffectiveList<Term> candidate = new SingleEffectiveList<>(Arrays.asList(r1, r2), true);
        assertThat(r2.getEnd(), is(LocalDate.of(1945, 1, 1)));
        candidate.insertBackwards(r6);
        assertThat("Previous element end date should be pushed back", r2.getEnd(), is(LocalDate.of(1942, 1, 1)));
        assertThat(r6.getStart(), is(LocalDate.of(1942, 1, 1)));
        assertThat(r6.getEnd(), is(LocalDate.of(1973, 1, 1)));
    }

    @Test
    void testInsertForwards() {
        SingleEffectiveList<Term> candidate = new SingleEffectiveList<>(Arrays.asList(r1, r3, r4), true);
        assertThat(r3.getStart(), is(LocalDate.of(1945, 1, 1)));
        assertThat(r7.getEnd(), is(LocalDate.of(1948, 1, 1)));
        candidate.insertForwards(r7);
        assertThat(r7.getEnd(), is(LocalDate.of(1948, 1, 1)));
        assertThat("Next element start date should be pushed forward", r3.getStart(), is(LocalDate.of(1948, 1, 1)));
    }

    @Test
    void testInsertForwardsSingle() {
        SingleEffectiveList<Term> candidate = new SingleEffectiveList<>(Arrays.asList(r2), true);
        assertThat(r2.getEnd(), is(LocalDate.of(1945, 1, 1)));
        candidate.insertForwards(r7);
        assertThat(r2.getEnd(), is(LocalDate.of(1945, 1, 1)));
        assertThat("Added element start date should be pushed forward", r7.getStart(), is(LocalDate.of(1945, 1, 1)));
        assertThat(r7.getEnd(), is(LocalDate.of(1948, 1, 1)));
    }

    @Test
    void testInsertForwardsGap() {
        SingleEffectiveList<Term> candidate = new SingleEffectiveList<>(Arrays.asList(r1, r2, r4), true);
        assertThat(r2.getEnd(), is(LocalDate.of(1945, 1, 1)));
        assertThat(r4.getStart(), is(LocalDate.of(1963, 1, 1)));
        candidate.insertForwards(r7);
        assertThat(r2.getEnd(), is(LocalDate.of(1945, 1, 1)));
        assertThat(r4.getStart(), is(LocalDate.of(1963, 1, 1)));
        assertThat("Added element start date should be pushed forward", r7.getStart(), is(LocalDate.of(1945, 1, 1)));
        assertThat(r7.getEnd(), is(LocalDate.of(1948, 1, 1)));
    }

    @Test
    void testInsertForwardsNoPrevious() {
        SingleEffectiveList<Term> candidate = new SingleEffectiveList<>(Arrays.asList(r2, r3), true);
        assertThat(r2.getStart(), is(LocalDate.of(1933, 1, 1)));
        assertThat(r3.getStart(), is(LocalDate.of(1945, 1, 1)));
        r5.setEnd(LocalDate.of(1935, 1, 1));
        candidate.insertForwards(r5);
        assertThat(r2.getStart(), is(LocalDate.of(1935, 1, 1)));
        assertThat(r3.getStart(), is(LocalDate.of(1945, 1, 1)));
        assertThat(r5.getStart(), is(LocalDate.of(1920, 1, 1)));
        assertThat(r5.getEnd(), is(LocalDate.of(1935, 1, 1)));
    }

    @Test
    void testInsertForwardsNoPreviousOverlap() {
        SingleEffectiveList<Term> candidate = new SingleEffectiveList<>(Arrays.asList(r3, r4), true);
        assertThat(r3.getStart(), is(LocalDate.of(1945, 1, 1)));
        candidate.insertForwards(r7);
        assertThat("Next element start date should be pushed forward", r3.getStart(), is(LocalDate.of(1948, 1, 1)));
        assertThat(r7.getStart(), is(LocalDate.of(1933, 1, 1)));
        assertThat(r7.getEnd(), is(LocalDate.of(1948, 1, 1)));
    }

    @Test
    void testInsertForwardsNoNext() {
        SingleEffectiveList<Term> candidate = new SingleEffectiveList<>(Arrays.asList(r1, r2), true);
        assertThat(r2.getEnd(), is(LocalDate.of(1945, 1, 1)));
        candidate.insertForwards(r4);
        assertThat(r2.getEnd(), is(LocalDate.of(1945, 1, 1)));
        assertThat(r4.getStart(), is(LocalDate.of(1963, 1, 1)));
        assertThat(r4.getEnd(), is(LocalDate.of(1973, 1, 1)));
    }

    @Test
    void testInsertForwardsNoNextOverlap() {
        SingleEffectiveList<Term> candidate = new SingleEffectiveList<>(Arrays.asList(r1, r2), true);
        assertThat(r2.getEnd(), is(LocalDate.of(1945, 1, 1)));
        assertThat(r6.getStart(), is(LocalDate.of(1942, 1, 1)));
        candidate.insertForwards(r6);
        assertThat(r2.getEnd(), is(LocalDate.of(1945, 1, 1)));
        assertThat("Added element start date should be pushed forward", r6.getStart(), is(LocalDate.of(1945, 1, 1)));
    }

    @Test
    void testListener() {
        AtomicBoolean listenerHit = new AtomicBoolean(false);
        SingleEffectiveList<Term> candidate = new SingleEffectiveList<>(Arrays.asList(r1, r2), true);
        candidate.addListener((ListChangeListener<Term>) lcl -> listenerHit.set(true));
        candidate.add(r3);
        assertThat(listenerHit.get(), is(true));
    }

    @Test
    void testAddIndexValid() {
        SingleEffectiveList<Term> candidate = new SingleEffectiveList<>(Arrays.asList(r1, r4), true);
        candidate.add(1, r7);
        assertThat(candidate.size(), is(3));
        assertThat(candidate.indexOf(r7), is(1));
    }

    /** The index passed is ignored: an element always takes its place by date. */
    @Test
    void testAddIndexValidButWrongIndex() {
        SingleEffectiveList<Term> candidate = new SingleEffectiveList<>(Arrays.asList(r1, r4), true);
        candidate.add(0, r7);
        assertThat(candidate.indexOf(r7), is(1));
        candidate = new SingleEffectiveList<>(Arrays.asList(r1, r4), true);
        candidate.add(2, r7);
        assertThat(candidate.indexOf(r7), is(1));
    }

    /** Adding an overlapping element at an index is refused with {@link IllegalArgumentException}: the element, not the index, is what cannot be added. */
    @Test
    void testAddIndexInvalid() {
        SingleEffectiveList<Term> candidate = new SingleEffectiveList<>(Arrays.asList(r1, r4), true);
        assertThrows(IllegalArgumentException.class, () -> candidate.add(2, r6));
    }

    /** Adding at an index is held to the same gap rule as adding without one. */
    @Test
    void testAddIndexGapsNotAllowed() {
        SingleEffectiveList<Term> candidate = new SingleEffectiveList<>(Arrays.asList(r3, r4), false);
        assertThrows(IllegalArgumentException.class, () -> candidate.add(0, r1));
        assertThat(candidate, contains(r3, r4));
        candidate.add(0, r2);
        assertThat(candidate, contains(r2, r3, r4));
    }

}
