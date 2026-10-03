package io.github.ctgnz.fxtivity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Predicate;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.transformation.FilteredList;

/**
 * A succession: things that are in effect one at a time, never two at once, optionally with no gaps between them.
 * <p>
 * Suits anything that has exactly one holder at a time - the chief executive of a company, the name a person goes by. An element that would overlap another is refused, and unless
 * gaps are allowed, so is one that would leave a period with nothing in effect.
 * <p>
 * {@link #insertForwards(Effective)} and {@link #insertBackwards(Effective)} make room for a new element by adjusting its neighbours rather than refusing it.
 *
 * @param <E>
 *            the element type
 * @author ctg
 */
public class SingleEffectiveList<E extends Effective> extends EffectiveList<E> {
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
    public void add(int index, E element) {
        if (!isGapsAllowed() && leavesGap(element)) {
            throw new IllegalArgumentException("Gaps are not allowed");
        }
        super.add(index, element);
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
        return sourceList.stream().sorted(Effective.REVERSE_DATE_ORDER).filter(element -> element.overlaps(dateRange)).findFirst();
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

    /**
     * Removes the element at {@code index}, refusing to leave a gap while gaps are not allowed.
     *
     * @throws IllegalArgumentException
     *             if removing the element would leave a gap
     * @see #remove(int, Removal)
     */
    @Override
    public E remove(int index) {
        return remove(index, Removal.Refused);
    }

    /**
     * Removes the element at {@code index}, doing as {@code removal} says about any gap that leaves.
     * <p>
     * A gap is left only by removing an element from between two others while gaps are not allowed. Removing the first or last element, or any element while gaps are allowed,
     * leaves none, and {@code removal} is not consulted.
     *
     * @param index
     *            the element's position
     * @param removal
     *            what to do about the gap
     * @return the element removed
     * @throws IllegalArgumentException
     *             if the removal would leave a gap and is {@linkplain Removal#Refused refused}
     */
    public E remove(int index, Removal removal) {
        E element = get(index);
        removeIndices(new TreeSet<>(Set.of(index)), removal);
        return element;
    }

    /**
     * Removes {@code element}, refusing to leave a gap while gaps are not allowed.
     *
     * @throws IllegalArgumentException
     *             if removing the element would leave a gap
     * @see #remove(Effective, Removal)
     */
    @Override
    public boolean remove(Object element) {
        int index = indexOf(element);
        return index >= 0 && removeIndices(new TreeSet<>(Set.of(index)), Removal.Refused);
    }

    /**
     * Removes {@code element}, doing as {@code removal} says about any gap that leaves.
     *
     * @param element
     *            the element
     * @param removal
     *            what to do about the gap
     * @return true if the element was in the list
     * @throws IllegalArgumentException
     *             if the removal would leave a gap and is {@linkplain Removal#Refused refused}
     * @see #remove(int, Removal)
     */
    public boolean remove(E element, Removal removal) {
        int index = indexOf(element);
        return index >= 0 && removeIndices(new TreeSet<>(Set.of(index)), removal);
    }

    /**
     * Removes every element in {@code collection} at once, refusing to leave a gap while gaps are not allowed.
     *
     * @throws IllegalArgumentException
     *             if removing the elements would leave a gap
     * @see #removeAll(Collection, Removal)
     */
    @Override
    public boolean removeAll(Collection<?> collection) {
        return removeAll(collection, Removal.Refused);
    }

    /**
     * Removes every element in {@code collection} at once, doing as {@code removal} says about any gap that leaves.
     * <p>
     * The removal is judged as a whole: removing a run of elements from either end leaves no gap, and a run from the middle leaves one.
     *
     * @param collection
     *            the elements to remove
     * @param removal
     *            what to do about each gap
     * @return true if the list changed
     * @throws IllegalArgumentException
     *             if the removal would leave a gap and is {@linkplain Removal#Refused refused}
     * @see #remove(int, Removal)
     */
    public boolean removeAll(Collection<?> collection, Removal removal) {
        return removeIndices(indicesWhere(collection::contains), removal);
    }

    /**
     * Keeps only the elements in {@code collection}, refusing to leave a gap while gaps are not allowed.
     *
     * @throws IllegalArgumentException
     *             if removing the others would leave a gap
     * @see #retainAll(Collection, Removal)
     */
    @Override
    public boolean retainAll(Collection<?> collection) {
        return retainAll(collection, Removal.Refused);
    }

    /**
     * Keeps only the elements in {@code collection}, removing the others at once and doing as {@code removal} says about any gap that leaves.
     *
     * @param collection
     *            the elements to keep
     * @param removal
     *            what to do about each gap
     * @return true if the list changed
     * @throws IllegalArgumentException
     *             if the removal would leave a gap and is {@linkplain Removal#Refused refused}
     * @see #removeAll(Collection, Removal)
     */
    public boolean retainAll(Collection<?> collection, Removal removal) {
        return removeIndices(indicesWhere(element -> !collection.contains(element)), removal);
    }

    /**
     * Removes every element {@code filter} accepts at once, refusing to leave a gap while gaps are not allowed.
     *
     * @throws IllegalArgumentException
     *             if removing the elements would leave a gap
     * @see #removeIf(Predicate, Removal)
     */
    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        return removeIf(filter, Removal.Refused);
    }

