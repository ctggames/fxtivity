package io.github.ctgnz.fxtivity.control;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.lang.ref.WeakReference;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.ctgnz.fxtivity.Effectivity;
import io.github.ctgnz.fxtivity.control.EffectiveDatePicker.Format;

/**
 * The picker sets the effective date and follows it, and never sets a date outside the active range - whichever way the date is chosen.
 * <p>
 * The active range is 1900-01-01 up to but excluding 2000-01-01.
 */
class EffectiveDatePickerTest {

    private static final LocalDate START = LocalDate.of(1900, 1, 1);
    private static final LocalDate END = LocalDate.of(2000, 1, 1);

    @BeforeEach
    void init() {
        Effectivity.forDates(LocalDate.of(1950, 6, 15), START, END);
    }

    private static DatePicker datePicker(EffectiveDatePicker picker) {
        return (DatePicker) picker.getChildren().stream().filter(DatePicker.class::isInstance).findFirst().orElseThrow();
    }

    private static Map<String, Button> buttons(EffectiveDatePicker picker) {
        return picker.getChildren().stream().filter(Button.class::isInstance).map(Button.class::cast).collect(Collectors.toMap(button -> button.getTooltip().getText(), button -> button));
    }

    /** Usable as soon as it is built - no container has to finish setting it up. */
    @Test
    void testReadyOnceBuilt() {
        FxThread.run(() -> {
            EffectiveDatePicker picker = new EffectiveDatePicker(Format.ISO_DATE);
            assertThat(datePicker(picker).isDisabled(), is(false));
            assertThat(datePicker(picker).getValue(), is(LocalDate.of(1950, 6, 15)));
        });
    }

    @Test
    void testChoosingADateMovesTheEffectiveDate() {
        FxThread.run(() -> {
            EffectiveDatePicker picker = new EffectiveDatePicker(Format.ISO_DATE);
            datePicker(picker).setValue(LocalDate.of(1960, 1, 1));
            assertThat(Effectivity.when(), is(LocalDate.of(1960, 1, 1)));
            buttons(picker).get("Next Year").fire();
            assertThat(Effectivity.when(), is(LocalDate.of(1961, 1, 1)));
        });
    }

    @Test
    void testFollowsTheEffectiveDateMovedElsewhere() {
        FxThread.run(() -> {
            EffectiveDatePicker picker = new EffectiveDatePicker(Format.ISO_DATE);
            Effectivity.forDate(LocalDate.of(1975, 3, 1));
            assertThat(datePicker(picker).getValue(), is(LocalDate.of(1975, 3, 1)));
        });
    }

    /** A typed date outside the range, or one that cannot be read, is refused: no exception, the date unmoved, the picker showing it again. */
    @Test
    void testARefusedDate() {
        FxThread.run(() -> {
            EffectiveDatePicker picker = new EffectiveDatePicker(Format.ISO_DATE);
            datePicker(picker).setValue(LocalDate.of(2050, 1, 1));
            assertThat(Effectivity.when(), is(LocalDate.of(1950, 6, 15)));
            assertThat(datePicker(picker).getValue(), is(LocalDate.of(1950, 6, 15)));
            datePicker(picker).setValue(null);
            assertThat(datePicker(picker).getValue(), is(LocalDate.of(1950, 6, 15)));
            assertThat(datePicker(picker).getConverter().fromString("not a date"), is(nullValue()));
        });
    }

    /** Each step is allowed exactly when it lands inside the range - which includes its first day, and excludes the day it ends. */
    @Test
    void testTheStepsStopAtTheEdgesOfTheRange() {
        FxThread.run(() -> {
            EffectiveDatePicker picker = new EffectiveDatePicker(Format.ISO_DATE);
            Map<String, Button> buttons = buttons(picker);

            Effectivity.forDate(LocalDate.of(1900, 1, 2));
            assertThat("back a day to the first day", buttons.get("Previous Day").isDisabled(), is(false));
            Effectivity.forDate(START);
            assertThat(buttons.get("Previous Day").isDisabled(), is(true));
            assertThat(buttons.get("Previous Month").isDisabled(), is(true));
            assertThat(buttons.get("Previous Year").isDisabled(), is(true));

            Effectivity.forDate(LocalDate.of(1999, 12, 30));
            assertThat("forward a day to the last day", buttons.get("Next Day").isDisabled(), is(false));
            Effectivity.forDate(LocalDate.of(1999, 12, 1));
            assertThat("forward a month to the day the range ends", buttons.get("Next Month").isDisabled(), is(true));
            Effectivity.forDate(LocalDate.of(1999, 1, 1));
            assertThat("forward a year to the day the range ends", buttons.get("Next Year").isDisabled(), is(true));
            assertThat(buttons.get("Next Month").isDisabled(), is(false));
        });
    }

    /** A new active range shows at once, in the label and in the steps, without the date having to move. */
    @Test
    void testFollowsTheActiveRange() {
        FxThread.run(() -> {
            EffectiveDatePicker picker = new EffectiveDatePicker(Format.ISO_DATE);
            Label range = (Label) picker.getChildren().getLast();
            assertThat(range.getText(), is("[1900-01-01 to 2000-01-01]"));
            assertThat(buttons(picker).get("Next Year").isDisabled(), is(false));
            Effectivity.forDates(LocalDate.of(1950, 6, 15), START, LocalDate.of(1951, 1, 1));
            assertThat(range.getText(), is("[1900-01-01 to 1951-01-01]"));
            assertThat(buttons(picker).get("Next Year").isDisabled(), is(true));
        });
    }

    /** The steps a format offers: a day step only where days are shown, a month step only where months are. */
    @Test
    void testTheStepsFollowTheFormat() {
        FxThread.run(() -> {
            assertThat(buttons(new EffectiveDatePicker(Format.YEAR_ERA)).keySet().size(), is(2));
            assertThat(buttons(new EffectiveDatePicker(Format.MONTH_YEAR)).keySet().size(), is(4));
            assertThat(buttons(new EffectiveDatePicker(Format.DAY_MONTH_YEAR)).keySet().size(), is(6));
        });
    }

    /** A picker nothing refers to can be collected: the effective date, which lives as long as the application, holds it only weakly. */
    @Test
    void testADiscardedPickerIsCollected() {
        WeakReference<EffectiveDatePicker> reference = FxThread.call(() -> new WeakReference<>(new EffectiveDatePicker(Format.ISO_DATE)));
        for (int i = 0; i < 20 && reference.get() != null; i++) {
            System.gc();
            int year = 1951 + i;
            FxThread.run(() -> Effectivity.forDate(LocalDate.of(year, 1, 1)));
        }
        assertThat(reference.get(), is(nullValue()));
    }

}
