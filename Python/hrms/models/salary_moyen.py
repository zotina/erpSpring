import frappe

class SalaryMoyen:
    """Modèle pour filtrer les fiches de paie selon des critères."""
    def get_moyen(self):
        """
        Récupère le dernier salaire de base pour un employé avant une date spécifiée.
        
        Args:
            employee_id (str): ID de l'employé
            month_year (str): Mois et année (format: YYYY-MM)
        
        Returns:
            dict: Informations du salaire ou None
        """
        try:
            
            base = frappe.db.sql("""
                select avg(amount) as moyen from `tabSalary Detail` 
where salary_component = "Salaire Base"
            """, as_dict=True)
            
            if base:
                return {
                    'moyen': base[0].moyen
                }
            
            return None
        
        except Exception as e:
            frappe.log_error(f"Erreur lors de la recherche de salaire de base : {str(e)}", "Salary History Error")
            return {
                "status": "error",
                "message": str(e),
                "data": None
            }