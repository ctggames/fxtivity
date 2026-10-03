package io.github.ctgnz.fxtivity.control;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import javafx.scene.layout.Region;
import javafx.scene.shape.SVGPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.ctgnz.fxtivity.DateRange;
import io.github.ctgnz.fxtivity.Effective;
import io.github.ctgnz.fxtivity.EffectiveProperty;
import io.github.ctgnz.fxtivity.Effectivity;

/** The date cell, the property pane, and the icons. */
class ControlsTest {

    @BeforeEach
    void init() {
        Effectivity.forDates(LocalDate.of(1950, 6, 15), LocalDate.of(1900, 1, 1), LocalDate.of(2000, 1, 1));
    }

    /** A day outside the range cannot be chosen - including the day the range ends, which it excludes. */
    @Test
    void testADateCellOnlyOffersDaysInTheRange() {
        FxThread.run(() -> {
            EffectiveDateCell cell = new EffectiveDateCell();
            cell.updateItem(LocalDate.of(1900, 1, 1), false);
            assertThat(cell.isDisabled(), is(false));
            cell.updateItem(LocalDate.of(1999, 12, 31), false);
            assertThat(cell.isDisabled(), is(false));
            cell.updateItem(LocalDate.of(2000, 1, 1), false);
            assertThat(cell.isDisabled(), is(true));
            cell.updateItem(LocalDate.of(1899, 12, 31), false);
            assertThat(cell.isDisabled(), is(true));
            cell.updateItem(null, true);
            assertThat(cell.isDisabled(), is(true));
        });
    }

    @Test
    void testADateCellWithItsOwnRange() {
        FxThread.run(() -> {
            EffectiveDateCell cell = new EffectiveDateCell(null, DateRange.closedOpen(LocalDate.of(1960, 1, 1), LocalDate.of(1970, 1, 1)));
            cell.updateItem(LocalDate.of(1950, 6, 15), false);
            assertThat("the effective date, outside this cell's range", cell.isDisabled(), is(true));
            cell.updateItem(LocalDate.of(1965, 1, 1), false);
            assertThat(cell.isDisabled(), is(false));
        });
    }

    @Test
    void testThePropertyPaneShowsTheChanges() {
        FxThread.run(() -> {
            Owner owner = new Owner();
            EffectiveProperty<String> rank = new EffectiveProperty<>(owner);
            rank.setValue(LocalDate.of(1940, 1, 1), "Junior");
            EffectivePropertyPane<String> pane = new EffectivePropertyPane<>(String::toUpperCase);
            pane.setProperty(rank);
            assertThat(pane.view.getItems().stream().map(entry -> pane.displayFunction.apply(entry.getValue())).toList(), contains("JUNIOR"));

            rank.setValue(LocalDate.of(1950, 1, 1), "Senior");
            rank.setValue(LocalDate.of(1945, 1, 1), "Middle");
            assertThat("follows the property, in date order", pane.view.getItems().stream().map(entry -> entry.getValue()).toList(), contains("Junior", "Middle", "Senior"));
            rank.remove(LocalDate.of(1945, 1, 1));
            assertThat(pane.view.getItems().stream().map(entry -> entry.getValue()).toList(), contains("Junior", "Senior"));
            assertThrows(UnsupportedOperationException.class, () -> pane.view.getItems().clear());

            pane.setProperty(null);
            assertThat(pane.view.getItems(), is(empty()));
        });
    }

    @Test
    void testAnIconIsItsPath() {
        FxThread.run(() -> {
            Region icon = Icons.load("nav-back");
            assertThat(icon.getShape(), instanceOf(SVGPath.class));
            assertThat(((SVGPath) icon.getShape()).getContent(), is("M11 2 L4 8 L11 14 Z"));
            assertThat(icon.getPrefWidth(), is(Icons.SIZE));
            assertThat(Icons.load("nav-forward").getShape(), instanceOf(SVGPath.class));
        });
    }

    @Test
    void testAMissingIcon() {
        assertThrows(IllegalArgumentException.class, () -> Icons.load("no-such-icon"));
    }

    private static final class Owner implements Effective {
        @Override
        public LocalDate getStart() {
            return LocalDate.of(1930, 1, 1);
        }

        @Override
        public LocalDate getEnd() {
            return Effectivity.FOREVER;
        }

        @Override
        public void setStart(LocalDate startDate) {
        }

        @Override
        public void setEnd(LocalDate endDate) {
        }
    }

}
