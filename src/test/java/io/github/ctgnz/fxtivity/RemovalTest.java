package io.github.ctgnz.fxtivity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javafx.collections.ListChangeListener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Removing elements from a succession that allows no gaps: refused, or the gap closed by a neighbour, as chosen when the succession was created - on every path that removes.
 * <p>
 * The effective date is 1955 throughout, in b.
 *
 * <pre>
 *  a 1940 ---- 1950
 *  b           1950 ---- 1960
 *  c                     1960 ---- 1970
 *  d                               1970 ---- 1980
 * </pre>
 */
class RemovalTest {

    private Term a;
    private Term b;
    private Term c;
    private Term d;

    @BeforeEach
    void init() {
        Effectivity.forDates(LocalDate.of(1955, 1, 1), LocalDate.of(1900, 1, 1), Effectivity.FOREVER);
        a = Term.of("a", 1940, 1950);
        b = Term.of("b", 1950, 1960);
        c = Term.of("c", 1960, 1970);
        d = Term.of("d", 1970, 1980);
    }

    private static LocalDate year(int year) {
        return LocalDate.of(year, 1, 1);
    }

    private SingleEffectiveList<Term> abcd(Removal removal) {
        return new SingleEffectiveList<>(List.of(a, b, c, d), false, removal);
    }

    /** Every way of removing b alone, or b and c together - each leaving a gap between a and d or c. */
    private List<Consumer<SingleEffectiveList<Term>>> middleRemovals() {
        return List.of(list -> list.remove(1), list -> list.remove(b), list -> list.removeAll(List.of(b)), list -> list.removeAll(List.of(b, c)), list -> list.retainAll(List.of(a, d)),
            list -> list.removeIf(term -> term == b), list -> list.subList(1, 3).clear(), list -> {
                Iterator<Term> iterator = list.iterator();
                iterator.next();
                iterator.next();
                iterator.remove();
            });
    }

    @Test
    void testRefusedIsTheDefault() {
        assertThat(new SingleEffectiveList<Term>().getRemoval(), is(Removal.Refused));
        assertThat(new SingleEffectiveList<>(List.of(a, b)).getRemoval(), is(Removal.Refused));
        assertThat(new EffectiveMap<String, Term>().getRemoval(), is(Removal.Refused));
    }

    /** Refused on every path, and a refused removal changes nothing and announces nothing. */
    @Test
    void testARemovalLeavingAGapIsRefused() {
        for (Consumer<SingleEffectiveList<Term>> removal : middleRemovals()) {
            init();
            SingleEffectiveList<Term> list = abcd(Removal.Refused);
            List<Object> fired = new ArrayList<>();
            list.addListener((ListChangeListener<Term>) fired::add);
            assertThrows(IllegalArgumentException.class, () -> removal.accept(list));
            assertThat(list, contains(a, b, c, d));
            assertThat(fired, is(empty()));
        }
    }

    /** Removing from either end, or a whole run at an end, leaves no gap - so it is never refused, and nothing else moves. */
    @Test
    void testTheEndsCanAlwaysBeRemoved() {
        SingleEffectiveList<Term> list = abcd(Removal.Refused);
        list.remove(0);
        assertThat(list, contains(b, c, d));
        list.remove(d);
        assertThat(list, contains(b, c));

        init();
        list = abcd(Removal.Refused);
        list.subList(2, 4).clear();
        assertThat("a run at the end, removed at once", list, contains(a, b));

        init();
        list = abcd(Removal.Refused);
        list.removeAll(List.of(a, b));
        assertThat(list, contains(c, d));
        assertThat(c.getStart(), is(year(1960)));

        list.clear();
        assertThat(list, is(empty()));
    }

    @Test
    void testExtendsPrevious() {
        for (Consumer<SingleEffectiveList<Term>> removal : middleRemovals()) {
            init();
            SingleEffectiveList<Term> list = abcd(Removal.ExtendsPrevious);
            removal.accept(list);
            assertThat("a now runs up to the next start", a.getEnd(), is(list.get(1).getStart()));
            assertThat(list.hasGaps(), is(false));
            assertThat("a is now in effect on 1955", list.effective(), contains(a));
        }
    }

    @Test
    void testStartsNextEarlier() {
        for (Consumer<SingleEffectiveList<Term>> removal : middleRemovals()) {
            init();
            SingleEffectiveList<Term> list = abcd(Removal.StartsNextEarlier);
            removal.accept(list);
            Term next = list.get(1);
            assertThat("the next now starts where a ends", next.getStart(), is(year(1950)));
            assertThat(list.hasGaps(), is(false));
            assertThat("it is now in effect on 1955", list.effective(), contains(next));
        }
    }

    /** The removal and the neighbour's new dates are one change. */
    @Test
    void testClosingAGapIsOneChange() {
        SingleEffectiveList<Term> list = abcd(Removal.ExtendsPrevious);
        List<Object> fired = new ArrayList<>();
        list.addListener((ListChangeListener<Term>) fired::add);
        list.remove(b);
        assertThat(fired, hasSize(1));
    }

    /** Where gaps are allowed, removing leaves the gap and moves nothing, whatever the removal. */
    @Test
    void testGapsAllowed() {
        SingleEffectiveList<Term> list = new SingleEffectiveList<>(List.of(a, b, c), true, Removal.ExtendsPrevious);
        list.remove(b);
        assertThat(list, contains(a, c));
        assertThat(a.getEnd(), is(year(1950)));
        assertThat(c.getStart(), is(year(1960)));
    }

    @Test
    void testAMapPassesItOn() {
        EffectiveMap<String, Term> refusing = new EffectiveMap<>();
        refusing.load(Map.of("seat", List.of(a, b, c)));
        assertThrows(IllegalArgumentException.class, () -> refusing.remove("seat", b));
        assertThat(refusing.get("seat"), contains(a, b, c));

        init();
        EffectiveMap<String, Term> closing = new EffectiveMap<>(false, Removal.ExtendsPrevious);
        closing.load(Map.of("seat", List.of(a, b, c)));
        assertThat(closing.remove("seat", b), is(true));
        assertThat(a.getEnd(), is(year(1960)));
        assertThat(closing.getEffectiveRecords().effective(), contains(a));
    }

}
