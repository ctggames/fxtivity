package io.github.ctgnz.fxtivity;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Gives a period to a value that has none of its own, so that it can live in an {@link EffectiveList}.
 * <p>
 * {@link EffectiveProperty#toWrappedList()} uses this to turn a value's history into one element per span of time.
 *
 * @param <V>
 *            the type of the wrapped value
 * @author ctg
 */
public class EffectiveWrapper<V> implements Effective {

    private LocalDate start;
    private LocalDate end;
    private V delegate;

    /**
     * Wraps {@code delegate} with a period.
     *
     * @param start
     *            the first date in effect
     * @param end
     *            the first date no longer in effect
     * @param delegate
     *            the value
     */
    public EffectiveWrapper(LocalDate start, LocalDate end, V delegate) {
        this.start = start;
        this.end = end;
        this.delegate = delegate;
    }

    /**
     * The wrapped value.
     *
     * @return the value
     */
    public V getDelegate() {
        return delegate;
    }

    @Override
    @JsonIgnore
    public LocalDate getEnd() {
        return end;
    }

    @Override
    @JsonIgnore
    public LocalDate getStart() {
        return start;
    }

    /**
     * Replaces the wrapped value.
     *
     * @param delegate
     *            the new value
     */
    public void setDelegate(V delegate) {
        this.delegate = delegate;
    }

    @Override
    public void setEnd(LocalDate endDate) {
        this.end = endDate;
    }

    @Override
    public void setStart(LocalDate startDate) {
        this.start = startDate;
    }

    @Override
    public String toString() {
        return "EffectiveWrapper[start=" + start + ",end=" + end + ",delegate=" + delegate + "]";
    }

}
