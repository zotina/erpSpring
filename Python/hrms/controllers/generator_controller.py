import frappe
from frappe import _
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
        else:
            frappe.throw(f"Aucun salaire de base trouvé pour l'employé {emp} avant {target_date}")
    
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
                
                if not last_baseSalaryInfo:
                    if montant : 
                        last_baseSalaryInfo = {
                            'amount': montant,
                            'salary_structure': getMaxStructureSalary    
                        }
                    else :
                        frappe.throw(f"Aucun salaire de base trouvé et aucun montant suggeré")
                    
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
        
@frappe.whitelist(allow_guest=False)
def updateBaseAssignement(salary_component, montant, infOrSup, minusOrPlus, taux):
    """
    Met à jour les structures de salaire en modifiant directement le salaire de base et ajuste
    les autres composants qui dépendent du salaire de base (SB/sb). Annule et recrée les salary slips associés.

    Args:
        salary_component (str): Composant de salaire à filtrer
        montant (float): Montant de référence pour le filtre
        infOrSup (int): 0 pour inférieur, 1 pour supérieur
        minusOrPlus (int): 0 pour augmenter, 1 pour diminuer
        taux (str/float): Pourcentage de modification
    """
    updated_structures = []
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

        # Grouper par structure de salaire
        structures_to_update = {}
        for slip_data in salary_slips:
            if slip_data.salary_structure not in structures_to_update:
                structures_to_update[slip_data.salary_structure] = {
                    'current_base': float(slip_data.amount),
                    'slips': []
                }
            structures_to_update[slip_data.salary_structure]['slips'].append(slip_data.parent)

        for structure_name, structure_data in structures_to_update.items():
            try:
                # Annuler les salary slips liés
                cancelled_slips = []
                for slip_name in structure_data['slips']:
                    try:
                        salary_slip = frappe.get_doc("Salary Slip", slip_name)
                        if salary_slip.docstatus == 1:  # Submitted
                            salary_slip.cancel()
                            cancelled_slips.append({
                                'name': slip_name,
                                'employee': salary_slip.employee,
                                'start_date': salary_slip.start_date,
                                'end_date': salary_slip.end_date
                            })
                            frappe.logger().info(f"Cancelled salary slip {slip_name}")
                    except Exception as slip_error:
                        error_msg = f"Erreur lors de l'annulation du slip {slip_name}: {str(slip_error)}"
                        errors.append(error_msg)
                        frappe.log_error(error_msg, "Salary Slip Cancellation Error")
                        continue

                # Calculer le nouveau salaire de base
                current_base = structure_data['current_base']
                if minusOrPlus == 0:  # Augmenter
                    new_base = current_base + (current_base * (taux / 100))
                else:  # Diminuer
                    new_base = current_base - (current_base * (taux / 100))

                # Charger la structure de salaire existante
                salary_structure = frappe.get_doc("Salary Structure", structure_name)

                # Mettre à jour le composant "Salaire Base"
                base_component_updated = False
                for earning in salary_structure.earnings:
                    if earning.salary_component == "Salaire Base" or earning.abbr in ["SB", "sb"]:
                        earning.formula = str(new_base)
                        base_component_updated = True
                        frappe.logger().info(f"Updated base salary to {new_base} for {earning.salary_component}")
                        break

                if not base_component_updated:
                    errors.append(f"Composant Salaire Base non trouvé dans la structure {structure_name}")
                    continue

                # Ajuster les autres composants dépendant du salaire de base
                for earning in salary_structure.earnings:
                    if earning.salary_component != "Salaire Base" and earning.formula:
                        formula = earning.formula.lower()
                        if 'sb' in formula or 'base' in formula:
                            try:
                                if minusOrPlus == 0:  # Base augmentée, réduire les autres
                                    reduction_factor = 1 - (taux / 100)
                                    if '*' in earning.formula:
                                        earning.formula = f"({earning.formula}) * {reduction_factor}"
                                    else:
                                        earning.formula = f"{earning.formula} * {reduction_factor}"
                                else:  # Base réduite, augmenter les autres
                                    increase_factor = 1 + (taux / 100)
                                    if '*' in earning.formula:
                                        earning.formula = f"({earning.formula}) * {increase_factor}"
                                    else:
                                        earning.formula = f"{earning.formula} * {increase_factor}"
                                frappe.logger().info(f"Adjusted formula for {earning.salary_component}: {earning.formula}")
                            except Exception as formula_error:
                                frappe.logger().error(f"Error adjusting formula for {earning.salary_component}: {str(formula_error)}")

                # Ajuster les déductions
                for deduction in salary_structure.deductions:
                    if deduction.formula:
                        formula = deduction.formula.lower()
                        if 'sb' in formula or 'base' in formula:
                            try:
                                if minusOrPlus == 0:  # Base augmentée, réduire les déductions
                                    reduction_factor = 1 - (taux / 100)
                                    if '*' in deduction.formula:
                                        deduction.formula = f"({deduction.formula}) * {reduction_factor}"
                                    else:
                                        deduction.formula = f"{deduction.formula} * {reduction_factor}"
                                else:  # Base réduite, augmenter les déductions
                                    increase_factor = 1 + (taux / 100)
                                    if '*' in deduction.formula:
                                        deduction.formula = f"({deduction.formula}) * {increase_factor}"
                                    else:
                                        deduction.formula = f"{deduction.formula} * {increase_factor}"
                                frappe.logger().info(f"Adjusted deduction formula for {deduction.salary_component}: {deduction.formula}")
                            except Exception as formula_error:
                                frappe.logger().error(f"Error adjusting deduction formula for {deduction.salary_component}: {str(formula_error)}")

                # Sauvegarder et soumettre la structure modifiée
                try:
                    salary_structure.save()
                    salary_structure.submit()
                    frappe.logger().info(f"Updated and submitted salary structure {structure_name}")
                except Exception as save_error:
                    error_msg = f"Erreur lors de la sauvegarde de la structure {structure_name}: {str(save_error)}"
                    errors.append(error_msg)
                    frappe.log_error(error_msg, "Salary Structure Save Error")
                    continue

                # Recréer les salary slips avec la structure modifiée
                updated_slips = []
                for slip_info in cancelled_slips:
                    try:
                        new_slip = frappe.new_doc("Salary Slip")
                        new_slip.employee = slip_info['employee']
                        new_slip.salary_structure = structure_name
                        new_slip.start_date = slip_info['start_date']
                        new_slip.end_date = slip_info['end_date']
                        new_slip.amended_from = slip_info['name']
                        new_slip.insert()
                        new_slip.submit()
                        updated_slips.append({
                            "old_slip": slip_info['name'],
                            "new_slip": new_slip.name,
                            "employee": slip_info['employee'],
                            "period": f"{slip_info['start_date']} - {slip_info['end_date']}"
                        })
                        frappe.logger().info(f"Created new salary slip {new_slip.name} for employee {slip_info['employee']}")
                    except Exception as slip_creation_error:
                        error_msg = f"Erreur lors de la création du nouveau slip pour l'employé {slip_info['employee']}: {str(slip_creation_error)}"
                        errors.append(error_msg)
                        frappe.log_error(error_msg, "New Salary Slip Creation Error")

                updated_structures.append({
                    "structure_name": structure_name,
                    "old_base": current_base,
                    "new_base": new_base,
                    "adjustment_percentage": taux,
                    "adjustment_type": "increase" if minusOrPlus == 0 else "decrease",
                    "updated_slips": updated_slips,
                    "slips_processed": len(updated_slips),
                    "structure_modified": True
                })

            except Exception as structure_error:
                error_msg = f"Erreur lors de la mise à jour de la structure {structure_name}: {str(structure_error)}"
                errors.append(error_msg)
                frappe.log_error(error_msg, "Salary Structure Update Error")

        # Commit les changements
        frappe.db.commit()

        return {
            "status": "success" if not errors else "partial_success",
            "updated_structures": updated_structures,
            "errors": errors,
            "total_processed": len(structures_to_update),
            "message": f"Traitement terminé. {len(updated_structures)} structure(s) modifiée(s) avec slips recréés."
        }

    except Exception as e:
        frappe.db.rollback()
        frappe.log_error(f"Erreur générale lors de la mise à jour des structures : {str(e)}", "Base Assignment Update Error")
        return {
            "status": "error",
            "updated_structures": [],
            "errors": [f"Erreur générale: {str(e)}"],
            "total_processed": 0,
            "message": "Échec du traitement - rollback effectué"
        }