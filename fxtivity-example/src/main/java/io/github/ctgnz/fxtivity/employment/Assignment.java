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

/**
 * A person's assignment to a department for a period. A person can be assigned to several departments at once, independently of which one manages them.
 * <p>
 * Saved with the person, naming the department by its id; the department itself is found when the {@link Register} is read back.
 */
@JsonPropertyOrder({
    "department", "start", "end"
})
public final class Assignment implements Effective {
    private @JsonBackReference("assignments") Person person;
    private @JsonIgnore Department department;
    private @JsonIgnore String departmentId;
    private LocalDate start;
    private LocalDate end;

    Assignment(Person person, Department department, LocalDate start, LocalDate end) {
        this.person = person;
        this.department = department;
        this.departmentId = department.getId();
        this.start = start;
        this.end = end;
    }

    @JsonCreator
    Assignment(@JsonProperty("department") String departmentId, @JsonProperty("start") LocalDate start, @JsonProperty("end") LocalDate end) {
        this.departmentId = departmentId;
        this.start = start;
        this.end = end;
    }

    /** The department assigned to. */
    public Department department() {
        return department;
    }

    @JsonGetter("department")
    String departmentId() {
        return departmentId;
    }

    @Override
    public LocalDate getEnd() {
        return end;
    }

    @Override
    public LocalDate getStart() {
        return start;
    }

    /** Who is assigned. */
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
        return department + " " + getEffectivity();
    }

    void resolve(Map<String, Department> departments) {
        this.department = departments.get(departmentId);
    }
}
