# hrms/controllers/generator_controller.py

import frappe
from frappe import _
from hrms.services.payroll_generator_service import PayrollGeneratorService
from hrms.services.salary_adjuster_service import SalaryAdjusterService
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