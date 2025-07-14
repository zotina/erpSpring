# hrms/models/payroll_data.py

import frappe


class PayrollData:
    """Modèle pour l'accès aux données de paie depuis la base de données"""
    
    def get_salary_slips_for_period(self, start_date, end_date):
        """
        Récupère les bulletins de salaire pour une période donnée
        
        Args:
            start_date (str): Date de début
            end_date (str): Date de fin
            
        Returns:
            list: Liste des bulletins de salaire
        """
        return frappe.get_all(
            "Salary Slip",
            fields=["posting_date", "net_pay", "total_deduction", "employee", "currency", "gross_pay"],
            filters={"posting_date": ["between", [start_date, end_date]]},
            order_by="posting_date"
        )
    
    def get_salary_slips_with_details(self, filters):
        """
        Récupère les bulletins de salaire avec les détails de base
        
        Args:
            filters (dict): Filtres pour the requête
            
        Returns:
            list: Liste des bulletins avec détails
        """
        return frappe.get_all(
            "Salary Slip",
            fields=["name", "employee", "employee_name", "gross_pay", "total_deduction", "net_pay", "currency"],
            filters=filters
        )
    
    def get_salary_slips_with_names_for_period(self, start_date, end_date):
        """
        Récupère les bulletins de salaire avec les noms pour une période
        
        Args:
            start_date (str): Date de début
            end_date (str): Date de fin
            
        Returns:
            list: Liste des bulletins avec noms
        """
        return frappe.get_all(
            "Salary Slip",
            fields=["posting_date", "net_pay", "total_deduction", "employee", "currency", "gross_pay", "name"],
            filters={"posting_date": ["between", [start_date, end_date]]},
            order_by="posting_date"
        )
    
    def get_salary_details(self, slip_name, detail_type):
        """
        Récupère les détails de salaire (gains ou déductions) pour un bulletin
        
        Args:
            slip_name (str): Nom du bulletin de salaire
            detail_type (str): Type de détail ('earnings' ou 'deductions')
            
        Returns:
            list: Liste des détails de salaire
        """
        return frappe.get_all(
            "Salary Detail",
            fields=["salary_component", "amount"],
            filters={
                "parent": slip_name, 
                "parentfield": detail_type
            }
        )