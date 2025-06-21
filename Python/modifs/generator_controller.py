# hrms/controllers/generator_controller

import frappe
from frappe import _
import copy
from frappe.utils import getdate, get_last_day
from dateutil.relativedelta import relativedelta
from hrms.dto.payroll_dto import PayrollDTO
from hrms.services.hrms_insertion import (
    insert_salary_assignments,
    insert_salary_slips
)

def getLastBaseSalaryEmpFromDate(emp, monthYear):
    """
    Récupère le dernier salaire de base pour un employé avant une date spécifiée.
    
    Endpoint: /api/method/hrms.controllers.generator_controller.getLastBaseSalaryEmpFromDate?emp=HR-EMP-00961&monthYear=2025-06
    Méthode: GET
    """
    try:
        target_date = f"{monthYear}-01"
        
        salary_slips = frappe.db.sql("""
            SELECT 
                ss.name,
                ss.posting_date,
                ss.salary_structure,
                sd.amount
            FROM 
                `tabSalary Slip` ss 
            INNER JOIN 
                `tabSalary Detail` sd ON ss.name = sd.parent
            INNER JOIN 
                `tabSalary Component` sc ON sd.salary_component = sc.name
            WHERE
                ss.employee = %(employee)s
                AND ss.posting_date < %(target_date)s
                AND ss.docstatus = 1
                AND sc.type = 'Earning'
                AND LOWER(sc.salary_component_abbr) = 'sb'
            ORDER BY 
                ss.posting_date DESC
            LIMIT 1
        """, {
            'employee': emp,
            'target_date': target_date
        }, as_dict=True)
        
        if salary_slips:
            return {
                'amount': salary_slips[0].amount,
                'posting_date': salary_slips[0].posting_date,
                'salary_structure': salary_slips[0].salary_structure 
            }
    
    except Exception as e:
        frappe.log_error(f"Erreur lors de la recherche de salaire de base : {str(e)}", "API Error")
        return {
            "status": "error",
            "message": str(e),
            "data": None
        }

def getMaxStructureSalary():
    slary_structure = frappe.db.sql("""
        select name from `tabSalary Structure` where creation = (select max(creation) from `tabSalary Structure`) limit 1;
    """, as_dict=True)
    
    if slary_structure:
        return {
            'name': slary_structure[0].name
        }
    else:
        frappe.throw(f"Aucun structure trouve")

    
def generatePayrollDTO(emp, monthDebut, monthFin,montant):
    """
    Génère des objets PayrollDTO pour un employé entre deux mois spécifiés.
    """
    try:
        dateStart = getdate(f"{monthDebut}-01")
        dateEnd = getdate(f"{monthFin}-01")
        result_list = []
        
        current_date = dateStart
        while current_date <= dateEnd:
            try:
                last_baseSalaryInfo = getLastBaseSalaryEmpFromDate(emp, current_date.strftime("%Y-%m"))
                
                if isinstance(last_baseSalaryInfo, dict) and last_baseSalaryInfo.get("status") == "error":
                    frappe.log_error(f"Erreur de salaire de base: {last_baseSalaryInfo.get('message')}", "Payroll Generation")
                    current_date += relativedelta(months=1)
                    continue
                
                if montant > 0:
                    last_baseSalaryInfo = {
                            'amount': montant,
                            'salary_structure': getMaxStructureSalary()['name']    
                        }
                else :
                    if not last_baseSalaryInfo:
                            frappe.throw(f"Aucun salaire de base trouvé et aucun montant suggeré")
                print(emp)
                employee_ref = frappe.db.get_value("Employee", {"name": emp}, ["ref"])
                existing_slip = frappe.db.exists("Salary Slip", {
                    "employee": emp,
                    "start_date": current_date,
                    "end_date": get_last_day(current_date)
                })
                
                if existing_slip:
                    print(f"Salary slip already exists for {emp} for {current_date.strftime('%Y-%m')}")
                    current_date += relativedelta(months=1)
                    continue
                
                dto = PayrollDTO(
                    mois=current_date.strftime("%Y-%m-%d"),
                    ref_employe=employee_ref,
                    salaire_base=last_baseSalaryInfo['amount'],
                    salaire=last_baseSalaryInfo['salary_structure']
                )
                
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
        frappe.log_error(f"Erreur lors de la génération des DTOs de paie : {str(e)}", "API Error")
        return {
            "status": "error",
            "message": str(e),
            "data": []
        }

