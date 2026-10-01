package io.github.ctgnz.fxtivity.employment;

import java.time.LocalDate;

import io.github.ctgnz.fxtivity.Effectivity;
import io.github.ctgnz.fxtivity.IEffectiveEntity;

/**
 * Fowler's {@code Employment}: a person's employment by a company, in effect for a period.
 * <p>
 * Fowler's methods are kept under their own names where they still say something - {@link #company()}, {@link #isEffectiveOn(LocalDate)} - so the example reads against his. His
 * {@code end(MfDate)} and {@code setEffectivity(DateRange)} are supplied by {@link IEffectiveEntity}.
 */
public final class Employment implements IEffectiveEntity {

    private final Company company;
    private LocalDate start;
    private LocalDate end;

    /**
     * Employment by {@code company} from {@code startDate}, until further notice.
     *
     * @param company
     *            the employer
     * @param startDate
     *            the first day of the employment
     */
    public Employment(Company company, LocalDate startDate) {
        this(company, startDate, Effectivity.FOREVER);
    }

    /**
     * Employment by {@code company} for a known period.
     *
     * @param company
     *            the employer
     * @param startDate
     *            the first day of the employment
     * @param endDate
     *            the first day after it
     */
    public Employment(Company company, LocalDate startDate, LocalDate endDate) {
        this.company = company;
        this.start = startDate;
        this.end = endDate;
    }

    /** Fowler's {@code company()}. */
    public Company company() {
        return company;
    }

    @Override
    public LocalDate getEnd() {
        return end;
    }

    @Override
    public LocalDate getStart() {
        return start;
    }

    /** Fowler's {@code isEffectiveOn(MfDate)}. */
    public boolean isEffectiveOn(LocalDate date) {
        return containsDate(date);
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
        return company + " " + getEffectivity();
    }

}
