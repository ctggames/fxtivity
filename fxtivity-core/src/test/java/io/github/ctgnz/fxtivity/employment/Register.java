package io.github.ctgnz.fxtivity.employment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * The whole model, as saved: the companies, with their departments, and the people.
 * <p>
 * A person refers to a department across the model - the one managing them, the ones they are assigned to - so on disk it names the department by its id. Reading resolves those
 * names once everything is read: every person then registers with the departments they name, which is what rebuilds each department's derived lists. The relationships are saved
 * once, on the end that owns them.
 */
@JsonPropertyOrder({
    "companies", "people"
})
public final class Register {
    private final List<Company> companies;
    private final List<Person> people;

    /**
     * A register of {@code companies} and {@code people}, resolving every reference a person makes to a department.
     *
     * @param companies
     *            the companies, with their departments
     * @param people
     *            the people
     */
    @JsonCreator
    public Register(@JsonProperty("companies") List<Company> companies, @JsonProperty("people") List<Person> people) {
        this.companies = List.copyOf(companies);
        this.people = List.copyOf(people);
        Map<String, Department> departments = new HashMap<>();
        companies.forEach(company -> company.departments().forEach(department -> departments.put(department.getId(), department)));
        people.forEach(person -> person.resolve(departments));
    }

    @JsonGetter("companies")
    public List<Company> companies() {
        return companies;
    }

    @JsonGetter("people")
    public List<Person> people() {
        return people;
    }
}
