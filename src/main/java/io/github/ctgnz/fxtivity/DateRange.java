package io.github.ctgnz.fxtivity;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A closed-open span of dates, {@code [lowerEndpoint, upperEndpoint)}: the lower endpoint is included and the upper is not.
 * <p>
 * Closed-open is what makes adjacent periods meet without overlapping or leaving a gap. A period ending on 1 July and the next beginning on 1 July share no day between them and
 * miss none, so "ends when the next one starts" is simply {@code end.equals(next.start)}.
 * <p>
 * A range whose endpoints are equal is <em>empty</em>: it contains no date, but it is a legitimate value, and it is what {@link #intersection(DateRange)} returns for two periods
 * that touch without sharing a day.
 * <p>
 * The method names follow Guava's {@code Range} - {@code closedOpen}, {@code lowerEndpoint}, {@code encloses}, {@code isConnected} - so that code written against one reads
 * correctly against the other.
 *
 * @param lowerEndpoint
 *            the first date in the range
 * @param upperEndpoint
 *            the first date after the range
 * @author ctg
 */
public record DateRange(LocalDate lowerEndpoint, LocalDate upperEndpoint) {

    /**
     * @throws NullPointerException
     *             if either endpoint is null
     * @throws IllegalArgumentException
     *             if the lower endpoint is after the upper
     */
    public DateRange {
        Objects.requireNonNull(lowerEndpoint, "lowerEndpoint");
        Objects.requireNonNull(upperEndpoint, "upperEndpoint");
        if (lowerEndpoint.isAfter(upperEndpoint)) {
            throw new IllegalArgumentException("Invalid range: " + lowerEndpoint + ".." + upperEndpoint);
        }
    }

    /**
     * The range from {@code lower}, included, up to {@code upper}, excluded.
     *
     * @param lower
     *            the first date in the range
     * @param upper
     *            the first date after the range
     * @return the range
     */
    public static DateRange closedOpen(LocalDate lower, LocalDate upper) {
        return new DateRange(lower, upper);
    }

    /**
     * The range holding exactly one date.
     * <p>
     * Written as {@code [date, date + 1 day)}, so it contains {@code date} and nothing else. Its {@link #upperEndpoint()} is therefore the following day rather than {@code date}
     * itself, which differs from a closed single-value range but contains exactly the same dates.
     *
     * @param date
     *            the only date in the range
     * @return the range
     */
    public static DateRange singleton(LocalDate date) {
        return new DateRange(date, date.plusDays(1));
    }

    /**
     * Whether {@code date} falls within the range: on or after the lower endpoint and strictly before the upper.
     *
     * @param date
     *            the date to test
     * @return true if the range contains it
     */
    public boolean contains(LocalDate date) {
        return !date.isBefore(lowerEndpoint) && date.isBefore(upperEndpoint);
    }

    /**
     * Whether every date in {@code other} is also in this range. An empty range is enclosed by any range whose bounds surround its endpoint.
     *
     * @param other
     *            the range to test
     * @return true if this range encloses it
     */
    public boolean encloses(DateRange other) {
        return !lowerEndpoint.isAfter(other.lowerEndpoint) && !upperEndpoint.isBefore(other.upperEndpoint);
    }

    /**
     * The span shared by this range and {@code other}, which may be empty when the two only touch.
     *
     * @param other
     *            the range to intersect with
     * @return the shared span
     * @throws IllegalArgumentException
     *             if the ranges are not {@linkplain #isConnected(DateRange) connected}
     */
    public DateRange intersection(DateRange other) {
        if (!isConnected(other)) {
            throw new IllegalArgumentException("Ranges are not connected: " + this + " and " + other);
        }
        LocalDate lower = lowerEndpoint.isAfter(other.lowerEndpoint) ? lowerEndpoint : other.lowerEndpoint;
        LocalDate upper = upperEndpoint.isBefore(other.upperEndpoint) ? upperEndpoint : other.upperEndpoint;
        return new DateRange(lower, upper);
    }

    /**
     * Whether some range, possibly empty, is enclosed by both this range and {@code other}. Ranges that merely touch are connected; their intersection is empty.
     *
     * @param other
     *            the range to test
     * @return true if the ranges are connected
     */
    public boolean isConnected(DateRange other) {
        return !lowerEndpoint.isAfter(other.upperEndpoint) && !other.lowerEndpoint.isAfter(upperEndpoint);
    }

    /**
     * Whether the range contains no dates, which is the case exactly when its endpoints are equal.
     *
     * @return true if the range is empty
     */
    public boolean isEmpty() {
        return lowerEndpoint.equals(upperEndpoint);
    }

    /**
     * The smallest range enclosing both this range and {@code other}, including any gap between them.
     *
     * @param other
     *            the range to span with
     * @return the spanning range
     */
    public DateRange span(DateRange other) {
        LocalDate lower = lowerEndpoint.isBefore(other.lowerEndpoint) ? lowerEndpoint : other.lowerEndpoint;
        LocalDate upper = upperEndpoint.isAfter(other.upperEndpoint) ? upperEndpoint : other.upperEndpoint;
        return new DateRange(lower, upper);
    }

    @Override
    public String toString() {
        return "[" + lowerEndpoint + ".." + upperEndpoint + ")";
    }

}
