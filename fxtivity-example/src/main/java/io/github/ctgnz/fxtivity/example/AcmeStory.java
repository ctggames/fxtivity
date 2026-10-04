package io.github.ctgnz.fxtivity.example;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

import io.github.ctgnz.fxtivity.Effectivity;
import io.github.ctgnz.fxtivity.employment.Appointment;
import io.github.ctgnz.fxtivity.employment.Company;
import io.github.ctgnz.fxtivity.employment.Department;
import io.github.ctgnz.fxtivity.employment.Employment;
import io.github.ctgnz.fxtivity.employment.Person;
import io.github.ctgnz.fxtivity.employment.Register;

/**
 * The story the example tells: Acme, from a handful of people in 1990 to a company of departments thirty years later.
 * <p>
 * Built with a fixed seed, so it is the same story every time, and saved as the {@code acme.yml} the application opens - regenerate that with {@link #main(String[])} after
 * changing anything here. Every change goes through the model's own methods, so the story is held to the same rules an editor is.
 */
public final class AcmeStory {

    /** The first day of the story, and of the active range. */
    public static final LocalDate FIRST = LocalDate.of(1990, 1, 1);
    /** The day after the story ends: the end of the active range. */
    public static final LocalDate AFTER = LocalDate.of(2021, 1, 1);

    private static final List<String> FIRST_NAMES = List.of("Ada", "Ben", "Cara", "Dev", "Ella", "Finn", "Gita", "Hugo", "Iris", "Jack", "Kiri", "Liam", "Mia", "Noah", "Olive", "Pita", "Quinn",
        "Rosa", "Sam", "Tui", "Uma", "Vic", "Wren", "Xander", "Yuki", "Zane", "Aroha", "Bruno", "Chloe", "Dan", "Erin", "Fern");
    private static final List<String> SURNAMES = List.of("Abbott", "Barker", "Chen", "Dixon", "Evans", "Fraser", "Grant", "Hart", "Ito", "Jones", "Kaur", "Lowe", "Moana", "Ngata", "Owen", "Patel",
        "Quill", "Reid", "Singh", "Tane", "Usher", "Vega", "Walsh", "Young", "Zhou");

    private final Random random = new Random(17);
    private final Set<String> names = new LinkedHashSet<>();
    private final List<Person> people = new ArrayList<>();
    private Company acme;

    private AcmeStory() {
    }

    /** The story, built afresh. */
    public static Register build() {
        return new AcmeStory().tell();
    }

    /**
     * Writes the story to {@code args[0]}, which is where {@code acme.yml} is regenerated from.
     *
     * @param args
     *            the file to write
     * @throws IOException
     *             if it cannot be written
     */
    public static void main(String[] args) throws IOException {
        Effectivity.forDates(FIRST, FIRST, AFTER);
        Yaml.write(build(), Path.of(args[0]));
    }

    private Register tell() {
        acme = new Company("Acme", FIRST);
        acme.name().setValue(LocalDate.of(2005, 7, 1), "Acme Holdings");

        open("Sales", FIRST);
        Department engineering = open("Engineering", LocalDate.of(1992, 4, 1));
        open("Finance", LocalDate.of(1996, 1, 1));
        Department marketing = open("Marketing", LocalDate.of(2003, 1, 1));
        marketing.name().setValue(LocalDate.of(2014, 1, 1), "Brand");
        Department research = open("Research", LocalDate.of(2008, 6, 1));

        appoint("Grace Hollis", FIRST, LocalDate.of(1999, 3, 1));
        appoint("Martin Okafor", LocalDate.of(1999, 3, 1), LocalDate.of(2008, 9, 1));
        appoint("Priya Raman", LocalDate.of(2008, 9, 1), LocalDate.of(2016, 2, 1));
        appoint("Tom Vale", LocalDate.of(2016, 2, 1), Effectivity.FOREVER);

        seat("chair", "Grace Hollis", FIRST, LocalDate.of(2002, 1, 1));
        seat("chair", "Lena Ford", LocalDate.of(2002, 1, 1), LocalDate.of(2013, 7, 1));
        seat("chair", "Martin Okafor", LocalDate.of(2013, 7, 1), Effectivity.FOREVER);
        seat("finance", "Sam Ito", LocalDate.of(1996, 1, 1), LocalDate.of(2006, 1, 1));
        seat("finance", "Rangi Tait", LocalDate.of(2006, 1, 1), Effectivity.FOREVER);
        seat("independent", "Joan Price", LocalDate.of(2000, 1, 1), LocalDate.of(2009, 1, 1));
        seat("independent", "Ari Cohen", LocalDate.of(2011, 1, 1), Effectivity.FOREVER);

        LocalDate researchCloses = LocalDate.of(2018, 1, 1);
        for (int year = 1990; year <= 2020; year++) {
            if (year == researchCloses.getYear()) {
                // Research closes, handing its people back to the company, and Engineering takes on those it managed the day before.
                List<Person> researchers = research.managedOn(researchCloses.minusDays(1));
                research.close(researchCloses);
                researchers.forEach(person -> person.moveTo(engineering, researchCloses));
            }
            int hires = year == 1990 ? 4 : 2 + (year - 1990) / 6;
            for (int i = 0; i < hires; i++) {
                hire(year);
            }
        }
        return new Register(List.of(acme), people);
    }

