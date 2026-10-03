package io.github.ctgnz.fxtivity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A listener on a collection learns exactly what it holds, from the change events alone.
 * <p>
 * Each test keeps a mirror built from nothing but the events the collection fires about itself, and compares it with the collection after every change. A mirror that drifts is a
 * control bound to the collection showing rows in the wrong order, or rows that are no longer there.
 */
class ChangeEventsTest {

    private Term a;
    private Term b;
    private Term c;

    /** A copy of a list, kept up to date from its change events and nothing else. */
    private static final class Mirror<E> implements ListChangeListener<E> {
        private final List<E> copy = new ArrayList<>();

        Mirror(ObservableList<E> list) {
            copy.addAll(list);
            list.addListener(this);
        }

        @Override
        public void onChanged(Change<? extends E> change) {
            while (change.next()) {
                if (change.wasPermutated()) {
                    List<E> moved = new ArrayList<>(copy);
                    for (int i = change.getFrom(); i < change.getTo(); i++) {
                        moved.set(change.getPermutation(i), copy.get(i));
                    }
                    copy.clear();
                    copy.addAll(moved);
                } else if (!change.wasUpdated()) {
                    copy.subList(change.getFrom(), change.getFrom() + change.getRemovedSize()).clear();
                    copy.addAll(change.getFrom(), change.getAddedSubList());
                }
            }
        }
    }

    @BeforeEach
    void init() {
        Effectivity.forDates(LocalDate.of(1950, 1, 1), LocalDate.of(1900, 1, 1), Effectivity.FOREVER);
        a = Term.of("a", 1940, 1950);
        b = Term.of("b", 1950, 1960);
        c = Term.of("c", 1960, 1970);
    }

    private static <E extends IEffectiveEntity> void assertMirrored(EffectiveList<E> list, Mirror<E> mirror) {
        assertThat(mirror.copy, is(list));
    }

    @Test
    void testAddOutOfOrder() {
        SingleEffectiveList<Term> list = new SingleEffectiveList<>(true);
        Mirror<Term> mirror = new Mirror<>(list);
        list.add(c);
        list.add(a);
        list.add(b);
        assertThat(list, contains(a, b, c));
        assertMirrored(list, mirror);
    }

    @Test
    void testAddAllOutOfOrder() {
        MultiEffectiveList<Term> list = new MultiEffectiveList<>();
        Mirror<Term> mirror = new Mirror<>(list);
        list.addAll(c, a, b);
        assertThat(list, contains(a, b, c));
        assertMirrored(list, mirror);
    }

    @Test
    void testAddAtIndex() {
        SingleEffectiveList<Term> list = new SingleEffectiveList<>(List.of(b, c));
        Mirror<Term> mirror = new Mirror<>(list);
        list.add(2, a);
        assertThat(list, contains(a, b, c));
        assertMirrored(list, mirror);
    }

    @Test
    void testRemoveAtIndex() {
        SingleEffectiveList<Term> list = new SingleEffectiveList<>(List.of(a, b, c), true);
        Mirror<Term> mirror = new Mirror<>(list);
        list.remove(1);
        assertThat(list, contains(a, c));
        assertMirrored(list, mirror);
        list.remove(0);
        list.remove(0);
        assertThat(list, is(empty()));
        assertMirrored(list, mirror);
    }

    @Test
    void testRemoveElement() {
        MultiEffectiveList<Term> list = new MultiEffectiveList<>(List.of(a, b, c));
        Mirror<Term> mirror = new Mirror<>(list);
        list.remove(b);
        assertThat(list, contains(a, c));
        assertMirrored(list, mirror);
    }

    @Test
    void testSetAllOutOfOrder() {
        SingleEffectiveList<Term> list = new SingleEffectiveList<>(List.of(a));
        Mirror<Term> mirror = new Mirror<>(list);
        list.setAll(c, b);
        assertThat(list, contains(b, c));
        assertMirrored(list, mirror);
    }

