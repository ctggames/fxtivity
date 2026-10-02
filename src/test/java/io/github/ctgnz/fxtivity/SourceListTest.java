package io.github.ctgnz.fxtivity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javafx.collections.ListChangeListener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.github.ctgnz.yamlflock.FlockYamlFactory;

/**
 * The full contents of a collection can be read, but every change goes through the collection's own methods, which is where its rules are enforced.
 * <p>
 * {@link EffectiveList#getSourceList()} is the read side: what an editor shows, and what a model typically serialises. Writing through it would skip the checks for overlaps, gaps
 * and order, so it is read-only - and loading a model must therefore still put each element in through the collection, not through the view it was written from.
 */
class SourceListTest {

    /** A collection field persisted as the library prescribes: written from its source list, loaded through {@link EffectiveList#load(java.util.Collection)}. */
    @JsonPropertyOrder({
        "name", "shifts"
    })
    static final class Roster {
        private String name;
        private final SingleEffectiveList<Shift> shifts = new SingleEffectiveList<>();

        Roster() {
        }

        Roster(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @JsonManagedReference
        @JsonGetter("shifts")
        List<Shift> getShifts() {
            return shifts.getSourceList();
        }

        @JsonManagedReference
        @JsonSetter("shifts")
        void setShifts(List<Shift> loaded) {
            shifts.load(loaded);
        }

        SingleEffectiveList<Shift> shifts() {
            return shifts;
        }
    }

    @JsonPropertyOrder({
        "worker", "start", "end"
    })
    static final class Shift implements IEffectiveEntity {
        private @JsonBackReference Roster roster;
        private String worker;
        private LocalDate start;
        private LocalDate end;

        Shift() {
        }

        Shift(String worker, int fromYear, int toYear) {
            this.worker = worker;
            this.start = LocalDate.of(fromYear, 1, 1);
            this.end = LocalDate.of(toYear, 1, 1);
        }

        public String getWorker() {
            return worker;
        }

        @Override
        public LocalDate getStart() {
            return start;
        }

        @Override
        public LocalDate getEnd() {
            return end;
        }

        @Override
        public void setStart(LocalDate startDate) {
            this.start = startDate;
        }

        @Override
        public void setEnd(LocalDate endDate) {
            this.end = endDate;
        }

        Roster roster() {
            return roster;
        }

        @Override
        public String toString() {
            return worker;
        }
    }

    private static ObjectMapper mapper() {
        ObjectMapper mapper = new ObjectMapper(FlockYamlFactory.builder().build());
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setDefaultPropertyInclusion(Include.NON_DEFAULT);
        return mapper;
    }

    @BeforeEach
    void init() {
        Effectivity.forDates(LocalDate.of(1950, 1, 1), LocalDate.of(1900, 1, 1), Effectivity.FOREVER);
    }

    @Test
    void testTheSourceListCannotBeWrittenTo() {
        SingleEffectiveList<Term> list = new SingleEffectiveList<>(List.of(Term.of("a", 1940, 1960)));
        Term overlapping = Term.of("b", 1950, 1970);
        assertThrows(UnsupportedOperationException.class, () -> list.getSourceList().add(overlapping));
        assertThrows(UnsupportedOperationException.class, () -> list.getSourceList().remove(0));
        assertThrows(UnsupportedOperationException.class, () -> list.getSourceList().clear());
        assertThat("nothing got past the collection's rules", list.size(), is(1));
    }

    /** Read-only, but still live: a change made through the collection shows in the view, and is announced on it. */
    @Test
    void testTheSourceListFollowsTheCollection() {
        SingleEffectiveList<Term> list = new SingleEffectiveList<>(true);
        List<Term> announced = new ArrayList<>();
        list.getSourceList().addListener((ListChangeListener<Term>) change -> {
            while (change.next()) {
                announced.addAll(change.getAddedSubList());
            }
        });
        Term a = Term.of("a", 1940, 1960);
        list.add(a);
        assertThat(list.getSourceList(), contains(a));
        assertThat(announced, contains(a));
    }

    @Test
    void testARosterRoundTrips() throws Exception {
        Roster roster = new Roster("Night");
        roster.shifts().add(new Shift("Ana", 1940, 1950));
        roster.shifts().add(new Shift("Ben", 1950, 1960));

        Roster read = mapper().readValue(mapper().writeValueAsString(roster), Roster.class);

        assertThat(read.shifts(), hasSize(2));
        assertThat(read.shifts().get(0).getWorker(), is("Ana"));
        assertThat(read.shifts().get(1).getWorker(), is("Ben"));
        assertThat("the back reference is restored", read.shifts().get(0).roster(), is(sameInstance(read)));
    }

    /** The second shift overlaps the first. Loading goes through the collection's rules, and a breach fails the load - naming where - rather than leaving a shift out. */
    @Test
    void testLoadingGoesThroughTheCollectionsRules() {
        String yaml = """
                        name: Night
                        shifts:
                        - {worker: Ana, start: 1940-01-01, end: 1960-01-01}
                        - {worker: Ben, start: 1950-01-01, end: 1970-01-01}
                        """;
        JsonMappingException failure = assertThrows(JsonMappingException.class, () -> mapper().readValue(yaml, Roster.class));
        assertThat(failure.getPathReference(), containsString("Roster[\"shifts\"]"));
        assertThat(failure.getOriginalMessage(), containsString("Ana"));
        assertThat(failure.getOriginalMessage(), containsString("overlaps"));
    }

}
