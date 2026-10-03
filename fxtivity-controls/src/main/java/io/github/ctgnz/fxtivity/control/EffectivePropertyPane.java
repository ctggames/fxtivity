package io.github.ctgnz.fxtivity.control;

import java.util.Objects;
import java.util.function.Function;

import javafx.collections.FXCollections;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;

import io.github.ctgnz.fxtivity.EffectiveProperty;
import io.github.ctgnz.fxtivity.EffectiveProperty.Entry;

/**
 * A starting point for an editor of an {@link EffectiveProperty}: the property's changes, one per row, each as its date and its value.
 * <p>
 * It shows the changes; editing them is left to the application building on it, which knows what the values are and how they should be entered. The list is the changes as they
 * were when the property was set - an {@code EffectiveProperty} is not observable - so after changing the property, call {@link #refresh()}.
 *
 * @param <T>
 *            the type of the property's value
 */
public class EffectivePropertyPane<T> extends VBox {

    /** One change: its date and its value, as the display function shows it. */
    private final class EntryListCell extends ListCell<Entry<T>> {
        @Override
        protected void updateItem(Entry<T> item, boolean empty) {
            super.updateItem(item, empty);
            setGraphic(null);
            if (item == null || empty) {
                setText("");
            } else {
                setText(String.format("%s: %s", item.getDate(), displayFunction.apply(item.getValue())));
            }
        }
    }

    /** The property shown, or null. */
    protected EffectiveProperty<T> property;
    /** The list of changes. */
    protected final ListView<Entry<T>> view;
    /** How a value is shown. */
    protected final Function<T, String> displayFunction;

    /** A pane showing each value as its {@code toString()}. */
    public EffectivePropertyPane() {
        this(String::valueOf);
    }

    /**
     * A pane showing each value through {@code displayFunction}.
     *
     * @param displayFunction
     *            how a value is shown
     */
    // Calls getChildren(), which a subclass could override, and gives the list view a cell factory that refers back to the pane. A subclass overriding getChildren() would
    // see itself uninitialised there; the cells are not made until the list is shown.
    @SuppressWarnings("this-escape")
    public EffectivePropertyPane(Function<T, String> displayFunction) {
        this.displayFunction = Objects.requireNonNull(displayFunction);
        view = new ListView<>();
        view.setCellFactory(param -> new EntryListCell());
        getChildren().add(view);
    }

    /**
     * The property shown.
     *
     * @return the property, or null
     */
    public EffectiveProperty<T> getProperty() {
        return property;
    }

    /** Shows the property's changes as they are now, after it has been changed. */
    public void refresh() {
        view.setItems(property == null ? FXCollections.observableArrayList() : FXCollections.observableArrayList(property.getEntries()));
    }

    /**
     * Shows {@code property}'s changes.
     *
     * @param property
     *            the property, or null to show nothing
     */
    public void setProperty(EffectiveProperty<T> property) {
        this.property = property;
        refresh();
    }

}
