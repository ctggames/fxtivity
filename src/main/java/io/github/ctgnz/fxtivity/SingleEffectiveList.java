package io.github.ctgnz.fxtivity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.transformation.FilteredList;

/**
 * A succession: things that are in effect one at a time, never two at once, optionally with no gaps between them.
 * <p>
 * Suits anything that has exactly one holder at a time - the chief executive of a company, the name a person goes by. An element that would overlap another is refused, and unless
 * gaps are allowed, so is one that would leave a period with nothing in effect.
 * <p>
 * {@link #insertForwards(IEffectiveEntity)} and {@link #insertBackwards(IEffectiveEntity)} make room for a new element by adjusting its neighbours rather than refusing it.
 *
 * @param <E>
 *            the element type
 * @author ctg
 */
public class SingleEffectiveList<E extends IEffectiveEntity> extends EffectiveList<E> {
    public final BooleanProperty gapsAllowed = new SimpleBooleanProperty();

    /** An empty succession that allows no gaps. */
    public SingleEffectiveList() {
        this(false);
    }

    /**
     * An empty succession.
     *
     * @param gapsAllowed
     *            whether gaps between elements are allowed
     */
    public SingleEffectiveList(boolean gapsAllowed) {
        this(new ArrayList<>(), gapsAllowed);
    }

    /**
     * A succession holding {@code source}'s elements, allowing no gaps.
     *
     * @param source
     *            the initial elements
     */
    public SingleEffectiveList(List<E> source) {
        this(source, false);
    }

    /**
     * A succession holding {@code source}'s elements.
     *
     * @param source
     *            the initial elements
     * @param gapsAllowed
     *            whether gaps between elements are allowed
     * @throws IllegalArgumentException
     *             if any of the elements overlap, or gaps are not allowed and the elements have one
     */
    // Calls the overridable hasGaps() before a subclass has finished initialising. Safe while no subclass overrides it, which none does.
    @SuppressWarnings("this-escape")
    public SingleEffectiveList(List<E> source, boolean gapsAllowed) {
        super(source);
        if (hasOverlaps(sourceList)) {
            throw new IllegalArgumentException("Overlaps are not allowed");
        }
        if (!gapsAllowed && hasGaps()) {
            throw new IllegalArgumentException("Gaps are not allowed");
        }
        this.gapsAllowed.set(gapsAllowed);
    }

