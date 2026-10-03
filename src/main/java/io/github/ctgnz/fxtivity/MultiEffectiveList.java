package io.github.ctgnz.fxtivity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Things that may be in effect at the same time as one another - the employments a person holds, which can overlap.
 *
 * @param <E>
 *            the element type
 * @author ctg
 */
public class MultiEffectiveList<E extends Effective> extends EffectiveList<E> {

    /** An empty list. */
    public MultiEffectiveList() {
    }

    /**
     * A list holding {@code source}'s elements.
     *
     * @param source
     *            the initial elements
     */
    public MultiEffectiveList(List<E> source) {
        super(source);
    }

    /**
     * An empty list, holding elements with the same dates in {@code tieBreak}'s order.
     *
     * @param tieBreak
     *            the order of elements with the same dates
     */
    public MultiEffectiveList(Comparator<? super E> tieBreak) {
        this(new ArrayList<>(), tieBreak);
    }

    /**
     * A list holding {@code source}'s elements, in date order and then {@code tieBreak}'s.
     * <p>
     * Without a tie-break, elements with the same dates are held in the order they were added, which is not a property of the data: the same contents can be held - and saved - in
     * a different order after a different series of edits. A tie-break makes the order depend on the elements alone.
     *
     * @param source
     *            the initial elements
     * @param tieBreak
     *            the order of elements with the same dates
     */
    public MultiEffectiveList(List<E> source, Comparator<? super E> tieBreak) {
        super(source, Objects.requireNonNull(tieBreak));
    }

    /**
     * The span from the earliest start to the latest end, or the single date {@link Effectivity#FOREVER} when the list is empty.
     */
    @Override
    public DateRange getDateRange() {
        if (sourceList.isEmpty()) {
            return DateRange.singleton(Effectivity.FOREVER);
        }
        LocalDate minDate = sourceList.stream().map(Effective::getStart).min(LocalDate::compareTo).get();
        LocalDate maxDate = sourceList.stream().map(Effective::getEnd).max(LocalDate::compareTo).get();
        return Effectivity.toEffectiveRange(minDate, maxDate);
    }

    @Override
    public boolean isOverlapsAllowed() {
        return true;
    }

}
