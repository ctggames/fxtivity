Feature: Employment

  Fowler's own example of the pattern. A person is employed by companies over time, each
  employment in effect for its own period - and a person may hold more than one at once, so the
  employments are free to overlap.

  He uses it to make a further point: people make mistakes, and the record has to be correctable
  after the fact. Adding the next employment and ending the current one are the everyday, forward
  in time changes. Correcting a period already recorded is the other kind, and it needs direct
  access to the dates.

  Background:
    Given India Inc was founded on 1990-01-01
    And Peninsula Inc was founded on 1990-01-01
    And Dublin Inc was founded on 1990-01-01
    And Wellington was born on 1960-01-01

  Scenario: an employment is in effect from its first day until further notice
    Given Wellington was employed by India Inc from 1999-12-01
    Then Wellington was employed by India Inc on 1999-12-01
    And Wellington was employed by India Inc on 2050-01-01
    And Wellington was not employed by India Inc on 1999-11-30

  Scenario: an employment that has ended
    Given Wellington was employed by India Inc from 1999-12-01
    When Wellington left India Inc on 2000-05-01
    Then Wellington was employed by India Inc on 2000-04-30
    And Wellington was not employed by India Inc on 2000-05-01

  Scenario: overlapping employments are both in effect
    Given Wellington was employed by India Inc from 1999-12-01 to 2000-05-01
    And Wellington was employed by Peninsula Inc from 2000-04-01
    When the effective date moves to 2000-04-15
    Then Wellington is employed by "India Inc, Peninsula Inc"

  Scenario: correcting a mistake after the fact
    Fowler's correction: Wellington was recorded as joining Peninsula Inc in April, but in May he
    was in fact at Dublin Inc, and only joined Peninsula Inc in June.

    Given Wellington was employed by India Inc from 1999-12-01 to 2000-05-01
    And Wellington was employed by Peninsula Inc from 2000-04-01
    When it turns out Wellington actually joined Peninsula Inc on 2000-06-01
    And Wellington was employed by Dublin Inc from 2000-05-01 to 2000-06-01
    And the effective date moves to 2000-05-15
    Then Wellington is employed by "Dublin Inc"
