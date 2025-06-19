# Copyright (c) 2025, Frappe Technologies Pvt. Ltd. and Contributors
# See license.txt

# import frappe
from frappe.tests import IntegrationTestCase, UnitTestCase


# On IntegrationTestCase, the doctype test records and all
# link-field test record dependencies are recursively loaded
# Use these module variables to add/remove to/from that list
EXTRA_TEST_RECORD_DEPENDENCIES = []  # eg. ["User"]
IGNORE_TEST_RECORD_DEPENDENCIES = []  # eg. ["User"]


class UnitTestsalary_structure_csv(UnitTestCase):
	"""
	Unit tests for salary_structure_csv.
	Use this class for testing individual functions and methods.
	"""

	pass


class IntegrationTestsalary_structure_csv(IntegrationTestCase):
	"""
	Integration tests for salary_structure_csv.
	Use this class for testing interactions between multiple components.
	"""

	pass
