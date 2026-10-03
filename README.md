# fxtivity

An implementation of the [Effectivity pattern](https://martinfowler.com/eaaDev/Effectivity.html) on JavaFX: things that are in effect for a period, values that change over time, and observable collections that all follow one application-wide effective date.

```java
MultiEffectiveList<Employment> employments = new MultiEffectiveList<>();
employments.add(new Employment(indiaInc, LocalDate.of(1999, 12, 1), LocalDate.of(2000, 5, 1)));
employments.add(new Employment(peninsulaInc, LocalDate.of(2000, 4, 1)));

table.setItems(employments.effective());

Effectivity.forDate(LocalDate.of(2000, 4, 15));   // the table shows both
Effectivity.forDate(LocalDate.of(2000, 6, 1));    // the table shows Peninsula Inc alone
```

Nothing in that example tells the table the date moved. It finds out because the list does.

## Why

Most data that matters changes over time. People change jobs and names, companies are renamed and change hands, a post is held by one person and then another. Recording only the current state throws away the history, and with it the ability to answer "what was true on this date?" — which is often the question that matters most.

Fowler's Effectivity pattern is the simplest answer: give each object a period during which it is in effect, and filter by date. His own caution is that it pushes work onto the client — every query that wants current information has to remember to check the period.

On JavaFX that cost can be removed almost entirely. A user interface is always looking at the world as it was on *some* date, so make that date a single observable value, and have every effective collection filter itself against it. Then moving the date is one call, and every table, tree and chart bound to those collections updates to show the world on the new date — with no listener or binding written for the purpose anywhere in the application.

That there is exactly **one** effective date per application is the point rather than a limitation. Two collections disagreeing about what "now" means would be a defect, and a screen showing two different dates at once would be lying about one of them.

## The pieces

| | |
| --- | --- |
| **`Effective`** | Anything in effect for a period. Implement `getStart`, `getEnd` and their setters; everything else follows. |
| **`Effectivity`** | A period, closed-open: in effect from its start up to but excluding its end. Also holds the application's effective date. |
| **`DateRange`** | A closed-open span of dates, with `contains`, `encloses`, `intersection` and `span`. |
| **`MultiEffectiveList`** | Things that may be in effect at the same time — a person's employments. |
| **`SingleEffectiveList`** | A succession, one at a time — a company's chief executives. Refuses overlaps, and gaps unless allowed. |
| **`EffectiveMap`** | A succession per key — the seats on a board, each changing hands on its own timetable. |
| **`EffectiveProperty`** | A value that changes during its owner's lifetime — a person's name. This is Fowler's *Temporal Property*. |

### Closed-open periods

A period includes its first day and excludes its last. That is what lets one period hand over to the next cleanly: a post that ends on 1 July and its successor's that starts on 1 July share no day and miss none, so "this ends when that begins" is simply the same date written twice. A period with no end runs until `Effectivity.FOREVER`.

### Making room in a succession

An appointment that would overlap the incumbent is refused rather than quietly accepted. Making room is a separate, explicit act, and there are two ways to do it: `insertBackwards` lets the newcomer take over early, cutting the incumbent's term short, while `insertForwards` lets them serve their full term and pushes their successor's start back.

## Installing

```xml
<dependency>
    <groupId>io.github.ctgnz</groupId>
    <artifactId>fxtivity-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

Requires Java 25. Brings in `javafx-base` — beans and collections, nothing that starts a toolkit, so neither the library nor its tests need a display — and `jackson-annotations`, which declares how the model is serialised without making you depend on anything that serialises it.

## Writing and reading

The model is plain Jackson. An entity's period is written as its `start` and `end`. An `EffectiveProperty` is written as its list of changes alone, with its owner restored from the owner's side by a managed reference, so it is never repeated:

```yaml
id: Jane
start: 1970-01-01
end: 9999-12-31
name:
- {date: 1970-01-01, value: Jane}
- {date: 1995-06-01, value: Jane Brown}
```

Each change on its own line comes from [yaml-flock](https://github.com/ctgnz/yaml-flock), which is an **optional** dependency: add it if you write YAML and want that shape. Without it the annotation is simply ignored, so a project writing JSON — or not serialising at all — inherits no YAML stack for it.

### Collections

A collection is persisted through a getter over its source list and a setter that calls `load`:

```java
private final SingleEffectiveList<Shift> shifts = new SingleEffectiveList<>(true); // gaps allowed

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
```

Reading then fills the collection you declared, configured as you declared it. Annotating the field alone is not enough: Jackson replaces it with a new collection built with the defaults, which loses the configuration, and the default rules quietly drop any entries they refuse.

`load` enforces the collection's rules, and all or nothing. A file that breaks them, such as an overlap in a succession or a gap where none is allowed, fails to load, and Jackson's message gives the path to the entry. It does not load with entries missing. An `EffectiveMap` follows the same pattern, with `getSourceMap()` as the getter and `load(Map)` in the setter.

## The specification

**[Read the scenarios at ctgnz.github.io/fxtivity](https://ctgnz.github.io/fxtivity/)**.

They are Cucumber features in [`fxtivity-core/src/test/resources/features`](fxtivity-core/src/test/resources/features), written against Fowler's own example — people, companies and the employments between them — and extended with the cases his pattern leaves to others: a name that changes over time, an office with one holder at a time, and a board whose seats each have their own history.

| Feature | Covers |
| --- | --- |
| `periods.feature` | closed-open periods: containing, overlapping, enclosing, continuing on |
| `effective-date.feature` | one date for the whole application, and every collection following it |
| `employment.feature` | Fowler's example, including correcting a mistake after the fact |
| `succession.feature` | one holder at a time, vacancies, and the two ways of making room |
| `names.feature` | a value that changes during its owner's lifetime |
| `board.feature` | a succession per key |
| `serialisation.feature` | the written form, and the owner restored on reading back |

## Licence

Apache License 2.0. See [LICENSE](LICENSE).
