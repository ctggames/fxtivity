package io.github.ctgnz.fxtivity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javafx.collections.ListChangeListener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.ctgnz.fxtivity.EffectiveProperty.Entry;
import io.github.ctgnz.fxtivity.consumer.ExtensibleOwner;

/** A value changing during its owner's lifetime: bounded by the owner's period, and read back as one span per value. */
class EffectivePropertyTest {

    private Term owner;
    private EffectiveProperty<String> candidate;

    @BeforeEach
    void init() {
        Effectivity.forDates(LocalDate.of(1975, 1, 1), LocalDate.of(1900, 1, 1), Effectivity.FOREVER);
        owner = Term.of("owner", 1970, 1989);
        candidate = new EffectiveProperty<>(owner);
    }

    /** One span per change, including a change that repeats the previous value - spans are not merged by value. */
    @Test
    void testToList() {
        candidate.setValue(LocalDate.of(1970, 1, 1), "Junior");
        candidate.setValue(LocalDate.of(1975, 1, 1), "Senior");
        candidate.setValue(LocalDate.of(1979, 1, 1), "Principal");
        candidate.setValue(LocalDate.of(1985, 1, 1), "Principal");
        assertThat(candidate.toWrappedList(), hasSize(4));
    }

    @Test
    void testToListWithOneEntry() {
        candidate.setValue(LocalDate.of(1970, 1, 1), "Junior");
        assertThat(candidate.toWrappedList(), hasSize(1));
    }

    /** A history with no changes yet is ordinary - just created, or emptied - and reads as no spans at all. */
    @Test
    void testToWrappedListWhenEmpty() {
        assertThat(candidate.toWrappedList(), hasSize(0));
    }

    @Test
    void testToListWhenEmpty() {
        assertThat(candidate.toList(value -> Term.of(value, 1970, 1971)), hasSize(0));
    }

    /** Values that are already effective come back as themselves, given their spans' dates - no class argument, no cast. */
    @Test
    void testToListOfValuesThatAreAlreadyEffective() {
        EffectiveProperty<Term> terms = new EffectiveProperty<>(owner);
        assertThat(terms.toList(Function.identity()), hasSize(0));
        Term junior = Term.of("junior", 1900, 1901);
        Term senior = Term.of("senior", 1900, 1901);
        terms.setValue(LocalDate.of(1970, 1, 1), junior);
        terms.setValue(LocalDate.of(1975, 1, 1), senior);
        SingleEffectiveList<Term> spans = terms.toList(Function.identity());
        assertThat(spans, contains(junior, senior));
        assertThat(junior.getEnd(), is(LocalDate.of(1975, 1, 1)));
        assertThat(senior.getEnd(), is(LocalDate.of(1989, 1, 1)));
    }

    @Test
    void testAnExtensibleOwner() {
        ExtensibleOwner extensible = new ExtensibleOwner();
        assertThat(extensible.name().setValue(LocalDate.of(1980, 1, 1), "Ext"), is(true));
        assertThat(extensible.name().getOwner(), is(extensible));
    }

    /** Emptying a history that had changes leaves it reading as no spans, rather than as the last one it held. */
    @Test
    void testToWrappedListWhenEmptied() {
        candidate.setValue(LocalDate.of(1970, 1, 1), "Junior");
        candidate.clear();
        assertThat(candidate.toWrappedList(), hasSize(0));
    }

    /** The entries are live and read-only: a list view can show them directly, and a change made through the property is announced there, in its place by date. */
    @Test
    void testTheEntriesAreLiveAndReadOnly() {
        candidate.setValue(LocalDate.of(1970, 1, 1), "Junior");
        candidate.setValue(LocalDate.of(1979, 1, 1), "Principal");
        List<String> announced = new ArrayList<>();
        candidate.getEntries().addListener((ListChangeListener<Entry<String>>) change -> {
            while (change.next()) {
                assertThat("one entry added in its place, not a re-sort", change.wasPermutated(), is(false));
                change.getAddedSubList().forEach(entry -> announced.add(change.getFrom() + ":" + entry.getValue()));
            }
        });
        candidate.setValue(LocalDate.of(1975, 1, 1), "Senior");
        assertThat(announced, contains("1:Senior"));
        assertThat(candidate.getEntries().stream().map(Entry::getValue).toList(), contains("Junior", "Senior", "Principal"));
        assertThrows(UnsupportedOperationException.class, () -> candidate.getEntries().clear());
    }

    /** A value can only be set within the owner's period. */
    @Test
    void testRange() {
        assertThat(candidate.setValue(LocalDate.of(1960, 1, 1), "Junior"), is(false));
        assertThat(candidate.setValue(LocalDate.of(1976, 1, 1), "Junior"), is(true));
        assertThat(candidate.setValue(LocalDate.of(1990, 1, 1), "Junior"), is(false));
    }

    /** The value on a date is set by the latest change on or before it, and there is none outside the owner's period. */
    @Test
    void testEffectiveValue() {
        candidate.setValue(LocalDate.of(1970, 1, 1), "Junior");
        candidate.setValue(LocalDate.of(1975, 1, 1), "Senior");
        candidate.setValue(LocalDate.of(1979, 1, 1), "Principal");
        candidate.setValue(LocalDate.of(1985, 1, 1), "Principal");
        assertThat(candidate.getEffectiveValue(LocalDate.of(1974, 1, 1)), is("Junior"));
        assertThat(candidate.getEffectiveValue(LocalDate.of(1976, 1, 1)), is("Senior"));
        assertThat(candidate.getEffectiveValue(LocalDate.of(1982, 1, 1)), is("Principal"));
        assertThat(candidate.getEffectiveValue(LocalDate.of(1962, 1, 1)), nullValue());
        assertThat(candidate.getEffectiveValue(LocalDate.of(1992, 1, 1)), nullValue());
    }

}
