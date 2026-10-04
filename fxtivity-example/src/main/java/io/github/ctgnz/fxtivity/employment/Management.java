package io.github.ctgnz.fxtivity.employment;

import java.time.LocalDate;

import io.github.ctgnz.fxtivity.Effective;

/**
 * One span of a department managing a person: derived from the person's managing-department history, which is the end that owns the relationship, and never saved.
 */
public final class Management implements Effective {
    private final Person person;
    private LocalDate start;
    private LocalDate end;

    Management(Person person, LocalDate start, LocalDate end) {
        this.person = person;
        this.start = start;
        this.end = end;
    }

    @Override
    public LocalDate getEnd() {
        return end;
    }

    @Override
    public LocalDate getStart() {
        return start;
    }

    /** Who was managed. */
    public Person person() {
        return person;
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
        return person + " " + getEffectivity();
    }
}
