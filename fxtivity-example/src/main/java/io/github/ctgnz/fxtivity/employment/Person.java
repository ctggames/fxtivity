package io.github.ctgnz.fxtivity.employment;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;

import io.github.ctgnz.fxtivity.Effective;
import io.github.ctgnz.fxtivity.EffectiveProperty;
import io.github.ctgnz.fxtivity.EffectiveProperty.Entry;
import io.github.ctgnz.fxtivity.EffectiveWrapper;
import io.github.ctgnz.fxtivity.Effectivity;
import io.github.ctgnz.fxtivity.MultiEffectiveList;

/**
 * A person, from Fowler's example: someone who holds employments, several at once if need be.
 * <p>
 * Given a lifespan, so that what can be said about them is bounded by when they were alive, and a name that changes over time - which is the case Fowler's pattern does not cover
 * on its own, and the one {@link EffectiveProperty} exists for.
 * <p>
 * A person is managed by at most one department at a time - or, with none, by the company directly - and may be assigned to several. Both relationships are owned here: the
 * managing department is an {@link EffectiveProperty}, the assignments a list, and every change registers the person with the departments involved, whose own lists follow.
 */
@JsonPropertyOrder({
    "id", "start", "end", "name", "employments", "managedBy", "assignments"
})
public final class Person implements Effective {
    private String id;
    private LocalDate start;
    private LocalDate end = Effectivity.FOREVER;
    private final @JsonManagedReference EffectiveProperty<String> name = new EffectiveProperty<>(this);
    private final @JsonIgnore EffectiveProperty<Department> managedBy = new EffectiveProperty<>(this);
    private final @JsonIgnore MultiEffectiveList<Assignment> assignments = new MultiEffectiveList<>();
    private final @JsonIgnore MultiEffectiveList<Employment> employments = new MultiEffectiveList<>();
    // The departments this person is registered with as managed, so a change can withdraw from them before registering afresh.
    private final @JsonIgnore Set<Department> managingDepartments = Collections.newSetFromMap(new IdentityHashMap<>());
    // The managing departments as read, by id, until the Register resolves them.
    private @JsonIgnore List<Entry<String>> managedByIds = List.of();

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
        addEmployment(employment);
        return employment;
    }

    /** Fowler's {@code addEmployment(Employment)}. */
    public void addEmployment(Employment employment) {
        employment.employ(this);
        employments.add(employment);
        employment.company().register(employment);
    }

    /**
     * Changes {@code employment}'s dates - Fowler's correction of a mistake after the fact - through the person's employments and the company's, so that both follow.
     *
     * @return false, changing nothing, if the person's employments refuse the new dates
     */
    public boolean reschedule(Employment employment, LocalDate startDate, LocalDate endDate) {
        Company company = employment.company();
        company.unregister(employment);
        boolean changed = employments.reschedule(employment, startDate, endDate);
        company.register(employment);
        return changed;
    }

    /** Ends {@code employment} on {@code date}: the person left. */
    public boolean leave(Employment employment, LocalDate date) {
        return reschedule(employment, employment.getStart(), date);
    }

    /** Every assignment the person has held, in date order, read-only: they are made and ended through this person. */
    public List<Assignment> assignments() {
        return assignments.getSourceList();
    }

    /**
     * Assigns the person to {@code department} from {@code from} up to {@code to}.
     *
     * @return the assignment, or null if the department is not open for the whole of it
     */
    public Assignment assignTo(Department department, LocalDate from, LocalDate to) {
        if (!department.isValidFor(from, to)) {
            return null;
        }
        Assignment assignment = new Assignment(this, department, from, to);
        assignments.add(assignment);
        department.register(assignment);
        return assignment;
    }

    /** Every employment the person has held, in date order. */
    public MultiEffectiveList<Employment> employments() {
        return employments;
    }

    /** Ends {@code assignment} on {@code date}, or withdraws it altogether if it had not started by then. */
    public void endAssignment(Assignment assignment, LocalDate date) {
        Department department = assignment.department();
        department.unregister(assignment);
        if (date.isAfter(assignment.getStart())) {
            assignments.reschedule(assignment, assignment.getStart(), date);
            department.register(assignment);
        } else {
            assignments.remove(assignment);
        }
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

    /** The department managing the person on {@code date}, or null if the company managed them directly. */
    public Department managedBy(LocalDate date) {
        return managedBy.getEffectiveValue(date);
    }

    /**
     * Moves the person under {@code department} from {@code from}, or under the company directly if it is null, until the next change. A department that closes before then hands
     * them back to the company when it closes.
     *
     * @return false, changing nothing, if the date is outside the person's life or the department is not open on it
     */
    public boolean moveTo(Department department, LocalDate from) {
        if (department != null && !department.containsDate(from)) {
            return false;
        }
        if (!managedBy.setValue(from, department)) {
            return false;
        }
        if (department != null) {
            LocalDate next = managedBy.getEntries().stream().map(Entry::getDate).filter(date -> date.isAfter(from)).findFirst().orElse(end);
            if (department.getEnd().isBefore(next)) {
                managedBy.setValue(department.getEnd(), null);
            }
        }
        registerManagement();
        return true;
    }

    /** The name the person went by over time. */
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

    @JsonManagedReference("employments")
    @JsonGetter("employments")
    List<Employment> getEmployments() {
        return employments.getSourceList();
    }

    @JsonManagedReference("employments")
    @JsonSetter("employments")
    void setEmployments(List<Employment> loaded) {
        employments.load(loaded);
    }

    @JsonManagedReference("assignments")
    @JsonGetter("assignments")
    List<Assignment> getAssignments() {
        return assignments.getSourceList();
    }

    @JsonManagedReference("assignments")
    @JsonSetter("assignments")
    void setAssignments(List<Assignment> loaded) {
        assignments.load(loaded);
    }

    @JsonGetter("managedBy")
    List<Entry<String>> getManagedBy() {
        return managedBy.getEntries().stream().map(entry -> new Entry<>(entry.getDate(), entry.getValue() == null ? null : entry.getValue().getId())).toList();
    }

    @JsonSetter("managedBy")
    void setManagedBy(List<Entry<String>> loaded) {
        managedByIds = new ArrayList<>(loaded);
    }

    /** After a load: finds the companies and departments named by id, and registers with them. */
    void resolve(Map<String, Company> companies, Map<String, Department> departments) {
        for (Employment employment : employments) {
            employment.resolve(companies);
            employment.company().register(employment);
        }
        managedByIds.forEach(entry -> managedBy.setValue(entry.getDate(), entry.getValue() == null ? null : departments.get(entry.getValue())));
        managedByIds = List.of();
        registerManagement();
        for (Assignment assignment : assignments) {
            assignment.resolve(departments);
            assignment.department().register(assignment);
        }
    }

    // Withdraws from every department managing the person, then registers each span of the managing-department history with its department.
    private void registerManagement() {
        managingDepartments.forEach(department -> department.unregisterManagementOf(this));
        managingDepartments.clear();
        for (EffectiveWrapper<Department> span : managedBy.toWrappedList()) {
            Department department = span.getDelegate();
            if (department != null) {
                department.register(new Management(this, span.getStart(), span.getEnd()));
                managingDepartments.add(department);
            }
        }
    }
}
