package io.github.ctgnz.fxtivity;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Something that is in effect for a period: it has a {@linkplain #getStart() start} and an {@linkplain #getEnd() end}, and is in effect from the start up to but excluding the end.
 * <p>
 * Implementing this is what lets an object live in an {@link EffectiveList}, which shows only the elements in effect on the application's {@linkplain Effectivity#when() effective
 * date}. The two abstract accessors are the whole of the contract; everything else is derived from them.
 *
 * @author ctg
 */
public interface IEffectiveEntity {

    /** Orders by start date, then by end date. */
    Comparator<IEffectiveEntity> DATE_ORDER = Comparator.comparing(IEffectiveEntity::getStart).thenComparing(IEffectiveEntity::getEnd);

    /** Orders by start date, latest first, then by end date. */
    Comparator<IEffectiveEntity> REVERSE_DATE_ORDER = Comparator.comparing(IEffectiveEntity::getStart).reversed().thenComparing(IEffectiveEntity::getEnd);

    /**
     * Whether this is in effect on {@code date}.
     *
     * @param date
     *            the date to test
     * @return true if in effect
     */
    default boolean containsDate(LocalDate date) {
        return getEffectivity().contains(date);
    }

    /**
     * Sets both ends of the period.
     *
     * @param startDate
     *            the first date in effect
     * @param endDate
     *            the first date no longer in effect
     */
    default void dates(LocalDate startDate, LocalDate endDate) {
        setEffectivity(Effectivity.create(startDate, endDate));
    }

    /** Ends the period at the end of the {@linkplain Effectivity#activeRange() active range}. */
    default void end() {
        setEnd(Effectivity.activeEnd());
    }

    /**
     * Ends the period.
     *
     * @param endDate
     *            the first date no longer in effect
     */
    default void end(LocalDate endDate) {
        setEnd(endDate);
    }

    /**
     * The period as a range.
     *
     * @return the range
     */
    @JsonIgnore
    default DateRange getEffectiveDates() {
        return getEffectivity().getEffectiveDates();
    }

    /**
     * The period.
     *
     * @return the period
     */
    @JsonIgnore
    default Effectivity getEffectivity() {
        return Effectivity.create(getStart(), getEnd());
    }

    /**
     * The first date no longer in effect.
     *
     * @return the end
     */
    LocalDate getEnd();

    /**
     * The first date in effect.
     *
     * @return the start
     */
    LocalDate getStart();

    /**
     * Whether this starts after {@code other}, or starts with it and ends after it.
     *
     * @param <E>
     *            the type of the other entity
     * @param other
     *            the entity to compare with
     * @return true if this comes after it
     */
    default <E extends IEffectiveEntity> boolean isAfter(E other) {
        if (getStart().equals(other.getStart())) {
            return getEnd().isAfter(other.getEnd());
        }
        return getStart().isAfter(other.getStart());
    }

    /**
     * Whether this starts before {@code other}, or starts with it and ends before it.
     *
     * @param <E>
     *            the type of the other entity
     * @param other
     *            the entity to compare with
     * @return true if this comes before it
     */
    default <E extends IEffectiveEntity> boolean isBefore(E other) {
        if (getStart().equals(other.getStart())) {
            return getEnd().isBefore(other.getEnd());
        }
        return getStart().isBefore(other.getStart());
    }

    /**
     * Whether this is in effect on the application's {@linkplain Effectivity#when() effective date}.
     *
     * @return true if in effect
     */
    @JsonIgnore
    default boolean isEffective() {
        return isEffective(Effectivity.when());
    }

    /**
     * Whether this is in effect on {@code onDate}.
     *
     * @param onDate
     *            the date to test
     * @return true if in effect
     */
    default boolean isEffective(LocalDate onDate) {
        return getEffectivity().contains(onDate);
    }

    /**
     * Whether this is in effect on {@code atDate}.
     *
     * @param atDate
     *            the date to test
     * @return true if in effect
     */
    default boolean isValid(LocalDate atDate) {
        DateRange range = getEffectiveDates();
        return range.contains(atDate);
    }

    /**
     * Whether this is in effect on both {@code startDate} and {@code endDate}.
     *
     * @param startDate
     *            the first date to test
     * @param endDate
     *            the second date to test
     * @return true if in effect on both
     */
    default boolean isValidFor(LocalDate startDate, LocalDate endDate) {
        DateRange range = getEffectiveDates();
        return range.contains(startDate) && range.contains(endDate);
    }

    /**
     * Whether this and {@code other} share at least one day.
     *
     * @param other
     *            the entity to test
     * @return true if they overlap
     */
    default boolean overlaps(IEffectiveEntity other) {
        return overlaps(other.getEffectiveDates());
    }

    /**
     * Whether this shares at least one day with {@code other}.
     *
     * @param other
     *            the range to test
     * @return true if they overlap
     */
    default boolean overlaps(DateRange other) {
        return getEffectivity().overlaps(other);
    }

    /**
     * Sets both ends of the period from another.
     *
     * @param effectivity
     *            the period to take the dates from
     */
    default void setEffectivity(Effectivity effectivity) {
        setStart(effectivity.getStart());
        setEnd(effectivity.getEnd());
    }

    /**
     * Sets the first date in effect.
     *
     * @param startDate
     *            the start
     */
    void setStart(LocalDate startDate);

    /**
     * Sets the first date no longer in effect.
     *
     * @param endDate
     *            the end
     */
    void setEnd(LocalDate endDate);

    /** Starts the period at the start of the {@linkplain Effectivity#activeRange() active range}. */
    default void start() {
        setStart(Effectivity.activeStart());
    }

    /**
     * Starts the period.
     *
     * @param startDate
     *            the first date in effect
     */
    default void start(LocalDate startDate) {
        setStart(startDate);
    }

    /**
     * Every date in the period, in order.
     *
     * @return the dates
     */
    default Stream<LocalDate> streamDates() {
        return getStart().datesUntil(getEnd());
    }

}
