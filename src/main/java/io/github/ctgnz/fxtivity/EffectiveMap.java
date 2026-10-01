package io.github.ctgnz.fxtivity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;

/**
 * Successions kept by key: for each key, the things that held it over time.
 * <p>
 * Suits anything with several positions that each change hands - the seats on a board, the regions of a country - where each position has its own history. Keys are held in order,
 * and each key's records in date order.
 * <p>
 * {@link #getRecords(Comparable)} gives one key's history as a {@link SingleEffectiveList}; {@link #getEffectiveRecords()} gives every record across all keys as a
 * {@link MultiEffectiveList}.
 *
 * @param <K>
 *            the key type
 * @param <E>
 *            the record type
 * @author ctg
 */
public class EffectiveMap<K extends Comparable<K>, E extends IEffectiveEntity> {

    private static final Comparator<IEffectiveEntity> RECORD_ORDER = Comparator.comparing(IEffectiveEntity::getStart).thenComparing(IEffectiveEntity::getEnd);

    public final BooleanProperty gapsAllowed = new SimpleBooleanProperty();
    private final NavigableMap<K, NavigableSet<E>> delegate;
    private MultiEffectiveList<E> effectiveRecords;

    /** An empty map, with keys in natural order and no gaps allowed. */
    public EffectiveMap() {
        this(false);
    }

    /**
     * An empty map, with keys in natural order.
     *
     * @param gapsAllowed
     *            whether a key's history may have gaps
     */
    public EffectiveMap(boolean gapsAllowed) {
        this.gapsAllowed.set(gapsAllowed);
        this.delegate = new TreeMap<>();
    }

    /**
     * An empty map.
     *
     * @param gapsAllowed
     *            whether a key's history may have gaps
     * @param keyComparator
     *            the order to hold keys in
     */
    public EffectiveMap(boolean gapsAllowed, Comparator<K> keyComparator) {
        this.gapsAllowed.set(gapsAllowed);
        this.delegate = new TreeMap<>(keyComparator);
    }

    /**
     * An empty map allowing no gaps.
     *
     * @param keyComparator
     *            the order to hold keys in
     */
    public EffectiveMap(Comparator<K> keyComparator) {
        this(false, keyComparator);
    }

    /**
     * Whether {@code key} has any records.
     *
     * @param key
     *            the key
     * @return true if it has
     */
    public boolean containsKey(K key) {
        return delegate.containsKey(key);
    }

    /**
     * Whether a key's history may have gaps.
     *
     * @return the property
     */
    public BooleanProperty gapsAllowedProperty() {
        return gapsAllowed;
    }

    /**
     * {@code key}'s records, in date order, as a read-only view.
     *
     * @param key
     *            the key
     * @return the records, empty if the key has none
     */
    public SortedSet<E> get(K key) {
        NavigableSet<E> records = delegate.get(key);
        return records == null ? Collections.emptySortedSet() : Collections.unmodifiableSortedSet(records);
    }

    /**
     * Every record across all keys, as one list that allows overlaps.
     *
     * @return the records
     */
    public MultiEffectiveList<E> getEffectiveRecords() {
        if (effectiveRecords == null) {
            this.effectiveRecords = new MultiEffectiveList<>(FXCollections.observableArrayList(values()));
        }
        return effectiveRecords;
    }

    /**
     * {@code key}'s history as a succession.
     *
     * @param key
     *            the key
     * @return a new succession holding the key's records
     */
    public SingleEffectiveList<E> getRecords(K key) {
        return new SingleEffectiveList<>(FXCollections.observableArrayList(get(key)), isGapsAllowed());
    }

    /**
     * Whether a key's history may have gaps.
     *
     * @return true if allowed
     */
    public boolean isGapsAllowed() {
        return gapsAllowed.get();
    }

    /**
     * Whether the map has no records.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    /**
     * The keys that have records, in order.
     *
     * @return the keys
     */
    public SortedSet<K> keySet() {
        return Collections.unmodifiableSortedSet(delegate.navigableKeySet());
    }

    /**
     * Records {@code value} against {@code key}.
     *
     * @param key
     *            the key
     * @param value
     *            the record
     * @return true if the map changed
     */
    public boolean put(K key, E value) {
        if (delegate.computeIfAbsent(key, k -> new TreeSet<>(RECORD_ORDER)).add(value)) {
            if (effectiveRecords != null) {
                effectiveRecords.add(value);
            }
            return true;
        }
        return false;
    }

    /**
     * Records each of {@code values} against {@code key}.
     *
     * @param key
     *            the key
     * @param values
     *            the records
     * @return true if the map changed
     */
    public boolean putAll(K key, Iterable<? extends E> values) {
        boolean changed = false;
        for (E value : values) {
            // Created on the first record rather than up front, so an empty iterable leaves no empty key behind.
            changed |= delegate.computeIfAbsent(key, k -> new TreeSet<>(RECORD_ORDER)).add(value);
        }
        if (changed) {
            values.forEach(effectiveRecords::add);
            return true;
        }
        return false;
    }

    /**
     * Sets whether a key's history may have gaps.
     *
     * @param gapsAllowed
     *            true to allow them
     */
    public void setGapsAllowed(boolean gapsAllowed) {
        this.gapsAllowed.set(gapsAllowed);
    }

    /**
     * How many records there are across all keys.
     *
     * @return the count
     */
    public int size() {
        return delegate.values().stream().mapToInt(Collection::size).sum();
    }

    /**
     * Every record across all keys, in key order and then date order.
     *
     * @return the records
     */
    public Collection<E> values() {
        List<E> all = new ArrayList<>();
        delegate.values().forEach(all::addAll);
        return Collections.unmodifiableList(all);
    }

}
