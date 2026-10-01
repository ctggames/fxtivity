package io.github.ctgnz.fxtivity;

import java.time.LocalDate;

/**
 * A plain effective entity for the unit tests: an identifier and a period, and nothing else.
 * <p>
 * Equality is identity, deliberately. Several tests hold two terms with the same dates and need them to remain two terms, and others assert that a list returns the very element
 * that was put in.
 */
final class Term implements IEffectiveEntity {

    private final String id;
    private LocalDate start;
    private LocalDate end;

    Term(String id, LocalDate start, LocalDate end) {
        this.id = id;
        this.start = start;
        this.end = end;
    }

    /** A term running from 1 January of {@code fromYear} up to but excluding 1 January of {@code toYear}. */
    static Term of(String id, int fromYear, int toYear) {
        return new Term(id, LocalDate.of(fromYear, 1, 1), LocalDate.of(toYear, 1, 1));
    }

    String id() {
        return id;
    }

    @Override
    public LocalDate getEnd() {
        return end;
    }

    @Override
    public LocalDate getStart() {
        return start;
    }

    @Override
    public void setEnd(LocalDate endDate) {
        this.end = endDate;
    }

    @Override
    public void setStart(LocalDate startDate) {
        this.start = startDate;
    }

    @Override
    public String toString() {
        return id + " " + start + ".." + end;
    }

}
