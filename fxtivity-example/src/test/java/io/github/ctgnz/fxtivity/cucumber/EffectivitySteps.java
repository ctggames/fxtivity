package io.github.ctgnz.fxtivity.cucumber;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.ParameterType;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.ctgnz.fxtivity.EffectiveWrapper;
import io.github.ctgnz.fxtivity.Effectivity;
import io.github.ctgnz.fxtivity.SingleEffectiveList;
import io.github.ctgnz.fxtivity.employment.Appointment;
import io.github.ctgnz.fxtivity.employment.Company;
import io.github.ctgnz.fxtivity.employment.Department;
import io.github.ctgnz.fxtivity.employment.Employment;
import io.github.ctgnz.fxtivity.employment.Person;
import io.github.ctgnz.fxtivity.employment.Register;
import io.github.ctgnz.yamlflock.FlockYamlFactory;

/**
 * The vocabulary the feature files are written in.
 * <p>
 * Deliberately thin: every step either builds a piece of the example or asserts something about it. Nothing here decides what the library does - a decision hidden in the harness
 * would make the scenarios describe the harness rather than the library.
 */
public class EffectivitySteps {

    /** A date well inside the active range, so that no scenario depends on the day it happens to run. */
    private static final LocalDate DEFAULT_EFFECTIVE_DATE = LocalDate.of(2000, 6, 1);
    private static final LocalDate ACTIVE_START = LocalDate.of(1900, 1, 1);

    // In the order the scenario introduces them, so the register is written in that order.
    private final Map<String, Person> people = new LinkedHashMap<>();
    private final Map<String, Company> companies = new LinkedHashMap<>();
    private final Map<String, Department> departments = new HashMap<>();
    private final Map<String, Employment> employments = new HashMap<>();

    private Effectivity period;
    private Effectivity otherPeriod;
    private boolean outcome;
    private Exception refused;
    private String written;

    /**
     * The effective date is shared by the whole process, so each scenario starts from the same one rather than inheriting whatever the last left behind, or whatever today is.
     */
    @Before
    public void pinTheEffectiveDate() {
        Effectivity.forDates(DEFAULT_EFFECTIVE_DATE, ACTIVE_START, Effectivity.FOREVER);
    }

    @ParameterType("\\d{4}-\\d{2}-\\d{2}")
    public LocalDate date(String text) {
        return LocalDate.parse(text);
    }

    /** A capitalised name of one or more words - Wellington, India Inc - so that the scenarios need no quotes around the people and companies in them. */
    @ParameterType("[A-Z][A-Za-z]*(?: [A-Z][A-Za-z]*)*")
    public String name(String text) {
        return text;
    }

    // ---------------------------------------------------------------------------------------------------- periods

    @Given("a period from {date} to {date}")
    public void aPeriod(LocalDate start, LocalDate end) {
        period = Effectivity.create(start, end);
    }

    @Given("a period from {date} with no end")
    public void aPeriodWithNoEnd(LocalDate start) {
        period = Effectivity.create(start, null);
    }

    @And("another period from {date} to {date}")
    public void anotherPeriod(LocalDate start, LocalDate end) {
        otherPeriod = Effectivity.create(start, end);
    }

    @Then("it is in effect on {date}")
    public void itIsInEffectOn(LocalDate date) {
        assertThat(period.contains(date), is(true));
    }

    @Then("it is not in effect on {date}")
    public void itIsNotInEffectOn(LocalDate date) {
        assertThat(period.contains(date), is(false));
    }

    @Then("the periods overlap")
    public void thePeriodsOverlap() {
        assertThat(period.overlaps(otherPeriod), is(true));
        assertThat(otherPeriod.overlaps(period), is(true));
    }

    @Then("the periods do not overlap")
    public void thePeriodsDoNotOverlap() {
        assertThat(period.overlaps(otherPeriod), is(false));
        assertThat(otherPeriod.overlaps(period), is(false));
    }

    @And("the second continues straight on from the first")
    public void theSecondContinuesFromTheFirst() {
        assertThat(otherPeriod.continuesAfter(period), is(true));
        assertThat(period.continuesBefore(otherPeriod), is(true));
    }

    @Then("the first encloses the second")
    public void theFirstEnclosesTheSecond() {
        assertThat(period.encloses(otherPeriod), is(true));
        assertThat(otherPeriod.encloses(period), is(false));
    }

    // ---------------------------------------------------------------------------------------------------- the effective date

    @Given("the effective date is {date}")
    @When("the effective date moves to {date}")
    public void theEffectiveDateIs(LocalDate date) {
        Effectivity.forDate(date);
    }

    @Given("the active range is {date} to {date}")
    public void theActiveRangeIs(LocalDate start, LocalDate end) {
        Effectivity.forDates(start, start, end);
    }

