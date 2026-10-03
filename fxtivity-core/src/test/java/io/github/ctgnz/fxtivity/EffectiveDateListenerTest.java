package io.github.ctgnz.fxtivity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.lang.ref.WeakReference;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringExpression;
import javafx.beans.value.ChangeListener;
import javafx.util.Subscription;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Whoever listens to the effective date decides how: strongly, weakly, or with a subscription to cancel - and can stop.
 * <p>
 * The collections are the case that has to be weak. The effective date lives as long as the application, so a collection listening strongly would never be collected.
 */
class EffectiveDateListenerTest {

    private final List<LocalDate> heard = new ArrayList<>();
    private final List<Runnable> cleanup = new ArrayList<>();

    @BeforeEach
    void init() {
        Effectivity.forDates(LocalDate.of(1950, 1, 1), LocalDate.of(1900, 1, 1), Effectivity.FOREVER);
    }

    @AfterEach
    void removeListeners() {
        cleanup.forEach(Runnable::run);
    }

    private static void collectGarbage() {
        for (int i = 0; i < 5; i++) {
            System.gc();
        }
    }

    /** A listener nobody else holds is not silently dropped: it is the owner's to remove. */
    @Test
    void testAStrongListenerKeepsListening() {
        ChangeListener<LocalDate> listener = (obs, oldDate, newDate) -> heard.add(newDate);
        Effectivity.effectiveDateProperty().addListener(listener);
        cleanup.add(() -> Effectivity.effectiveDateProperty().removeListener(listener));
        collectGarbage();
        Effectivity.forDate(LocalDate.of(1960, 1, 1));
        assertThat(heard, contains(LocalDate.of(1960, 1, 1)));
    }

    @Test
    void testAListenerCanBeRemoved() {
        ChangeListener<LocalDate> listener = (obs, oldDate, newDate) -> heard.add(newDate);
        Effectivity.effectiveDateProperty().addListener(listener);
        Effectivity.forDate(LocalDate.of(1960, 1, 1));
        Effectivity.effectiveDateProperty().removeListener(listener);
        Effectivity.forDate(LocalDate.of(1970, 1, 1));
        assertThat(heard, contains(LocalDate.of(1960, 1, 1)));
    }

    @Test
    void testASubscriptionCanBeCancelled() {
        Subscription subscription = Effectivity.effectiveDateProperty().subscribe((LocalDate date) -> heard.add(date));
        Effectivity.forDate(LocalDate.of(1960, 1, 1));
        subscription.unsubscribe();
        Effectivity.forDate(LocalDate.of(1970, 1, 1));
        assertThat("the current date on subscribing, then the change", heard, contains(LocalDate.of(1950, 1, 1), LocalDate.of(1960, 1, 1)));
    }

    /** No listener at all: a label showing the date follows it through a binding. */
    @Test
    void testTheDateCanBeBoundTo() {
        StringExpression label = Bindings.convert(Effectivity.effectiveDateProperty());
        Effectivity.forDate(LocalDate.of(1960, 1, 1));
        assertThat(label.get(), is("1960-01-01"));
    }

    /** A collection nothing else refers to can be collected, because its own listener on the date is weak. */
    @Test
    void testAnUnreachableCollectionIsCollected() {
        WeakReference<SingleEffectiveList<Term>> reference = new WeakReference<>(new SingleEffectiveList<>(List.of(Term.of("a", 1940, 1960))));
        for (int i = 0; i < 20 && reference.get() != null; i++) {
            collectGarbage();
            // Moving the date gives the property the chance to purge listeners whose target has gone.
            Effectivity.forDate(LocalDate.of(1951 + i, 1, 1));
        }
        assertThat(reference.get(), is(nullValue()));
    }

}
