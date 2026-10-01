package io.github.ctgnz.fxtivity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import io.github.ctgnz.yamlflock.YamlFlowStyle;

/**
 * A value that changes over time, owned by something that is itself in effect for a period.
 * <p>
 * The history is a list of {@linkplain Entry entries}, each recording that from a given date the value was something new. The value on any date is the one set by the latest entry
 * on or before it. A value can only be set within the owner's period, because outside it the owner did not exist to have one.
 * <p>
 * Serialised as the list of entries alone - the owner is a back reference, restored by Jackson from the owner's side - and each entry is written as one flow-style line when
 * yaml-flock is present.
 *
 * @param <T>
 *            the type of the value
 * @param <E>
 *            the type of the owner
 * @author ctg
 */
public class EffectiveProperty<T, E extends IEffectiveEntity> {

    /**
     * One change in the history: from {@link #getDate()} onwards, the value is {@link #getValue()}.
     *
     * @param <T>
     *            the type of the value
     */
    @YamlFlowStyle
    public static class Entry<T> {
        private LocalDate date;
        private T value;

        /**
         * A change to {@code value} on {@code date}.
         *
         * @param date
         *            the date the value takes effect
         * @param value
         *            the value
         */
        @JsonCreator
        public Entry(@JsonProperty("date") LocalDate date, @JsonProperty("value") T value) {
            this.date = date;
            this.value = value;
        }

        /**
         * The date the value takes effect.
         *
         * @return the date
         */
        public LocalDate getDate() {
            return date;
        }

        /**
         * The value from that date onwards.
         *
         * @return the value
         */
        public T getValue() {
            return value;
        }

        /**
         * Moves the date the value takes effect.
         *
         * @param date
         *            the new date
         */
        public void setDate(LocalDate date) {
            this.date = date;
        }

        /**
         * Replaces the value.
         *
         * @param value
         *            the new value
         */
        public void setValue(T value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "EffectiveProperty.Entry[date=" + date + ",value=" + value + "]";
        }

    }

    private @JsonBackReference E owner;
    private @JsonValue List<Entry<T>> entries;
    private @JsonIgnore final Map<LocalDate, T> index = new TreeMap<>();

    /**
     * An empty history belonging to {@code owner}.
     *
     * @param owner
     *            the owner, whose period bounds the history
     */
    public EffectiveProperty(E owner) {
        this.owner = owner;
        this.entries = new ArrayList<>();
    }

    /**
     * A history read back from its entries. The owner is set separately, by Jackson's back reference or by {@link #setOwner(IEffectiveEntity)}.
     *
     * @param entries
     *            the entries
     */
    @JsonCreator
    public EffectiveProperty(List<Entry<T>> entries) {
        this.entries = entries;
        entries.forEach(entry -> index.put(entry.getDate(), entry.getValue()));
    }

    /** Removes every entry. */
    public void clear() {
        entries.clear();
        index.clear();
    }

    /**
     * Replaces this history with another's.
     *
     * @param other
     *            the history to copy
     */
    public void copy(EffectiveProperty<T, E> other) {
        clear();
        this.entries.addAll(other.entries);
        this.index.putAll(other.index);
    }

    /**
     * The value on the application's {@linkplain Effectivity#when() effective date}.
     *
     * @return the value, or null if none was in effect
     */
    @JsonIgnore
    public T getEffectiveValue() {
        return getEffectiveValue(Effectivity.when());
    }

    /**
     * The value on {@code atDate}: the one set by the latest entry on or before it.
     *
     * @param atDate
     *            the date
     * @return the value, or null if none was in effect
     */
    public T getEffectiveValue(LocalDate atDate) {
        if (isInRange(atDate)) {
            Optional<LocalDate> first = index.keySet().stream().sorted(Comparator.reverseOrder()).filter(val -> val.isBefore(atDate) || val.equals(atDate)).findFirst();
            return first.map(index::get).orElse(null);
        }
        return null;
    }

    /**
     * The history, as serialised.
     *
     * @return the entries, in date order
     */
    @JsonValue
    public List<Entry<T>> getEntries() {
        return entries;
    }

    /**
     * The entry recorded for exactly {@code onDate}.
     *
     * @param onDate
     *            the date
     * @return the entry, if there is one for that date
     */
    public Optional<Entry<T>> getEntry(LocalDate onDate) {
        return entries.stream().filter(entry -> Objects.equals(entry.getDate(), onDate)).findFirst();
    }

    /**
     * The value at the end of the owner's period.
     *
     * @return the value
     */
    public T getFinalValue() {
        return getEffectiveValue(owner.getEnd());
    }

    /**
     * The value at the start of the owner's period.
     *
     * @return the value
     */
    public T getInitialValue() {
        return getEffectiveValue(owner.getStart());
    }

    /**
     * The owner, whose period bounds the history.
     *
     * @return the owner
     */
    public E getOwner() {
        return owner;
    }

    /**
     * The number of entries.
     *
     * @return the count
     */
    public int getSize() {
        return entries.size();
    }

    /**
     * Every value the history has held, in date order.
     *
     * @return the values
     */
    public List<T> values() {
        return entries.stream().map(Entry::getValue).toList();
    }

    /**
     * Whether an entry is recorded for exactly {@code date}.
     *
     * @param date
     *            the date
     * @return true if there is one
     */
    public boolean hasEntry(LocalDate date) {
        return index.containsKey(date);
    }