    /**
     * Removes every element {@code filter} accepts at once, doing as {@code removal} says about any gap that leaves.
     *
     * @param filter
     *            which elements to remove
     * @param removal
     *            what to do about each gap
     * @return true if the list changed
     * @throws IllegalArgumentException
     *             if the removal would leave a gap and is {@linkplain Removal#Refused refused}
     * @see #removeAll(Collection, Removal)
     */
    public boolean removeIf(Predicate<? super E> filter, Removal removal) {
        return removeIndices(indicesWhere(filter), removal);
    }

    // Behind clear() and a sub-list's clear(), neither of which can be told what to do about a gap: they refuse to leave one.
    @Override
    protected void removeRange(int fromIndex, int toIndex) {
        SortedSet<Integer> indices = new TreeSet<>();
        for (int i = fromIndex; i < toIndex; i++) {
            indices.add(i);
        }
        removeIndices(indices, Removal.Refused);
    }

    /** A gap a removal would leave, between two elements that remain. */
    record Gap<E>(E previous, E next) {
    }

    // Removes the elements at indices as one change, unchecked. The base class removes one at a time, so this is the only place that does.
    void removeAt(SortedSet<Integer> indices) {
        beginChange();
        try {
            for (int index : indices.reversed()) {
                super.remove(index);
            }
        } finally {
            endChange();
        }
    }

    private SortedSet<Integer> indicesWhere(Predicate<? super E> filter) {
        SortedSet<Integer> indices = new TreeSet<>();
        for (int i = 0; i < size(); i++) {
            if (filter.test(get(i))) {
                indices.add(i);
            }
        }
        return indices;
    }

    // Judges the whole removal before anything changes: removed one at a time, the end of a run of elements would look like the middle of the list.
    private boolean removeIndices(SortedSet<Integer> indices, Removal removal) {
        if (indices.isEmpty()) {
            return false;
        }
        List<Gap<E>> gaps = new ArrayList<>();
        if (!isGapsAllowed()) {
            E previous = null;
            boolean removedSince = false;
            for (int i = 0; i < size(); i++) {
                if (indices.contains(i)) {
                    removedSince = true;
                    continue;
                }
                E next = get(i);
                if (removedSince && previous != null && hasGap(previous, next)) {
                    gaps.add(new Gap<>(previous, next));
                }
                previous = next;
                removedSince = false;
            }
        }
        // One change for the removal and any neighbour moved to close a gap.
        beginChange();
        try {
            removal.remove(this, indices, gaps);
        } finally {
            endChange();
        }
        return true;
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
    String breach(List<E> sorted) {
        String overlap = super.breach(sorted);
        if (overlap != null || isGapsAllowed()) {
            return overlap;
        }
        for (int i = 1; i < sorted.size(); i++) {
            if (hasGap(sorted.get(i - 1), sorted.get(i))) {
                return "Nothing is in effect between " + sorted.get(i - 1) + " and " + sorted.get(i);
            }
        }
        return null;
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
            sorted.sort(Effective.DATE_ORDER);
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