    @When("the effective date is moved to {date}")
    public void theEffectiveDateIsMovedTo(LocalDate date) {
        try {
            Effectivity.forDate(date);
        } catch (IllegalArgumentException e) {
            refused = e;
        }
    }

    @Then("the move is refused")
    public void theMoveIsRefused() {
        assertThat(refused != null, is(true));
    }

    // ---------------------------------------------------------------------------------------------------- people, companies and employment

    @Given("{name} was founded on {date}")
    public void wasFounded(String company, LocalDate founded) {
        companies.put(company, new Company(company, founded));
    }

    @Given("{name} was born on {date}")
    public void wasBorn(String person, LocalDate born) {
        people.put(person, new Person(person, born));
    }

    @And("{name} died on {date}")
    public void died(String person, LocalDate died) {
        people.get(person).setEnd(died);
    }

    @Given("{name} was employed by {name} from {date} to {date}")
    public void wasEmployedFor(String person, String company, LocalDate start, LocalDate end) {
        Employment employment = new Employment(companies.get(company), start, end);
        employments.put(person + "@" + company, employment);
        people.get(person).addEmployment(employment);
    }

    @Given("{name} was employed by {name} from {date}")
    public void wasEmployedFrom(String person, String company, LocalDate start) {
        employments.put(person + "@" + company, people.get(person).addEmployment(companies.get(company), start));
    }

    @When("it turns out {name} actually joined {name} on {date}")
    public void itTurnsOutTheyActuallyJoined(String person, String company, LocalDate start) {
        Employment employment = employments.get(person + "@" + company);
        assertThat(people.get(person).reschedule(employment, start, employment.getEnd()), is(true));
    }

    @When("{name} left {name} on {date}")
    public void left(String person, String company, LocalDate end) {
        assertThat(people.get(person).leave(employments.get(person + "@" + company), end), is(true));
    }

    @Then("{name} is employed by {string}")
    public void isEmployedBy(String person, String expected) {
        assertThat(names(people.get(person).employments().effective().stream().map(Employment::company).toList()), is(expected));
    }

    @Then("{name} is employed by nobody")
    public void isEmployedByNobody(String person) {
        assertThat(people.get(person).employments().effective().isEmpty(), is(true));
    }

    @Then("{name} was employed by {name} on {date}")
    public void wasEmployedByOn(String person, String company, LocalDate date) {
        assertThat(employments.get(person + "@" + company).isEffectiveOn(date), is(true));
    }

    @Then("{name} was not employed by {name} on {date}")
    public void wasNotEmployedByOn(String person, String company, LocalDate date) {
        assertThat(employments.get(person + "@" + company).isEffectiveOn(date), is(false));
    }

    // ---------------------------------------------------------------------------------------------------- succession

    @Given("{name} was chief executive of {name} from {date} to {date}")
    public void wasChiefExecutive(String holder, String company, LocalDate start, LocalDate end) {
        assertThat(chiefExecutives(company).add(new Appointment(holder, start, end)), is(true));
    }

    @Given("{name} allows gaps between chief executives")
    public void allowsGaps(String company) {
        chiefExecutives(company).setGapsAllowed(true);
    }

    @When("{name} is appointed chief executive of {name} from {date} to {date}")
    public void isAppointed(String holder, String company, LocalDate start, LocalDate end) {
        outcome = chiefExecutives(company).add(new Appointment(holder, start, end));
    }

    @When("{name} is appointed chief executive of {name} from {date} to {date}, taking over from the incumbent")
    public void isAppointedBackwards(String holder, String company, LocalDate start, LocalDate end) {
        outcome = chiefExecutives(company).insertBackwards(new Appointment(holder, start, end));
    }

    @When("{name} is appointed chief executive of {name} from {date} to {date}, serving the full term")
    public void isAppointedForwards(String holder, String company, LocalDate start, LocalDate end) {
        outcome = chiefExecutives(company).insertForwards(new Appointment(holder, start, end));
    }

    @Then("the appointment is refused")
    public void theAppointmentIsRefused() {
        assertThat(outcome, is(false));
    }

    @Then("the appointment is accepted")
    public void theAppointmentIsAccepted() {
        assertThat(outcome, is(true));
    }

    @Then("the chief executive of {name} is {word}")
    public void theChiefExecutiveIs(String company, String holder) {
        assertThat(chiefExecutives(company).getEffectiveRecord().map(Appointment::holder).orElse("nobody"), is(holder));
    }

    @Then("the chief executives of {name} were:")
    public void theChiefExecutivesWere(String company, DataTable table) {
        assertThat(rows(chiefExecutives(company).stream().map(a -> List.of(a.holder(), a.getStart().toString(), a.getEnd().toString())).toList()),
            is(rows(table.asLists().subList(1, table.height()))));
    }

