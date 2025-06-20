# hrms/hrms/doctype/fiscal_year_generator/fiscal_year_generator.py

import frappe
from frappe import _
from frappe.utils import getdate, add_years, get_last_day
import json

@frappe.whitelist(allow_guest=False)
def create_fiscal_years(anneeMin, anneeMax):
    """
    Endpoint: /api/method/hrms.controllers.util_controller.create_fiscal_years?anneeMin=1999&anneeMax=2030
    Method: GET
    Retourne les totaux brut, net et déductions pour chaque mois de l'année ayant des Salary Slips
    """
    try:
        # Validation des paramètres
        annee_min = int(anneeMin)
        annee_max = int(anneeMax)
        
        if annee_min > annee_max:
            frappe.throw(_("L'année minimum ne peut pas être supérieure à l'année maximum"))
        
        created_fiscal_years = []
        existing_fiscal_years = []
        errors = []
        
        # Boucle pour créer chaque année fiscale
        for year in range(annee_min, annee_max + 1):
            try:
                fiscal_year_name = str(year)
                
                # Vérifier si l'année fiscale existe déjà
                if frappe.db.exists("Fiscal Year", fiscal_year_name):
                    existing_fiscal_years.append(fiscal_year_name)
                    continue
                
                # Dates de début et fin de l'année fiscale
                year_start_date = f"{year}-01-01"
                year_end_date = f"{year}-12-31"
                
                # Créer le document Fiscal Year
                fiscal_year = frappe.get_doc({
                    "doctype": "Fiscal Year",
                    "year": fiscal_year_name,
                    "year_start_date": year_start_date,
                    "year_end_date": year_end_date,
                    "auto_created": 1
                })
                
                fiscal_year.insert(ignore_permissions=True)
                created_fiscal_years.append(fiscal_year_name)
                
            except Exception as e:
                error_msg = f"Erreur lors de la création de l'année {year}: {str(e)}"
                errors.append(error_msg)
                frappe.log_error(error_msg, "Fiscal Year Creation Error")
        
        # Commit des changements
        frappe.db.commit()
        
        return {
            "status": "success",
            "message": f"Traitement terminé pour les années {annee_min} à {annee_max}",
            "data": {
                "created_fiscal_years": created_fiscal_years,
                "existing_fiscal_years": existing_fiscal_years,
                "errors": errors,
                "total_created": len(created_fiscal_years),
                "total_existing": len(existing_fiscal_years),
                "total_errors": len(errors)
            }
        }
        
    except ValueError:
        frappe.throw(_("Les paramètres anneeMin et anneeMax doivent être des entiers valides"))
    except Exception as e:
        frappe.log_error(f"Erreur dans create_fiscal_years: {str(e)}", "API Error")
        return {
            "status": "error",
            "message": str(e),
            "data": None
        }


@frappe.whitelist(allow_guest=False)
def delete_fiscal_years_range(anneeMin, anneeMax, confirm=False):
    """
    API pour supprimer les années fiscales dans une plage donnée
    
    Args:
        anneeMin (int): Année minimum
        anneeMax (int): Année maximum
        confirm (bool): Confirmation de suppression
    
    Returns:
        dict: Résultat de l'opération
    """
    try:
        if not confirm:
            return {
                "status": "warning",
                "message": "Veuillez confirmer la suppression en passant confirm=1",
                "data": None
            }
        
        annee_min = int(anneeMin)
        annee_max = int(anneeMax)
        
        deleted_years = []
        errors = []
        
        for year in range(annee_min, annee_max + 1):
            try:
                fiscal_year_name = str(year)
                
                if frappe.db.exists("Fiscal Year", fiscal_year_name):
                    # Vérifier s'il y a des transactions liées
                    linked_docs = frappe.get_all(
                        "GL Entry",
                        filters={"fiscal_year": fiscal_year_name},
                        limit=1
                    )
                    
                    if linked_docs:
                        errors.append(f"Impossible de supprimer l'année {year}: des transactions existent")
                        continue
                    
                    frappe.delete_doc("Fiscal Year", fiscal_year_name)
                    deleted_years.append(fiscal_year_name)
                    
            except Exception as e:
                error_msg = f"Erreur lors de la suppression de l'année {year}: {str(e)}"
                errors.append(error_msg)
        
        frappe.db.commit()
        
        return {
            "status": "success",
            "message": f"Suppression terminée pour les années {annee_min} à {annee_max}",
            "data": {
                "deleted_years": deleted_years,
                "errors": errors,
                "total_deleted": len(deleted_years),
                "total_errors": len(errors)
            }
        }
        
    except Exception as e:
        frappe.log_error(f"Erreur dans delete_fiscal_years_range: {str(e)}", "API Error")
        return {
            "status": "error",
            "message": str(e),
            "data": None
        }