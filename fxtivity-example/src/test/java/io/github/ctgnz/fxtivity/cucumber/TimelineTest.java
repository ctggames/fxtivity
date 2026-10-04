package io.github.ctgnz.fxtivity.cucumber;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import io.github.ctgnz.fxtivity.Effectivity;

/** The pictures in the report: lanes for overlaps, open ends at the edge, the effective date marked, and nothing drawn outside the picture. */
class TimelineTest {

    private static Timeline.Bar bar(String label, int from, int to) {
        return new Timeline.Bar(label, LocalDate.of(from, 1, 1), to == 0 ? Effectivity.FOREVER : LocalDate.of(to, 1, 1));
    }

    private static int count(String svg, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(svg);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static int height(String svg) {
        Matcher matcher = Pattern.compile("height=\"(\\d+)\"").matcher(svg);
        matcher.find();
        return Integer.parseInt(matcher.group(1));
    }

    /** Bars that overlap take a lane each, so the track is taller; a hand-over - one ending as the next begins - shares a lane. */
    @Test
    void testOverlapsTakeLanesOfTheirOwn() {
        String handOver = new Timeline().track("t", List.of(bar("a", 1990, 1995), bar("b", 1995, 2000))).svg(LocalDate.of(1992, 1, 1));
        String overlap = new Timeline().track("t", List.of(bar("a", 1990, 1996), bar("b", 1995, 2000))).svg(LocalDate.of(1992, 1, 1));
        assertThat(height(overlap) > height(handOver), is(true));
    }

    /** A period with no end runs to the edge of the chart and is marked as running on, rather than stretching the scale to the year 9999. */
    @Test
    void testAnOpenEndedPeriod() {
        String svg = new Timeline().track("t", List.of(bar("a", 1990, 0))).svg(LocalDate.of(1992, 1, 1));
        assertThat(svg, containsString("until further notice"));
        assertThat(svg, containsString("<path"));
        assertThat("no axis tick anywhere near 9999", svg, not(containsString(">9999<")));
    }

    @Test
    void testTheEffectiveDateIsMarked() {
        String svg = new Timeline().track("t", List.of(bar("a", 1990, 1995))).svg(LocalDate.of(1992, 6, 1));
        assertThat(svg, containsString("effective date 1992-06-01"));
        assertThat(count(svg, "stroke-dasharray"), is(1));
    }

    @Test
    void testLabelsAreEscaped() {
        String svg = new Timeline().track("A & B <ltd>", List.of(bar("\"quoted\"", 1990, 1995))).svg(LocalDate.of(1992, 1, 1));
        assertThat(svg, containsString("A &amp; B &lt;ltd&gt;"));
        assertThat(svg, not(containsString("<ltd>")));
    }

    /** Every label stays inside the picture, wherever its bar is - including a short one starting at the right-hand edge. */
    @Test
    void testNothingIsDrawnOutsideThePicture() {
        String svg = new Timeline().track("t", List.of(bar("India Inc", 1990, 2000), bar("Peninsula Incorporated Limited", 2000, 0))).svg(LocalDate.of(1995, 1, 1));
        Matcher width = Pattern.compile("width=\"(\\d+)\"").matcher(svg);
        width.find();
        int right = Integer.parseInt(width.group(1));
        Matcher text = Pattern.compile("<text x=\"([\\d.]+)\"[^>]*text-anchor=\"start\">([^<]*)</text>").matcher(svg);
        while (text.find()) {
            assertThat(text.group(2), Double.parseDouble(text.group(1)) + text.group(2).length() * 5.6, lessThanOrEqualTo((double) right));
        }
    }

}