    // ---------------------------------------------------------------------------------------------------- the board

    @Given("{name} held the {word} seat on the board of {name} from {date} to {date}")
    public void heldTheSeat(String holder, String seat, String company, LocalDate start, LocalDate end) {
        companies.get(company).board().put(seat, new Appointment(holder, start, end));
    }

    @Then("the {word} seat on the board of {name} was held by {string}, in turn")
    public void theSeatWasHeldBy(String seat, String company, String expected) {
        assertThat(companies.get(company).board().get(seat).stream().map(Appointment::holder).collect(Collectors.joining(", ")), is(expected));
    }

    @Then("the {word} seat on the board of {name} is held by {name}")
    public void theSeatIsHeldBy(String seat, String company, String holder) {
        SingleEffectiveList<Appointment> history = companies.get(company).board().getRecords(seat);
        assertThat(history.getEffectiveRecord().map(Appointment::holder).orElse("nobody"), is(holder));
    }

    @Then("the board of {name} has {int} appointments across {int} seats")
    public void theBoardHas(String company, int appointments, int seats) {
        assertThat(companies.get(company).board().size(), is(appointments));
        assertThat(companies.get(company).board().keySet().size(), is(seats));
    }

    // ---------------------------------------------------------------------------------------------------- names over time

    @Given("{name} was renamed {string} on {date}")
    public void wasRenamed(String owner, String name, LocalDate date) {
        assertThat(rename(owner, name, date), is(true));
    }

    @When("{name} is renamed {string} on {date}")
    public void isRenamed(String owner, String name, LocalDate date) {
        outcome = rename(owner, name, date);
    }

    @Then("the change is refused")
    public void theChangeIsRefused() {
        assertThat(outcome, is(false));
    }

    @Then("the name of {name} on {date} was {string}")
    public void theNameOnWas(String owner, LocalDate date, String expected) {
        assertThat(nameOf(owner, date), is(expected));
    }

    @Then("the name of {name} is {string}")
    public void theNameIs(String owner, String expected) {
        Person person = people.get(owner);
        assertThat(person != null ? person.name().getEffectiveValue() : companies.get(owner).name().getEffectiveValue(), is(expected));
    }

    @When("the name history of {name} is pruned")
    public void theNameHistoryIsPruned(String owner) {
        people.get(owner).name().prune();
    }

    @Then("the name history of {name} has {int} change(s)")
    public void theNameHistoryHas(String owner, int changes) {
        assertThat(people.get(owner).name().getSize(), is(changes));
    }

    @Then("the names of {name} were:")
    public void theNamesWere(String owner, DataTable table) {
        List<EffectiveWrapper<String>> spans = people.get(owner).name().toWrappedList();
        assertThat(rows(spans.stream().map(span -> List.of(span.getDelegate(), span.getStart().toString(), span.getEnd().toString())).toList()), is(rows(table.asLists().subList(1, table.height()))));
    }

    // ---------------------------------------------------------------------------------------------------- departments

    @Given("{name} opened a(n) {word} department on {date}")
    public void openedADepartment(String company, String department, LocalDate opened) {
        departments.put(department, companies.get(company).openDepartment(department, opened).orElseThrow());
    }

    @When("{name} opens a(n) {word} department on {date}")
    public void opensADepartment(String company, String department, LocalDate opened) {
        outcome = companies.get(company).openDepartment(department, opened).isPresent();
    }

    @Then("the opening is refused")
    public void theOpeningIsRefused() {
        assertThat(outcome, is(false));
    }

    @Given("the {word} department closed on {date}")
    @When("the {word} department closes on {date}")
    public void theDepartmentCloses(String department, LocalDate date) {
        assertThat(departments.get(department).close(date), is(true));
    }

    @Given("{name} was managed by the {word} department from {date}")
    public void wasManagedByTheDepartment(String person, String department, LocalDate from) {
        assertThat(people.get(person).moveTo(departments.get(department), from), is(true));
    }

    @Given("{name} was managed by {name} directly from {date}")
    public void wasManagedDirectly(String person, String company, LocalDate from) {
        assertThat(people.get(person).moveTo(null, from), is(true));
    }

    @When("{name} is transferred to the {word} department on {date}")
    public void isTransferred(String person, String department, LocalDate from) {
        outcome = people.get(person).moveTo(departments.get(department), from);
    }

    @Then("the transfer is refused")
    public void theTransferIsRefused() {
        assertThat(outcome, is(false));
    }

    @Then("{name} was managed by the {word} department on {date}")
    public void wasManagedByTheDepartmentOn(String person, String department, LocalDate date) {
        assertThat(people.get(person).managedBy(date), is(departments.get(department)));
    }

    @Then("{name} was managed by {name} directly on {date}")
    public void wasManagedDirectlyOn(String person, String company, LocalDate date) {
        assertThat(people.get(person).managedBy(date), is(nullValue()));
    }

