package io.github.ctgnz.fxtivity.employment;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.github.ctgnz.fxtivity.EffectiveProperty;
import io.github.ctgnz.fxtivity.Effectivity;
import io.github.ctgnz.fxtivity.IEffectiveEntity;
import io.github.ctgnz.fxtivity.MultiEffectiveList;

/**
 * A person, from Fowler's example: someone who holds employments, several at once if need be.
 * <p>
 * Given a lifespan, so that what can be said about them is bounded by when they were alive, and a name that changes over time - which is the case Fowler's pattern does not cover
 * on its own, and the one {@link EffectiveProperty} exists for.
 */
@JsonPropertyOrder({
    "id", "start", "end", "name"
})
public final class Person implements IEffectiveEntity {

    private String id;
    private LocalDate start;
    private LocalDate end = Effectivity.FOREVER;
    private final @JsonManagedReference EffectiveProperty<String, Person> name = new EffectiveProperty<>(this);
    private final @JsonIgnore MultiEffectiveList<Employment> employments = new MultiEffectiveList<>();

    Person() {
    }

    /**
     * A person born on {@code born}, known by {@code id} from birth.
     *
     * @param id
     *            the person's identity, and their name at birth
     * @param born
     *            the day they were born
     */
    public Person(String id, LocalDate born) {
        this.id = id;
        this.start = born;
        name.setValue(born, id);
    }

    /** Fowler's {@code addEmployment(Company, MfDate)}: employment by {@code company} from {@code startDate}, until further notice. */
    public Employment addEmployment(Company company, LocalDate startDate) {
        Employment employment = new Employment(company, startDate);
        employments.add(employment);
        return employment;
    }

    /** Fowler's {@code addEmployment(Employment)}. */
    public void addEmployment(Employment employment) {
        employments.add(employment);
    }

    /** Every employment the person has held, in date order. */
    public MultiEffectiveList<Employment> employments() {
        return employments;
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

    /** The name the person went by over time. */
    public EffectiveProperty<String, Person> name() {
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
