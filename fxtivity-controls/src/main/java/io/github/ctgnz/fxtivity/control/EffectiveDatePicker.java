package io.github.ctgnz.fxtivity.control;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.function.UnaryOperator;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import javafx.util.converter.LocalDateStringConverter;

import io.github.ctgnz.fxtivity.DateRange;
import io.github.ctgnz.fxtivity.Effectivity;

/**
 * The control that sets the application's effective date.
 * <p>
 * It typically sits in the top menu bar, in view on every page, so there is one place to change the date while moving around the application - the other half of the rule that
 * there is one effective date per application. It shows the date, with buttons to step back and forward by a year, a month or a day as the format allows, and the active range the
 * date may move within.
 * <p>
 * It follows the date as well as setting it: moved by anything else, the picker shows the new date. A date outside the active range is never set - the day cells disable it, each
 * button is disabled while its step would leave the range, and a typed date outside it, or one that cannot be read, is refused and the picker shows the current date again.
 */
public class EffectiveDatePicker extends HBox {

    /** How the date is shown, and so which steps the picker offers: a day step only where days are shown, and a month step only where months are. */
    public enum Format {
            YEAR_ERA("y G", false, false),
            MONTH_YEAR("MMMM y", false, true),
            MONTH_YEAR_ERA("MMMM y G", false, true),
            DAY_MONTH_YEAR("d MMM y", true, true),
            MONTH_DAY_YEAR("MMM d y", true, true),
            ISO_DATE("yyyy-MM-dd", true, true);

        private final String format;
        private final boolean useDay;
        private final boolean useMonth;

        Format(String format, boolean useDay, boolean useMonth) {
            this.format = format;
            this.useDay = useDay;
            this.useMonth = useMonth;
        }

        /** The {@link DateTimeFormatter} pattern. */
        public String getFormat() {
            return format;
        }

        /** Whether days are shown, and so stepped through. */
        public boolean isUseDay() {
            return useDay;
        }

        /** Whether months are shown, and so stepped through. */
        public boolean isUseMonth() {
            return useMonth;
        }

    }

    private final DatePicker datePicker;
    private final StringConverter<LocalDate> converter;
    private final Label dateRange;
    // Shows the date when anything else moves it. Held in a field because it is registered weakly: the effective date lives as long as the application, and a picker that is
    // discarded must not be kept alive by it.
    private final ChangeListener<LocalDate> effectiveDateListener = (obs, oldValue, newValue) -> showEffectiveDate();

    /** A picker showing years only. */
    public EffectiveDatePicker() {
        this(Format.YEAR_ERA);
    }

    /**
     * A picker showing the date in {@code dateFormat}.
     *
     * @param dateFormat
     *            how the date is shown, and so which steps are offered
     */
    // The picker hands itself, in lambdas that call back into it, to the controls it builds, to its bindings and to the effective date's listeners. None of them runs until
    // something changes - a date chosen, the effective date or the active range moved - which is after construction, so nothing a subclass overrides is reached early.
    @SuppressWarnings("this-escape")
    public EffectiveDatePicker(Format dateFormat) {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(4);

        converter = new RefusingConverter(DateTimeFormatter.ofPattern(dateFormat.getFormat()));
        datePicker = new DatePicker(Effectivity.when());
        datePicker.setDayCellFactory(picker -> new EffectiveDateCell());
        datePicker.setConverter(converter);
        datePicker.valueProperty().addListener((obs, oldValue, newValue) -> dateChosen(newValue));

        dateRange = new Label();
        dateRange.textProperty().bind(Bindings.createStringBinding(() -> describe(Effectivity.activeRange()), Effectivity.activeRangeProperty()));

        getChildren().add(new Label("Effective Date: "));
        getChildren().add(step("Y", "Previous Year", date -> date.minusYears(1), false));
        if (dateFormat.isUseMonth()) {
            getChildren().add(step("M", "Previous Month", date -> date.minusMonths(1), false));
        }
        if (dateFormat.isUseDay()) {
            getChildren().add(step("D", "Previous Day", date -> date.minusDays(1), false));
        }
        getChildren().add(datePicker);
        if (dateFormat.isUseDay()) {
            getChildren().add(step("D", "Next Day", date -> date.plusDays(1), true));
        }
        if (dateFormat.isUseMonth()) {
            getChildren().add(step("M", "Next Month", date -> date.plusMonths(1), true));
        }
        getChildren().add(step("Y", "Next Year", date -> date.plusYears(1), true));
        getChildren().add(dateRange);

        Effectivity.effectiveDateProperty().addListener(new WeakChangeListener<>(effectiveDateListener));
    }

    /**
     * Called with each date chosen in the picker - from the calendar, a step button, or typed. A date in the active range becomes the effective date; anything else is refused, and
     * the picker shows the effective date again.
     *
     * @param chosen
     *            the date chosen, or null if what was typed could not be read
     */
    protected void dateChosen(LocalDate chosen) {
        if (chosen == null || !Effectivity.activeRange().contains(chosen)) {
            showEffectiveDate();
        } else if (!chosen.equals(Effectivity.when())) {
            Effectivity.forDate(chosen);
        }
    }

    /** Shows the effective date, after it has moved or a choice has been refused. */
    protected void showEffectiveDate() {
        datePicker.setValue(Effectivity.when());
    }

    private Button step(String text, String tooltip, UnaryOperator<LocalDate> move, boolean forward) {
        Button button = new Button(text);
        button.setGraphic(Icons.load(forward ? "nav-forward" : "nav-back"));
        if (forward) {
            button.setContentDisplay(ContentDisplay.RIGHT);
        }
        button.setTooltip(new Tooltip(tooltip));
        // One rule for every step: allowed exactly when it lands inside the active range, which excludes its end. While a refused entry is being put
        // back there is briefly no date at all, and no step from it.
        button.disableProperty()
            .bind(Bindings.createBooleanBinding(() -> datePicker.getValue() == null || !Effectivity.activeRange().contains(move.apply(datePicker.getValue())), datePicker.valueProperty(),
                Effectivity.activeRangeProperty()));
        button.setOnAction(evt -> datePicker.setValue(move.apply(datePicker.getValue())));
        return button;
    }

    private String describe(DateRange range) {
        return String.format("[%s to %s]", converter.toString(range.lowerEndpoint()), converter.toString(range.upperEndpoint()));
    }

    /** Reads a typed date as null, rather than throwing, when it cannot be read - so the picker refuses it as it refuses a date outside the range. */
    private static final class RefusingConverter extends StringConverter<LocalDate> {
        private final LocalDateStringConverter delegate;

        RefusingConverter(DateTimeFormatter formatter) {
            this.delegate = new LocalDateStringConverter(formatter, formatter);
        }

        @Override
        public LocalDate fromString(String text) {
            try {
                return delegate.fromString(text);
            } catch (DateTimeParseException e) {
                return null;
            }
        }

        @Override
        public String toString(LocalDate date) {
            return delegate.toString(date);
        }
    }

}
