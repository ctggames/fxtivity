package io.github.ctgnz.fxtivity.example;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.github.ctgnz.fxtivity.employment.Register;
import io.github.ctgnz.yamlflock.FlockYamlFactory;

/** Reading and writing a {@link Register} as YAML, with yaml-flock's layout: one change per line, so that a single change shows as a single changed line in a diff. */
public final class Yaml {

    private Yaml() {
    }

    /** The mapper the example reads and writes with. */
    public static ObjectMapper mapper() {
        ObjectMapper mapper = new ObjectMapper(FlockYamlFactory.builder().build());
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setDefaultPropertyInclusion(Include.NON_DEFAULT);
        return mapper;
    }

    /** The register written as YAML. */
    public static String write(Register register) throws IOException {
        return mapper().writeValueAsString(register);
    }

    /** Writes the register to {@code file}. */
    public static void write(Register register, Path file) throws IOException {
        Files.writeString(file, write(register));
    }

    /** Reads a register, failing - with the path to the entry - if anything in it breaks the rules of the collection it belongs to. */
    public static Register read(InputStream in) throws IOException {
        return mapper().readValue(in, Register.class);
    }

    /** Reads a register from {@code file}. */
    public static Register read(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            return read(in);
        }
    }

}
