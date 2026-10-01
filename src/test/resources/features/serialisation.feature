Feature: Writing and reading

  A value's history is written as its list of changes, and nothing else: the owner is already
  the thing being written, so the history refers back to it rather than repeating it. Written as
  YAML with yaml-flock present, each change takes one line - which is what keeps a long history
  readable, and what makes a single change show up as a single changed line in a diff.

  Background:
    Given Jane was born on 1970-01-01
    And Jane was renamed "Jane Brown" on 1995-06-01

  Scenario: a name's history is written one change per line
    When Jane is written as YAML
    Then the YAML is:
      """
      id: Jane
      start: 1970-01-01
      end: 9999-12-31
      name:
      - {date: 1970-01-01, value: Jane}
      - {date: 1995-06-01, value: Jane Brown}
      """

  Scenario: reading it back restores the history and its owner
    The history only knows which dates are in range by asking its owner, so a name that can be
    read on a date after reading back proves the owner was restored, not just the entries.

    When Jane is written as YAML and read back
    Then the name of Jane on 1995-05-31 was "Jane"
    And the name of Jane on 1995-06-01 was "Jane Brown"