    @Then("the {word} department managed {string} on {date}")
    public void theDepartmentManaged(String department, String expected, LocalDate date) {
        assertThat(departments.get(department).managedOn(date).stream().map(Person::getId).collect(Collectors.joining(", ")), is(expected));
    }

    @Then("the {word} department managed nobody on {date}")
    public void theDepartmentManagedNobody(String department, LocalDate date) {
        assertThat(departments.get(department).managedOn(date), is(empty()));
    }

    @Given("{name} was assigned to the {word} department from {date} to {date}")
    public void wasAssigned(String person, String department, LocalDate from, LocalDate to) {
        assertThat(people.get(person).assignTo(departments.get(department), from, to), is(notNullValue()));
    }

    @Given("{name} was assigned to the {word} department from {date}")
    public void wasAssignedUntilFurtherNotice(String person, String department, LocalDate from) {
        wasAssigned(person, department, from, Effectivity.FOREVER);
    }

    @When("{name} is assigned to the {word} department from {date} to {date}")
    public void isAssigned(String person, String department, LocalDate from, LocalDate to) {
        outcome = people.get(person).assignTo(departments.get(department), from, to) != null;
    }

    @Then("the assignment is refused")
    public void theAssignmentIsRefused() {
        assertThat(outcome, is(false));
    }

    @Then("{name} was assigned to {string} on {date}")
    public void wasAssignedOn(String person, String expected, LocalDate date) {
        assertThat(assignedOn(person, date), is(expected));
    }

    @Then("{name} was assigned to nothing on {date}")
    public void wasAssignedToNothingOn(String person, LocalDate date) {
        assertThat(assignedOn(person, date), is(""));
    }

    @Then("the {word} department had {string} assigned on {date}")
    public void theDepartmentHadAssigned(String department, String expected, LocalDate date) {
        assertThat(departments.get(department).assigned().stream().filter(assignment -> assignment.containsDate(date)).map(assignment -> assignment.person().getId()).collect(Collectors.joining(", ")),
            is(expected));
    }

    // ---------------------------------------------------------------------------------------------------- the register

    @When("the register is written as YAML")
    public void theRegisterIsWritten() throws Exception {
        written = mapper().writeValueAsString(new Register(List.copyOf(companies.values()), List.copyOf(people.values())));
    }

    @When("the register is written as YAML and read back")
    public void theRegisterIsWrittenAndReadBack() throws Exception {
        theRegisterIsWritten();
        Register register = mapper().readValue(written, Register.class);
        companies.clear();
        people.clear();
        departments.clear();
        register.companies().forEach(company -> {
            companies.put(company.getId(), company);
            company.departments().forEach(department -> departments.put(department.getId(), department));
        });
        register.people().forEach(person -> people.put(person.getId(), person));
    }

    // ---------------------------------------------------------------------------------------------------- writing and reading

    @When("{name} is written as YAML")
    public void isWrittenAsYaml(String person) throws Exception {
        written = mapper().writeValueAsString(people.get(person));
    }

    @Then("the YAML is:")
    public void theYamlIs(String expected) {
        assertThat(written.strip(), is(expected.strip()));
    }

    @When("{name} is written as YAML and read back")
    public void isWrittenAndReadBack(String person) throws Exception {
        String yaml = mapper().writeValueAsString(people.get(person));
        people.put(person, mapper().readValue(yaml, Person.class));
    }

    // ---------------------------------------------------------------------------------------------------- support

    private SingleEffectiveList<Appointment> chiefExecutives(String company) {
        return companies.get(company).chiefExecutives();
    }

    private boolean rename(String owner, String name, LocalDate date) {
        Person person = people.get(owner);
        return person != null ? person.name().setValue(date, name) : companies.get(owner).name().setValue(date, name);
    }

    private String nameOf(String owner, LocalDate date) {
        Person person = people.get(owner);
        return person != null ? person.name().getEffectiveValue(date) : companies.get(owner).name().getEffectiveValue(date);
    }

    private String assignedOn(String person, LocalDate date) {
        return people.get(person).assignments().stream().filter(assignment -> assignment.containsDate(date)).map(assignment -> assignment.department().getId()).collect(Collectors.joining(", "));
    }

    private static String names(List<Company> list) {
        return list.stream().map(Company::getId).collect(Collectors.joining(", "));
    }

    private static String rows(List<List<String>> rows) {
        return rows.stream().map(row -> String.join(" | ", row)).collect(Collectors.joining("\n"));
    }

    private static ObjectMapper mapper() {
        ObjectMapper mapper = new ObjectMapper(FlockYamlFactory.builder().build());
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setDefaultPropertyInclusion(Include.NON_DEFAULT);
        return mapper;
    }

}
