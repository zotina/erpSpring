# hrms/controllers/generator_controller.py

import frappe
from frappe import _
from hrms.services.payroll_generator_service import PayrollGeneratorService
from hrms.services.salary_adjuster_service import SalaryAdjusterService
from hrms.services.global_salary_adjuster_service import GlobalSalaryAdjusterService
from hrms.services.historical_adjuster_service import HistoricalAdjusterService
import json
from hrms.models.salary_moyen import SalaryMoyen

@frappe.whitelist(allow_guest=False)
def insert_slip_period(emp, monthDebut, monthFin, montant, ecraser, moyen):
    """
    API endpoint pour insérer des fiches de paie pour une période donnée.
    Args:
        emp (str): ID de l'employé
        monthDebut (str): Mois de début (format: YYYY-MM)
        monthFin (str): Mois de fin (format: YYYY-MM)
        montant (float): Montant du salaire de base (0 pour utiliser le dernier salaire)
        ecraser (int): 0 pour écraser, 1 pour ne pas écraser
        moyen (int): 0 pour utiliser le montant saisi, 1 pour utiliser la moyenne
    Returns:
        dict: Résultat de l'opération avec status, message et data
    """
    try:
        service = PayrollGeneratorService()
        moyen = int(moyen)
        
        if moyen == 0:
            salary_moyen = SalaryMoyen()
            montant = float(salary_moyen.get_moyen()['moyen'])
        else:
            montant = float(montant or 0)
        
        return service.generate_payroll_period(emp, monthDebut, monthFin, montant, int(ecraser))
        
    except Exception as e:
        frappe.log_error(f"Erreur dans insert_slip_period: {str(e)}", "API Error")
        return {
            "status": "error",
            "message": str(e),
            "data": None
        }

@frappe.whitelist(allow_guest=False)
def updateBaseAssignement(salary_component, montant, infOrSup, minusOrPlus, taux):
    """
    API endpoint pour mettre à jour le salaire de base des fiches de paie selon des critères.
    
    Args:
        salary_component (str): Composant de salaire à filtrer
        montant (float): Montant de référence pour le filtreint
        infOrSup (int): 0 pour inférieur, 1 pour supérieur
        minusOrPlus (int): 0 pour augmenter, 1 pour diminuer
        taux (str/float): Pourcentage de modification
    
    Returns:
        dict: Résultat de l'opération avec détails des modifications
    """
    try:
        service = SalaryAdjusterService()
        return service.adjust_salary_slips(
            salary_component=salary_component,
            reference_amount=float(montant),
            comparison_type=int(infOrSup),
            adjustment_type=int(minusOrPlus),
            adjustment_rate=float(taux)
        )
    except Exception as e:
        frappe.log_error(f"Erreur dans updateBaseAssignement: {str(e)}", "API Error")
        return {
            "status": "error",
            "updated_slips": [],
            "errors": [f"Erreur générale: {str(e)}"],
            "total_processed": 0,
            "message": "Échec du traitement - rollback effectué"
        }


@frappe.whitelist(allow_guest=False)
def adjust_base_salary_for_month(month_year, percentage_value):
    """
    API endpoint to adjust the base salary of all salary slips for a given month.
    
    Args:
        month_year (str): The target month in 'YYYY-MM' format.
        percentage_value (str/float): The percentage for adjustment.
        
    Returns:
        dict: Result of the operation.
    """
    try:
        service = GlobalSalaryAdjusterService()
        return service.adjust_salaries_for_month(
            month_year=str(month_year),
            percentage_value=float(percentage_value)
        )
    except Exception as e:
        print(f"Error in adjust_base_salary_for_month: {str(e)}", "API Error")
        return {
            "status": "error",
            "message": f"Failed to initiate adjustment: {str(e)}",
            "data": None
        }


@frappe.whitelist(allow_guest=False)
def reapply_historical_adjustments(adjustments):
    """
    API endpoint to re-apply a list of historical adjustments.
    Expects a JSON string representing a list of adjustment objects.
    """
    try:
        # The payload from Spring will be a JSON string
        adjustments_list = json.loads(adjustments)
        
        service = HistoricalAdjusterService()
        result = service.apply_monthly_adjustments(adjustments_list)
        return result
        
    except Exception as e:
        frappe.log_error(f"Error in reapply_historical_adjustments: {e}", "API Error")
        return {
            "status": "error",
            "message": f"Failed to initiate historical adjustment: {e}",
            "data": [],
            "errors": [str(e)]
        }