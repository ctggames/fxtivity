Feature: A name that changes over time

  Fowler's pattern gives an object a period. It says nothing about a value that changes during
  that period - a person who marries and takes a new name, a company that is renamed. That is the
  Temporal Property pattern, and it is what an effective property is.

  The history is a list of changes, each saying "from this date, the value is this". The value on
  any date is set by the latest change on or before it. A value can only change while its owner
  exists: nobody had a name before they were born.

  Background:
    Given Jane was born on 1970-01-01
    And Jane was renamed "Jane Brown" on 1995-06-01

  Scenario: the name on a date is set by the latest change on or before it
    Then the name of Jane on 1970-01-01 was "Jane"
    And the name of Jane on 1995-05-31 was "Jane"
    And the name of Jane on 1995-06-01 was "Jane Brown"
    And the name of Jane on 2020-01-01 was "Jane Brown"

  Scenario: the name follows the effective date
    When the effective date moves to 1990-01-01
    Then the name of Jane is "Jane"
    When the effective date moves to 2000-01-01
    Then the name of Jane is "Jane Brown"

  Scenario: a name cannot change before its owner existed
    When Jane is renamed "Janet" on 1960-01-01
    Then the change is refused
    And the name of Jane on 1970-01-01 was "Jane"

  Scenario: a company's name changes the same way
    Given India Inc was founded on 1990-01-01
    And India Inc was renamed "India Holdings" on 2005-01-01
    Then the name of India Inc on 2004-12-31 was "India Inc"
    And the name of India Inc on 2005-01-01 was "India Holdings"

  Scenario: a change to the same name changes nothing, and pruning removes it
    Given Jane was renamed "Jane Brown" on 2001-01-01
    Then the name history of Jane has 3 changes
    When the name history of Jane is pruned
    Then the name history of Jane has 2 changes
    And the name of Jane on 2001-01-01 was "Jane Brown"

  Scenario: the history read as spans of time
    Given Jane died on 2020-01-01
    Then the names of Jane were:
      | name       | from       | to         |
      | Jane       | 1970-01-01 | 1995-06-01 |
      | Jane Brown | 1995-06-01 | 2020-01-01 |