    /**
     * Whether the history has no entries.
     *
     * @return true if empty
     */
    @JsonIgnore
    public boolean isEmpty() {
        return index.isEmpty();
    }

    /**
     * Whether {@code atDate} falls within the owner's period, including its end date.
     *
     * @param atDate
     *            the date
     * @return true if in range
     */
    @JsonIgnore
    public boolean isInRange(LocalDate atDate) {
        return owner.getEffectiveDates().contains(atDate) || Objects.equals(owner.getEnd(), atDate);
    }

    /** Removes each entry that sets the same value as the one before it, since it changes nothing. */
    public void prune() {
        Entry<T> prev = null;
        for (Iterator<Entry<T>> it = entries.iterator(); it.hasNext();) {
            Entry<T> next = it.next();
            if (prev != null && Objects.equals(next.getValue(), prev.getValue())) {
                it.remove();
                index.remove(next.getDate());
            } else {
                prev = next;
            }
        }
    }

    /**
     * Removes the entry recorded for exactly {@code date}.
     *
     * @param date
     *            the date
     * @return the value it set, or null if there was none
     */
    public T remove(LocalDate date) {
        if (index.containsKey(date)) {
            entries.removeIf(val -> Objects.equals(val.getDate(), date));
            return index.remove(date);
        }
        return null;
    }

    /**
     * Sets the owner, whose period bounds the history.
     *
     * @param owner
     *            the owner
     */
    public void setOwner(E owner) {
        this.owner = owner;
    }

    /**
     * Records that from {@code onDate} the value is {@code value}, replacing any entry already on that date.
     *
     * @param onDate
     *            the date the value takes effect
     * @param value
     *            the value
     * @return false, changing nothing, if the date is outside the owner's period
     */
    public boolean setValue(LocalDate onDate, T value) {
        if (isInRange(onDate)) {
            if (index.containsKey(onDate)) {
                index.remove(onDate);
                entries.removeIf(val -> val.date.equals(onDate));
            }
            this.index.put(onDate, value);
            entries.add(new Entry<>(onDate, value));
            entries.sort(Comparator.comparing(Entry::getDate));
            return true;
        }
        return false;
    }

    /**
     * Moves the history from {@code endDate} onwards into {@code other}, which starts with a copy of the value in effect on that date.
     *
     * @param endDate
     *            the date to split at
     * @param other
     *            the history to receive the later entries
     * @param copyFunction
     *            how to copy the value carried across the split
     */
    public void split(LocalDate endDate, EffectiveProperty<T, E> other, Function<T, T> copyFunction) {
        other.setValue(endDate, copyFunction.apply(getEffectiveValue(endDate)));
        List<Entry<T>> postDated = entries.stream().filter(entry -> entry.getDate().isAfter(endDate) || entry.getDate().equals(endDate)).toList();
        postDated.forEach(entry -> {
            other.setValue(entry.getDate(), entry.getValue());
            remove(entry.getDate());
        });
    }

    /**
     * The history as one element per span of time, for values that are already {@link IEffectiveEntity effective entities}.
     * <p>
     * {@code valueType} is not used at run time; it is there to tell the compiler what {@code W} is.
     *
     * @param <W>
     *            the element type, which the values already are
     * @param valueType
     *            the element type
     * @return one element per span, each carrying its span's dates
     */
    @SuppressWarnings("unchecked")
    public <W extends IEffectiveEntity> SingleEffectiveList<W> toList(Class<W> valueType) {
        // Through Function<?, ?> because Function.identity() infers Function<Object, Object> here, and a cast between two differently
        // parameterised instances of the same generic type is a compile error rather than a warning.
        return toList((Function<T, W>) (Function<?, ?>) Function.identity());
    }

    /**
     * The history as one element per span of time, each built from the value by {@code wrapperFunction}.
     *
     * @param <W>
     *            the element type
     * @param wrapperFunction
     *            how to build an element from a value
     * @return one element per span, each carrying its span's dates
     */
    public <W extends IEffectiveEntity> SingleEffectiveList<W> toList(Function<T, W> wrapperFunction) {
        SingleEffectiveList<W> list = new SingleEffectiveList<>(false);
        Iterator<Entry<T>> it = entries.iterator();
        Entry<T> from = it.next();
        while (it.hasNext()) {
            Entry<T> to = it.next();
            W wrapper = wrapperFunction.apply(from.value);
            wrapper.dates(from.date, to.date);
            list.add(wrapper);
            from = to;
        }
        W wrapper = wrapperFunction.apply(from.value);
        wrapper.dates(from.date, owner.getEnd());
        list.add(wrapper);
        return list;
    }

    @Override
    public String toString() {
        return "EffectiveProperty[values=" + entries + "]";
    }

    /**
     * The history as one {@link EffectiveWrapper} per span of time.
     *
     * @return one wrapper per span
     */
    public SingleEffectiveList<EffectiveWrapper<T>> toWrappedList() {
        SingleEffectiveList<EffectiveWrapper<T>> list = new SingleEffectiveList<>(false);
        Iterator<Entry<T>> it = entries.iterator();
        Entry<T> from = it.next();
        while (it.hasNext()) {
            Entry<T> to = it.next();
            list.add(new EffectiveWrapper<>(from.date, to.date, from.value));
            from = to;
        }
        list.add(new EffectiveWrapper<>(from.date, owner.getEnd(), from.value));
        return list;
    }

}
