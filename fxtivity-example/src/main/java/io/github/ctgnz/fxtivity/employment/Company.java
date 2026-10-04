package io.github.ctgnz.fxtivity.employment;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;

import javafx.collections.ObservableList;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;

import io.github.ctgnz.fxtivity.Effective;
import io.github.ctgnz.fxtivity.EffectiveMap;
import io.github.ctgnz.fxtivity.EffectiveProperty;
import io.github.ctgnz.fxtivity.Effectivity;
import io.github.ctgnz.fxtivity.MultiEffectiveList;
import io.github.ctgnz.fxtivity.SingleEffectiveList;

/**
 * A company, from Fowler's example, given a lifespan and a history of its own.
 * <p>
 * It is founded and may be dissolved, so it is in effect for a period. Its name changes over time - companies get renamed - and it has a succession of chief executives, exactly
 * one at a time, and a board whose seats each change hands independently.
 * <p>
 * Its employments are recorded on each person's side; the company's list of them is derived, read-only, and kept in step as each person registers their employments with it - the
 * same arrangement as a department's.
 * <p>
 * {@code final}, which the harness classes all are: the {@link EffectiveProperty} is created with {@code this} as its owner, and javac's {@code this-escape} check would flag that
 * in any class a subclass could extend.
 */
@JsonPropertyOrder({
    "id", "start", "end", "name", "departments", "chiefExecutives", "board"
})
public final class Company implements Effective {

    private String id;
    private LocalDate start;
    private LocalDate end = Effectivity.FOREVER;
    private final @JsonManagedReference EffectiveProperty<String> name = new EffectiveProperty<>(this);
    private final MultiEffectiveList<Department> departments = new MultiEffectiveList<>(Comparator.comparing(Department::getId));
    private final @JsonIgnore SingleEffectiveList<Appointment> chiefExecutives = new SingleEffectiveList<>();
    private final @JsonIgnore EffectiveMap<String, Appointment> board = new EffectiveMap<>(true);
    private final @JsonIgnore MultiEffectiveList<Employment> employments = new MultiEffectiveList<>(Comparator.comparing(employment -> employment.person().getId()));

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

    /** The company's departments over time - several at once - read-only: they are opened through the company and closed through themselves. */
    public ObservableList<Department> departments() {
        return departments.getSourceList();
    }

    /** The departments open on the application's effective date: read-only, and following the date. */
    public ObservableList<Department> departmentsInEffect() {
        return departments.effective();
    }

    /** Everyone the company has employed, read-only: employments are made and ended on the person's side. */
    public ObservableList<Employment> employments() {
        return employments.getSourceList();
    }

    /** The employments in effect on the application's effective date: read-only, and following the date. */
    public ObservableList<Employment> employmentsInEffect() {
        return employments.effective();
    }

    /** The department with {@code id}, if the company has had one. */
    public Optional<Department> department(String id) {
        return departments.stream().filter(department -> department.getId().equals(id)).findFirst();
    }

    /**
     * Opens a department called {@code id} on {@code opened}.
     *
     * @return the department, or empty if the company was not in existence that day
     */
    public Optional<Department> openDepartment(String id, LocalDate opened) {
        if (!containsDate(opened)) {
            return Optional.empty();
        }
        Department department = new Department(this, id, opened);
        departments.add(department);
        return Optional.of(department);
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

    @JsonManagedReference("departments")
    @JsonGetter("departments")
    List<Department> getDepartments() {
        return departments.getSourceList();
    }

    @JsonManagedReference("departments")
    @JsonSetter("departments")
    void setDepartments(List<Department> loaded) {
        departments.load(loaded);
    }

    @JsonGetter("chiefExecutives")
    List<Appointment> getChiefExecutives() {
        return chiefExecutives.getSourceList();
    }

    @JsonSetter("chiefExecutives")
    void setChiefExecutives(List<Appointment> loaded) {
        chiefExecutives.load(loaded);
    }

    @JsonGetter("board")
    SortedMap<String, ObservableList<Appointment>> getBoard() {
        return board.getSourceMap();
    }

    @JsonSetter("board")
    void setBoard(Map<String, List<Appointment>> loaded) {
        board.load(loaded);
    }

    void reschedule(Department department, LocalDate opened, LocalDate closed) {
        departments.reschedule(department, opened, closed);
    }

    // Idempotent, so registering again - a register built over a model already in memory - adds nothing.
    void register(Employment employment) {
        if (!employments.contains(employment)) {
            employments.add(employment);
        }
    }

    void unregister(Employment employment) {
        employments.remove(employment);
    }

}
