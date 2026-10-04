Feature: Departments

  A company opens departments as it grows, each in effect for its own period within the
  company's life. A person is managed by at most one department at a time - and by the company
  directly when by none - and the department managing them changes over time. Separately, a
  person can be assigned to several departments at once.

  Each relationship is owned by the person: the managing department is a value that changes over
  time, and the assignments a list of their own. A department's view of who it manages, and who
  is assigned to it, follows from those - so the two ends cannot disagree, and a department that
  closes leaves nobody behind in it.

  Background:
    Given Acme was founded on 1990-01-01
    And Ann was born on 1960-01-01
    And Bob was born on 1965-01-01
    And Acme opened a Sales department on 1990-01-01
    And Acme opened an Engineering department on 1995-01-01

  Scenario: a department opens within the company's own life
    When Acme opens a Research department on 1985-01-01
    Then the opening is refused

  Scenario: the department managing a person changes over time
    Given Ann was managed by the Sales department from 1990-01-01
    And Ann was managed by the Engineering department from 1996-01-01
    Then Ann was managed by the Sales department on 1995-12-31
    And Ann was managed by the Engineering department on 1996-01-01

  Scenario: with no department, a person is managed by the company directly
    Given Ann was managed by the Sales department from 1990-01-01
    And Ann was managed by Acme directly from 1993-01-01
    Then Ann was managed by the Sales department on 1992-12-31
    And Ann was managed by Acme directly on 1993-01-01

  Scenario: a department sees who it manages, following each person's history
    Given Ann was managed by the Sales department from 1990-01-01
    And Bob was managed by the Sales department from 1992-01-01
    And Ann was managed by the Engineering department from 1996-01-01
    Then the Sales department managed "Ann" on 1991-01-01
    And the Sales department managed "Ann, Bob" on 1995-01-01
    And the Sales department managed "Bob" on 1996-01-01
    And the Engineering department managed "Ann" on 1996-01-01

  Scenario: a person is only moved to a department that is open
    When Ann is transferred to the Engineering department on 1993-01-01
    Then the transfer is refused

  Scenario: assignments can overlap, independently of who manages the person
    Given Ann was managed by the Sales department from 1990-01-01
    And Ann was assigned to the Sales department from 1991-01-01 to 1997-01-01
    And Ann was assigned to the Engineering department from 1995-01-01 to 1999-01-01
    Then Ann was assigned to "Sales, Engineering" on 1996-01-01
    And Ann was assigned to "Engineering" on 1998-01-01
    And the Engineering department had "Ann" assigned on 1996-01-01

  Scenario: an assignment falls within the department's life
    When Ann is assigned to the Engineering department from 1993-01-01 to 1996-01-01
    Then the assignment is refused

  Scenario: a department that closes hands its people back to the company
    Given Ann was managed by the Sales department from 1990-01-01
    And Ann was assigned to the Sales department from 1991-01-01
    When the Sales department closes on 2000-01-01
    Then Ann was managed by the Sales department on 1999-12-31
    And Ann was managed by Acme directly on 2000-01-01
    And the Sales department managed nobody on 2000-01-01
    And Ann was assigned to "Sales" on 1999-12-31
    And Ann was assigned to nothing on 2000-01-01

  Scenario: the register is written with each relationship once, on the end that owns it
    A person names the departments by id. A department's own view of who it manages, and who is
    assigned to it, is not written at all: it is rebuilt from the people when the register is read.

    Given Ann was managed by the Sales department from 1990-01-01
    And Ann was managed by the Engineering department from 1996-01-01
    And Bob was assigned to the Engineering department from 1995-01-01 to 1999-01-01
    When the register is written as YAML
    Then the YAML is:
      """
      companies:
      - id: Acme
        start: 1990-01-01
        end: 9999-12-31
        name:
        - {date: 1990-01-01, value: Acme}
        departments:
        - id: Sales
          start: 1990-01-01
          end: 9999-12-31
          name:
          - {date: 1990-01-01, value: Sales}
        - id: Engineering
          start: 1995-01-01
          end: 9999-12-31
          name:
          - {date: 1995-01-01, value: Engineering}
      people:
      - id: Ann
        start: 1960-01-01
        end: 9999-12-31
        name:
        - {date: 1960-01-01, value: Ann}
        managedBy:
        - {date: 1990-01-01, value: Sales}
        - {date: 1996-01-01, value: Engineering}
      - id: Bob
        start: 1965-01-01
        end: 9999-12-31
        name:
        - {date: 1965-01-01, value: Bob}
        assignments:
        - department: Engineering
          start: 1995-01-01
          end: 1999-01-01
      """

  Scenario: reading the register back rebuilds both ends
    Given Ann was managed by the Sales department from 1990-01-01
    And Ann was managed by the Engineering department from 1996-01-01
    And Bob was assigned to the Engineering department from 1995-01-01 to 1999-01-01
    When the register is written as YAML and read back
    Then Ann was managed by the Engineering department on 1997-01-01
    And the Engineering department managed "Ann" on 1997-01-01
    And the Sales department managed "Ann" on 1995-01-01
    And Bob was assigned to "Engineering" on 1997-01-01
    And the Engineering department had "Bob" assigned on 1997-01-01
