package io.github.ctgnz.fxtivity.example;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.stream.Collectors;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import io.github.ctgnz.fxtivity.Effectivity;
import io.github.ctgnz.fxtivity.control.EffectiveDatePicker;
import io.github.ctgnz.fxtivity.employment.Appointment;
import io.github.ctgnz.fxtivity.employment.Company;
import io.github.ctgnz.fxtivity.employment.Department;
import io.github.ctgnz.fxtivity.employment.Employment;
import io.github.ctgnz.fxtivity.employment.Person;
import io.github.ctgnz.fxtivity.employment.Register;

/**
 * The example's window: the effective date at the top, and a company seen on that date below it.
 * <p>
 * No view here holds a date of its own. The lists - departments, the people each manages, employments, a chief executive - are effective lists' {@code effective()} views, so they
 * follow the date by themselves. What is not a list - a name on the date, which department manages someone - is read on the date, and re-read when it moves: the one listener on
 * the effective date, which does nothing else.
 */
public final class ExampleView extends BorderPane {

    private final Register register;
    private final Company company;
    final Label companyName = new Label();
    final Label chiefExecutive = new Label();
    final TreeView<Object> organisation;
    final TableView<Employment> employees;
    final TableView<String> board;
    final EditorPane editor;
    final TabPane tabs;
    final Tab editTab;
    // Re-reads what is dated but not a list. Held in a field and registered weakly, because opening another file replaces this view while the effective date lives on.
    private final ChangeListener<LocalDate> dateMoved = (obs, oldDate, newDate) -> showDated();

    /**
     * A view of the first company in {@code register}.
     *
     * @param register
     *            the model
     */
    public ExampleView(Register register) {
        this.register = register;
        this.company = register.companies().getFirst();

        companyName.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        ObservableList<Appointment> chiefExecutives = company.chiefExecutives().effective();
        chiefExecutive.textProperty().bind(Bindings.createStringBinding(() -> "Chief executive: " + (chiefExecutives.isEmpty() ? "none" : chiefExecutives.getFirst().holder()), chiefExecutives));
        HBox header = new HBox(24, companyName, chiefExecutive);
        header.setPadding(new Insets(8));

        organisation = organisation();
        employees = employees();
        board = board();
        editor = new EditorPane(company);
        editTab = new Tab("Edit", editor);
        tabs = new TabPane(new Tab("Organisation", organisation), new Tab("Employees", employees), new Tab("Board", board));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        setTop(new VBox(new EffectiveDatePicker(EffectiveDatePicker.Format.DAY_MONTH_YEAR), header));
        setCenter(tabs);

        Effectivity.effectiveDateProperty().addListener(new WeakChangeListener<>(dateMoved));
        showDated();
    }

    /** The model shown. */
    public Register register() {
        return register;
    }

    /** Shows the edit tab, or hides it. */
    public void setEditing(boolean editing) {
        if (editing && !tabs.getTabs().contains(editTab)) {
            tabs.getTabs().add(editTab);
            tabs.getSelectionModel().select(editTab);
        } else if (!editing) {
            tabs.getTabs().remove(editTab);
        }
    }

    // The company, its departments open on the date, and the people each manages on the date - every level an effective list's effective() view.
    private TreeView<Object> organisation() {
        TreeItem<Object> root = new TreeItem<>(company);
        root.setExpanded(true);
        TreeItems.bindChildren(root, company.departmentsInEffect(), department -> {
            TreeItem<Object> branch = new TreeItem<>(department);
            branch.setExpanded(true);
            TreeItems.bindChildren(branch, department.managedInEffect(), management -> new TreeItem<>(management.person()));
            return branch;
        });
        TreeView<Object> tree = new TreeView<>(root);
        tree.setCellFactory(view -> new TreeCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : describe(item));
            }
        });
        return tree;
    }

    private String describe(Object item) {
        return switch (item) {
            case Company c -> c.name().getEffectiveValue();
            case Department d -> d.name().getEffectiveValue() + " (" + d.managedInEffect().size() + ")";
            case Person p -> p.name().getEffectiveValue();
            default -> item.toString();
        };
    }

    private TableView<Employment> employees() {
        SortedList<Employment> sorted = new SortedList<>(company.employmentsInEffect());
        TableView<Employment> table = new TableView<>(sorted);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        TableColumn<Employment, String> name = new TableColumn<>("Name");
        name.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().person().name().getEffectiveValue()));
        TableColumn<Employment, String> department = new TableColumn<>("Department");
        department.setCellValueFactory(row -> {
            Department managing = row.getValue().person().managedBy(Effectivity.when());
            return new ReadOnlyStringWrapper(managing == null ? "(the company)" : managing.name().getEffectiveValue());
        });
        TableColumn<Employment, String> assigned = new TableColumn<>("Also assigned to");
        assigned.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue()
            .person()
            .assignments()
            .stream()
            .filter(a -> a.isEffective())
            .map(a -> a.department().name().getEffectiveValue())
            .collect(Collectors.joining(", "))));
        TableColumn<Employment, LocalDate> joined = new TableColumn<>("Joined");
        joined.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().getStart()));
        table.getColumns().add(name);
        table.getColumns().add(department);
        table.getColumns().add(assigned);
        table.getColumns().add(joined);
        table.getSortOrder().add(name);
        name.setComparator(Comparator.naturalOrder());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        return table;
    }

    private TableView<String> board() {
        TableView<String> table = new TableView<>(FXCollections.observableArrayList(company.board().keySet()));
        TableColumn<String, String> seat = new TableColumn<>("Seat");
        seat.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue()));
        TableColumn<String, String> holder = new TableColumn<>("Held by");
        holder.setCellValueFactory(row -> {
            ObservableList<Appointment> holding = company.board().getRecords(row.getValue()).effective();
            return new ReadOnlyStringWrapper(holding.isEmpty() ? "vacant" : holding.getFirst().holder());
        });
        table.getColumns().add(seat);
        table.getColumns().add(holder);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        return table;
    }

    // What is read on the date rather than held in an effective list: the company's name, and every cell's text, which the views re-read on refresh.
    private void showDated() {
        companyName.setText(company.name().getEffectiveValue());
        organisation.refresh();
        employees.refresh();
        employees.sort();
        board.refresh();
        editor.refresh();
    }

}
