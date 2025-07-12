# hrms/services/historical_adjuster_service.py

import frappe
import copy
import datetime
from frappe.utils import get_first_day, get_last_day

from hrms.models.payroll_data import PayrollData # Leveraging existing model

class HistoricalAdjusterService:
    """
    Service to re-apply a series of historical salary adjustments to salary slips.
    This operation is transactional.
    """
    
    def __init__(self):
        self.payroll_data = PayrollData()

    def apply_monthly_adjustments(self, adjustments):
        """
        Applies a list of monthly percentage adjustments.

        Args:
            adjustments (list[dict]): A list of adjustments to apply.
                Example: [{"month_year": "2025-01", "percentage": -10.0}, ...]

        Returns:
            dict: A report of the operations.
        """
        if not isinstance(adjustments, list):
            raise ValueError("Adjustments must be a list of dictionaries.")

        processed_months = []
        all_errors = []

        try:
            # Loop through each adjustment provided (e.g., one for Jan, one for Feb)
            for adj in adjustments:
                month_year = adj.get("month_year")
                percentage = adj.get("percentage")

                if not all([month_year, isinstance(percentage, (int, float))]):
                    all_errors.append(f"Skipping invalid adjustment record: {adj}")
                    continue
                
                # Process all slips for this specific month with its specific percentage
                month_results, month_errors = self._process_month(month_year, percentage)
                
                if month_results:
                    processed_months.append({
                        "month": month_year,
                        "percentage_applied": percentage,
                        "slips_updated": month_results
                    })
                if month_errors:
                    all_errors.extend(month_errors)

            # If any error occurred in any month, the entire transaction is rolled back.
            if all_errors:
                raise Exception("One or more errors occurred during processing. See error log for details.")

            # If we reach here, all months were processed successfully.
            frappe.db.commit()
            return self._format_response(
                processed_months, [], "All specified adjustments were applied successfully."
            )

        except Exception as e:
            frappe.db.rollback()
            frappe.log_error(f"Error during historical adjustment: {e}", "Historical Adjustment Error")
            return self._format_error_response(all_errors, str(e))

    def _process_month(self, month_year, percentage_value):
        """Finds and adjusts all salary slips for a single month."""
        updated_slips = []
        errors = []
        
        year, month = map(int, month_year.split('-'))
        start_date = get_first_day(datetime.date(year, month, 1))
        end_date = get_last_day(datetime.date(year, month, 1))

        slips_in_month = self.payroll_data.get_salary_slips_with_details({
            "posting_date": ["between", [start_date, end_date]],
            "docstatus": 1
        })

        for slip_summary in slips_in_month:
            # Re-use the robust single-slip processing logic
            slip_result = self._process_single_slip(slip_summary['name'], percentage_value)
            if slip_result['success']:
                updated_slips.append(slip_result['data'])
            else:
                errors.append(slip_result['error'])
        
        return updated_slips, errors

    def _process_single_slip(self, slip_name, percentage_value):
        """Processes a single salary slip (cancel, delete, recalculate, create)."""
        # This logic is very similar to the GlobalSalaryAdjusterService
        try:
            earnings = self.payroll_data.get_salary_details(slip_name, 'earnings')
            current_base = next((float(e['amount']) for e in earnings if e['salary_component'] == 'Salaire Base'), None)

            if current_base is None:
                raise ValueError("'Salaire Base' not found.")

            slip_info = self._extract_slip_info(slip_name)
            self._cancel_and_delete_slip(slip_name)
            
            new_base = current_base * (1 + (percentage_value / 100.0))
            
            original_structure = self._backup_and_modify_structure(slip_info['salary_structure'], new_base)
            new_slip = self._create_new_salary_slip(slip_info)
            self._restore_structure(slip_info['salary_structure'], original_structure)
            
            return {
                'success': True,
                'data': self._format_slip_result(slip_info, new_slip.name, current_base, new_base)
            }
        except Exception as e:
            return {'success': False, 'error': f"Failed on slip {slip_name}: {e}"}

    # Helper methods (_extract_slip_info, _cancel_and_delete_slip, etc.) are identical
    # to the ones in `global_salary_adjuster_service.py`. We can copy them here to make
    # this service self-contained.

    def _extract_slip_info(self, slip_name):
        doc = frappe.get_doc("Salary Slip", slip_name)
        return {'employee': doc.employee, 'employee_name': doc.employee_name, 'salary_structure': doc.salary_structure, 'start_date': doc.start_date, 'end_date': doc.end_date, 'posting_date': doc.posting_date, 'company': doc.company, 'old_slip_name': slip_name}

    def _cancel_and_delete_slip(self, slip_name):
        slip = frappe.get_doc("Salary Slip", slip_name)
        if slip.docstatus == 1: slip.cancel()
        frappe.delete_doc("Salary Slip", slip_name, ignore_permissions=True, force=True)

    def _backup_and_modify_structure(self, structure_name, new_base):
        structure = frappe.get_doc("Salary Structure", structure_name)
        original_data = {'earnings': copy.deepcopy(structure.earnings)}
        base_found = False
        for earning in structure.earnings:
            if earning.salary_component == "Salaire Base":
                earning.formula = str(new_base)
                base_found = True
                break
        if not base_found: raise Exception(f"'Salaire Base' not in structure {structure_name}.")
        structure.save(ignore_version=True); structure.submit()
        return original_data

    def _create_new_salary_slip(self, slip_info):
        new_slip = frappe.get_doc({"doctype": "Salary Slip", "employee": slip_info['employee'], "salary_structure": slip_info['salary_structure'], "start_date": slip_info['start_date'], "end_date": slip_info['end_date'], "posting_date": slip_info['posting_date'], "company": slip_info['company']})
        new_slip.insert(ignore_permissions=True); new_slip.submit()
        return new_slip

    def _restore_structure(self, structure_name, original_data):
        structure = frappe.get_doc("Salary Structure", structure_name)
        structure.earnings = original_data['earnings']
        structure.save(ignore_version=True); structure.submit()

    def _format_slip_result(self, slip_info, new_slip_name, old_base, new_base):
        return {"employee": slip_info['employee_name'], "old_slip": slip_info['old_slip_name'], "new_slip": new_slip_name, "old_base_salary": old_base, "new_base_salary": new_base}

    def _format_response(self, data, errors, message):
        return {"status": "error" if errors else "success", "message": message, "data": data, "errors": errors}

    def _format_error_response(self, errors, exc_message):
        full_error_list = errors + [f"Transaction failed and was rolled back. Reason: {exc_message}"]
        return self._format_response([], full_error_list, "An error occurred during processing.")