# hrms/services/global_salary_adjuster_service.py

import frappe
import copy
import datetime
from frappe.utils import get_first_day, get_last_day

# Import the data model from the new context files provided
from hrms.models.payroll_data import PayrollData

class GlobalSalaryAdjusterService:
    """Service to adjust the base salary for all employees for a given month."""
    
    def __init__(self):
        """Initialize the service with its required data models."""
        self.payroll_data = PayrollData()

    def adjust_salaries_for_month(self, month_year, percentage_value):
        """
        Adjusts the base salary for all slips in a given month by a percentage.

        This is a transactional operation. All slips will be updated successfully,
        or no changes will be saved.

        Args:
            month_year (str): The month and year (format: YYYY-MM).
            percentage_value (float): The percentage to adjust the salary.
                                      Positive for increase, negative for decrease.

        Returns:
            dict: Result of the operation.
        """
        self._validate_params(month_year, percentage_value)
        
        updated_slips = []
        errors = []

        try:
            year, month = map(int, month_year.split('-'))
            start_date = get_first_day(datetime.date(year, month, 1))
            end_date = get_last_day(datetime.date(year, month, 1))

            # Use the flexible PayrollData method to fetch the target salary slips.
            # We filter for submitted documents within the date range.
            filters = {
                "posting_date": ["between", [start_date, end_date]],
                "docstatus": 1 
            }
            slips_to_process = self.payroll_data.get_salary_slips_with_details(filters)
            
            if not slips_to_process:
                return self._format_response([], [], 0, "No submitted salary slips found for the specified period.")

            # Process each slip individually within a single database transaction
            for slip_summary in slips_to_process:
                result = self._process_single_slip(slip_summary['name'], percentage_value)
                if result['success']:
                    updated_slips.append(result['data'])
                else:
                    errors.append(result['error'])
            
            # If any errors occurred during processing, raise an exception to trigger a rollback
            if errors:
                raise Exception("One or more salary slips failed to process. Rolling back all changes.")

            # If all operations were successful, commit the transaction
            frappe.db.commit()
            message = f"Processing complete. {len(updated_slips)} slip(s) adjusted successfully."
            return self._format_response(updated_slips, [], len(slips_to_process), message)

        except Exception as e:
            # If any error occurs, roll back all database changes
            frappe.db.rollback()
            frappe.log_error(f"General error during global salary adjustment: {str(e)}", "Global Salary Adjustment Error")
            return self._format_error_response(str(e), errors)

    def _process_single_slip(self, slip_name, percentage_value):
        """Processes a single salary slip by its name."""
        try:
            # Use PayrollData to get earnings details, which is cleaner
            earnings = self.payroll_data.get_salary_details(slip_name, 'earnings')
            current_base_amount = None
            for earning in earnings:
                if earning.get('salary_component') == 'Salaire Base':
                    current_base_amount = float(earning.get('amount', 0))
                    break

            if current_base_amount is None:
                raise ValueError("'Salaire Base' component not found in earnings.")

            # Extract essential info before deleting
            slip_info = self._extract_slip_info(slip_name)

            # Cancel and delete the old slip
            self._cancel_and_delete_slip(slip_name)
            
            # Calculate new base salary
            new_base = current_base_amount * (1 + (percentage_value / 100.0))
            
            # Backup, modify the salary structure, and get the original state back
            original_structure = self._backup_and_modify_structure(slip_info['salary_structure'], new_base)
            
            # Create the new salary slip using the modified structure
            new_slip = self._create_new_salary_slip(slip_info)
            
            # IMPORTANT: Restore the structure to its original state immediately
            self._restore_structure(slip_info['salary_structure'], original_structure)
            
            return {
                'success': True,
                'data': self._format_slip_result(slip_info, new_slip.name, current_base_amount, new_base)
            }
        except Exception as e:
            error_msg = f"Failed to process slip {slip_name}: {str(e)}"
            frappe.log_error(error_msg, "Single Slip Processing Error")
            return {'success': False, 'error': error_msg}

    def _validate_params(self, month_year, percentage_value):
        """Validates input parameters."""
        if not isinstance(percentage_value, (float, int)):
            raise ValueError("Percentage value must be a number.")
        try:
            datetime.datetime.strptime(month_year, '%Y-%m')
        except ValueError:
            raise ValueError("Incorrect date format, should be YYYY-MM")

    def _extract_slip_info(self, slip_name):
        """Extracts key information from a salary slip document."""
        doc = frappe.get_doc("Salary Slip", slip_name)
        return {
            'employee': doc.employee,
            'employee_name': doc.employee_name,
            'salary_structure': doc.salary_structure,
            'start_date': doc.start_date,
            'end_date': doc.end_date,
            'posting_date': doc.posting_date,
            'company': doc.company,
            'old_slip_name': slip_name
        }

    def _cancel_and_delete_slip(self, slip_name):
        """Cancels and deletes a submitted salary slip."""
        slip = frappe.get_doc("Salary Slip", slip_name)
        if slip.docstatus == 1:
            slip.cancel()
        frappe.delete_doc("Salary Slip", slip_name, ignore_permissions=True, force=True)

    def _backup_and_modify_structure(self, structure_name, new_base_amount):
        """Temporarily modifies the base salary formula in the salary structure."""
        structure = frappe.get_doc("Salary Structure", structure_name)
        original_data = {'earnings': copy.deepcopy(structure.earnings)}
        
        base_component_found = False
        for earning in structure.earnings:
            if earning.salary_component == "Salaire Base":
                earning.formula = str(new_base_amount)
                base_component_found = True
                break
        
        if not base_component_found:
            raise Exception(f"'Salaire Base' not found in earnings of structure {structure_name}.")
            
        structure.save(ignore_version=True)
        structure.submit()
        return original_data

    def _create_new_salary_slip(self, slip_info):
        """Creates a new salary slip based on the modified structure."""
        new_slip = frappe.get_doc({
            "doctype": "Salary Slip",
            "employee": slip_info['employee'],
            "salary_structure": slip_info['salary_structure'],
            "start_date": slip_info['start_date'],
            "end_date": slip_info['end_date'],
            "posting_date": slip_info['posting_date'],
            "company": slip_info['company']
        })
        new_slip.insert(ignore_permissions=True)
        new_slip.submit()
        return new_slip

    def _restore_structure(self, structure_name, original_data):
        """Restores the salary structure to its original state."""
        structure = frappe.get_doc("Salary Structure", structure_name)
        structure.earnings = original_data['earnings']
        structure.save(ignore_version=True)
        structure.submit()

    def _format_slip_result(self, slip_info, new_slip_name, old_base, new_base):
        """Formats the result for a single processed slip."""
        return {
            "employee": slip_info['employee_name'],
            "period": f"{slip_info['start_date']} to {slip_info['end_date']}",
            "old_slip": slip_info['old_slip_name'],
            "new_slip": new_slip_name,
            "old_base_salary": old_base,
            "new_base_salary": new_base
        }

    def _format_response(self, updated, errors, total, message):
        """Formats the final JSON response."""
        status = "success"
        if errors:
            status = "partial_success" if updated else "error"
        return {"status": status, "message": message, "data": updated, "errors": errors, "total_processed": total}
    
    def _format_error_response(self, error_msg, processed_errors):
        """Formats an error response when the whole process fails."""
        all_errors = processed_errors if processed_errors else []
        all_errors.append(f"General error: {error_msg}")
        return self._format_response([], all_errors, 0, "Processing failed. All changes have been rolled back.")