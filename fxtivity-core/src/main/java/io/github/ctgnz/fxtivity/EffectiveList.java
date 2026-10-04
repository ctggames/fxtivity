package io.github.ctgnz.fxtivity;

import static io.github.ctgnz.fxtivity.Effective.DATE_ORDER;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ModifiableObservableListBase;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

/**
 * An observable list of things that are each in effect for a period, kept in date order, with a live view of those in effect on the application's {@linkplain Effectivity#when()
 * effective date}.
 * <p>
 * The whole list holds every element regardless of date. {@link #effective()} holds only those in effect on the effective date, and follows it: move the date with
 * {@link Effectivity#forDate(LocalDate)} and that view updates, along with anything bound to it.
 * <p>
 * Whether two elements may overlap in time is decided by the subclass: {@link SingleEffectiveList} holds one thing at a time, {@link MultiEffectiveList} holds any number at once.
 *
 * @param <E>
 *            the element type
 * @author ctg
 */
public abstract class EffectiveList<E extends Effective> extends ModifiableObservableListBase<E> {

    /** Every element, in date order. */
    protected final ObservableList<E> sourceList;
    /** The elements in effect on the application's effective date. */
    protected final FilteredList<E> filtered;
    // The one read-only view of the source list handed out, so every caller sees the same instance and a listener on it stays attached.
    private final ObservableList<E> readOnlySource;
    // Re-filters whenever the effective date moves. Held in a field because it is registered weakly: the list keeps it alive for exactly
    // as long as the list itself lives, so a discarded list stops listening rather than being pinned by the date forever.
    private final ChangeListener<LocalDate> effectiveDateListener = (obs, oldValue, newValue) -> updateFilter();
    // Date order, then the tie-break if there is one.
    private final Comparator<E> order;

    /** An empty list. */
    public EffectiveList() {
        this(new ArrayList<>());
    }

    /**
     * A list holding {@code source}'s elements, in date order.
     *
     * @param source
     *            the initial elements
     */
    public EffectiveList(List<E> source) {
        this(source, null);
    }

    /**
     * A list holding {@code source}'s elements, in date order and then {@code tieBreak}'s.
     *
     * @param source
     *            the initial elements
     * @param tieBreak
     *            the order of elements with the same dates, or null to keep them in the order they were added
     */
    // The FilteredList evaluates isActive on construction, before a subclass has finished initialising, so an override reading the
    // subclass's own fields would see them unset. Safe while no subclass overrides isActive, which none does.
    @SuppressWarnings("this-escape")
    protected EffectiveList(List<E> source, Comparator<? super E> tieBreak) {
        Comparator<E> dates = DATE_ORDER::compare;
        this.order = tieBreak == null ? dates : dates.thenComparing(tieBreak);
        this.sourceList = FXCollections.observableArrayList(source);
        sourceList.sort(order);
        this.readOnlySource = FXCollections.unmodifiableObservableList(sourceList);
        this.filtered = sourceList.filtered(this::isActive);
        Effectivity.effectiveDateProperty().addListener(new WeakChangeListener<>(effectiveDateListener));
    }

    @Override
    public boolean add(E element) {
        if (!isOverlapsAllowed() && overlaps(element)) {
            return false;
        }
        return super.add(element);
    }

    /**
     * Adds {@code element} at its place by date: the index is ignored.
     *
     * @throws IllegalArgumentException
     *             if the element breaks the collection's rules
     */
    @Override
    public void add(int index, E element) {
        if (!isOverlapsAllowed() && overlaps(element)) {
            throw new IllegalArgumentException("Overlapping element is not allowed");
        }
        super.add(indexByDate(element), element);
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        if (!isOverlapsAllowed() && collection.stream().anyMatch(EffectiveList.this::overlaps)) {
            return false;
        }
        return super.addAll(collection);
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        throw new UnsupportedOperationException("EffectiveList can only be ordered by date");
    }

    /**
     * The elements in effect on the application's effective date, as a live view that follows the date.
     *
     * @return the view
     */
    public FilteredList<E> effective() {
        return filtered;
    }

    /**
     * How many elements are in effect on the effective date.
     *
     * @return the count
     */
    public int effectiveSize() {
        return filtered.size();
    }

