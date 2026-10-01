package io.github.ctgnz.fxtivity.employment;

import java.time.LocalDate;

import io.github.ctgnz.fxtivity.IEffectiveEntity;

/**
 * Someone holding an office for a period: a company's chief executive, or one seat on its board.
 * <p>
 * An office has one holder at a time, which is what makes a company's chief executives a succession rather than a list of employments.
 */
public final class Appointment implements IEffectiveEntity {

    private final String holder;
    private LocalDate start;
    private LocalDate end;

    /**
     * {@code holder} holding the office from {@code start} up to but excluding {@code end}.
     *
     * @param holder
     *            who held it
     * @param start
     *            the first day in office
     * @param end
     *            the first day out of office
     */
    public Appointment(String holder, LocalDate start, LocalDate end) {
        this.holder = holder;
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

    /** Who held the office. */
    public String holder() {
        return holder;
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
        return holder + " " + getEffectivity();
    }

}
