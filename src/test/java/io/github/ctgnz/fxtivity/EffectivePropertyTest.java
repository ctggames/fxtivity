package io.github.ctgnz.fxtivity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** A value changing during its owner's lifetime: bounded by the owner's period, and read back as one span per value. */
class EffectivePropertyTest {

    private Term owner;
    private EffectiveProperty<String, Term> candidate;

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
