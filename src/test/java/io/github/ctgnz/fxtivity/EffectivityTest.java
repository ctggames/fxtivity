package io.github.ctgnz.fxtivity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/** Periods: containment at both ends, overlapping, enclosing, and continuing straight on from one another. */
class EffectivityTest {

    private Effectivity candidate;

    @Test
    void testContains() {
        candidate = Effectivity.create(LocalDate.of(1970, 1, 1), LocalDate.of(2051, 1, 1));
        assertThat(candidate.contains(LocalDate.of(2020, 3, 15)), is(true));
        assertThat(candidate.contains(LocalDate.of(1969, 4, 24)), is(false));
        assertThat(candidate.contains(LocalDate.of(2060, 3, 15)), is(false));
    }

    @Test
    void testLowerBounds() {
        candidate = Effectivity.create(LocalDate.of(1970, 1, 1), LocalDate.of(2051, 1, 1));
        assertThat(candidate.contains(LocalDate.of(1969, 12, 31)), is(false));
        assertThat(candidate.contains(LocalDate.of(1970, 1, 1)), is(true));
    }

    @Test
    void testUpperBounds() {
        candidate = Effectivity.create(LocalDate.of(1970, 1, 1), LocalDate.of(2051, 1, 1));
        assertThat(candidate.contains(LocalDate.of(2050, 12, 31)), is(true));
        assertThat(candidate.contains(LocalDate.of(2051, 1, 1)), is(false));
    }

    @Test
    void testOverlaps() {
        candidate = Effectivity.create(LocalDate.of(1990, 1, 1), LocalDate.of(2021, 1, 1));
        assertThat(candidate.overlaps(Effectivity.create(LocalDate.of(1970, 1, 1), LocalDate.of(2001, 1, 1))), is(true));
        assertThat(candidate.overlaps(Effectivity.create(LocalDate.of(2010, 1, 1), LocalDate.of(2051, 1, 1))), is(true));
        assertThat(candidate.overlaps(Effectivity.create(LocalDate.of(1970, 1, 1), LocalDate.of(2051, 1, 1))), is(true));
        assertThat(candidate.overlaps(Effectivity.create(LocalDate.of(1995, 1, 1), LocalDate.of(2015, 1, 1))), is(true));
        assertThat(candidate.overlaps(Effectivity.create(LocalDate.of(1970, 1, 1), LocalDate.of(1989, 12, 31))), is(false));
        assertThat(candidate.overlaps(Effectivity.create(LocalDate.of(2021, 1, 2), LocalDate.of(2051, 1, 1))), is(false));
        assertThat(candidate.overlaps(Effectivity.create(LocalDate.of(1970, 1, 1), LocalDate.of(1990, 1, 1))), is(false));
        assertThat(candidate.overlaps(Effectivity.create(LocalDate.of(2021, 1, 1), LocalDate.of(2051, 1, 1))), is(false));
    }

    @Test
    void testEncloses() {
        candidate = Effectivity.create(LocalDate.of(1990, 1, 1), LocalDate.of(2021, 1, 1));
        assertThat(candidate.encloses(Effectivity.create(LocalDate.of(1995, 1, 1), LocalDate.of(2005, 1, 1))), is(true));
        assertThat("Overlapping before is not enclosed", candidate.encloses(Effectivity.create(LocalDate.of(1970, 1, 1), LocalDate.of(2005, 1, 1))), is(false));
        assertThat("Overlapping after is not enclosed", candidate.encloses(Effectivity.create(LocalDate.of(1995, 1, 1), LocalDate.of(2051, 1, 1))), is(false));
        Effectivity bigger = Effectivity.create(LocalDate.of(1970, 1, 1), LocalDate.of(2051, 1, 1));
        assertThat("Enclosure is not commutative", bigger.encloses(candidate), is(true));
        assertThat("Enclosure is not commutative", candidate.encloses(bigger), is(false));
    }

    @Test
    void testIsContinuousBefore() {
        candidate = Effectivity.create(LocalDate.of(1990, 1, 1), LocalDate.of(2021, 1, 1));
        assertThat(candidate.continuesBefore(Effectivity.create(LocalDate.of(2021, 1, 1), LocalDate.of(2051, 1, 1))), is(true));
        Effectivity immediatelyAfter = Effectivity.create(LocalDate.of(1970, 1, 1), LocalDate.of(1990, 1, 1));
        assertThat(candidate.continuesAfter(immediatelyAfter), is(true));
        assertThat("Range that is continuesAfter should not be continuesBefore", candidate.continuesBefore(immediatelyAfter), is(false));
        assertThat("Gap of one day should not be continuesBefore", candidate.continuesBefore(Effectivity.create(LocalDate.of(2021, 1, 2), LocalDate.of(2051, 1, 1))), is(false));
        assertThat("Long gap before should not be continuesBefore", candidate.continuesBefore(Effectivity.create(LocalDate.of(2030, 1, 1), LocalDate.of(2051, 1, 1))), is(false));
    }

    @Test
    void testIsContinuousAfter() {
        candidate = Effectivity.create(LocalDate.of(1990, 1, 1), LocalDate.of(2021, 1, 1));
        assertThat(candidate.continuesAfter(Effectivity.create(LocalDate.of(1970, 1, 1), LocalDate.of(1990, 1, 1))), is(true));
        assertThat("Range that is continuesBefore should not be continuesAfter", candidate.continuesAfter(Effectivity.create(LocalDate.of(2021, 1, 1), LocalDate.of(2051, 1, 1))), is(false));
        assertThat("Range ahead of candidate should not be continuesAfter", candidate.continuesAfter(Effectivity.create(LocalDate.of(2024, 6, 1), LocalDate.of(2051, 1, 1))), is(false));
        assertThat("Gap of one day should not be continuesAfter", candidate.continuesAfter(Effectivity.create(LocalDate.of(1970, 1, 1), LocalDate.of(1989, 12, 31))), is(false));
        assertThat("Long gap before should not be continuesAfter", candidate.continuesAfter(Effectivity.create(LocalDate.of(1970, 1, 1), LocalDate.of(1985, 6, 30))), is(false));
    }

}
