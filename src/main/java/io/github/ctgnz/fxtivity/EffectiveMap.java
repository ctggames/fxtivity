package io.github.ctgnz.fxtivity;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

/**
 * Successions kept by key: for each key, the things that held it over time.
 * <p>
 * Suits anything with several positions that each change hands - the seats on a board, the regions of a country - where each position has its own history. Keys are held in order,
 * and each key's records in date order.
 * <p>
 * Each key's history is a live {@link SingleEffectiveList}, given by {@link #getRecords(Comparable)}: the map's own, so its rules apply to every record put under the key, and an
 * editor changes the map by changing it. {@link #getEffectiveRecords()} gives every record across all keys as one {@link MultiEffectiveList}, kept in step with the keys'
 * histories.
 * <p>
 * A key is in the map while its history has at least one record.
 *
 * @param <K>
 *            the key type
 * @param <E>
 *            the record type
 * @author ctg
 */
public class EffectiveMap<K extends Comparable<K>, E extends IEffectiveEntity> {

    public final BooleanProperty gapsAllowed = new SimpleBooleanProperty();
    // Every history handed out, including any that are empty - so an editor holding one keeps writing to the map after removing its last record.
    private final NavigableMap<K, SingleEffectiveList<E>> histories;
    private final AllRecords<E> effectiveRecords = new AllRecords<>();
    private final ListChangeListener<E> relay = effectiveRecords::relay;

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
        this.histories = new TreeMap<>();
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
        this.histories = new TreeMap<>(keyComparator);
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
        SingleEffectiveList<E> history = histories.get(key);
        return history != null && !history.isEmpty();
    }

    /**
     * Whether a key's history may have gaps. Every key's history follows it.
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
    public List<E> get(K key) {
        SingleEffectiveList<E> history = histories.get(key);
        return history == null ? Collections.emptyList() : history.getSourceList();
    }

    /**
     * Every record across all keys, as one list that allows overlaps.
     * <p>
     * Read-only, and live: it follows every change to the keys' histories. Changes are made through {@link #put(Comparable, IEffectiveEntity)},
     * {@link #remove(Comparable, IEffectiveEntity)}, or a key's history.
     *
     * @return the records
     */
    public MultiEffectiveList<E> getEffectiveRecords() {
        return effectiveRecords;
    }

    /**
     * {@code key}'s history as a succession.
     * <p>
     * The map's own, live: changes made through it are changes to the map, with the succession's rules enforced. The same instance on every call, including for a key with no
     * records yet - adding the first one puts the key in the map.
     *
     * @param key
     *            the key
     * @return the key's history
     */
    public SingleEffectiveList<E> getRecords(K key) {
        return histories.computeIfAbsent(key, k -> {
            SingleEffectiveList<E> history = new SingleEffectiveList<>(isGapsAllowed());
            history.gapsAllowed.bind(gapsAllowed);
            history.addListener(relay);
            return history;
        });
    }

    /**
     * The whole map, read-only: every key that has records, in key order, with its history in date order.
     * <p>
     * The histories are live; the set of keys is as it was when this was called.
     *
     * @return the map
     */
    public SortedMap<K, ObservableList<E>> getSourceMap() {
        SortedMap<K, ObservableList<E>> source = new TreeMap<>(histories.comparator());
        histories.forEach((key, history) -> {
            if (!history.isEmpty()) {
                source.put(key, history.getSourceList());
            }
        });
        return Collections.unmodifiableSortedMap(source);
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
        return effectiveRecords.isEmpty();
    }

    /**
     * The keys that have records, in order, as they are when this is called.
     *
     * @return the keys
     */
    public SortedSet<K> keySet() {
        SortedSet<K> keys = new TreeSet<>(histories.comparator());
        histories.forEach((key, history) -> {
            if (!history.isEmpty()) {
                keys.add(key);
            }
        });
        return Collections.unmodifiableSortedSet(keys);
    }

    /**
     * Replaces the whole map with {@code loaded}, or fails if any key's history breaks the rules.
     * <p>
     * All or nothing: every key is checked before any is changed. A key not in {@code loaded} is left with no records. As {@link EffectiveList#load(Collection)}, this is what a
     * model calls from a setter to load the map it declared, with {@link #getSourceMap()} as the getter.
     *
     * @param loaded
     *            each key's records, in any order
     * @throws IllegalArgumentException
     *             naming the key and the first breach of the rules in its history
     */
    public void load(Map<K, ? extends Collection<? extends E>> loaded) {
        loaded.forEach((key, records) -> {
            String breach = getRecords(key).breach(EffectiveList.byDate(records));
            if (breach != null) {
                throw new IllegalArgumentException(key + ": " + breach);
            }
        });
        histories.forEach((key, history) -> {
            if (!loaded.containsKey(key)) {
                history.clear();
            }
        });
        loaded.forEach((key, records) -> getRecords(key).load(records));
    }

    /**
     * Records {@code value} against {@code key}, if it keeps to the rules of the key's history.
     *
     * @param key
     *            the key
     * @param value
     *            the record
     * @return true if the map changed
     */
    public boolean put(K key, E value) {
        return getRecords(key).add(value);
    }

    /**
     * Records each of {@code values} against {@code key}, one at a time: each that keeps to the rules of the key's history is added, whether or not others are refused.
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
            changed |= put(key, value);
        }
        return changed;
    }

    /**
     * Removes {@code value} from {@code key}'s history.
     *
     * @param key
     *            the key
     * @param value
     *            the record
     * @return true if the map changed
     */
    public boolean remove(K key, E value) {
        SingleEffectiveList<E> history = histories.get(key);
        return history != null && history.remove(value);
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
        return effectiveRecords.size();
    }

    /**
     * Every record across all keys, in key order and then date order.
     *
     * @return the records
     */
    public Collection<E> values() {
        return histories.values().stream().flatMap(Collection::stream).toList();
    }

    /** Every record across all keys: derived from the keys' histories, so it refuses every change made to it directly. */
    private static final class AllRecords<E extends IEffectiveEntity> extends MultiEffectiveList<E> {

        // Repeats a change to one key's history. Calls the base class's add and remove directly, which the overrides below refuse.
        void relay(ListChangeListener.Change<? extends E> change) {
            while (change.next()) {
                for (E removed : change.getRemoved()) {
                    super.remove(indexOf(removed));
                }
                for (E added : change.getAddedSubList()) {
                    super.add(0, added);
                }
            }
        }

        @Override
        public boolean add(E element) {
            throw new UnsupportedOperationException("Every record belongs to a key: put it in the map");
        }

        @Override
        public void add(int index, E element) {
            throw new UnsupportedOperationException("Every record belongs to a key: put it in the map");
        }

        @Override
        public boolean addAll(Collection<? extends E> collection) {
            throw new UnsupportedOperationException("Every record belongs to a key: put it in the map");
        }

        @Override
        public E remove(int index) {
            throw new UnsupportedOperationException("Every record belongs to a key: remove it from the map");
        }

        @Override
        public boolean reschedule(E element, LocalDate start, LocalDate end) {
            throw new UnsupportedOperationException("Every record belongs to a key: reschedule it in its key's history");
        }

        @Override
        public boolean setAll(Collection<? extends E> collection) {
            throw new UnsupportedOperationException("Every record belongs to a key: put it in the map");
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            throw new UnsupportedOperationException("Every record belongs to a key: remove it from the map");
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            throw new UnsupportedOperationException("Every record belongs to a key: remove it from the map");
        }

        @Override
        public boolean removeIf(Predicate<? super E> filter) {
            throw new UnsupportedOperationException("Every record belongs to a key: remove it from the map");
        }

        @Override
        protected void removeRange(int fromIndex, int toIndex) {
            throw new UnsupportedOperationException("Every record belongs to a key: remove it from the map");
        }
    }

}
