# hrms/services/salary_adjuster_service.py

import frappe
import copy
from hrms.models.salary_slip_filter import SalarySlipFilter
from hrms.models.salary_structure import SalaryStructureModel

class SalaryAdjusterService:
    """Service pour ajuster les salaires de base des fiches de paie."""
    
    def __init__(self):
        self.filter_model = SalarySlipFilter()
        self.structure_model = SalaryStructureModel()
    
    def adjust_salary_slips(self, salary_component, reference_amount, comparison_type, adjustment_type, adjustment_rate):
        """
        Ajuste les salaires de base selon les critères spécifiés.
        
        Args:
            salary_component (str): Composant de salaire à filtrer
            reference_amount (float): Montant de référence
            comparison_type (int): 0 pour inférieur, 1 pour supérieur
            adjustment_type (int): 0 pour augmenter, 1 pour diminuer
            adjustment_rate (float): Pourcentage d'ajustement
        
        Returns:
            dict: Résultat de l'opération
        """
        self._validate_parameters(adjustment_rate, reference_amount, adjustment_type)
        
        updated_slips = []
        errors = []
        
        try:
            # Récupérer les fiches de paie à traiter
            salary_slips = self.filter_model.get_filtered_salary_slips(
                salary_component, reference_amount, comparison_type
            )
            
            if not salary_slips:
                return self._format_empty_response()
            
            # Traiter chaque fiche individuellement
            for slip_data in salary_slips:
                result = self._process_single_slip(slip_data, adjustment_type, adjustment_rate)
                
                if result['success']:
                    updated_slips.append(result['data'])
                else:
                    errors.append(result['error'])
            
            frappe.db.commit()
            
            return self._format_final_response(updated_slips, errors, len(salary_slips))
            
        except Exception as e:
            frappe.db.rollback()
            frappe.log_error(f"Erreur générale lors de l'ajustement : {str(e)}", "Salary Adjustment Error")
            return self._format_error_response(str(e))
    
    def _validate_parameters(self, adjustment_rate, reference_amount, adjustment_type):
        """Valide les paramètres d'entrée."""
        if adjustment_rate < 0:
            raise ValueError("Le taux ne peut pas être négatif")
        
        if reference_amount < 0:
            raise ValueError("Le montant de référence ne peut pas être négatif")
        
        if adjustment_type not in [0, 1]:
            raise ValueError("Le type d'ajustement doit être 0 (augmenter) ou 1 (diminuer)")
    
    def _process_single_slip(self, slip_data, adjustment_type, adjustment_rate):
        """
        Traite une seule fiche de paie.
        
        Returns:
            dict: Résultat du traitement avec 'success', 'data' ou 'error'
        """
        try:
            slip_name = slip_data['parent']
            current_base = float(slip_data['amount'])
            structure_name = slip_data['salary_structure']
            
            # Sauvegarder les informations avant suppression
            slip_info = self._extract_slip_info(slip_name)
            
            # Supprimer l'ancienne fiche
            self._cancel_and_delete_slip(slip_name)
            
            # Calculer le nouveau salaire
            new_base = self._calculate_new_base_salary(current_base, adjustment_type, adjustment_rate)
            
            # Traiter la structure de salaire
            original_structure = self._backup_and_modify_structure(structure_name, new_base)
            
            # Créer la nouvelle fiche
            new_slip_name = self._create_new_salary_slip(slip_info, structure_name)
            
            # Restaurer la structure originale
            self._restore_structure(structure_name, original_structure)
            
            return {
                'success': True,
                'data': self._format_slip_result(slip_info, new_slip_name, structure_name, 
                                               current_base, new_base, adjustment_rate, adjustment_type)
            }
            
        except Exception as e:
            error_msg = f"Erreur lors du traitement du slip {slip_data.get('parent', 'inconnu')}: {str(e)}"
            frappe.log_error(error_msg, "Single Slip Processing Error")
            return {
                'success': False,
                'error': error_msg
            }
    
    def _extract_slip_info(self, slip_name):
        """Extrait les informations d'une fiche de paie."""
        salary_slip = frappe.get_doc("Salary Slip", slip_name)
        return {
            'name': slip_name,
            'employee': salary_slip.employee,
            'employee_name': salary_slip.employee_name,
            'start_date': salary_slip.start_date,
            'end_date': salary_slip.end_date,
            'posting_date': salary_slip.posting_date,
            'company': salary_slip.company
        }
    
    def _cancel_and_delete_slip(self, slip_name):
        """Annule et supprime une fiche de paie."""
        salary_slip = frappe.get_doc("Salary Slip", slip_name)
        if salary_slip.docstatus == 1:
            salary_slip.cancel()
        frappe.delete_doc("Salary Slip", slip_name, ignore_permissions=False)
        print(f"Cancelled and deleted salary slip {slip_name}")
    
    def _calculate_new_base_salary(self, current_base, adjustment_type, adjustment_rate):
        """Calcule le nouveau salaire de base."""
        if adjustment_type == 0:  # Augmenter
            return current_base + (current_base * (adjustment_rate / 100))
        else:  # Diminuer
            return current_base - (current_base * (adjustment_rate / 100))
    
    def _backup_and_modify_structure(self, structure_name, new_base):
        """Sauvegarde et modifie la structure de salaire."""
        salary_structure = frappe.get_doc("Salary Structure", structure_name)
        
        # Sauvegarder l'état original
        original_structure = {
            'earnings': copy.deepcopy(salary_structure.earnings),
            'deductions': copy.deepcopy(salary_structure.deductions)
        }
        
        # Modifier le composant Salaire Base
        base_updated = False
        for earning in salary_structure.earnings:
            if earning.salary_component == "Salaire Base" or earning.abbr in ["SB", "sb"]:
                earning.formula = str(new_base)
                base_updated = True
                print(f"Updated base salary to {new_base} for {earning.salary_component}")
                break
        
        if not base_updated:
            raise Exception(f"Composant Salaire Base non trouvé dans la structure {structure_name}")
        
        # Sauvegarder et soumettre
        salary_structure.save()
        salary_structure.submit()
        
        return original_structure
    
    def _create_new_salary_slip(self, slip_info, structure_name):
        """Crée une nouvelle fiche de paie."""
        salary_slip_data = {
            "doctype": "Salary Slip",
            "employee": slip_info['employee'],
            "employee_name": slip_info['employee_name'],
            "salary_structure": structure_name,
            "start_date": slip_info['start_date'],
            "end_date": slip_info['end_date'],
            "posting_date": slip_info['posting_date'],
            "company": slip_info['company']
        }
        
        slip_doc = frappe.get_doc(salary_slip_data)
        slip_doc.insert()
        
        # Vérifier et ajouter le composant Salaire Base si nécessaire
        self._ensure_base_salary_component(slip_doc)
        
        slip_doc.submit()
        print(f"Created new salary slip {slip_doc.name}")
        
        return slip_doc.name
    
    def _ensure_base_salary_component(self, slip_doc):
        """Assure que le composant Salaire Base existe dans la fiche."""
        salary_detail_exists = frappe.db.exists({
            "doctype": "Salary Detail",
            "parenttype": "Salary Slip",
            "parent": slip_doc.name,
            "salary_component": "Salaire Base"
        })
        
        if not salary_detail_exists:
            # Récupérer le montant depuis la structure
            structure = frappe.get_doc("Salary Structure", slip_doc.salary_structure)
            base_amount = None
            
            for earning in structure.earnings:
                if earning.salary_component == "Salaire Base":
                    base_amount = float(earning.formula) if earning.formula.isdigit() else 0
                    break
            
            if base_amount:
                slip_doc.append("earnings", {
                    "salary_component": "Salaire Base",
                    "amount": base_amount,
                    "abbr": "SB"
                })
                slip_doc.save()
                print(f"Manually added Salaire Base to Salary Slip {slip_doc.name}")
    
    def _restore_structure(self, structure_name, original_structure):
        """Restaure la structure de salaire à son état original."""
        try:
            salary_structure = frappe.get_doc("Salary Structure", structure_name)
            salary_structure.earnings = original_structure['earnings']
            salary_structure.deductions = original_structure['deductions']
            salary_structure.save()
            salary_structure.submit()
            print(f"Restored original salary structure {structure_name}")
        except Exception as e:
            frappe.log_error(f"Erreur lors de la restauration de la structure {structure_name}: {str(e)}", 
                           "Structure Restore Error")
    
    def _format_slip_result(self, slip_info, new_slip_name, structure_name, old_base, new_base, rate, adj_type):
        """Formate le résultat pour un slip traité."""
        return {
            "old_slip": slip_info['name'],
            "new_slip": new_slip_name,
            "employee": slip_info['employee_name'],
            "period": f"{slip_info['start_date']} - {slip_info['end_date']}",
            "structure_name": structure_name,
            "old_base": old_base,
            "new_base": new_base,
            "adjustment_percentage": rate,
            "adjustment_type": "increase" if adj_type == 0 else "decrease"
        }
    
    def _format_empty_response(self):
        """Formate la réponse quand aucun slip n'est trouvé."""
        return {
            "status": "success",
            "updated_slips": [],
            "errors": [],
            "total_processed": 0,
            "message": "Aucun salary slip trouvé pour les critères donnés."
        }
    
    def _format_final_response(self, updated_slips, errors, total_processed):
        """Formate la réponse finale."""
        return {
            "status": "success" if not errors else "partial_success",
            "updated_slips": updated_slips,
            "errors": errors,
            "total_processed": total_processed,
            "message": f"Traitement terminé. {len(updated_slips)} slip(s) modifié(s) avec nouveau salaire de base."
        }
    
    def _format_error_response(self, error_message):
        """Formate la réponse d'erreur."""
        return {
            "status": "error",
            "updated_slips": [],
            "errors": [f"Erreur générale: {error_message}"],
            "total_processed": 0,
            "message": "Échec du traitement - rollback effectué"
        }