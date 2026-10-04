Feature: Periods of effect

  Everything in this library hangs off one idea: a thing is in effect for a period, from the day
  it starts up to but not including the day it ends.

  Excluding the end day is what lets one period hand over to the next cleanly. A role that ends
  on 1 July and its successor that starts on 1 July share no day and miss none, so "this ends when
  that begins" is simply the same date written twice.

  Scenario: a period includes its first day and excludes its last
    Given a period from 2000-01-01 to 2000-07-01
    Then it is in effect on 2000-01-01
    And it is in effect on 2000-06-30
    And it is not in effect on 2000-07-01

  Scenario: periods that meet do not overlap
    Given a period from 2000-01-01 to 2000-07-01
    And another period from 2000-07-01 to 2001-01-01
    Then the periods do not overlap
    And the second continues straight on from the first

  Scenario: periods that share a day overlap
    Given a period from 2000-01-01 to 2000-07-02
    And another period from 2000-07-01 to 2001-01-01
    Then the periods overlap

  Scenario: one period encloses another
    Given a period from 2000-01-01 to 2001-01-01
    And another period from 2000-03-01 to 2000-09-01
    Then the first encloses the second

  Scenario: a period with no end runs until further notice
    Given a period from 2000-01-01 with no end
    Then it is in effect on 2999-12-31
