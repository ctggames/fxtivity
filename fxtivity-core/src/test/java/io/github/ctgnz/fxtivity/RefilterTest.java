package io.github.ctgnz.fxtivity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javafx.collections.ListChangeListener;
import javafx.collections.transformation.SortedList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Moving the effective date changes what is in effect in one step: a listener on {@code effective()} is told of the old contents becoming the new, and never of anything in between
 * - in particular, never of an element that is not in effect on either date.
 * <p>
 * Each term runs one decade, so on any date exactly one is in effect.
 */
class RefilterTest {

    private final List<Term> terms = new ArrayList<>();

    @BeforeEach
    void init() {
        Effectivity.forDates(LocalDate.of(1955, 1, 1), LocalDate.of(1900, 1, 1), Effectivity.FOREVER);
        terms.clear();
        for (int decade = 1900; decade < 2000; decade += 10) {
            terms.add(Term.of("t" + decade, decade, decade + 10));
        }
    }

    @Test
    void testAListenerOnlySeesWhatIsInEffect() {
        MultiEffectiveList<Term> list = new MultiEffectiveList<>(terms);
        List<Term> seen = new ArrayList<>();
        list.effective().addListener((ListChangeListener<Term>) change -> {
            while (change.next()) {
                change.getAddedSubList().stream().filter(term -> !term.isEffective(Effectivity.when())).forEach(seen::add);
            }
            change.getList().stream().filter(term -> !term.isEffective(Effectivity.when())).forEach(seen::add);
        });
        Effectivity.forDate(LocalDate.of(1965, 1, 1));
        Effectivity.forDate(LocalDate.of(1925, 1, 1));
        assertThat("elements out of effect, seen while the date moved", seen, is(empty()));
        assertThat(list.effective(), contains(terms.get(2)));
    }

    /** One change per move: old contents to new. */
    @Test
    void testAMoveIsOneChange() {
        MultiEffectiveList<Term> list = new MultiEffectiveList<>(terms);
        List<Object> changes = new ArrayList<>();
        list.effective().addListener((ListChangeListener<Term>) changes::add);
        Effectivity.forDate(LocalDate.of(1965, 1, 1));
        assertThat(changes.size(), is(1));
    }

    /**
     * A view sorted by something that only exists within an element's own period - as a person's name is null before they were born - survives the date moving. Shown with the
     * term's end year, null outside the term.
     */
    @Test
    void testASortedViewByADatedKey() {
        MultiEffectiveList<Term> list = new MultiEffectiveList<>(terms);
        SortedList<Term> sorted = new SortedList<>(list.effective(), Comparator.comparing(term -> term.isEffective(Effectivity.when()) ? term.getEnd() : null));
        // JavaFX catches what a list listener throws and hands it to the thread's handler, so a failure inside the sort would otherwise only be logged.
        List<Throwable> thrown = new ArrayList<>();
        Thread.UncaughtExceptionHandler previous = Thread.currentThread().getUncaughtExceptionHandler();
        Thread.currentThread().setUncaughtExceptionHandler((thread, e) -> thrown.add(e));
        try {
            Effectivity.forDate(LocalDate.of(1965, 1, 1));
            Effectivity.forDate(LocalDate.of(1935, 1, 1));
        } finally {
            Thread.currentThread().setUncaughtExceptionHandler(previous);
        }
        assertThat(thrown, is(empty()));
        assertThat(sorted, contains(terms.get(3)));
    }

}
