package io.github.ctgnz.fxtivity.cucumber;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import io.github.ctgnz.fxtivity.Effective;
import io.github.ctgnz.fxtivity.Effectivity;

/**
 * A picture of how a scenario's periods line up: one row per track, a bar per period, and the effective date as a line across them all - drawn as SVG for the published report.
 * <p>
 * Each track is something with a history - a person's employments, a seat on the board, the departments of a company. Bars that overlap within a track take a lane each. The scale
 * is fitted to the dates in the picture, whatever their span; a period with no end runs off the right edge rather than stretching the scale to the year 9999. A bar in effect on
 * the effective date is drawn solid, and every other one pale.
 */
final class Timeline {

    private static final int LABEL_WIDTH = 190;
    private static final int CHART_WIDTH = 620;
    private static final int RIGHT_MARGIN = 20;
    private static final int AXIS_HEIGHT = 34;
    private static final int LANE_HEIGHT = 20;
    private static final int TRACK_GAP = 6;
    private static final double CHAR_WIDTH = 5.6;

    private static final String BACKGROUND = "#fcfcfa";
    private static final String TEXT = "#22211e";
    private static final String MUTED = "#6b6860";
    private static final String RULE = "#e2dfd7";
    private static final String IN_EFFECT = "#7a5c2e";
    private static final String OUT_OF_EFFECT = "#e6dccb";
    private static final String EFFECTIVE_DATE = "#b23a2e";

    /** A labelled period. */
    record Bar(String label, LocalDate start, LocalDate end) {
        boolean openEnded() {
            return end == null || !end.isBefore(Effectivity.FOREVER);
        }
    }

    private record Track(String name, List<List<Bar>> lanes) {
    }

    private final List<Track> tracks = new ArrayList<>();

    /** A bar for {@code period}, labelled {@code label}. */
    static Bar bar(String label, Effective period) {
        return new Bar(label, period.getStart(), period.getEnd());
    }

    /** A bar for {@code period}, labelled {@code label}. */
    static Bar bar(String label, Effectivity period) {
        return new Bar(label, period.getStart(), period.getEnd());
    }

    /** Adds a track of {@code bars}, if there are any, laying overlapping ones out in lanes of their own. */
    Timeline track(String name, List<Bar> bars) {
        if (bars.isEmpty()) {
            return this;
        }
        List<List<Bar>> lanes = new ArrayList<>();
        for (Bar bar : bars.stream().sorted(Comparator.comparing(Bar::start)).toList()) {
            List<Bar> lane = lanes.stream().filter(candidate -> !end(candidate.getLast()).isAfter(bar.start())).findFirst().orElse(null);
            if (lane == null) {
                lane = new ArrayList<>();
                lanes.add(lane);
            }
            lane.add(bar);
        }
        tracks.add(new Track(name, lanes));
        return this;
    }

    boolean isEmpty() {
        return tracks.isEmpty();
    }

    /** The picture, with the effective date marked. */
    String svg(LocalDate effectiveDate) {
        LocalDate first = effectiveDate;
        LocalDate last = effectiveDate;
        boolean anyOpen = false;
        for (Track track : tracks) {
            for (List<Bar> lane : track.lanes()) {
                for (Bar bar : lane) {
                    first = Effectivity.earlier(first, bar.start());
                    last = Effectivity.later(last, bar.openEnded() ? bar.start() : bar.end());
                    anyOpen |= bar.openEnded();
                }
            }
        }
        long span = Math.max(30, ChronoUnit.DAYS.between(first, last));
        // Room on the right for what has no end to be seen running on, and a little either side.
        LocalDate from = first.minusDays(span / 30);
        LocalDate to = last.plusDays(span / (anyOpen ? 4 : 30));
        Step step = Step.fitting(from, to);
        from = step.floor(from);
        to = step.ceiling(to);
        Scale scale = new Scale(from, to);

        int lanes = tracks.stream().mapToInt(track -> track.lanes().size()).sum();
        int height = AXIS_HEIGHT + lanes * LANE_HEIGHT + tracks.size() * TRACK_GAP + 22;
        int width = LABEL_WIDTH + CHART_WIDTH + RIGHT_MARGIN;
        StringBuilder svg = new StringBuilder();
        svg.append(String.format(Locale.ROOT, "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 %d %d\" font-family=\"sans-serif\" font-size=\"11\">%n", width,
            height, width, height));
        svg.append(String.format(Locale.ROOT, "<rect width=\"%d\" height=\"%d\" fill=\"%s\"/>%n", width, height, BACKGROUND));

        for (LocalDate tick = from; !tick.isAfter(to); tick = step.next(tick)) {
            double x = scale.x(tick);
            svg.append(String.format(Locale.ROOT, "<line x1=\"%.1f\" y1=\"%d\" x2=\"%.1f\" y2=\"%d\" stroke=\"%s\"/>%n", x, AXIS_HEIGHT - 6, x, height - 18, RULE));
            svg.append(String.format(Locale.ROOT, "<text x=\"%.1f\" y=\"%d\" fill=\"%s\" text-anchor=\"middle\">%s</text>%n", x, AXIS_HEIGHT - 12, MUTED, step.label(tick)));
        }

        int y = AXIS_HEIGHT;
        for (Track track : tracks) {
            svg.append(String.format(Locale.ROOT, "<text x=\"8\" y=\"%d\" fill=\"%s\">%s</text>%n", y + 14, TEXT, escape(track.name())));
            for (List<Bar> lane : track.lanes()) {
                for (int i = 0; i < lane.size(); i++) {
                    // The room either side of the bar, up to its neighbours in the lane, is where its label may go if it does not fit inside.
                    double after = i + 1 < lane.size() ? scale.x(lane.get(i + 1).start()) : LABEL_WIDTH + CHART_WIDTH + RIGHT_MARGIN;
                    double before = i > 0 ? scale.x(end(lane.get(i - 1))) : LABEL_WIDTH;
                    drawBar(svg, scale, lane.get(i), y, effectiveDate, before, after);
                }
                y += LANE_HEIGHT;
            }
            svg.append(String.format(Locale.ROOT, "<line x1=\"0\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"%s\"/>%n", y + TRACK_GAP / 2, width, y + TRACK_GAP / 2, RULE));
            y += TRACK_GAP;
        }

        double now = scale.x(effectiveDate);
        svg.append(String.format(Locale.ROOT, "<line x1=\"%.1f\" y1=\"%d\" x2=\"%.1f\" y2=\"%d\" stroke=\"%s\" stroke-width=\"2\" stroke-dasharray=\"4 3\"/>%n", now, AXIS_HEIGHT - 4, now, y,
            EFFECTIVE_DATE));
        svg.append(
            String.format(Locale.ROOT, "<text x=\"%.1f\" y=\"%d\" fill=\"%s\" text-anchor=\"middle\">effective date %s</text>%n", Math.min(now, width - 90.0), y + 14, EFFECTIVE_DATE, effectiveDate));
        svg.append("</svg>\n");
        return svg.toString();
    }

