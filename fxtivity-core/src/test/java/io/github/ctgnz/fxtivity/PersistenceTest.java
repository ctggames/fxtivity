package io.github.ctgnz.fxtivity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.github.ctgnz.yamlflock.FlockYamlFactory;

/**
 * A saved model loads into the collections it declared, configured as declared, and a file that breaks their rules fails to load.
 * <p>
 * The {@link Club} declares one of each collection, each configured away from its default, and persists them as {@link EffectiveList#load(java.util.Collection)} prescribes.
 * Loading through a plain field instead would replace each collection with a default one, losing its configuration and, with it, any entry the default rules refuse.
 */
class PersistenceTest {

    /** A club with a succession of captains that may have gaps, members who may overlap, and a succession per committee role, which may have gaps. */
    @JsonPropertyOrder({
        "captains", "members", "committee"
    })
    static final class Club {
        private final SingleEffectiveList<Term> captains = new SingleEffectiveList<>(true);
        private final MultiEffectiveList<Term> members = new MultiEffectiveList<>();
        private final EffectiveMap<String, Term> committee = new EffectiveMap<>(true);
        // The collections as constructed, to check that loading filled these rather than replacing them.
        private final @JsonIgnore List<Object> declared = List.of(captains, members, committee);

        @JsonGetter("captains")
        List<Term> getCaptains() {
            return captains.getSourceList();
        }

        @JsonSetter("captains")
        void setCaptains(List<Term> loaded) {
            captains.load(loaded);
        }

        @JsonGetter("members")
        List<Term> getMembers() {
            return members.getSourceList();
        }

        @JsonSetter("members")
        void setMembers(List<Term> loaded) {
            members.load(loaded);
        }

        @JsonGetter("committee")
        SortedMap<String, ObservableList<Term>> getCommittee() {
            return committee.getSourceMap();
        }

        @JsonSetter("committee")
        void setCommittee(Map<String, List<Term>> loaded) {
            committee.load(loaded);
        }
    }

    private static ObjectMapper mapper() {
        ObjectMapper mapper = new ObjectMapper(FlockYamlFactory.builder().build());
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setDefaultPropertyInclusion(Include.NON_DEFAULT);
        return mapper;
    }

    private static final String CLUB = """
                    captains:
                    - {id: Ana, start: 1940-01-01, end: 1950-01-01}
                    - {id: Ben, start: 1960-01-01, end: 1970-01-01}
                    members:
                    - {id: Cy, start: 1940-01-01, end: 1970-01-01}
                    - {id: Di, start: 1945-01-01, end: 1955-01-01}
                    committee:
                      secretary:
                      - {id: Ed, start: 1940-01-01, end: 1945-01-01}
                      - {id: Fay, start: 1950-01-01, end: 1960-01-01}
                      treasurer:
                      - {id: Gus, start: 1940-01-01, end: 1970-01-01}
                    """;

    @BeforeEach
    void init() {
        Effectivity.forDates(LocalDate.of(1950, 1, 1), LocalDate.of(1900, 1, 1), Effectivity.FOREVER);
    }

    private static List<String> ids(List<Term> terms) {
        return terms.stream().map(Term::id).toList();
    }

    /** Each collection is the one declared, still configured as declared - so the captains' gap and the secretaries' gap load, which a default succession would refuse. */
    @Test
    void testLoadingKeepsTheDeclaredCollections() throws Exception {
        Club club = mapper().readValue(CLUB, Club.class);
        assertThat(club.captains, is(sameInstance(club.declared.get(0))));
        assertThat(club.members, is(sameInstance(club.declared.get(1))));
        assertThat(club.committee, is(sameInstance(club.declared.get(2))));
        assertThat(club.captains.isGapsAllowed(), is(true));
        assertThat(club.committee.isGapsAllowed(), is(true));
        assertThat(ids(club.captains), contains("Ana", "Ben"));
        assertThat(ids(club.members), contains("Cy", "Di"));
        assertThat(ids(club.committee.getRecords("secretary")), contains("Ed", "Fay"));
        assertThat(ids(club.committee.getRecords("treasurer")), contains("Gus"));
        assertThat("the combined view follows a loaded map", club.committee.getEffectiveRecords(), hasSize(3));
    }

    @Test
    void testAClubRoundTrips() throws Exception {
        Club club = mapper().readValue(CLUB, Club.class);
        String written = mapper().writeValueAsString(club);
        Club again = mapper().readValue(written, Club.class);
        assertThat(mapper().writeValueAsString(again), is(written));
        assertThat(again.committee.keySet(), contains("secretary", "treasurer"));
    }

    @Test
    void testABreachInAListFailsTheLoad() {
        String overlapping = CLUB.replace("{id: Ben, start: 1960-01-01", "{id: Ben, start: 1945-01-01");
        JsonMappingException failure = assertThrows(JsonMappingException.class, () -> mapper().readValue(overlapping, Club.class));
        assertThat(failure.getPathReference(), containsString("Club[\"captains\"]"));
        assertThat(failure.getOriginalMessage(), containsString("overlaps"));
    }

    @Test
    void testABreachInAMapFailsTheLoadNamingTheKey() {
        String overlapping = CLUB.replace("{id: Fay, start: 1950-01-01", "{id: Fay, start: 1944-01-01");
        JsonMappingException failure = assertThrows(JsonMappingException.class, () -> mapper().readValue(overlapping, Club.class));
        assertThat(failure.getPathReference(), containsString("Club[\"committee\"]"));
        assertThat(failure.getOriginalMessage(), containsString("secretary"));
    }

    /** Loading replaces the contents in date order, all at once, and announces it as one change. */
    @Test
    void testLoadReplacesTheContents() {
        SingleEffectiveList<Term> list = new SingleEffectiveList<>(List.of(Term.of("old", 1900, 1910)));
        List<ListChangeListener.Change<? extends Term>> fired = new ArrayList<>();
        list.addListener((ListChangeListener<Term>) fired::add);
        Term later = Term.of("later", 1950, 1960);
        Term earlier = Term.of("earlier", 1940, 1950);
        list.load(List.of(later, earlier));
        assertThat(list, contains(earlier, later));
        assertThat(fired, hasSize(1));
    }

    /** A refused load changes nothing, and names the breach. */
    @Test
    void testARefusedLoadChangesNothing() {
        Term old = Term.of("old", 1900, 1910);
        SingleEffectiveList<Term> list = new SingleEffectiveList<>(List.of(old));
        IllegalArgumentException gap = assertThrows(IllegalArgumentException.class, () -> list.load(List.of(Term.of("a", 1940, 1950), Term.of("b", 1955, 1960))));
        assertThat(gap.getMessage(), containsString("Nothing is in effect between"));
        assertThat(list, contains(old));

        EffectiveMap<String, Term> map = new EffectiveMap<>();
        map.put("kept", old);
        Map<String, List<Term>> loaded = Map.of("fine", List.of(Term.of("c", 1940, 1950)), "broken", List.of(Term.of("d", 1940, 1950), Term.of("e", 1945, 1955)));
        assertThrows(IllegalArgumentException.class, () -> map.load(loaded));
        assertThat("no key was changed", map.keySet(), contains("kept"));
        assertThat(map.getRecords("fine"), is(empty()));
    }

}
