package io.github.ctgnz.fxtivity;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * A plain effective entity for the unit tests: an identifier and a period, and nothing else.
 * <p>
 * Equality is identity, deliberately. Several tests hold two terms with the same dates and need them to remain two terms, and others assert that a list returns the very element
 * that was put in.
 */
@JsonPropertyOrder({
    "id", "start", "end"
})
final class Term implements Effective {

    private final String id;
    private LocalDate start;
    private LocalDate end;

    @JsonCreator
    Term(@JsonProperty("id") String id, @JsonProperty("start") LocalDate start, @JsonProperty("end") LocalDate end) {
        this.id = id;
        this.start = start;
        this.end = end;
    }

    /** A term running from 1 January of {@code fromYear} up to but excluding 1 January of {@code toYear}. */
    static Term of(String id, int fromYear, int toYear) {
        return new Term(id, LocalDate.of(fromYear, 1, 1), LocalDate.of(toYear, 1, 1));
    }

    @JsonProperty("id")
    String id() {
        return id;
    }

    @Override
    public LocalDate getEnd() {
        return end;
    }

    @Override
    public LocalDate getStart() {
        return start;
    }

    @Override
    public void setEnd(LocalDate endDate) {
        this.end = endDate;
    }

    @Override
    public void setStart(LocalDate startDate) {
        this.start = startDate;
    }

    @Override
    public String toString() {
        return id + " " + start + ".." + end;
    }

}
