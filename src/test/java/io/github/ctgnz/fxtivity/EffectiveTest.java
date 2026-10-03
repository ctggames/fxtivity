package io.github.ctgnz.fxtivity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/** What an entity's period answers about itself. */
class EffectiveTest {

    private static LocalDate year(int year) {
        return LocalDate.of(year, 1, 1);
    }

    /** In effect throughout a range: including its own period, whose end date is the first day it is not in effect. */
    @Test
    void testIsValidFor() {
        Term term = Term.of("term", 1940, 1950);
        assertThat("its own period", term.isValidFor(year(1940), year(1950)), is(true));
        assertThat("within it", term.isValidFor(year(1942), year(1945)), is(true));
        assertThat("starting before it", term.isValidFor(year(1939), year(1950)), is(false));
        assertThat("running past it", term.isValidFor(year(1940), year(1951)), is(false));
    }

    /** The period is a snapshot: changing it leaves the entity as it was. Dates are changed on the entity, or through its collection. */
    @Test
    void testTheEffectivityIsASnapshot() {
        Term term = Term.of("term", 1940, 1950);
        Effectivity period = term.getEffectivity();
        period.end(year(1945));
        assertThat(term.getEnd(), is(year(1950)));
        assertThat("a fresh snapshot each time", term.getEffectivity().getEnd(), is(year(1950)));
    }

}