    @Override
    public boolean add(E element) {
        if (!isGapsAllowed() && leavesGap(element)) {
            return false;
        }
        return super.add(element);
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        for (E element : collection) {
            if (!add(element)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether gaps between elements are allowed.
     *
     * @return the property
     */
    public BooleanProperty gapsAllowedProperty() {
        return gapsAllowed;
    }

    /**
     * The unbroken stretches of time the succession covers, one per run of elements with no gap between them.
     *
     * @return the stretches, in date order
     */
    public List<DateRange> getContinuousSpans() {
        if (sourceList.isEmpty()) {
            return Collections.emptyList();
        }
        if (hasGaps()) {
            List<DateRange> span = new ArrayList<>();
            E prev = getFirst();
            DateRange effectiveDates = prev.getEffectiveDates();
            for (E next : sourceList) {
                if (next != prev && !hasGap(prev, next)) {
                    effectiveDates = effectiveDates.span(next.getEffectiveDates());
                } else {
                    span.add(effectiveDates);
                    effectiveDates = next.getEffectiveDates();
                }
            }
            span.add(effectiveDates);
            return Collections.unmodifiableList(span);
        }
        return Collections.singletonList(getSpan());
    }

    /**
     * The element in effect on {@code atDate}.
     *
     * @param atDate
     *            the date
     * @return the element, if one was in effect
     */
    public Optional<E> getEffective(LocalDate atDate) {
        return sourceList.stream().filter(element -> element.containsDate(atDate)).findFirst();
    }

    /**
     * The elements in effect at any point within {@code effectiveDates}.
     *
     * @param effectiveDates
     *            the range
     * @return the elements
     */
    public FilteredList<E> getEffective(DateRange effectiveDates) {
        return filtered(element -> element.overlaps(effectiveDates));
    }

    /**
     * The earliest element in effect at any point within {@code atDate}.
     *
     * @param atDate
     *            the range
     * @return the element, if any
     */
    public Optional<E> getFirstEffective(DateRange atDate) {
        return sourceList.stream().filter(element -> element.overlaps(atDate)).findFirst();
    }

    /**
     * The latest element in effect at any point within {@code dateRange}.
     *
     * @param dateRange
     *            the range
     * @return the element, if any
     */
    public Optional<E> getLastEffective(DateRange dateRange) {
        return sourceList.stream().sorted(IEffectiveEntity.REVERSE_DATE_ORDER).filter(element -> element.overlaps(dateRange)).findFirst();
    }

    /**
     * The element after {@code previous}.
     *
     * @param previous
     *            an element
     * @return the next element, if any
     */
    public Optional<E> getNext(E previous) {
        if (previous == null) {
            return Optional.empty();
        }
        return sourceList.stream().filter(element -> element.isAfter(previous)).findFirst();
    }

    /**
     * The element before {@code element}.
     *
     * @param element
     *            an element
     * @return the previous element, if any
     */
    public Optional<E> getPrevious(E element) {
        E previous = getFirst();
        if (element == null || previous == null || Objects.equals(element, previous) || previous.isAfter(element)) {
            return Optional.empty();
        }
        E next = getNext(previous).orElse(null);
        while (next != null && next.isBefore(element)) {
            previous = next;
            next = getNext(next).orElse(null);
        }
        return Optional.ofNullable(previous);
    }

    /**
     * The span from the first element's start to the last element's end, including any gaps.
     *
     * @return the span, or null if the succession is empty
     */
    public DateRange getSpan() {
        if (sourceList.isEmpty()) {
            return null;
        }
        DateRange effectiveDates = getFirst().getEffectiveDates();
        for (E e : sourceList) {
            effectiveDates = effectiveDates.span(e.getEffectiveDates());
        }
        return effectiveDates;
    }

    /**
     * Whether there is a period with nothing in effect between {@code previous} ending and {@code next} starting.
     *
     * @param previous
     *            the earlier element
     * @param next
     *            the later element
     * @return true if there is a gap
     */
    public boolean hasGap(E previous, E next) {
        if (previous == null || next == null) {
            return false;
        }
        return next.getStart().isAfter(previous.getEnd());
    }

    /**
     * Whether there is a gap anywhere in the succession.
     *
     * @return true if there is a gap
     */
    public boolean hasGaps() {
        return hasGaps(sourceList);
    }

    /**
     * Adds {@code element}, making room by ending whatever was in effect when it starts, and ending {@code element} itself where the next one begins.
     *
     * @param element
     *            the element to insert
     * @return true if it was added
     */
    public boolean insertBackwards(E element) {
        E next = getFirst();
        E previous = null;
        while (next != null && next.getStart().isBefore(element.getStart())) {
            previous = next;
            next = getNext(next).orElse(null);
        }
        if (previous != null && previous.overlaps(element)) {
            move(indexOf(previous), previous.getStart(), element.getStart());
        }
        if (next == null || hasGap(element, next)) {
            return add(element);
        }
        if (next.overlaps(element)) {
            element.end(next.getStart());
        }
        return add(element);
    }

    /**
     * Adds {@code element}, making room by starting whatever follows it at its end, and starting {@code element} itself where the previous one ends.
     *
     * @param element
     *            the element to insert
     * @return true if it was added
     */
    public boolean insertForwards(E element) {
        E previous = getLast();
        E next = null;
        while (previous != null && previous.getEnd().isAfter(element.getEnd())) {
            next = previous;
            previous = getPrevious(previous).orElse(null);
        }
        if (next != null && next.overlaps(element)) {
            move(indexOf(next), element.getEnd(), next.getEnd());
        }
        if (previous == null || hasGap(previous, element)) {
            return add(element);
        }
        if (previous.overlaps(element)) {
            element.start(previous.getEnd());
        }
        return add(element);
    }

    /**
     * Whether gaps between elements are allowed.
     *
     * @return true if allowed
     */
    public boolean isGapsAllowed() {
        return gapsAllowed.get();
    }

    @Override
    public boolean isOverlapsAllowed() {
        return false;
    }

    @Override
    public boolean setAll(Collection<? extends E> collection) {
        if (!isGapsAllowed() && hasGaps(collection)) {
            return false;
        }
        return super.setAll(collection);
    }

    /**
     * Sets whether gaps between elements are allowed.
     *
     * @param gapsAllowed
     *            true to allow them
     */
    public void setGapsAllowed(boolean gapsAllowed) {
        this.gapsAllowed.set(gapsAllowed);
    }

    @Override
    boolean opensGap(int index, LocalDate start, LocalDate end) {
        if (isGapsAllowed()) {
            return false;
        }
        E oldPrevious = index > 0 ? sourceList.get(index - 1) : null;
        E oldNext = index < sourceList.size() - 1 ? sourceList.get(index + 1) : null;
        E previous = null;
        E next = null;
        for (int i = 0; i < sourceList.size(); i++) {
            if (i == index) {
                continue;
            }
            E other = sourceList.get(i);
            if (other.getStart().isBefore(start)) {
                previous = other;
            } else {
                next = other;
                break;
            }
        }
        boolean gapAtNewPlace = previous != null && start.isAfter(previous.getEnd()) || next != null && next.getStart().isAfter(end);
        // Unless the element stays between its old neighbours, they become neighbours of each other.
        boolean stays = previous == oldPrevious && next == oldNext;
        return gapAtNewPlace || !stays && hasGap(oldPrevious, oldNext);
    }

    // Whether element, in its place by date, would start after the element before it ends, or end before the element after it starts.
    private boolean leavesGap(E element) {
        E previous = null;
        E next = null;
        for (E existing : sourceList) {
            if (existing.getStart().isBefore(element.getStart())) {
                previous = existing;
            } else {
                next = existing;
                break;
            }
        }
        return hasGap(previous, element) || hasGap(element, next);
    }

    private boolean hasGaps(Collection<? extends E> source) {
        if (source.size() > 1) {
            List<E> sorted = new ArrayList<>(source);
            sorted.sort(IEffectiveEntity.DATE_ORDER);
            Iterator<E> itr = sorted.iterator();
            E previous = itr.next();
            while (itr.hasNext()) {
                E next = itr.next();
                if (hasGap(previous, next)) {
                    return true;
                }
                previous = next;
            }
        }
        return false;
    }

}
