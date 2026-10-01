package io.github.ctgnz.fxtivity;

import java.time.LocalDate;
import java.util.Objects;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A period during which something is in effect, and - statically - the date the whole application is currently looking at.
 * <h2>The effective date</h2> There is exactly one effective date per application, held here, and every effective collection follows it. Moving it with {@link #forDate(LocalDate)}
 * updates every {@link EffectiveList} at once, so anything bound to one of those lists - a table, a chart, a tree - shows the state of the world on the new date without
 * registering a listener or binding of its own.
 * <p>
 * That it is application-wide is the point rather than a limitation. Two collections disagreeing about what "now" means within one application is a defect, so there is no way to
 * give one collection a date of its own.
 * <p>
 * The effective date always lies within the <em>active range</em>, set with {@link #forDates(LocalDate, LocalDate, LocalDate)}, which bounds what the application is prepared to
 * look at.
 * <h2>A period</h2> An instance is a {@linkplain DateRange closed-open} period from {@link #getStart()} up to but excluding {@link #getEnd()}. A period with no end runs until
 * {@link #FOREVER}.
 *
 * @author ctg
 */
public class Effectivity {
    /** The end of a period that has no end. */
    public static final LocalDate FOREVER = LocalDate.of(9999, 12, 31);
    private static final ObjectProperty<LocalDate> EDATE = new SimpleObjectProperty<>(LocalDate.now());
    private static final ObjectProperty<DateRange> ERANGE = new SimpleObjectProperty<>(toEffectiveRange(LocalDate.now(), FOREVER));

    /**
     * The end of the active range.
     *
     * @return the first date after the active range
     */
    public static LocalDate activeEnd() {
        return ERANGE.get().upperEndpoint();
    }

    /**
     * The range the effective date is allowed to move within.
     *
     * @return the active range
     */
    public static DateRange activeRange() {
        return ERANGE.get();
    }

    /**
     * The start of the active range.
     *
     * @return the first date in the active range
     */
    public static LocalDate activeStart() {
        return ERANGE.get().lowerEndpoint();
    }

    /**
     * A period covering the whole of the active range.
     *
     * @return the period
     */
    public static Effectivity create() {
        return new Effectivity(activeStart(), activeEnd());
    }

    /**
     * A period from {@code startDate} up to but excluding {@code endDate}.
     *
     * @param startDate
     *            the first date in effect
     * @param endDate
     *            the first date no longer in effect
     * @return the period
     */
    public static Effectivity create(LocalDate startDate, LocalDate endDate) {
        return new Effectivity(startDate, endDate);
    }

    /**
     * The earlier of two dates, treating null as absent rather than as either extreme.
     *
     * @param date1
     *            a date, or null
     * @param date2
     *            a date, or null
     * @return the earlier, or whichever is not null
     */
    public static LocalDate earlier(LocalDate date1, LocalDate date2) {
        if (date1 == null) {
            return date2;
        }
        if (date2 == null) {
            return date1;
        }
        if (date1.isAfter(date2)) {
            return date2;
        } else {
            return date1;
        }
    }

    /**
     * Moves the application's effective date, which every effective collection follows.
     *
     * @param effectiveDate
     *            the new effective date
     * @throws IllegalArgumentException
     *             if the date is outside the {@linkplain #activeRange() active range}
     */
    public static void forDate(LocalDate effectiveDate) {
        if (ERANGE.get().contains(effectiveDate)) {
            EDATE.set(effectiveDate);
        } else {
            throw new IllegalArgumentException("Effective date " + effectiveDate + " is outside the active range " + ERANGE.get());
        }
    }

    /**
     * Sets the active range and the effective date together.
     *
     * @param effectiveDate
     *            the new effective date
     * @param startDate
     *            the first date of the active range
     * @param endDate
     *            the first date after the active range
     */
    public static void forDates(LocalDate effectiveDate, LocalDate startDate, LocalDate endDate) {
        ERANGE.set(toEffectiveRange(startDate, endDate));
        EDATE.set(effectiveDate);
    }

    /**
     * The later of two dates, treating null as absent rather than as either extreme.
     *
     * @param date1
     *            a date, or null
     * @param date2
     *            a date, or null
     * @return the later, or whichever is not null
     */
    public static LocalDate later(LocalDate date1, LocalDate date2) {
        if (date1 == null) {
            return date2;
        }
        if (date2 == null) {
            return date1;
        }
        if (date1.isAfter(date2)) {
            return date1;
        } else {
            return date2;
        }
    }

    /**
     * Registers a listener on the effective date.
     * <p>
     * Held weakly, so the listener does not keep its owner alive; the caller must keep a strong reference for as long as it wants notifications.
     *
     * @param listener
     *            the listener to register
     */
    public static void listen(ChangeListener<? super LocalDate> listener) {
        EDATE.addListener(new WeakChangeListener<>(listener));
    }

    /** Moves the effective date back to today. */
    public static void reset() {
        EDATE.set(LocalDate.now());
    }

    /**
     * The closed-open range for a pair of dates.
     *
     * @param startDate
     *            the first date in the range
     * @param endDate
     *            the first date after the range
     * @return the range
     */
    public static DateRange toEffectiveRange(LocalDate startDate, LocalDate endDate) {
        return DateRange.closedOpen(startDate, endDate);
    }

    /**
     * The application's current effective date.
     *
     * @return the effective date
     */
    public static LocalDate when() {
        return EDATE.get();
    }

    private LocalDate start;
    private LocalDate end;
    private DateRange effectiveRange;

    /**
     * A period from {@code startDate} up to but excluding {@code endDate}.
     *
     * @param startDate
     *            the first date in effect, or null for today
     * @param endDate
     *            the first date no longer in effect, or null for {@link #FOREVER}
     */
    @JsonCreator
    public Effectivity(@JsonProperty("start") LocalDate startDate, @JsonProperty("end") LocalDate endDate) {
        this.start = Objects.requireNonNullElse(startDate, LocalDate.now());
        this.end = Objects.requireNonNullElse(endDate, FOREVER);
        updateRange();
    }

    /** A period from today with no end. */
    protected Effectivity() {
        this(LocalDate.now(), FOREVER);
    }

    /**
     * A period from {@code startDate} with no end.
     *
     * @param startDate
     *            the first date in effect, or null for today
     */
    protected Effectivity(LocalDate startDate) {
        this(Objects.requireNonNullElse(startDate, LocalDate.now()), FOREVER);
    }

    /**
     * Whether the period is in effect on {@code onDate}.
     *
     * @param onDate
     *            the date to test
     * @return true if in effect
     */
    public boolean contains(LocalDate onDate) {
        return effectiveRange.contains(onDate);
    }

    /**
     * Whether this period starts exactly where {@code other} ends, and later than it starts.
     *
     * @param other
     *            the period this might follow
     * @return true if this period continues straight on from it
     */
    public boolean continuesAfter(Effectivity other) {
        return start.isAfter(other.start) && other.end.equals(start);
    }

    /**
     * Whether this period ends exactly where {@code other} starts, and earlier than it ends.
     *
     * @param other
     *            the period this might precede
     * @return true if this period runs straight on into it
     */
    public boolean continuesBefore(Effectivity other) {
        return end.isBefore(other.end) && end.equals(other.start);
    }

    /**
     * Whether every date in {@code other} is also in this period.
     *
     * @param other
     *            the period to test
     * @return true if this period encloses it
     */
    public boolean encloses(Effectivity other) {
        return effectiveRange.encloses(other.effectiveRange);
    }

    /**
     * Moves the end of the period.
     *
     * @param endDate
     *            the first date no longer in effect, or null for {@link #FOREVER}
     */
    public void end(LocalDate endDate) {
        this.end = Objects.requireNonNullElse(endDate, FOREVER);
        updateRange();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Effectivity rhs) {
            return Objects.equals(getStart(), rhs.getStart()) && Objects.equals(getEnd(), rhs.getEnd());
        }
        return false;
    }

    /**
     * The period as a range.
     *
     * @return the range
     */
    @JsonIgnore
    public DateRange getEffectiveDates() {
        return effectiveRange;
    }

    /**
     * The first date no longer in effect.
     *
     * @return the end
     */
    public LocalDate getEnd() {
        return end;
    }

    /**
     * The first date in effect.
     *
     * @return the start
     */
    public LocalDate getStart() {
        return start;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getStart(), getEnd());
    }

    /**
     * Whether this period and {@code other} share at least one day. Periods that only touch do not overlap.
     *
     * @param other
     *            the period to test
     * @return true if they overlap
     */
    public boolean overlaps(Effectivity other) {
        return overlaps(other.effectiveRange);
    }

    /**
     * Whether this period and {@code dateRange} share at least one day. A period that only touches the range does not overlap it.
     *
     * @param dateRange
     *            the range to test
     * @return true if they overlap
     */
    public boolean overlaps(DateRange dateRange) {
        return effectiveRange.isConnected(dateRange) && !effectiveRange.intersection(dateRange).isEmpty();
    }

    /**
     * Moves the start of the period.
     *
     * @param startDate
     *            the first date in effect, or null for today
     */
    public void start(LocalDate startDate) {
        this.start = Objects.requireNonNullElse(startDate, LocalDate.now());
        updateRange();
    }

    @Override
    public String toString() {
        return String.format("%s..%s", start, end);
    }

    private void updateRange() {
        effectiveRange = toEffectiveRange(start, end);
    }

}
