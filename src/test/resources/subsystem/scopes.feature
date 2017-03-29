#Copyright 2017 Penny Rohr Curich
#
#Licensed under the Apache License, Version 2.0 (the "License");
#you may not use this file except in compliance with the License.
#You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
#Unless required by applicable law or agreed to in writing, software
#distributed under the License is distributed on an "AS IS" BASIS,
#WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#See the License for the specific language governing permissions and
#limitations under the License.


# This is a comment.

@Category("Scopes")
@Meta("Spring")
Feature: Spring Scenario Scope

  Scenario: A Scenario-Scoped Test Case
  This has a scenario-scoped object that resides in different Step classes.

    Given a non-scoped condition
    And scope condition "1"
    And scope condition 2
    When the scope is analyzed
    Then the scope in condition 1 and condition 2 are the same

  Scenario: Another Scenario-Scoped Test Case

    Given scope condition "3"
    And scope condition 2
    When the scope is analyzed
    Then the scope in condition 1 and condition 2 are the same