@frappe.whitelist(allow_guest=False)
def insert_slip_period(emp, monthDebut, monthFin,montant):
    """
    Insère des fiches de paie pour une période donnée avec gestion de transaction.
    """
    try: 
        frappe.db.begin()
        print("Transaction started for payroll insertion", "Payroll Transaction")

        dto_result = generatePayrollDTO(emp, monthDebut, monthFin,montant)
         
        if dto_result.get("status") == "error":
            frappe.db.rollback()
            return dto_result
        
        dto_list = dto_result.get("data", [])
        print("payroll")
        print(dto_list)
        
        if not dto_list:
            frappe.db.rollback()
            return {
                "status": "warning",
                "message": "Aucune fiche de paie à créer pour la période spécifiée",
                "data": []
            }
                
        try:
            insert_salary_assignments(dto_list)
            insert_salary_slips(dto_list)
        except Exception as insertion_error:
            frappe.db.rollback()
            frappe.log_error(f"Erreur lors de l'insertion: {str(insertion_error)}", "Payroll Insertion")
            return {
                "status": "error",
                "message": f"Erreur lors de l'insertion: {str(insertion_error)}",
                "data": None
            }
        
        frappe.db.commit()
        print("Transaction committed successfully", "Payroll Transaction")
        
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
            
    except Exception as e:
        frappe.db.rollback()
        frappe.log_error(f"Erreur lors de l'insertion des fiches de paie : {str(e)}", "API Error")
        return {
            "status": "error",
            "message": str(e),
            "data": None
        }
        
def getFiltredSalary(salary_component, montant, infOrSup):
    if not salary_component or not montant:
        frappe.throw("Paramètres salary_component et montant requis")
    
    try:
        montant = float(montant)
        infOrSup = int(infOrSup)
    except (ValueError, TypeError):
        frappe.throw("Paramètres montant et infOrSup doivent être numériques")
    
    if infOrSup not in [0, 1]:
        frappe.throw("infOrSup doit être 0 (inférieur) ou 1 (supérieur)")
    
    if infOrSup == 0:
        condition_sql = "< %(montant)s"
    else:
        condition_sql = "> %(montant)s"
    
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
        salaire_base = frappe.db.sql(query, {
            'salary_component': salary_component,
            'montant': montant
        }, as_dict=True)
        return salaire_base
    except Exception as e:
        frappe.log_error(f"Erreur dans getFiltredSalary: {str(e)}")
        frappe.throw("Erreur lors de l'exécution de la requête")

def updateDetailBase(slip_name, newBase):
    """
    Updates the base salary amount in Salary Detail for a given Salary Slip.
    
    Args:
        slip_name (str): Name of the Salary Slip document
        newBase (float): New base salary amount
    
    Returns:
        dict: Status of the operation
    """
    try:
        # Validate inputs
        if not slip_name or not newBase:
            frappe.throw(_("Salary Slip name and new base amount are required"))
            
        # Convert newBase to float and validate
        try:
            newBase = float(newBase)
            if newBase < 0:
                frappe.throw(_("Base amount cannot be negative"))
        except ValueError:
            frappe.throw(_("New base amount must be a valid number"))

        # Check if Salary Slip exists
        if not frappe.db.exists("Salary Slip", slip_name):
            frappe.throw(_("Salary Slip {0} does not exist").format(slip_name))

        # Update Salary Detail
        updated = frappe.db.sql("""
            UPDATE `tabSalary Detail`
            SET amount = %s
            WHERE parenttype = 'Salary Slip'
            AND salary_component = 'Salaire Base'
            AND parent = %s
        """, (newBase, slip_name))

        # Check if any rows were affected
        if updated:
            # Commit the transaction
            frappe.db.commit()
            
            # Recalculate totals in Salary Slip
            salary_slip = frappe.get_doc("Salary Slip", slip_name)
            salary_slip.calculate_totals()
            salary_slip.db_update()
            
            return {
                "status": "success",
                "message": _("Base salary updated successfully for Salary Slip {0}").format(slip_name)
            }
        else:
            frappe.throw(_("No matching Salary Detail found for Salaire Base in Salary Slip {0}").format(slip_name))

    except Exception as e:
        frappe.db.rollback()
        frappe.log_error(f"Error updating base salary for {slip_name}: {str(e)}")
        return {
            "status": "error",
            "message": str(e)
        }
        

