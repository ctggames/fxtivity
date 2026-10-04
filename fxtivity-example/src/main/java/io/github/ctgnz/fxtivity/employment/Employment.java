package io.github.ctgnz.fxtivity.employment;

import java.time.LocalDate;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.github.ctgnz.fxtivity.Effective;
import io.github.ctgnz.fxtivity.Effectivity;

/**
 * Fowler's {@code Employment}: a person's employment by a company, in effect for a period.
 * <p>
 * Fowler's methods are kept under their own names where they still say something - {@link #company()}, {@link #isEffectiveOn(LocalDate)} - so the example reads against his. His
 * {@code end(MfDate)} and {@code setEffectivity(DateRange)} are supplied by {@link Effective}, but an employment's dates are changed through its person, so that the person's
 * employments and the company's both follow.
 * <p>
 * Saved with the person, naming the company by its id; the company itself is found when the {@link Register} is read back.
 */
@JsonPropertyOrder({
    "company", "start", "end"
})
public final class Employment implements Effective {
    private @JsonBackReference("employments") Person person;
    private @JsonIgnore Company company;
    private @JsonIgnore String companyId;
    private LocalDate start;
    private LocalDate end;

    /**
     * Employment by {@code company} from {@code startDate}, until further notice.
     *
     * @param company
     *            the employer
     * @param startDate
     *            the first day of the employment
     */
    public Employment(Company company, LocalDate startDate) {
        this(company, startDate, Effectivity.FOREVER);
    }

    /**
     * Employment by {@code company} for a known period.
     *
     * @param company
     *            the employer
     * @param startDate
     *            the first day of the employment
     * @param endDate
     *            the first day after it
     */
    public Employment(Company company, LocalDate startDate, LocalDate endDate) {
        this.company = company;
        this.companyId = company.getId();
        this.start = startDate;
        this.end = endDate;
    }

    @JsonCreator
    Employment(@JsonProperty("company") String companyId, @JsonProperty("start") LocalDate start, @JsonProperty("end") LocalDate end) {
        this.companyId = companyId;
        this.start = start;
        this.end = end;
    }

    /** Fowler's {@code company()}. */
    public Company company() {
        return company;
    }

    @JsonGetter("company")
    String companyId() {
        return companyId;
    }

    @Override
    public LocalDate getEnd() {
        return end;
    }

    @Override
    public LocalDate getStart() {
        return start;
    }

    /** Fowler's {@code isEffectiveOn(MfDate)}. */
    public boolean isEffectiveOn(LocalDate date) {
        return containsDate(date);
    }

    /** Who is employed. */
    public Person person() {
        return person;
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
        return company + " " + getEffectivity();
    }

    void employ(Person employee) {
        this.person = employee;
    }

    void resolve(Map<String, Company> companies) {
        this.company = companies.get(companyId);
    }
}
