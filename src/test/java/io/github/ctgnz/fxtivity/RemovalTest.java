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
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javafx.collections.ListChangeListener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Removing elements from a succession that allows no gaps: refused, or the gap closed by a neighbour, as each removal says - and refused by every removal that cannot say.
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

    private SingleEffectiveList<Term> abcd() {
        return new SingleEffectiveList<>(List.of(a, b, c, d));
    }

    /** Every removal that is told what to do, removing b alone or b and c together - each leaving a gap. */
    private List<BiConsumer<SingleEffectiveList<Term>, Removal>> toldRemovals() {
        return List.of((list, removal) -> list.remove(1, removal), (list, removal) -> list.remove(b, removal), (list, removal) -> list.removeAll(List.of(b), removal),
            (list, removal) -> list.removeAll(List.of(b, c), removal), (list, removal) -> list.retainAll(List.of(a, d), removal), (list, removal) -> list.removeIf(term -> term == b, removal));
    }

    /** Every removal that cannot be told, removing b alone or b and c together. */
    private List<Consumer<SingleEffectiveList<Term>>> untoldRemovals() {
        return List.of(list -> list.remove(1), list -> list.remove(b), list -> list.removeAll(List.of(b)), list -> list.removeAll(List.of(b, c)), list -> list.retainAll(List.of(a, d)),
            list -> list.removeIf(term -> term == b), list -> list.subList(1, 3).clear(), list -> {
                Iterator<Term> iterator = list.iterator();
                iterator.next();
                iterator.next();
                iterator.remove();
            });
    }

    private void assertRefusedAndUnchanged(Consumer<SingleEffectiveList<Term>> removal) {
        init();
        SingleEffectiveList<Term> list = abcd();
        List<Object> fired = new ArrayList<>();
        list.addListener((ListChangeListener<Term>) fired::add);
        assertThrows(IllegalArgumentException.class, () -> removal.accept(list));
        assertThat(list, contains(a, b, c, d));
        assertThat(fired, is(empty()));
    }

    /** Refused, a removal changes nothing and announces nothing. */
    @Test
    void testRefused() {
        for (BiConsumer<SingleEffectiveList<Term>, Removal> removal : toldRemovals()) {
            assertRefusedAndUnchanged(list -> removal.accept(list, Removal.Refused));
        }
    }

    /** A removal that is not told what to do refuses to leave a gap. */
    @Test
    void testRefusedIsTheDefault() {
        for (Consumer<SingleEffectiveList<Term>> removal : untoldRemovals()) {
            assertRefusedAndUnchanged(removal);
        }
    }

    @Test
    void testExtendsPrevious() {
        for (BiConsumer<SingleEffectiveList<Term>, Removal> removal : toldRemovals()) {
            init();
            SingleEffectiveList<Term> list = abcd();
            removal.accept(list, Removal.ExtendsPrevious);
            assertThat("a now runs up to the next start", a.getEnd(), is(list.get(1).getStart()));
            assertThat(list.hasGaps(), is(false));
            assertThat("a is now in effect on 1955", list.effective(), contains(a));
        }
    }

    @Test
    void testStartsNextEarlier() {
        for (BiConsumer<SingleEffectiveList<Term>, Removal> removal : toldRemovals()) {
            init();
            SingleEffectiveList<Term> list = abcd();
            removal.accept(list, Removal.StartsNextEarlier);
            Term next = list.get(1);
            assertThat("the next now starts where a ends", next.getStart(), is(year(1950)));
            assertThat(list.hasGaps(), is(false));
            assertThat("it is now in effect on 1955", list.effective(), contains(next));
        }
    }

    /** The same list can be edited both ways, one removal at a time. */
    @Test
    void testEachRemovalChooses() {
        SingleEffectiveList<Term> list = abcd();
        list.remove(b, Removal.ExtendsPrevious);
        assertThat(a.getEnd(), is(year(1960)));
        list.remove(c, Removal.StartsNextEarlier);
        assertThat(d.getStart(), is(year(1960)));
        assertThat(list, contains(a, d));
        assertThrows(IllegalArgumentException.class, () -> new SingleEffectiveList<>(List.of(a, d, Term.of("e", 1980, 1990))).remove(d));
    }

    /** Removing from either end, or a whole run at an end, leaves no gap - so it is never refused, and nothing else moves. */
    @Test
    void testTheEndsCanAlwaysBeRemoved() {
        SingleEffectiveList<Term> list = abcd();
        list.remove(0);
        assertThat(list, contains(b, c, d));
        list.remove(d);
        assertThat(list, contains(b, c));

        init();
        list = abcd();
        list.subList(2, 4).clear();
        assertThat("a run at the end, removed at once", list, contains(a, b));

        init();
        list = abcd();
        list.removeAll(List.of(a, b), Removal.ExtendsPrevious);
        assertThat(list, contains(c, d));
        assertThat("no gap, so nothing moved", c.getStart(), is(year(1960)));

        list.clear();
        assertThat(list, is(empty()));
    }

    /** The removal and the neighbour's new dates are one change. */
    @Test
    void testClosingAGapIsOneChange() {
        SingleEffectiveList<Term> list = abcd();
        List<Object> fired = new ArrayList<>();
        list.addListener((ListChangeListener<Term>) fired::add);
        list.remove(b, Removal.ExtendsPrevious);
        assertThat(fired, hasSize(1));
    }

    /** Where gaps are allowed, removing leaves the gap and moves nothing, whatever the removal says. */
    @Test
    void testGapsAllowed() {
        SingleEffectiveList<Term> list = new SingleEffectiveList<>(List.of(a, b, c), true);
        list.remove(b, Removal.ExtendsPrevious);
        assertThat(list, contains(a, c));
        assertThat(a.getEnd(), is(year(1950)));
        assertThat(c.getStart(), is(year(1960)));
    }

    @Test
    void testAMap() {
        EffectiveMap<String, Term> map = new EffectiveMap<>();
        map.load(Map.of("seat", List.of(a, b, c)));
        assertThrows(IllegalArgumentException.class, () -> map.remove("seat", b));
        assertThat(map.get("seat"), contains(a, b, c));
        assertThat(map.remove("seat", b, Removal.ExtendsPrevious), is(true));
        assertThat(a.getEnd(), is(year(1960)));
        assertThat(map.getEffectiveRecords().effective(), contains(a));
    }

}
