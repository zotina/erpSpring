# hrms/services/payroll_generator_service.py

import frappe
from frappe.utils import getdate, get_last_day
from dateutil.relativedelta import relativedelta
from hrms.dto.payroll_dto import PayrollDTO
from hrms.services.hrms_insertion import insert_salary_assignments, insert_salary_slips
from hrms.models.salary_history import SalaryHistory
from hrms.models.salary_structure import SalaryStructureModel

class PayrollGeneratorService:
    """Service pour générer et insérer des fiches de paie sur une période."""
    
    def __init__(self):
        self.salary_history = SalaryHistory()
        self.salary_structure_model = SalaryStructureModel()
    
    def generate_payroll_period(self, employee_id, start_month, end_month, base_amount,ecraser):
        """
        Génère des fiches de paie pour un employé sur une période donnée.
        
        Args:
            employee_id (str): ID de l'employé
            start_month (str): Mois de début (YYYY-MM)
            end_month (str): Mois de fin (YYYY-MM)
            base_amount (float): Montant du salaire de base (0 pour automatique)
        
        Returns:
            dict: Résultat de l'opération
        """
        try:
            frappe.db.begin()
            frappe.log_error("Transaction started for payroll insertion", "Payroll Transaction")

            # Générer les DTOs de paie
            dto_result = self._generate_payroll_dtos(employee_id, start_month, end_month, base_amount,ecraser)
            
            if dto_result.get("status") == "error":
                frappe.db.rollback()
                return dto_result
            
            dto_list = dto_result.get("data", [])
            
            print(dto_list)
            
            if not dto_list:
                frappe.db.rollback()
                return {
                    "status": "warning",
                    "message": "Aucune fiche de paie à créer pour la période spécifiée",
                    "data": []
                }
            
            # Insérer les données
            self._insert_payroll_data(dto_list)
            
            frappe.db.commit()
            frappe.log_error("Transaction committed successfully", "Payroll Transaction")
            
            return self._format_success_response(dto_list)
            
        except Exception as e:
            frappe.db.rollback()
            frappe.log_error(f"Erreur lors de la génération de la paie : {str(e)}", "API Error")
            return {
                "status": "error",
                "message": str(e),
                "data": None
            }
    
    def _generate_payroll_dtos(self, employee_id, start_month, end_month, base_amount,ecraser):
        """Génère la liste des DTOs de paie pour la période."""
        try:
            start_date = getdate(f"{start_month}-01")
            end_date = getdate(f"{end_month}-01")
            result_list = []
            
            current_date = start_date
            while current_date <= end_date:
                try:
                    # Vérifier si la fiche existe déjà
                    print("process")
                    print(self._salary_slip_exists(employee_id, current_date))
                    existing = frappe.db.exists("Salary Slip", {
                        "employee": employee_id,
                        "start_date": current_date
                    })
                    print("existing ")
                    print(existing)
                    
                    if existing :
                        if ecraser == 0:
                            print(f"Salary slip already exists for {employee_id} for {current_date.strftime('%Y-%m')}")
                            current_date += relativedelta(months=1)
                            continue
                    
                    # Obtenir les informations salariales
                    salary_info = self._get_salary_info(employee_id, current_date, base_amount)
                    if not salary_info:
                        current_date += relativedelta(months=1)
                        continue
                    
                    # Créer le DTO
                    dto = self._create_payroll_dto(employee_id, current_date, salary_info)
                    result_list.append(dto)
                    
                    current_date += relativedelta(months=1)
                    
                except Exception as e:
                    frappe.log_error(f"Erreur lors de la génération du DTO pour {current_date.strftime('%Y-%m')}: {str(e)}", "Payroll Generation")
                    current_date += relativedelta(months=1)
                    continue
            
            return {
                "status": "success",
                "message": f"Génération réussie de {len(result_list)} DTO(s) de paie",
                "data": result_list
            }
            
        except Exception as e:
            frappe.log_error(f"Erreur lors de la génération des DTOs : {str(e)}", "API Error")
            return {
                "status": "error",
                "message": str(e),
                "data": []
            }
    
    def _salary_slip_exists(self, employee_id, date):
        """Vérifie si une fiche de paie existe déjà pour la période."""
        return frappe.db.exists("Salary Slip", {
            "employee": employee_id,
            "start_date": date,
            "end_date": get_last_day(date)
        })
    
    def _get_salary_info(self, employee_id, date, base_amount):
        """Obtient les informations salariales pour une période."""
        if base_amount > 0:
            return {
                'amount': base_amount,
                'salary_structure': self.salary_structure_model.get_latest_structure()['name']
            }
        else:
            salary_info = self.salary_history.get_last_base_salary(employee_id, date.strftime("%Y-%m"))
            if isinstance(salary_info, dict) and salary_info.get("status") == "error":
                frappe.log_error(f"Erreur de salaire de base: {salary_info.get('message')}", "Payroll Generation")
                return None
            
            if not salary_info:
                frappe.throw(f"Aucun salaire de base trouvé et aucun montant suggéré")
            
            return salary_info
    
    def _create_payroll_dto(self, employee_id, date, salary_info):
        """Crée un DTO de paie."""
        employee_ref = frappe.db.get_value("Employee", {"name": employee_id}, ["ref"])
        
        return PayrollDTO(
            mois=date.strftime("%Y-%m-%d"),
            ref_employe=employee_ref,
            salaire_base=salary_info['amount'],
            salaire=salary_info['salary_structure']
        )
    
    def _insert_payroll_data(self, dto_list):
        """Insère les données de paie."""
        try:
            insert_salary_assignments(dto_list)
            insert_salary_slips(dto_list)
        except Exception as e:
            raise Exception(f"Erreur lors de l'insertion: {str(e)}")
    
    def _format_success_response(self, dto_list):
        """Formate la réponse de succès."""
        return {
            "status": "success",
            "message": f"Fiches de paie créées avec succès pour {len(dto_list)} période(s)",
            "data": [
                {
                    'mois': dto.mois,
                    'ref_employe': dto.ref_employe,
                    'salaire_base': dto.salaire_base,
                    'salaire': dto.salaire
                } for dto in dto_list
            ]
        }