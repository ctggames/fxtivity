package io.github.ctgnz.fxtivity.example;

import java.time.LocalDate;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import io.github.ctgnz.fxtivity.Effectivity;
import io.github.ctgnz.fxtivity.Removal;
import io.github.ctgnz.fxtivity.SingleEffectiveList;
import io.github.ctgnz.fxtivity.control.EffectiveDateCell;
import io.github.ctgnz.fxtivity.employment.Appointment;
import io.github.ctgnz.fxtivity.employment.Company;
import io.github.ctgnz.fxtivity.employment.Department;

/**
 * Editing effective lists the way the library intends.
 * <p>
 * An editor shows a list's whole contents - {@code getSourceList()}, not the {@code effective()} view - because it edits history, not only today. Every change goes through the
 * list's own methods, so its rules are enforced: a change that would break one is refused, and shown as refused, rather than made. Dates are chosen with an
 * {@link EffectiveDateCell}, so no date outside the active range can be entered at all.
 */
final class EditorPane extends VBox {

    /** The three ways to put someone into a succession. */
    enum Appointing {
            Refusing_an_overlap("Appoint, refusing an overlap"),
            Taking_over_early("Appoint, taking over early"),
            Serving_the_full_term("Appoint, serving the full term");

        private final String label;

        Appointing(String label) {
            this.label = label;
        }

        boolean appoint(SingleEffectiveList<Appointment> succession, Appointment appointment) {
            return switch (this) {
                case Refusing_an_overlap -> succession.add(appointment);
                case Taking_over_early -> succession.insertBackwards(appointment);
                case Serving_the_full_term -> succession.insertForwards(appointment);
            };
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private final Company company;
    final TableView<Appointment> chiefExecutives;
    final TableView<Department> departments;
    final TextField holder = new TextField();
    final DatePicker from = datePicker();
    final DatePicker to = datePicker();
    final CheckBox untilFurtherNotice = new CheckBox("until further notice");
    final ChoiceBox<Appointing> appointing = new ChoiceBox<>();
    final ChoiceBox<Removal> removal = new ChoiceBox<>();
    final TextField departmentName = new TextField();
    final DatePicker departmentDate = datePicker();
    final Label status = new Label();

    EditorPane(Company company) {
        super(8);
        this.company = company;
        setPadding(new Insets(8));

        chiefExecutives = new TableView<>(company.chiefExecutives().getSourceList());
        TableColumn<Appointment, String> name = new TableColumn<>("Chief executive");
        name.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().holder()));
        chiefExecutives.getColumns().add(name);
        chiefExecutives.getColumns().add(dateColumn("From", true));
        chiefExecutives.getColumns().add(dateColumn("To", false));
        chiefExecutives.setPrefHeight(160);
        chiefExecutives.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        holder.setPromptText("who");
        appointing.getItems().setAll(Appointing.values());
        appointing.setValue(Appointing.Refusing_an_overlap);
        removal.getItems().setAll(Removal.values());
        removal.setValue(Removal.Refused);
        removal.setConverter(new StringConverter<>() {
            @Override
            public String toString(Removal value) {
                return value == null ? "" : switch (value) {
                    case Refused -> "refusing to leave a gap";
                    case ExtendsPrevious -> "the predecessor stays on";
                    case StartsNextEarlier -> "the successor starts early";
                };
            }

            @Override
            public Removal fromString(String text) {
                return null;
            }
        });
        to.disableProperty().bind(untilFurtherNotice.selectedProperty());
        Button appoint = new Button("Appoint");
        appoint.setOnAction(evt -> appoint());
        Button reschedule = new Button("Reschedule selected");
        reschedule.setOnAction(evt -> reschedule());
        Button remove = new Button("Remove selected,");
        remove.setOnAction(evt -> remove());

