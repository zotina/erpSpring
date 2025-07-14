# hrms/controllers/payroll_controller.py

import frappe
from frappe import _
from frappe.utils import get_last_day
from hrms.services.payroll_service import PayrollService
from hrms.models.payroll_data import PayrollData


@frappe.whitelist(allow_guest=False)
def get_monthly_payroll_summary(year):
    """
    Endpoint: /api/method/hrms.controllers.payroll_controller.get_monthly_payroll_summary?year=2024
    Method: GET
    Retourne les totaux brut, net et déductions pour chaque mois de l'année ayant des Salary Slips
    """
    try:
        service = PayrollService()
        result = service.get_monthly_summary(year)
        
        if not result:
            _set_error_response(
                f"Aucun bulletin de paie trouvé pour l'année {year}.",
                ["No salary slips found"]
            )
            return
        
        _set_success_response(
            f"Récapitulatif mensuel des salaires pour l'année {year} récupéré avec succès.",
            result
        )

    except Exception as e:
        _set_error_response(
            "Une erreur s'est produite lors de la récupération du récapitulatif mensuel des salaires.",
            [str(e)]
        )


@frappe.whitelist(allow_guest=False)
def get_payroll_components(year_month, employee_id=None):
    """
    Endpoint: /api/method/hrms.controllers.payroll_controller.get_payroll_components?year_month=2024-01&employee_id=EMP-001
    Method: GET
    Retourne le détail des composants de salaire pour un mois donné avec formatage visuel
    """
    service = PayrollService()
    return service.get_payroll_components_for_month(year_month, employee_id)


@frappe.whitelist(allow_guest=False)
def get_salary_evolution_data(year):
    """
    Endpoint: /api/method/hrms.controllers.payroll_controller.get_salary_evolution_data?year=2024
    Method: GET
    Retourne les données mensuelles de salaire pour l'année spécifiée
    """
    if not year:
        frappe.throw(_("L'année doit être spécifiée."))
    
    service = PayrollService()
    return service.get_salary_evolution(year)


def _set_success_response(message, data):
    """Configure une réponse de succès"""
    frappe.local.response["status"] = "success"
    frappe.local.response["message"] = message
    frappe.local.response["data"] = data


def _set_error_response(message, validation_errors=None):
    """Configure une réponse d'erreur"""
    frappe.local.response["status"] = "error"
    frappe.local.response["message"] = message
    frappe.local.response["validation_errors"] = validation_errors or []