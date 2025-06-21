# hrms/models/salary_structure.py

import frappe

class SalaryStructureModel:
    """Modèle pour gérer les structures de salaire."""
    
    def get_latest_structure(self):
        """
        Récupère la structure de salaire la plus récente.
        
        Returns:
            dict: Structure de salaire avec le nom
        """
        try:
            salary_structure = frappe.db.sql("""
                SELECT name 
                FROM `tabSalary Structure` 
                WHERE creation = (
                    SELECT MAX(creation) 
                    FROM `tabSalary Structure`
                ) 
                LIMIT 1
            """, as_dict=True)
            
            if salary_structure:
                return {
                    'name': salary_structure[0].name
                }
            else:
                frappe.throw("Aucune structure de salaire trouvée")
                
        except Exception as e:
            frappe.log_error(f"Erreur lors de la récupération de la structure : {str(e)}", "Salary Structure Error")
            frappe.throw("Erreur lors de la récupération de la structure de salaire")