package io.github.ctgnz.fxtivity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.github.ctgnz.yamlflock.FlockYamlFactory;

/**
 * Elements with the same dates are held in an order of the list's choosing, so the order is a property of the data rather than of the order things were added in.
 * <p>
 * Ann, Bob and Cat all hold the same period; Dan starts later.
 */
class TieBreakTest {

    private static final Comparator<Term> BY_ID = Comparator.comparing(Term::id);

    private Term ann;
    private Term bob;
    private Term cat;
    private Term dan;

    @BeforeEach
    void init() {
        Effectivity.forDates(LocalDate.of(1950, 1, 1), LocalDate.of(1900, 1, 1), Effectivity.FOREVER);
        ann = Term.of("ann", 1940, 1960);
        bob = Term.of("bob", 1940, 1960);
        cat = Term.of("cat", 1940, 1960);
        dan = Term.of("dan", 1945, 1960);
    }

    /** Dates first, always: the tie-break only orders elements with the same dates. */
    @Test
    void testTiesAreOrderedWhateverTheOrderAdded() {
        MultiEffectiveList<Term> list = new MultiEffectiveList<>(BY_ID);
        list.add(dan);
        list.add(cat);
        list.add(ann);
        list.add(bob);
        assertThat(list, contains(ann, bob, cat, dan));

        MultiEffectiveList<Term> all = new MultiEffectiveList<>(BY_ID);
        all.addAll(dan, cat, bob, ann);
        assertThat(all, contains(ann, bob, cat, dan));
    }

    @Test
    void testEveryWayOfFillingAList() {
        assertThat(new MultiEffectiveList<>(List.of(cat, dan, bob, ann), BY_ID), contains(ann, bob, cat, dan));

        MultiEffectiveList<Term> set = new MultiEffectiveList<>(BY_ID);
        set.setAll(cat, ann, dan, bob);
        assertThat(set, contains(ann, bob, cat, dan));

        MultiEffectiveList<Term> loaded = new MultiEffectiveList<>(BY_ID);
        loaded.load(List.of(bob, dan, cat, ann));
        assertThat(loaded, contains(ann, bob, cat, dan));
    }

    /** Rescheduled into a tie, an element takes its place among the others by the tie-break. */
    @Test
    void testRescheduling() {
        MultiEffectiveList<Term> list = new MultiEffectiveList<>(List.of(bob, dan), BY_ID);
        Term aaron = Term.of("aaron", 1930, 1940);
        list.add(aaron);
        list.reschedule(aaron, LocalDate.of(1940, 1, 1), LocalDate.of(1960, 1, 1));
        assertThat(list, contains(aaron, bob, dan));
    }

    /** Without a tie-break, elements with the same dates keep the order they were added in. */
    @Test
    void testWithoutATieBreak() {
        MultiEffectiveList<Term> list = new MultiEffectiveList<>();
        list.addAll(cat, ann, bob);
        assertThat(list, contains(cat, ann, bob));
    }

    /** A map's combined view orders records with the same dates by key, whatever order they were put in. */
    @Test
    void testAMapsCombinedViewTiesByKey() {
        EffectiveMap<String, Term> map = new EffectiveMap<>(true);
        map.put("treasurer", cat);
        map.put("chair", ann);
        map.put("secretary", bob);
        assertThat(map.getEffectiveRecords(), contains(ann, bob, cat));

        EffectiveMap<String, Term> reversed = new EffectiveMap<>(true, Comparator.<String> reverseOrder());
        reversed.put("chair", ann);
        reversed.put("treasurer", cat);
        reversed.put("secretary", bob);
        assertThat("in the map's own key order", reversed.getEffectiveRecords(), contains(cat, bob, ann));
    }

    /** A model with a tie-break is written the same however its entries came to be added - the reason for having one. */
    @Test
    void testTheSavedOrderIsAFunctionOfTheData() throws Exception {
        ObjectMapper mapper = new ObjectMapper(FlockYamlFactory.builder().build());
        mapper.registerModule(new JavaTimeModule());
        Members one = new Members();
        one.members.addAll(cat, ann, bob);
        init();
        Members other = new Members();
        other.members.addAll(bob, cat, ann);
        assertThat(mapper.writeValueAsString(other), is(mapper.writeValueAsString(one)));
    }

    static final class Members {
        private final MultiEffectiveList<Term> members = new MultiEffectiveList<>(BY_ID);

        @JsonGetter("members")
        List<Term> getMembers() {
            return members.getSourceList();
        }

        @JsonSetter("members")
        void setMembers(List<Term> loaded) {
            members.load(loaded);
        }
    }

}
