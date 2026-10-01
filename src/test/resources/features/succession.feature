Feature: Succession

  Some things have exactly one holder at a time. A company has one chief executive; a person goes
  by one name. A succession holds such things: never two at once, and by default no period with
  nobody at all.

  An appointment that would overlap the incumbent is refused, and so is one that would leave a
  vacancy, unless vacancies are allowed. Making room for a new appointment is a separate, explicit
  act, and there are two ways to do it - the newcomer can take over early, cutting the incumbent's
  term short, or serve their full term and push their successor's start back.

  Background:
    Given India Inc was founded on 1990-01-01
    And Alice was chief executive of India Inc from 1990-01-01 to 1995-01-01
    And Bob was chief executive of India Inc from 1995-01-01 to 2000-01-01

  Scenario: there is one chief executive at a time
    When the effective date moves to 1996-06-01
    Then the chief executive of India Inc is Bob

  Scenario: an appointment overlapping the incumbent is refused
    When Carol is appointed chief executive of India Inc from 1999-01-01 to 2002-01-01
    Then the appointment is refused

  Scenario: an appointment that would leave a vacancy is refused
    When Carol is appointed chief executive of India Inc from 2001-01-01 to 2005-01-01
    Then the appointment is refused

  Scenario: a vacancy is accepted when gaps are allowed
    Given India Inc allows gaps between chief executives
    When Carol is appointed chief executive of India Inc from 2001-01-01 to 2005-01-01
    Then the appointment is accepted
    When the effective date moves to 2000-06-01
    Then the chief executive of India Inc is nobody

  Scenario: a newcomer taking over early cuts the incumbent's term short
    When Carol is appointed chief executive of India Inc from 1993-01-01 to 1997-01-01, taking over from the incumbent
    Then the appointment is accepted
    And the chief executives of India Inc were:
      | holder | from       | to         |
      | Alice  | 1990-01-01 | 1993-01-01 |
      | Carol  | 1993-01-01 | 1995-01-01 |
      | Bob    | 1995-01-01 | 2000-01-01 |

  Scenario: a newcomer serving their full term pushes their successor's start back
    When Carol is appointed chief executive of India Inc from 1993-01-01 to 1997-01-01, serving the full term
    Then the appointment is accepted
    And the chief executives of India Inc were:
      | holder | from       | to         |
      | Alice  | 1990-01-01 | 1995-01-01 |
      | Carol  | 1995-01-01 | 1997-01-01 |
      | Bob    | 1997-01-01 | 2000-01-01 |