        departments = new TableView<>(company.departments());
        TableColumn<Department, String> department = new TableColumn<>("Department");
        department.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().name().getEffectiveValue()));
        TableColumn<Department, LocalDate> opened = new TableColumn<>("Opened");
        opened.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().getStart()));
        TableColumn<Department, String> closed = new TableColumn<>("Closed");
        closed.setCellValueFactory(row -> new ReadOnlyStringWrapper(open(row.getValue().getEnd())));
        departments.getColumns().add(department);
        departments.getColumns().add(opened);
        departments.getColumns().add(closed);
        departments.setPrefHeight(160);
        departments.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        departmentName.setPromptText("name");
        Button openDepartment = new Button("Open");
        openDepartment.setOnAction(evt -> openDepartment());
        Button closeDepartment = new Button("Close selected");
        closeDepartment.setOnAction(evt -> closeDepartment());

        getChildren()
            .add(new TitledPane("Chief executives - the whole succession", new VBox(8, chiefExecutives, new FlowPane(8, 8, holder, new Label("from"), from, new Label("to"), to, untilFurtherNotice),
                                                                                    new FlowPane(8, 8, appointing, appoint, reschedule, remove, removal))));
        getChildren().add(new TitledPane("Departments - every one the company has had",
                                         new VBox(8, departments, new FlowPane(8, 8, departmentName, new Label("on"), departmentDate, openDepartment, closeDepartment))));
        getChildren().add(status);
    }

    /** Re-reads the names and dates shown, after the effective date moves. */
    void refresh() {
        chiefExecutives.refresh();
        departments.refresh();
    }

    void appoint() {
        if (holder.getText().isBlank() || from.getValue() == null) {
            report(false, "Say who, and from when.");
            return;
        }
        Appointing how = appointing.getValue();
        report(how.appoint(company.chiefExecutives(), new Appointment(holder.getText().strip(), from.getValue(), end())), how + ": " + holder.getText().strip());
    }

    void reschedule() {
        Appointment selected = chiefExecutives.getSelectionModel().getSelectedItem();
        if (selected == null || from.getValue() == null) {
            report(false, "Select an appointment, and give its new dates.");
            return;
        }
        report(company.chiefExecutives().reschedule(selected, from.getValue(), end()), "Reschedule " + selected.holder());
    }

    void remove() {
        Appointment selected = chiefExecutives.getSelectionModel().getSelectedItem();
        if (selected == null) {
            report(false, "Select an appointment.");
            return;
        }
        try {
            company.chiefExecutives().remove(selected, removal.getValue());
            report(true, "Remove " + selected.holder());
        } catch (IllegalArgumentException e) {
            report(false, "Remove " + selected.holder() + " - " + e.getMessage());
        }
    }

    void openDepartment() {
        if (departmentName.getText().isBlank() || departmentDate.getValue() == null) {
            report(false, "Name the department, and the day it opens.");
            return;
        }
        report(company.openDepartment(departmentName.getText().strip(), departmentDate.getValue()).isPresent(), "Open " + departmentName.getText().strip());
    }

    void closeDepartment() {
        Department selected = departments.getSelectionModel().getSelectedItem();
        if (selected == null || departmentDate.getValue() == null) {
            report(false, "Select a department, and the day it closes.");
            return;
        }
        report(selected.close(departmentDate.getValue()), "Close " + selected.getId());
        departments.refresh();
    }

    private LocalDate end() {
        return untilFurtherNotice.isSelected() || to.getValue() == null ? Effectivity.FOREVER : to.getValue();
    }

    private void report(boolean done, String what) {
        status.setText((done ? "Done: " : "Refused: ") + what);
        status.setStyle(done ? "-fx-text-fill: -fx-text-base-color;" : "-fx-text-fill: firebrick;");
    }

    private TableColumn<Appointment, String> dateColumn(String title, boolean start) {
        TableColumn<Appointment, String> column = new TableColumn<>(title);
        column.setCellValueFactory(row -> new ReadOnlyStringWrapper(start ? row.getValue().getStart().toString() : open(row.getValue().getEnd())));
        return column;
    }

    private static String open(LocalDate end) {
        return Effectivity.FOREVER.equals(end) ? "" : end.toString();
    }

    // Days outside the active range cannot be chosen, so no date entered here can be outside it.
    private static DatePicker datePicker() {
        DatePicker picker = new DatePicker();
        picker.setDayCellFactory(p -> new EffectiveDateCell());
        picker.setPrefWidth(130);
        return picker;
    }

}
