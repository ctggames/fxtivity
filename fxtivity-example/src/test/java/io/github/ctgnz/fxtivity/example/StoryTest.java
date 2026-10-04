package io.github.ctgnz.fxtivity.example;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.ctgnz.fxtivity.Effectivity;
import io.github.ctgnz.fxtivity.employment.Company;
import io.github.ctgnz.fxtivity.employment.Register;

/** The story the application opens: the saved file is the story, and it reads back to the same thing. */
class StoryTest {

    @BeforeEach
    void init() {
        Effectivity.forDates(AcmeStory.FIRST, AcmeStory.FIRST, AcmeStory.AFTER);
    }

    private static String saved() throws IOException {
        try (InputStream in = ExampleApp.class.getResourceAsStream("acme.yml")) {
            // Line endings as git checked the file out, which on Windows may not be the ones it was written with.
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        }
    }

    /** The committed acme.yml is what the story builds - regenerate it with AcmeStory.main after changing the story. */
    @Test
    void testTheSavedFileIsTheStory() throws IOException {
        assertThat(Yaml.write(AcmeStory.build()), is(saved()));
    }

    @Test
    void testTheSavedFileReadsBackUnchanged() throws IOException {
        Register read = Yaml.read(new ByteArrayInputStream(saved().getBytes(StandardCharsets.UTF_8)));
        assertThat(Yaml.write(read), is(saved()));
    }

    /** The company grows: more people at work in 2020 than in 1995, under five departments' worth of history. */
    @Test
    void testTheCompanyGrows() throws IOException {
        Company acme = Yaml.read(new ByteArrayInputStream(saved().getBytes(StandardCharsets.UTF_8))).companies().getFirst();
        Effectivity.forDate(LocalDate.of(1995, 1, 1));
        int then = acme.employmentsInEffect().size();
        Effectivity.forDate(LocalDate.of(2020, 1, 1));
        int now = acme.employmentsInEffect().size();
        assertThat(now, greaterThan(then * 3));
        assertThat(acme.departments().size(), is(5));
    }

}
