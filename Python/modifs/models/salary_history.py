# hrms/models/salary_history.py

import frappe
from frappe.utils import getdate

class SalaryHistory:
    """Modèle pour gérer l'historique des salaires."""
    
    def get_last_base_salary(self, employee_id, month_year):
        """
        Récupère le dernier salaire de base pour un employé avant une date spécifiée.
        
        Args:
            employee_id (str): ID de l'employé
            month_year (str): Mois et année (format: YYYY-MM)
        
        Returns:
            dict: Informations du salaire ou None
        """
        try:
            target_date = f"{month_year}-01"
            
            salary_slips = frappe.db.sql("""
                SELECT 
                    ss.name,
                    ss.posting_date,
                    ss.salary_structure,
                    sd.amount
                FROM 
                    `tabSalary Slip` ss 
                INNER JOIN 
                    `tabSalary Detail` sd ON ss.name = sd.parent
                INNER JOIN 
                    `tabSalary Component` sc ON sd.salary_component = sc.name
                WHERE
                    ss.employee = %(employee)s
                    AND ss.posting_date < %(target_date)s
                    AND ss.docstatus = 1
                    AND sc.type = 'Earning'
                    AND LOWER(sc.salary_component_abbr) = 'sb'
                ORDER BY 
                    ss.posting_date DESC
                LIMIT 1
            """, {
                'employee': employee_id,
                'target_date': target_date
            }, as_dict=True)
            
            if salary_slips:
                return {
                    'amount': salary_slips[0].amount,
                    'posting_date': salary_slips[0].posting_date,
                    'salary_structure': salary_slips[0].salary_structure 
                }
            
            return None
        
        except Exception as e:
            frappe.log_error(f"Erreur lors de la recherche de salaire de base : {str(e)}", "Salary History Error")
            return {
                "status": "error",
                "message": str(e),
                "data": None
            }