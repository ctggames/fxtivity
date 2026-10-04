package io.github.ctgnz.fxtivity.employment;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javafx.collections.ObservableList;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.github.ctgnz.fxtivity.Effective;
import io.github.ctgnz.fxtivity.EffectiveProperty;
import io.github.ctgnz.fxtivity.Effectivity;
import io.github.ctgnz.fxtivity.MultiEffectiveList;

/**
 * A department of a company: opened, perhaps renamed, perhaps closed, within the company's own life.
 * <p>
 * Who a department manages, and who is assigned to it, are recorded on the people's side - a person's managing department is an {@link EffectiveProperty}, and their assignments a
 * list of their own. The department's lists are derived from those: read-only here, kept in step as each person registers with the departments they name, and rebuilt the same way
 * after a load. One end owns each relationship, and the other follows it.
 */
@JsonPropertyOrder({
    "id", "start", "end", "name"
})
public final class Department implements Effective {
    private String id;
    private LocalDate start;
    private LocalDate end = Effectivity.FOREVER;
    private @JsonBackReference("departments") Company company;
    private final @JsonManagedReference EffectiveProperty<String> name = new EffectiveProperty<>(this);
    private final @JsonIgnore MultiEffectiveList<Management> managed = new MultiEffectiveList<>(Comparator.comparing(management -> management.person().getId()));
    private final @JsonIgnore MultiEffectiveList<Assignment> assigned = new MultiEffectiveList<>(Comparator.comparing(assignment -> assignment.person().getId()));

    Department() {
    }

    Department(Company company, String id, LocalDate opened) {
        this.company = company;
        this.id = id;
        this.start = opened;
        name.setValue(opened, id);
    }

    /** Everyone assigned to the department over time, read-only: assignments are made and ended on the person's side. */
    public ObservableList<Assignment> assigned() {
        return assigned.getSourceList();
    }

    /**
     * Closes the department on {@code date}, handing everyone it manages back to the company and ending everyone's assignment to it, so nothing refers to it after it closes.
     *
     * @return false, changing nothing, if the department was not open on {@code date}
     */
    public boolean close(LocalDate date) {
        if (!containsDate(date)) {
            return false;
        }
        // Through the company's departments, not by setting the end here: those lists would go on showing the department as open until the effective date next moved.
        company.reschedule(this, start, date);
        for (Management management : new ArrayList<>(managed)) {
            if (management.getEnd().isAfter(date)) {
                management.person().moveTo(null, Effectivity.later(date, management.getStart()));
            }
        }
        for (Assignment assignment : new ArrayList<>(assigned)) {
            if (assignment.getEnd().isAfter(date)) {
                assignment.person().endAssignment(assignment, date);
            }
        }
        return true;
    }

    /** The company the department belongs to. */
    public Company company() {
        return company;
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

    /** Everyone the department has managed over time, each for the span they were managed by it, read-only: who manages a person is set on the person's side. */
    public ObservableList<Management> managed() {
        return managed.getSourceList();
    }

    /** Whom the department manages on the application's effective date: read-only, and following the date. */
    public ObservableList<Management> managedInEffect() {
        return managed.effective();
    }

    /** The assignments to the department in effect on the application's effective date: read-only, and following the date. */
    public ObservableList<Assignment> assignedInEffect() {
        return assigned.effective();
    }

    /** The people the department managed on {@code date}. */
    public List<Person> managedOn(LocalDate date) {
        return managed.stream().filter(management -> management.containsDate(date)).map(Management::person).toList();
    }

    /** The name the department went by over time. */
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

    // Idempotent, so registering again - a register built over a model already in memory - adds nothing.
    void register(Assignment assignment) {
        if (!assigned.contains(assignment)) {
            assigned.add(assignment);
        }
    }

    void register(Management management) {
        managed.add(management);
    }

    void unregister(Assignment assignment) {
        assigned.remove(assignment);
    }

    void unregisterManagementOf(Person person) {
        managed.removeIf(management -> management.person() == person);
    }

}
