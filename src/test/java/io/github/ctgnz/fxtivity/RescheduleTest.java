package io.github.ctgnz.fxtivity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javafx.collections.ListChangeListener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * An element's dates are changed through its collection, which checks the new dates against its rules, keeps itself in date order, and keeps what is in effect up to date.
 * <p>
 * The effective date is 1955 throughout, so b is the term in effect until something moves it.
 *
 * <pre>
 *  a 1940 ---- 1950
 *  b           1950 ---- 1960
 *  c                     1960 ---- 1970
 * </pre>
 */
class RescheduleTest {

    private Term a;
    private Term b;
    private Term c;

    @BeforeEach
    void init() {
        Effectivity.forDates(LocalDate.of(1955, 1, 1), LocalDate.of(1900, 1, 1), Effectivity.FOREVER);
        a = Term.of("a", 1940, 1950);
        b = Term.of("b", 1950, 1960);
        c = Term.of("c", 1960, 1970);
    }

    private static LocalDate year(int year) {
        return LocalDate.of(year, 1, 1);
    }

    private static List<ListChangeListener.Change<? extends Term>> listen(EffectiveList<Term> list) {
        List<ListChangeListener.Change<? extends Term>> fired = new ArrayList<>();
        list.addListener((ListChangeListener<Term>) fired::add);
        return fired;
    }

    @Test
    void testOverlapIsRefusedAndChangesNothing() {
        SingleEffectiveList<Term> list = new SingleEffectiveList<>(List.of(a, b, c));
        List<ListChangeListener.Change<? extends Term>> fired = listen(list);
        assertThat("b would overlap c", list.reschedule(b, year(1950), year(1965)), is(false));
        assertThat(b.getStart(), is(year(1950)));
        assertThat(b.getEnd(), is(year(1960)));
        assertThat(list, contains(a, b, c));
        assertThat(fired, is(empty()));
    }

    @Test
    void testAcceptedEditKeepsDateOrder() {
        SingleEffectiveList<Term> single = new SingleEffectiveList<>(List.of(a, b, c), true);
        assertThat(single.reschedule(a, year(1970), year(1980)), is(true));
        assertThat(single, contains(b, c, a));
    }

    @Test
    void testAcceptedEditKeepsDateOrderAllowingOverlaps() {
        MultiEffectiveList<Term> multi = new MultiEffectiveList<>(List.of(a, b, c));
        assertThat(multi.reschedule(c, year(1930), year(1990)), is(true));
        assertThat(multi, contains(c, a, b));
    }

    @Test
    void testAcceptedEditUpdatesWhatIsInEffect() {
        SingleEffectiveList<Term> single = new SingleEffectiveList<>(List.of(a, b, c), true);
        assertThat(single.effective(), contains(b));
        assertThat(single.reschedule(b, year(1950), year(1953)), is(true));
        assertThat("b now ends before the effective date", single.effective(), is(empty()));
    }

    @Test
    void testAcceptedEditUpdatesWhatIsInEffectAllowingOverlaps() {
        MultiEffectiveList<Term> multi = new MultiEffectiveList<>(List.of(a, b, c));
        assertThat(multi.reschedule(c, year(1954), year(1970)), is(true));
        assertThat("c now starts before the effective date", multi.effective(), contains(b, c));
        assertThat(Effectivity.when(), is(year(1955)));
    }

    /** Without gaps allowed, an edit is refused if it leaves a gap where the element was or where it now is. */
    @Test
    void testGapsNotAllowed() {
        SingleEffectiveList<Term> list = new SingleEffectiveList<>(List.of(a, b, c));
        assertThat("b would no longer reach c", list.reschedule(b, year(1950), year(1958)), is(false));
        assertThat("moving a to the end leaves nothing before b, but a would not start where c ends", list.reschedule(a, year(1971), year(1980)), is(false));
        assertThat("moving b leaves nothing in effect between a and c", list.reschedule(b, year(1970), year(1980)), is(false));
        assertThat(list, contains(a, b, c));
        assertThat("the outer ends can move freely", list.reschedule(a, year(1930), year(1950)), is(true));
        assertThat(list.reschedule(c, year(1960), year(1990)), is(true));
        assertThat("a moved from the front to the end, starting where c ends", list.reschedule(a, year(1990), year(1995)), is(true));
        assertThat(list, contains(b, c, a));
    }

    @Test
    void testAnElementNotInTheListIsRefused() {
        SingleEffectiveList<Term> list = new SingleEffectiveList<>(List.of(a, b), true);
        assertThat(list.reschedule(c, year(1980), year(1990)), is(false));
        assertThat(c.getStart(), is(year(1960)));
    }

    /** Making room shortens a neighbour, and that change shows in what is in effect straight away. */
    @Test
    void testInsertingUpdatesWhatIsInEffect() {
        SingleEffectiveList<Term> backwards = new SingleEffectiveList<>(List.of(a, b, c));
        Term d = Term.of("d", 1953, 1960);
        assertThat(backwards.insertBackwards(d), is(true));
        assertThat("b now ends in 1953", backwards.effective(), contains(d));

        a = Term.of("a", 1940, 1950);
        b = Term.of("b", 1950, 1960);
        c = Term.of("c", 1960, 1970);
        SingleEffectiveList<Term> forwards = new SingleEffectiveList<>(List.of(a, b, c));
        Term e = Term.of("e", 1950, 1956);
        assertThat(forwards.insertForwards(e), is(true));
        assertThat("b now starts in 1956", forwards.effective(), contains(e));
    }

}