    private static void drawBar(StringBuilder svg, Scale scale, Bar bar, int y, LocalDate effectiveDate, double roomBefore, double roomAfter) {
        double x1 = scale.x(bar.start());
        double x2 = bar.openEnded() ? LABEL_WIDTH + CHART_WIDTH : scale.x(bar.end());
        double w = Math.max(2, x2 - x1);
        boolean inEffect = !effectiveDate.isBefore(bar.start()) && (bar.openEnded() || effectiveDate.isBefore(bar.end()));
        String fill = inEffect ? IN_EFFECT : OUT_OF_EFFECT;
        // Outlined in the background colour, so that one period handing over to the next shows as two bars meeting rather than one.
        svg.append(String.format(Locale.ROOT, "<rect x=\"%.1f\" y=\"%d\" width=\"%.1f\" height=\"%d\" rx=\"3\" fill=\"%s\" stroke=\"%s\" stroke-width=\"1.5\"><title>%s</title></rect>%n", x1, y + 3, w,
            LANE_HEIGHT - 6, fill, BACKGROUND, escape(bar.label() + ": " + bar.start() + (bar.openEnded() ? " until further notice" : " up to " + bar.end()))));
        if (bar.openEnded()) {
            // A notched right end: this runs on.
            svg.append(String.format(Locale.ROOT, "<path d=\"M%.1f %d l6 %d l-6 %d z\" fill=\"%s\"/>%n", x2, y + 3, (LANE_HEIGHT - 6) / 2, (LANE_HEIGHT - 6) / 2, fill));
        }
        String label = bar.label();
        double textWidth = label.length() * CHAR_WIDTH;
        String inside = inEffect ? BACKGROUND : TEXT;
        if (textWidth + 8 <= w) {
            svg.append(text(x1 + 4, y, inside, "start", label));
        } else if (x1 + w + 4 + textWidth <= roomAfter) {
            svg.append(text(x1 + w + 4, y, TEXT, "start", label));
        } else if (x1 - 4 - textWidth >= roomBefore) {
            svg.append(text(x1 - 4, y, TEXT, "end", label));
        } else {
            // Nowhere for all of it: as much as fits inside, and the whole of it in the bar's tooltip.
            int fits = (int) ((w - 8) / CHAR_WIDTH) - 1;
            if (fits >= 2) {
                svg.append(text(x1 + 4, y, inside, "start", label.substring(0, Math.min(fits, label.length())) + "…"));
            }
        }
    }

    private static String text(double x, int y, String fill, String anchor, String text) {
        return String.format(Locale.ROOT, "<text x=\"%.1f\" y=\"%d\" fill=\"%s\" text-anchor=\"%s\">%s</text>%n", x, y + 14, fill, anchor, escape(text));
    }

    private static LocalDate end(Bar bar) {
        return bar.openEnded() ? Effectivity.FOREVER : bar.end();
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    /** Dates to x, across the chart. */
    private record Scale(LocalDate from, LocalDate to) {
        double x(LocalDate date) {
            double days = ChronoUnit.DAYS.between(from, to);
            double at = ChronoUnit.DAYS.between(from, date);
            return LABEL_WIDTH + Math.max(0, Math.min(1, at / days)) * CHART_WIDTH;
        }
    }

    /** The spacing of the axis's ticks: the finest that gives no more than a dozen. */
    private record Step(int months) {
        static Step fitting(LocalDate from, LocalDate to) {
            long months = ChronoUnit.MONTHS.between(from, to) + 1;
            for (int step : new int[] {
                1, 3, 6, 12, 24, 60, 120, 240, 600
            }) {
                if (months / step <= 12) {
                    return new Step(step);
                }
            }
            return new Step(1200);
        }

        LocalDate floor(LocalDate date) {
            if (months < 12) {
                return date.withDayOfMonth(1).minusMonths((date.getMonthValue() - 1) % months);
            }
            int years = months / 12;
            return LocalDate.of(Math.floorDiv(date.getYear(), years) * years, 1, 1);
        }

        LocalDate ceiling(LocalDate date) {
            LocalDate floor = floor(date);
            return floor.equals(date) ? date : next(floor);
        }

        LocalDate next(LocalDate tick) {
            return tick.plusMonths(months);
        }

        String label(LocalDate tick) {
            return months < 12 ? tick.toString().substring(0, 7) : String.valueOf(tick.getYear());
        }
    }

}