    @Override
    public E get(int index) {
        return sourceList.get(index);
    }

    /**
     * The span from the earliest start to the latest end, or the active range when the list is empty.
     *
     * @return the span
     */
    public DateRange getDateRange() {
        if (sourceList.isEmpty()) {
            return Effectivity.activeRange();
        }
        return Effectivity.toEffectiveRange(getMinDate(), getMaxDate());
    }

    /**
     * The element in effect on the effective date, when exactly one is.
     *
     * @return the element, or empty if none or several are in effect
     */
    public Optional<E> getEffectiveRecord() {
        return filtered.size() == 1 ? Optional.of(filtered.getFirst()) : Optional.empty();
    }

    @Override
    public E getFirst() {
        return sourceList.isEmpty() ? null : sourceList.getFirst();
    }

    @Override
    public E getLast() {
        return sourceList.isEmpty() ? null : sourceList.getLast();
    }

    /**
     * The latest end of any element.
     *
     * @return the date
     * @throws java.util.NoSuchElementException
     *             if the list is empty
     */
    public LocalDate getMaxDate() {
        return sourceList.stream().map(Effective::getEnd).max(LocalDate::compareTo).orElseThrow();
    }

    /**
     * The start of the first element, which is the earliest start of any.
     *
     * @return the date
     * @throws java.util.NoSuchElementException
     *             if the list is empty
     */
    public LocalDate getMinDate() {
        return sourceList.getFirst().getStart();
    }

    /**
     * Every element, in date order, whatever the effective date - the full contents an editor shows, as opposed to the {@linkplain #effective() effective view}.
     * <p>
     * Read-only, and live: changes made through this collection appear in it and are announced on it, but it cannot itself be changed. Every change goes through this collection's
     * own methods, which are where its rules - on overlaps, gaps and order - are enforced.
     *
     * @return a read-only view of every element
     */
    public ObservableList<E> getSourceList() {
        return readOnlySource;
    }

    /**
     * Whether two elements may be in effect at once.
     *
     * @return true if overlaps are allowed
     */
    public abstract boolean isOverlapsAllowed();

    /**
     * Whether {@code element} would share a day with any element already in the list.
     *
     * @param element
     *            the element to test
     * @return true if it overlaps one
     */
    public boolean overlaps(E element) {
        return sourceList.stream().filter(e -> !e.equals(element)).anyMatch(existing -> existing.overlaps(element));
    }

    /**
     * Changes {@code element}'s dates, if the new ones keep to the collection's rules.
     * <p>
     * This is how an element's dates are changed while it is in a collection. Setting them on the element directly bypasses the collection: nothing checks them, the collection's
     * date order is left wrong, and {@link #effective()} does not follow. Here the new dates are checked first, and if they are refused nothing changes. Otherwise the element
     * moves to its new place by date, announced as one change, and {@link #effective()} follows.
     *
     * @param element
     *            an element of this collection
     * @param start
     *            the new first date in effect
     * @param end
     *            the new first date no longer in effect
     * @return true if the dates were changed; false if they were refused, or {@code element} is not in this collection
     */
    public boolean reschedule(E element, LocalDate start, LocalDate end) {
        int index = sourceList.indexOf(element);
        if (index < 0) {
            return false;
        }
        DateRange dates = Effectivity.create(start, end).getEffectiveDates();
        if (!isOverlapsAllowed() && sourceList.stream().filter(other -> other != element).anyMatch(other -> other.overlaps(dates))) {
            return false;
        }
        if (opensGap(index, start, end)) {
            return false;
        }
        move(index, start, end);
        return true;
    }

    @Override
    public E set(int index, E element) {
        throw new UnsupportedOperationException("EffectiveList can only be ordered by date");
    }

    @Override
    public boolean setAll(Collection<? extends E> collection) {
        List<E> sorted = byDate(collection);
        if (breach(sorted) != null) {
            return false;
        }
        return super.setAll(sorted);
    }

