package io.github.ctgnz.fxtivity.employment;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.github.ctgnz.fxtivity.Effective;
import io.github.ctgnz.fxtivity.EffectiveMap;
import io.github.ctgnz.fxtivity.EffectiveProperty;
import io.github.ctgnz.fxtivity.Effectivity;
import io.github.ctgnz.fxtivity.SingleEffectiveList;

/**
 * A company, from Fowler's example, given a lifespan and a history of its own.
 * <p>
 * It is founded and may be dissolved, so it is in effect for a period. Its name changes over time - companies get renamed - and it has a succession of chief executives, exactly
 * one at a time, and a board whose seats each change hands independently.
 * <p>
 * {@code final}, which the harness classes all are: the {@link EffectiveProperty} is created with {@code this} as its owner, and javac's {@code this-escape} check would flag that
 * in any class a subclass could extend.
 */
@JsonPropertyOrder({
    "id", "start", "end", "name"
})
public final class Company implements Effective {

    private String id;
    private LocalDate start;
    private LocalDate end = Effectivity.FOREVER;
    private final @JsonManagedReference EffectiveProperty<String> name = new EffectiveProperty<>(this);
    private final @JsonIgnore SingleEffectiveList<Appointment> chiefExecutives = new SingleEffectiveList<>();
    private final @JsonIgnore EffectiveMap<String, Appointment> board = new EffectiveMap<>(true);

    Company() {
    }

    /**
     * A company founded on {@code founded}, trading under {@code id} from that day.
     *
     * @param id
     *            the company's identity, and its first name
     * @param founded
     *            the day it was founded
     */
    public Company(String id, LocalDate founded) {
        this.id = id;
        this.start = founded;
        name.setValue(founded, id);
    }

    /** The company's board: for each seat, who held it and when. */
    public EffectiveMap<String, Appointment> board() {
        return board;
    }

    /** The company's chief executives, one at a time. */
    public SingleEffectiveList<Appointment> chiefExecutives() {
        return chiefExecutives;
    }

    @Override
    public LocalDate getEnd() {
        return end;
    }

    public String getId() {
        return id;
    }

    @Override
    public LocalDate getStart() {
        return start;
    }

    /** The name the company traded under over time. */
    public EffectiveProperty<String> name() {
        return name;
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
        return id;
    }

}
