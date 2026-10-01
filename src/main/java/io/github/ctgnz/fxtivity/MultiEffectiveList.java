package io.github.ctgnz.fxtivity;

import java.time.LocalDate;
import java.util.List;

/**
 * Things that may be in effect at the same time as one another - the employments a person holds, which can overlap.
 *
 * @param <E>
 *            the element type
 * @author ctg
 */
public class MultiEffectiveList<E extends IEffectiveEntity> extends EffectiveList<E> {

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
     * The span from the earliest start to the latest end, or the single date {@link Effectivity#FOREVER} when the list is empty.
     */
    @Override
    public DateRange getDateRange() {
        if (sourceList.isEmpty()) {
            return DateRange.singleton(Effectivity.FOREVER);
        }
        LocalDate minDate = sourceList.stream().map(IEffectiveEntity::getStart).min(LocalDate::compareTo).get();
        LocalDate maxDate = sourceList.stream().map(IEffectiveEntity::getEnd).max(LocalDate::compareTo).get();
        return Effectivity.toEffectiveRange(minDate, maxDate);
    }

    @Override
    public boolean isOverlapsAllowed() {
        return true;
    }

}