    private Department open(String name, LocalDate opened) {
        return acme.openDepartment(name, opened).orElseThrow();
    }

    private void appoint(String holder, LocalDate start, LocalDate end) {
        if (!acme.chiefExecutives().add(new Appointment(holder, start, end))) {
            throw new IllegalStateException("Refused: " + holder);
        }
    }

    private void seat(String seat, String holder, LocalDate start, LocalDate end) {
        if (!acme.board().put(seat, new Appointment(holder, start, end))) {
            throw new IllegalStateException("Refused: " + holder + " in the " + seat + " seat");
        }
    }

    private void hire(int year) {
        LocalDate joined = date(year);
        Person person = new Person(name(), joined.minusYears(22 + random.nextInt(24)).withDayOfYear(1 + random.nextInt(365)));
        people.add(person);
        Employment employment = person.addEmployment(acme, joined);
        Department home = pick(department -> department.containsDate(joined));
        person.moveTo(home, joined);

        // Whether and when they leave is decided first, so that everything else that happens to them falls within their time here. The first few stay.
        LocalDate leaves = AFTER;
        if (people.size() > 4 && random.nextInt(100) < 35) {
            leaves = Effectivity.earlier(AFTER, joined.plusYears(2 + random.nextInt(10)).plusDays(random.nextInt(300)));
        }
        LocalDate left = leaves;

        LocalDate moves = joined.plusYears(2 + random.nextInt(8));
        if (moves.isBefore(left) && random.nextInt(4) == 0) {
            Department next = pick(department -> department != home && department.containsDate(moves));
            if (next != null) {
                person.moveTo(next, moves);
            }
        }
        LocalDate lentFrom = joined.plusMonths(6 + random.nextInt(36));
        LocalDate lentTo = lentFrom.plusMonths(6 + random.nextInt(24));
        if (lentTo.isBefore(left) && random.nextInt(10) < 3) {
            Department lent = pick(department -> department != home && department.isValidFor(lentFrom, lentTo));
            if (lent != null) {
                person.assignTo(lent, lentFrom, lentTo);
            }
        }
        LocalDate renamed = joined.plusYears(1 + random.nextInt(6));
        String surname = SURNAMES.get(random.nextInt(SURNAMES.size()));
        String newName = person.getId().split(" ")[0] + " " + surname;
        // A new name no one else has had, so that no two rows on any date read the same.
        if (renamed.isBefore(AFTER) && random.nextInt(10) == 0 && names.add(newName)) {
            person.name().setValue(renamed, newName);
        }
        if (left.isBefore(AFTER)) {
            person.leave(employment, left);
            person.moveTo(null, left);
        }
    }

    // A department chosen at random from those that pass, or null if none does.
    private Department pick(Predicate<Department> filter) {
        List<Department> candidates = acme.departments().stream().filter(filter).toList();
        return candidates.isEmpty() ? null : candidates.get(random.nextInt(candidates.size()));
    }

    private LocalDate date(int year) {
        return LocalDate.of(year, 1 + random.nextInt(12), 1 + random.nextInt(28));
    }

    private String name() {
        while (true) {
            String name = FIRST_NAMES.get(random.nextInt(FIRST_NAMES.size())) + " " + SURNAMES.get(random.nextInt(SURNAMES.size()));
            if (names.add(name)) {
                return name;
            }
        }
    }

}