@frappe.whitelist(allow_guest=False)
def updateBaseAssignement(salary_component, montant, infOrSup, minusOrPlus, taux):
    """
    Met à jour le salaire de base pour chaque salary slip individuellement en modifiant la structure de salaire associée,
    recalcule le slip avec le nouveau salaire de base, et restaure la structure à son état original après chaque slip.
    
    Args:
        salary_component (str): Composant de salaire à filtrer
        montant (float): Montant de référence pour le filtre
        infOrSup (int): 0 pour inférieur, 1 pour supérieur
        minusOrPlus (int): 0 pour augmenter, 1 pour diminuer
        taux (str/float): Pourcentage de modification
    """
    updated_slips = []
    errors = []

    try:
        # Convertir taux en float
        taux = float(taux) if isinstance(taux, (str, int, float)) else 0.0
        if taux < 0:
            raise ValueError("Le taux ne peut pas être négatif")

        # Convertir montant en float
        montant = float(montant) if isinstance(montant, (str, int, float)) else 0.0
        minusOrPlus = int(minusOrPlus)

        # Récupérer les salary slips filtrés
        salary_slips = getFiltredSalary(salary_component, montant, infOrSup)

        if not salary_slips:
            return {
                "status": "success",
                "updated_slips": [],
                "errors": [],
                "total_processed": 0,
                "message": "Aucun salary slip trouvé pour les critères donnés."
            }

        # Traiter chaque salary slip individuellement
        for slip_data in salary_slips:
            try:
                slip_name = slip_data.parent
                current_base = float(slip_data.amount)
                structure_name = slip_data.salary_structure

                # Annuler et supprimer le salary slip
                try:
                    salary_slip = frappe.get_doc("Salary Slip", slip_name)
                    if salary_slip.docstatus == 1:  # Submitted
                        salary_slip.cancel()
                    slip_info = {
                        'name': slip_name,
                        'employee': salary_slip.employee,
                        'employee_name': salary_slip.employee_name,
                        'start_date': salary_slip.start_date,
                        'end_date': salary_slip.end_date,
                        'posting_date': salary_slip.posting_date,
                        'company': salary_slip.company
                    }
                    frappe.delete_doc("Salary Slip", slip_name, ignore_permissions=False)
                    print(f"Cancelled and deleted salary slip {slip_name}")
                except Exception as slip_error:
                    error_msg = f"Erreur lors de l'annulation ou suppression du slip {slip_name}: {str(slip_error)}"
                    errors.append(error_msg)
                    frappe.log_error(error_msg, "Salary Slip Cancellation/Deletion Error")
                    continue

                # Calculer le nouveau salaire de base
                if minusOrPlus == 0:  # Augmenter
                    new_base = current_base + (current_base * (taux / 100))
                    base_adjusted_expr = f"(SB - (SB * {taux / 100}))"
                else:  # Diminuer
                    new_base = current_base - (current_base * (taux / 100))
                    base_adjusted_expr = f"(SB + (SB * {taux / 100}))"

                # Charger la structure de salaire et sauvegarder son état initial
                salary_structure = frappe.get_doc("Salary Structure", structure_name)
                original_structure = {
                    'earnings': copy.deepcopy(salary_structure.earnings),
                    'deductions': copy.deepcopy(salary_structure.deductions)
                }

                # Mettre à jour le composant "Salaire Base"
                base_component_updated = False
                for earning in salary_structure.earnings:
                    if earning.salary_component == "Salaire Base" or earning.abbr in ["SB", "sb"]:
                        earning.formula = str(new_base)  # Clear formula to use fixed amount
                        base_component_updated = True
                        print(f"Updated base salary to {new_base} for {earning.salary_component} in structure {structure_name}")
                        break

                if not base_component_updated:
                    errors.append(f"Composant Salaire Base non trouvé dans la structure {structure_name} pour le slip {slip_name}")
                    continue

                # # Ajuster les formules des autres composants dépendant du salaire de base
                # for earning in salary_structure.earnings:
                #     if earning.salary_component != "Salaire Base" and earning.formula:
                #         formula = earning.formula.lower()
                #         if 'sb' in formula or 'base' in formula:
                #             try:
                #                 new_formula = earning.formula.replace('SB', str(new_base)).replace('base', str(new_base))
                #                 earning.formula = new_formula
                #                 print(f"Adjusted formula for {earning.salary_component}: {earning.formula}")
                #             except Exception as formula_error:
                #                 print(f"Error adjusting formula for {earning.salary_component}: {str(formula_error)}")

                # # Ajuster les déductions
                # for deduction in salary_structure.deductions:
                #     if deduction.formula:
                #         formula = deduction.formula.lower()
                #         if 'sb' in formula or 'base' in formula:
                #             try:
                #                 new_formula = deduction.formula.replace('SB', str(new_base)).replace('base', str(new_base))
                #                 deduction.formula = new_formula
                #                 print(f"Adjusted deduction formula for {deduction.salary_component}: {deduction.formula}")
                #             except Exception as formula_error:
                #                 print(f"Error adjusting deduction formula for {deduction.salary_component}: {str(formula_error)}")

                # Sauvegarder et soumettre la structure modifiée
                try:
                    salary_structure.save()
                    salary_structure.submit()
                    print(f"Updated and submitted salary structure {structure_name} for slip {slip_name}")
                except Exception as save_error:
                    error_msg = f"Erreur lors de la sauvegarde de la structure {structure_name} pour le slip {slip_name}: {str(save_error)}"
                    errors.append(error_msg)
                    frappe.log_error(error_msg, "Salary Structure Save Error")
                    continue

                # Recréer le salary slip avec la structure modifiée
                try:
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

                    print(f"Inserting salary slip: {salary_slip_data}", "Salary Slip Debug")
                    slip_doc = frappe.get_doc(salary_slip_data)
                    slip_doc.insert()

                    # Vérifier si le Salaire Base existe dans les Salary Detail
                    salary_detail_exists = frappe.db.exists({
                        "doctype": "Salary Detail",
                        "parenttype": "Salary Slip",
                        "parent": slip_doc.name,
                        "salary_component": "Salaire Base"
                    })

                    if not salary_detail_exists:
                        # Ajouter manuellement le composant Salaire Base si absent
                        slip_doc.append("earnings", {
                            "salary_component": "Salaire Base",
                            "amount": new_base,
                            "abbr": "SB"
                        })
                        slip_doc.save()
                        print(f"Manually added Salaire Base to Salary Slip {slip_doc.name}")

                    slip_doc.submit()
                    updated_slips.append({
                        "old_slip": slip_info['name'],
                        "new_slip": slip_doc.name,
                        "employee": slip_info['employee_name'],
                        "period": f"{slip_info['start_date']} - {slip_info['end_date']}",
                        "structure_name": structure_name,
                        "old_base": current_base,
                        "new_base": new_base,
                        "adjustment_percentage": taux,
                        "adjustment_type": "increase" if minusOrPlus == 0 else "decrease"
                    })
                    print(f"Created new salary slip {slip_doc.name} for employee {slip_info['employee']}")
                except Exception as slip_creation_error:
                    error_msg = f"Erreur lors de la création du nouveau slip pour l'employé {slip_info['employee']}: {str(slip_creation_error)}"
                    errors.append(error_msg)
                    frappe.log_error(error_msg, "New Salary Slip Creation Error")
                    continue

                # Restaurer la Salary Structure à son état original
                try:
                    salary_structure.earnings = original_structure['earnings']
                    salary_structure.deductions = original_structure['deductions']
                    salary_structure.save()
                    salary_structure.submit()
                    print(f"Restored original salary structure {structure_name} after processing slip {slip_name}")
                except Exception as restore_error:
                    error_msg = f"Erreur lors de la restauration de la structure {structure_name} après le slip {slip_name}: {str(restore_error)}"
                    errors.append(error_msg)
                    frappe.log_error(error_msg, "Salary Structure Restore Error")

            except Exception as slip_processing_error:
                error_msg = f"Erreur lors du traitement du slip {slip_name}: {str(slip_processing_error)}"
                errors.append(error_msg)
                frappe.log_error(error_msg, "Salary Slip Processing Error")

        # Commit les changements
        frappe.db.commit()

        return {
            "status": "success" if not errors else "partial_success",
            "updated_slips": updated_slips,
            "errors": errors,
            "total_processed": len(salary_slips),
            "message": f"Traitement terminé. {len(updated_slips)} slip(s) modifié(s) avec nouveau salaire de base."
        }

    except Exception as e:
        frappe.db.rollback()
        frappe.log_error(f"Erreur générale lors de la mise à jour des slips : {str(e)}", "Base Assignment Update Error")
        return {
            "status": "error",
            "updated_slips": [],
            "errors": [f"Erreur générale: {str(e)}"],
            "total_processed": 0,
            "message": "Échec du traitement - rollback effectué"
        }