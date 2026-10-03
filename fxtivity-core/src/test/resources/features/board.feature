Feature: A board of directors

  A company's board has several seats, and each changes hands on its own timetable. The chair
  passing from one person to the next says nothing about who keeps the books. So the board is not
  one succession but many - one per seat - held side by side and keyed by the seat.

  Background:
    Given India Inc was founded on 1990-01-01
    And Alice held the Chair seat on the board of India Inc from 1990-01-01 to 2000-01-01
    And Bob held the Chair seat on the board of India Inc from 2000-01-01 to 2010-01-01
    And Carol held the Treasurer seat on the board of India Inc from 1990-01-01 to 2010-01-01

  Scenario: each seat has a history of its own
    Then the Chair seat on the board of India Inc was held by "Alice, Bob", in turn
    And the Treasurer seat on the board of India Inc was held by "Carol", in turn
    And the board of India Inc has 3 appointments across 2 seats

  Scenario: each seat follows the effective date
    When the effective date moves to 1995-01-01
    Then the Chair seat on the board of India Inc is held by Alice
    And the Treasurer seat on the board of India Inc is held by Carol
    When the effective date moves to 2005-01-01
    Then the Chair seat on the board of India Inc is held by Bob
    And the Treasurer seat on the board of India Inc is held by Carol
