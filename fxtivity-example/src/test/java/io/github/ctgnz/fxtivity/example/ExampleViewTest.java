package io.github.ctgnz.fxtivity.example;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;

import javafx.scene.control.TreeItem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.ctgnz.fxtivity.Effectivity;
import io.github.ctgnz.fxtivity.Removal;
import io.github.ctgnz.fxtivity.employment.Department;
import io.github.ctgnz.fxtivity.employment.Person;

/**
 * The window follows the one effective date in every view, and an edit shows in every view at once - with no view holding a date of its own, and no date having to move for a
 * change to appear.
 */
class ExampleViewTest {

    private ExampleView view;

    @BeforeEach
    void init() throws Exception {
        Effectivity.forDates(LocalDate.of(2010, 1, 1), AcmeStory.FIRST, AcmeStory.AFTER);
        view = FxThread.call(() -> {
            try (InputStream story = ExampleApp.class.getResourceAsStream("acme.yml")) {
                return new ExampleView(Yaml.read(story));
            }
        });
    }

    private List<String> departmentsInTree() {
        return view.organisation.getRoot().getChildren().stream().map(item -> ((Department) item.getValue()).getId()).toList();
    }

    private List<Person> peopleUnder(String department) {
        return view.organisation.getRoot()
            .getChildren()
            .stream()
            .filter(item -> ((Department) item.getValue()).getId().equals(department))
            .findFirst()
            .orElseThrow()
            .getChildren()
            .stream()
            .map(TreeItem::getValue)
            .map(Person.class::cast)
            .toList();
    }

    private Department department(String id) {
        return view.register().companies().getFirst().department(id).orElseThrow();
    }

    /** The tree - company, the departments open on the date in the order they opened, the people each manages on the date - follows the date at every level. */
    @Test
    void testTheTreeFollowsTheDate() {
        FxThread.run(() -> {
            assertThat(departmentsInTree(), contains("Sales", "Engineering", "Finance", "Marketing", "Research"));
            List<Person> researchers = department("Research").managedOn(LocalDate.of(2017, 12, 31));

            Effectivity.forDate(LocalDate.of(2017, 12, 31));
            assertThat(peopleUnder("Research"), is(researchers));

            Effectivity.forDate(LocalDate.of(2018, 1, 1));
            assertThat("Research has closed", departmentsInTree(), contains("Sales", "Engineering", "Finance", "Marketing"));
            assertThat("Engineering took its people on", peopleUnder("Engineering").containsAll(researchers), is(true));

            Effectivity.forDate(LocalDate.of(1991, 1, 1));
            assertThat(departmentsInTree(), contains("Sales"));
        });
    }

    @Test
    void testTheHeaderAndTablesFollowTheDate() {
        FxThread.run(() -> {
            assertThat(view.companyName.getText(), is("Acme Holdings"));
            assertThat(view.chiefExecutive.getText(), is("Chief executive: Priya Raman"));
            int employed = view.employees.getItems().size();

            Effectivity.forDate(LocalDate.of(2000, 1, 1));
            assertThat(view.companyName.getText(), is("Acme"));
            assertThat(view.chiefExecutive.getText(), is("Chief executive: Martin Okafor"));
            assertThat(view.employees.getItems().size() < employed, is(true));
        });
    }

    /** An appointment that would overlap is refused, and shown as refused; taking over early is accepted, and the header shows it without the date moving. */
    @Test
    void testAppointing() {
        FxThread.run(() -> {
            EditorPane editor = view.editor;
            editor.holder.setText("Zoe Lake");
            editor.from.setValue(LocalDate.of(2009, 1, 1));
            editor.untilFurtherNotice.setSelected(true);
            editor.appointing.setValue(EditorPane.Appointing.Refusing_an_overlap);
            editor.appoint();
            assertThat(editor.status.getText(), startsWith("Refused"));
            assertThat(view.chiefExecutive.getText(), is("Chief executive: Priya Raman"));

            editor.appointing.setValue(EditorPane.Appointing.Taking_over_early);
            editor.appoint();
            assertThat(editor.status.getText(), startsWith("Done"));
            assertThat(view.chiefExecutive.getText(), is("Chief executive: Zoe Lake"));
        });
    }

    /** Removing a chief executive from the middle of the succession leaves a gap: refused unless the removal says who covers it. */
    @Test
    void testRemoving() {
        FxThread.run(() -> {
            EditorPane editor = view.editor;
            editor.chiefExecutives.getSelectionModel().select(2);
            editor.removal.setValue(Removal.Refused);
            editor.remove();
            assertThat(editor.status.getText(), startsWith("Refused"));
            assertThat(view.chiefExecutive.getText(), is("Chief executive: Priya Raman"));

            editor.removal.setValue(Removal.ExtendsPrevious);
            editor.remove();
            assertThat(editor.status.getText(), startsWith("Done"));
            assertThat("Martin Okafor stays on until Tom Vale starts", view.chiefExecutive.getText(), is("Chief executive: Martin Okafor"));
        });
    }

    /** A department closed today goes from the tree at once, without the date having to move. */
    @Test
    void testClosingADepartmentShowsAtOnce() {
        FxThread.run(() -> {
            EditorPane editor = view.editor;
            editor.departments.getSelectionModel().select(department("Finance"));
            editor.departmentDate.setValue(LocalDate.of(2009, 6, 1));
            editor.closeDepartment();
            assertThat(editor.status.getText(), startsWith("Done"));
            assertThat(departmentsInTree(), not(hasItem("Finance")));
        });
    }

}
