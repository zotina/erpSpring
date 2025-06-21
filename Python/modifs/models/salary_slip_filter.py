# hrms/models/salary_slip_filter.py
import frappe

class SalarySlipFilter:
    """Modèle pour filtrer les fiches de paie selon des critères."""
    
    def get_filtered_salary_slips(self, salary_component, reference_amount, comparison_type):
        """
        Récupère les fiches de paie filtrées selon les critères.
        
        Args:
            salary_component (str): Composant de salaire à filtrer
            reference_amount (float): Montant de référence
            comparison_type (int): 0 pour inférieur, 1 pour supérieur
        
        Returns:
            list: Liste des fiches de paie correspondantes
        """
        self._validate_filter_params(salary_component, reference_amount, comparison_type)
        
        condition_sql = "< %(montant)s" if comparison_type == 0 else "> %(montant)s"
        
        query = f"""
            WITH eligible_parents AS (
                SELECT DISTINCT parent
                FROM `tabSalary Detail`
                WHERE salary_component = %(salary_component)s
                  AND amount {condition_sql}
                  AND parenttype = 'Salary Slip'
            ),
            base_salaries AS (
                SELECT
                    sd.amount,
                    sd.parent
                FROM `tabSalary Detail` sd
                INNER JOIN eligible_parents ep ON sd.parent = ep.parent
                WHERE sd.salary_component = 'Salaire Base'
            )
            SELECT
                p.amount,
                p.parent,
                sl.salary_structure
            FROM base_salaries p
            INNER JOIN `tabSalary Slip` sl ON sl.name = p.parent
        """
        
        try:
            return frappe.db.sql(query, {
                'salary_component': salary_component,
                'montant': reference_amount
            }, as_dict=True)
            
        except Exception as e:
            frappe.log_error(f"Erreur dans le filtrage des salary slips: {str(e)}", "Salary Filter Error")
            frappe.throw("Erreur lors de l'exécution de la requête de filtrage")
    
    def _validate_filter_params(self, salary_component, reference_amount, comparison_type):
        """Valide les paramètres de filtrage."""
        if not salary_component or reference_amount is None:
            frappe.throw("Paramètres salary_component et montant requis")
        
        try:
            float(reference_amount)
            int(comparison_type)
        except (ValueError, TypeError):
            frappe.throw("Paramètres montant et comparison_type doivent être numériques")
        
        if comparison_type not in [0, 1]:
            frappe.throw("comparison_type doit être 0 (inférieur) ou 1 (supérieur)")