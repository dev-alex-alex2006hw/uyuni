# Copyright (c) 2018 SUSE LLC
# Licensed under the terms of the MIT license.

Feature: Work with Union and Intersection buttons in the group list

  Scenario: Create a group sles
    Given I am on the groups page
    When I follow "Create Group"
    And I enter "sles" as "name"
    And I enter "sles systems" as "description"
    And I click on "Create Group"
    Then I should see a "System group sles created." text

  Scenario: Add systems to the group sles
    Given I am on the groups page
    When I follow "sles"
    And I follow "Target Systems"
    And I check the "sle-client" client
    And I check the "sle-minion" client
    And I check the "ssh-minion" client
    And I click on "Add Systems"
    Then I should see a "3 systems were added to sles server group." text

  Scenario: Create a group centos
    Given I am on the groups page
    When I follow "Create Group"
    And I enter "centos" as "name"
    And I enter "centos systems" as "description"
    And I click on "Create Group"
    Then I should see a "System group centos created." text

  Scenario: Add systems to the group centos
    Given I am on the groups page
    When I follow "centos"
    And I follow "Target Systems"
    And I check the "ceos-minion" client
    And I click on "Add Systems"
    Then I should see a "1 systems were added to centos server group." text

  Scenario: Create a group traditional
    Given I am on the groups page
    When I follow "Create Group"
    And I enter "traditional" as "name"
    And I enter "traditional systems" as "description"
    And I click on "Create Group"
    Then I should see a "System group traditional created." text

  Scenario: Add systems to the group traditional
    Given I am on the groups page
    When I follow "traditional"
    And I follow "Target Systems"
    And I check the "sle-client" client
    And I click on "Add Systems"
    Then I should see a "1 systems were added to traditional server group." text

  Scenario: Add the new group to SSM
    Given I am on the groups page
    When I click on "Use in SSM" for "sles"
    And I should see a "systems selected" text
    And I should see a "Selected Systems List" text
    Then I should see "sle-client" as link
    And I should see "sle-minion" as link
    And I should see "ssh-minion" as link

  Scenario: Add a union of 2 groups to SSM
    Given I am on the groups page
    When I check "sles" in the list
    And I check "centos" in the list
    And I click on "Work With Union"
    Then I should see "sle-client" as link
    And I should see "sle-minion" as link
    And I should see "ssh-minion" as link
    And I should see "ceos-minion" as link

  Scenario: Add an intersection of 2 groups to SSM
    Given I am on the groups page
    When I check "sles" in the list
    And I check "traditional" in the list
    And I click on "Work With Intersection"
    Then I should see "sle-client" as link
    And I should not see a "sle-minion" link
    And I should not see a "ssh-minion" link
    And I should not see a "ceos-minion" link

@cleanup
  Scenario: Cleanup: remove the new group
   Given I am on the groups page
   When I follow "sles" in the content area
   And I follow "Delete Group" in the content area
   And I click on "Confirm Deletion"
   Then I should see a "deleted" text
   When I follow "centos" in the content area
   And I follow "Delete Group" in the content area
   And I click on "Confirm Deletion"
   Then I should see a "deleted" text
   When I follow "traditional" in the content area
   And I follow "Delete Group" in the content area
   And I click on "Confirm Deletion"
   Then I should see a "deleted" text