    /** A refused {@code setAll} changes nothing, and so announces nothing that a listener would have to undo. */
    @Test
    void testSetAllRefused() {
        SingleEffectiveList<Term> list = new SingleEffectiveList<>(List.of(a));
        Mirror<Term> mirror = new Mirror<>(list);
        List<Term> fired = new ArrayList<>();
        list.addListener((ListChangeListener<Term>) change -> fired.add(null));
        assertThat("b and an overlapping term", list.setAll(b, Term.of("overlapping", 1955, 1965)), is(false));
        assertThat(list, contains(a));
        assertThat(fired, is(empty()));
        assertMirrored(list, mirror);
    }

    /** The list is ordered by date and nothing else, so sorting it another way is refused rather than quietly undone. */
    @Test
    void testSortIsRefused() {
        MultiEffectiveList<Term> list = new MultiEffectiveList<>(List.of(a, b, c));
        Mirror<Term> mirror = new Mirror<>(list);
        assertThrows(UnsupportedOperationException.class, () -> list.sort(Comparator.comparing(Term::toString).reversed()));
        assertThat(list, contains(a, b, c));
        assertMirrored(list, mirror);
    }

    @Test
    void testReschedule() {
        MultiEffectiveList<Term> list = new MultiEffectiveList<>(List.of(a, b, c));
        Mirror<Term> mirror = new Mirror<>(list);
        Mirror<Term> effective = new Mirror<>(list.effective());
        list.reschedule(c, LocalDate.of(1930, 1, 1), LocalDate.of(1990, 1, 1));
        assertThat(list, contains(c, a, b));
        assertMirrored(list, mirror);
        assertThat(effective.copy, contains(c, b));
    }

    /** Removing b closes its gap by moving a neighbour: one change, which the list's listeners and the effective view follow. */
    @Test
    void testRemovalClosingAGap() {
        for (Removal removal : List.of(Removal.ExtendsPrevious, Removal.StartsNextEarlier)) {
            init();
            SingleEffectiveList<Term> list = new SingleEffectiveList<>(List.of(a, b, c));
            Mirror<Term> mirror = new Mirror<>(list);
            Mirror<Term> effective = new Mirror<>(list.effective());
            Effectivity.forDate(LocalDate.of(1955, 1, 1));
            list.removeAll(List.of(b), removal);
            assertMirrored(list, mirror);
            assertThat(effective.copy, is(list.effective()));
            assertThat(effective.copy, hasSize(1));
        }
    }

    /** Elements with the same dates go in by the tie-break, wherever that puts them, and are announced there. */
    @Test
    void testTieBreak() {
        MultiEffectiveList<Term> list = new MultiEffectiveList<>(Comparator.comparing(Term::id).reversed());
        Mirror<Term> mirror = new Mirror<>(list);
        Term x = Term.of("x", 1940, 1950);
        Term y = Term.of("y", 1940, 1950);
        Term z = Term.of("z", 1940, 1950);
        list.add(x);
        list.add(z);
        list.add(y);
        assertThat(list, contains(z, y, x));
        assertMirrored(list, mirror);
        list.setAll(x, y, z);
        assertMirrored(list, mirror);
    }

    /** The full list, the read-only source and the effective view each tell their own listeners the same story. */
    @Test
    void testEveryViewAgrees() {
        SingleEffectiveList<Term> list = new SingleEffectiveList<>(true);
        Mirror<Term> mirror = new Mirror<>(list);
        Mirror<Term> source = new Mirror<>(list.getSourceList());
        Mirror<Term> effective = new Mirror<>(list.effective());
        list.addAll(c, a);
        list.add(b);
        list.remove(0);
        assertMirrored(list, mirror);
        assertThat(source.copy, is(list.getSourceList()));
        assertThat(effective.copy, is(list.effective()));
        assertThat(effective.copy, contains(b));
    }

}