    /**
     * Replaces the contents with {@code collection}, or fails if it breaks the collection's rules.
     * <p>
     * All or nothing, as {@link #setAll(Collection)}, but a refusal throws rather than returning false - which is what loading a saved model needs. A model loads each of its
     * collections by calling this from a setter, so that the collection it declared, configured as declared, is the one that is filled:
     *
     * <pre>
     * private final SingleEffectiveList&lt;Shift&gt; shifts = new SingleEffectiveList&lt;&gt;(true);
     *
     * &#64;JsonManagedReference
     * &#64;JsonGetter("shifts")
     * List&lt;Shift&gt; getShifts() {
     *     return shifts.getSourceList();
     * }
     *
     * &#64;JsonManagedReference
     * &#64;JsonSetter("shifts")
     * void setShifts(List&lt;Shift&gt; loaded) {
     *     shifts.load(loaded);
     * }
     * </pre>
     *
     * A file that breaks the rules then fails to load, with the path to the entry, rather than loading with entries missing.
     *
     * @param collection
     *            the elements, in any order
     * @throws IllegalArgumentException
     *             naming the first breach of the rules, by date
     */
    public void load(Collection<? extends E> collection) {
        List<E> sorted = byDate(collection);
        String breach = breach(sorted);
        if (breach != null) {
            throw new IllegalArgumentException(breach);
        }
        super.setAll(sorted);
    }

    @Override
    public int size() {
        return sourceList.size();
    }

    @Override
    protected void doAdd(int index, E element) {
        sourceList.add(index, element);
    }

    @Override
    protected E doRemove(int index) {
        return sourceList.remove(index);
    }

    @Override
    protected E doSet(int index, E element) {
        return sourceList.set(index, element);
    }

    // Whether moving the element at index to the new dates would leave a period with nothing in effect, where gaps are not allowed.
    boolean opensGap(int index, LocalDate start, LocalDate end) {
        return false;
    }

    // Gives the element at index new dates and moves it to its place by date, as one change. Unchecked: callers have already applied the rules.
    // Calls the base class's add and remove directly, so the move is not refused by the checks it has already passed.
    void move(int index, LocalDate start, LocalDate end) {
        beginChange();
        try {
            E element = super.remove(index);
            element.setStart(start);
            element.setEnd(end);
            super.add(indexByDate(element), element);
        } finally {
            endChange();
        }
    }

    // The first breach of the collection's rules among elements in date order, described, or null if there is none.
    String breach(List<E> sorted) {
        if (!isOverlapsAllowed()) {
            for (int i = 1; i < sorted.size(); i++) {
                if (sorted.get(i - 1).overlaps(sorted.get(i))) {
                    return sorted.get(i - 1) + " overlaps " + sorted.get(i);
                }
            }
        }
        return null;
    }

    // The elements in this list's order: by date, then by the tie-break.
    List<E> byDate(Collection<? extends E> collection) {
        List<E> sorted = new ArrayList<>(collection);
        sorted.sort(order);
        return sorted;
    }

    // Whether any two elements overlap. Compares each only with the one after it, which finds any overlap provided the list is in date order.
    boolean hasOverlaps(List<E> sorted) {
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i - 1).overlaps(sorted.get(i))) {
                return true;
            }
        }
        return false;
    }

    // Where element belongs by date and tie-break: after every element that sorts before or alongside it, so equal elements keep the order they were added in.
    private int indexByDate(E element) {
        int index = sourceList.size();
        while (index > 0 && order.compare(sourceList.get(index - 1), element) > 0) {
            index--;
        }
        return index;
    }

    /**
     * Whether {@code effectiveRecord} is in effect on the application's effective date.
     *
     * @param effectiveRecord
     *            the element
     * @return true if in effect
     */
    protected boolean isActive(E effectiveRecord) {
        return effectiveRecord.getEffectivity().contains(Effectivity.when());
    }

    /** Re-applies the filter after the effective date moves, as one change: from what was in effect to what is now. */
    protected void updateFilter() {
        // In one step. Clearing the predicate first would let every element through, and tell every listener so, before cutting back to what is in effect - a view built on
        // effective() would briefly see the list's whole history. And a new instance each time, which the FilteredList must see as a new predicate to re-filter at all: the
        // language does not promise a new object from a lambda or method reference, but it does from an anonymous class.
        filtered.setPredicate(new Predicate<E>() {
            @Override
            public boolean test(E element) {
                return isActive(element);
            }
        });
    }

}
