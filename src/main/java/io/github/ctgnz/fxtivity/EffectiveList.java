package io.github.ctgnz.fxtivity;

import static io.github.ctgnz.fxtivity.IEffectiveEntity.DATE_ORDER;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javafx.beans.value.ChangeListener;
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
public abstract class EffectiveList<E extends IEffectiveEntity> extends ModifiableObservableListBase<E> {

    /** Every element, in date order. */
    protected final ObservableList<E> sourceList;
    /** The elements in effect on the application's effective date. */
    protected final FilteredList<E> filtered;
    // The one read-only view of the source list handed out, so every caller sees the same instance and a listener on it stays attached.
    private final ObservableList<E> readOnlySource;
    // Re-filters whenever the effective date moves. Held in a field because it is registered weakly: the list keeps it alive for exactly
    // as long as the list itself lives, so a discarded list stops listening rather than being pinned by the date forever.
    private final ChangeListener<LocalDate> effectiveDateListener = (obs, oldValue, newValue) -> updateFilter();

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
    // The FilteredList evaluates isActive on construction, before a subclass has finished initialising, so an override reading the
    // subclass's own fields would see them unset. Safe while no subclass overrides isActive, which none does.
    @SuppressWarnings("this-escape")
    public EffectiveList(List<E> source) {
        this.sourceList = FXCollections.observableArrayList(source);
        sourceList.sort(DATE_ORDER);
        this.readOnlySource = FXCollections.unmodifiableObservableList(sourceList);
        this.filtered = sourceList.filtered(this::isActive);
        Effectivity.listen(effectiveDateListener);
    }

    @Override
    public boolean add(E element) {
        if (!isOverlapsAllowed() && overlaps(element)) {
            return false;
        }
        if (super.add(element)) {
            sourceList.sort(DATE_ORDER);
            return true;
        }
        return false;
    }

    @Override
    public void add(int index, E element) {
        if (!isOverlapsAllowed() && overlaps(element)) {
            throw new ArrayIndexOutOfBoundsException("Overlapping element is not allowed");
        }
        super.add(index, element);
        sourceList.sort(DATE_ORDER);
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        if (!isOverlapsAllowed() && collection.stream().anyMatch(EffectiveList.this::overlaps)) {
            return false;
        }
        if (super.addAll(collection)) {
            sourceList.sort(DATE_ORDER);
            return true;
        }
        return false;
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
     * The end of the last element.
     *
     * @return the date
     */
    public LocalDate getMaxDate() {
        return sourceList.getLast().getEnd();
    }

    /**
     * The start of the first element.
     *
     * @return the date
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

    @Override
    public E remove(int index) {
        E obj = get(index);
        if (sourceList.remove(obj)) {
            return obj;
        }
        return null;
    }

    @Override
    public E set(int index, E element) {
        throw new UnsupportedOperationException("EffectiveList can only be ordered by date");
    }

    @Override
    public boolean setAll(Collection<? extends E> collection) {
        List<? extends E> list = new ArrayList<>(sourceList);
        sourceList.clear();
        if (super.addAll(collection)) {
            sourceList.sort(DATE_ORDER);
            if (!isOverlapsAllowed() && collection.stream().anyMatch(this::overlaps)) {
                sourceList.clear();
                sourceList.addAll(list);
                return false;
            }
            return true;
        }
        return false;
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

    /** Re-applies the filter after the effective date moves. */
    protected void updateFilter() {
        filtered.setPredicate(null);
        filtered.setPredicate(this::isActive);
    }

}
