Feature: The effective date

  An application built on this library is always looking at the world as it was on one date: the
  effective date. There is exactly one, shared by the whole application, and every effective
  collection follows it.

  That is the point of the library rather than a detail of it. Move the date, and every
  collection shows what was in effect on the new one - and so does anything bound to those
  collections, a table or a tree or a chart, without a listener or a binding of its own. Two
  collections disagreeing about what "now" means would be a defect, so there is no way to give one
  collection a date of its own.

  Background:
    Given India Inc was founded on 1990-01-01
    And Peninsula Inc was founded on 1990-01-01
    And Wellington was born on 1960-01-01
    And Wellington was employed by India Inc from 1999-01-01 to 2000-05-01
    And Wellington was employed by Peninsula Inc from 2000-04-01

  Scenario: moving the effective date changes who is employed, without touching the list
    Given the effective date is 2000-01-01
    Then Wellington is employed by "India Inc"
    When the effective date moves to 2000-06-01
    Then Wellington is employed by "Peninsula Inc"

  Scenario: every collection follows the one effective date
    Given Duke was born on 1950-01-01
    And Duke was employed by India Inc from 1995-01-01
    And the effective date is 1999-06-01
    Then Wellington is employed by "India Inc"
    And Duke is employed by "India Inc"
    When the effective date moves to 1994-06-01
    Then Wellington is employed by nobody
    And Duke is employed by nobody

  Scenario: the effective date cannot leave the active range
    Given the active range is 1990-01-01 to 2010-01-01
    When the effective date is moved to 2015-01-01
    Then the move is refused
