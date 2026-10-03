package io.github.ctgnz.fxtivity.control;

import java.time.LocalDate;
import java.util.Objects;
import java.util.function.Supplier;

import javafx.css.PseudoClass;
import javafx.scene.control.DateCell;

import io.github.ctgnz.fxtivity.DateRange;
import io.github.ctgnz.fxtivity.Effectivity;

/**
 * A day in a {@link javafx.scene.control.DatePicker} that can only be chosen inside a range - by default the {@linkplain Effectivity#activeRange() active range}.
 * <p>
 * A day outside the range is disabled, so a picker using this cell cannot produce a date outside it at all. That is how an editor keeps the dates it enters within the active range
 * without the collections having to check: use it in any picker an editor shows, as {@code picker.setDayCellFactory(p -> new EffectiveDateCell())}.
 * <p>
 * Styled with two pseudo-classes: {@code :inactive} on a day outside the range, and {@code :selected} on the selected date.
 */
public class EffectiveDateCell extends DateCell {
    private final PseudoClass inactive = PseudoClass.getPseudoClass("inactive");
    private final PseudoClass selected = PseudoClass.getPseudoClass("selected");
    private final Supplier<LocalDate> selectedDate;
    private final DateRange activeRange;

    /** A day that can be chosen within the active range as it is now, with the effective date selected. */
    public EffectiveDateCell() {
        this(Effectivity::when, Effectivity.activeRange());
    }

    /**
     * A day that can be chosen within {@code activeRange}.
     *
     * @param selectedDate
     *            the date to show as selected, or null for the effective date
     * @param activeRange
     *            the days that can be chosen
     */
    public EffectiveDateCell(Supplier<LocalDate> selectedDate, DateRange activeRange) {
        this.selectedDate = Objects.requireNonNullElse(selectedDate, Effectivity::when);
        this.activeRange = Objects.requireNonNull(activeRange);
    }

    @Override
    public void updateItem(LocalDate item, boolean empty) {
        super.updateItem(item, empty);
        if (item == null || empty) {
            setDisable(true);
            pseudoClassStateChanged(inactive, true);
            pseudoClassStateChanged(selected, false);
        } else {
            setDisable(!activeRange.contains(item));
            pseudoClassStateChanged(inactive, !activeRange.contains(item));
            pseudoClassStateChanged(selected, item.equals(selectedDate.get()));
        }
    }

}
