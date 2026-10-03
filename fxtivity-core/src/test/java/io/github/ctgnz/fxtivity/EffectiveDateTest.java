package io.github.ctgnz.fxtivity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * There is one effective date per application, and every collection follows it.
 * <p>
 * No list has a date of its own to disagree with, so this checks the consequence rather than the absence: lists of both kinds, created before and after the date moves, always show
 * exactly what is in effect on it.
 */
class EffectiveDateTest {

    @BeforeEach
    void init() {
        Effectivity.forDates(LocalDate.of(1950, 1, 1), LocalDate.of(1900, 1, 1), Effectivity.FOREVER);
    }

    @Test
    void testEveryListAgreesOnWhatIsInEffect() {
        Term a = Term.of("a", 1940, 1960);
        Term b = Term.of("b", 1960, 1980);
        SingleEffectiveList<Term> before = new SingleEffectiveList<>(List.of(a, b));
        MultiEffectiveList<Term> multi = new MultiEffectiveList<>(List.of(a, b));
        Effectivity.forDate(LocalDate.of(1970, 1, 1));
        SingleEffectiveList<Term> after = new SingleEffectiveList<>(List.of(a, b));

        for (LocalDate date : List.of(LocalDate.of(1945, 1, 1), LocalDate.of(1959, 12, 31), LocalDate.of(1960, 1, 1), LocalDate.of(1985, 1, 1), LocalDate.of(1965, 6, 30))) {
            Effectivity.forDate(date);
            List<Term> inEffect = Stream.of(a, b).filter(term -> term.isEffective(date)).toList();
            assertThat("created before the date moved, on " + date, before.effective(), is(inEffect));
            assertThat("created after the date moved, on " + date, after.effective(), is(inEffect));
            assertThat("allowing overlaps, on " + date, multi.effective(), is(inEffect));
        }
    }

}
