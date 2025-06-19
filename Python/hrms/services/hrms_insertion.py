import frappe
from frappe import _
from frappe.utils import getdate, flt
from datetime import datetime
from calendar import monthrange
from hrms.dto.employee_dto import EmployeeDTO
from hrms.dto.salary_structure_dto import SalaryStructureDTO
from hrms.dto.payroll_dto import PayrollDTO
from datetime import datetime
from typing import List, Dict, Any
import random



def setup_hrms_data(employee_dtos: list[EmployeeDTO], salary_dtos: list[SalaryStructureDTO]):
    """
    Setup company with chart of accounts and default Holiday List.
    Args:
        employee_dtos: List of EmployeeDTO objects.
        salary_dtos: List of SalaryStructureDTO objects.
    Returns:
        List of errors (empty if successful).
    """
    errors = []

    try:
        # Setup companies and their accounts
        if employee_dtos:
            companies = set(employee.company for employee in employee_dtos if employee.company)
            
            for company in companies:
                if not company:
                    errors.append({
                        "line": 0,
                        "error_message": "Champ 'company' manquant ou invalide dans EmployeeDTO",
                        "data": {},
                        "file": "employeesCsv"
                    })
                    continue

                try:
                    # Normalize company name and suffix
                    company = company.strip()
                    company_suffix = 'MC' if company.lower().startswith('my company') else company[:2].upper()
                    print(f"Processing company: {company}, suffix: {company_suffix}")

                    # Verify reference company
                    ref_company = "noufre society"
                    if not frappe.db.exists("Company", ref_company):
                        ref_company = "_Test Company"
                        if not frappe.db.exists("Company", ref_company):
                            raise ValueError(f"Compagnie de référence '{ref_company}' n'existe pas")

                    # Create company if it doesn't exist
                    if not frappe.db.exists("Company", company):
                        print(f"Inserting company: {company}")
                        company_doc = frappe.new_doc("Company")
                        company_doc.company_name = company
                        company_doc.abbr = company
                        company_doc.default_currency = "EUR"
                        company_doc.create_chart_of_accounts_based_on = "Existing Company"
                        company_doc.existing_company = ref_company
                        company_doc.insert()
                        print(f"Created company: {company} with COA from {ref_company}")
                    else:
                        print(f"Company {company} already exists")

                    # Set default Holiday List for the company
                    holiday_list = get_or_create_holiday_list(company)
                    company_doc = frappe.get_doc("Company", company)
                    if not company_doc.default_holiday_list:
                        company_doc.default_holiday_list = holiday_list
                        company_doc.save()
                        print(f"Set default Holiday List '{holiday_list}' for company {company}")

                    # Find root account
                    root_account = frappe.db.get_value(
                        "Account",
                        {"company": company, "is_group": 1, "parent_account": ["is", None]},
                        "name"
                    )
                    if not root_account:
                        root_account_name = f"Root Account - {company_suffix}"
                        if not frappe.db.exists("Account", {"company": company, "account_name": "Root Account"}):
                            print(f"Inserting root account: {root_account_name}")
                            frappe.get_doc({
                                "doctype": "Account",
                                "account_name": "Root Account",
                                "company": company,
                                "is_group": 1,
                                "account_type": "",
                                "root_type": ""
                            }).insert()
                            root_account = root_account_name
                            print(f"Created fallback root account: {root_account_name} for {company}")
                        else:
                            root_account = frappe.db.get_value("Account", {"company": company, "account_name": "Root Account"}, "name")
                    print(f"Using root account: {root_account} for {company}")

                    # Setup HRMS accounts
                    def create_account_if_not_exists(account_name, parent_account, account_type, root_type, is_group=1):
                        existing_account = frappe.db.get_value(
                            "Account", 
                            {"company": company, "account_name": account_name}, 
                            "name"
                        )
                        if not existing_account:
                            try:
                                print(f"Inserting account: {account_name}")
                                account_doc = frappe.get_doc({
                                    "doctype": "Account",
                                    "account_name": account_name,
                                    "company": company,
                                    "parent_account": parent_account,
                                    "account_type": account_type,
                                    "root_type": root_type,
                                    "is_group": is_group
                                })
                                account_doc.insert()
                                print(f"Created account: {account_name}")
                                return account_doc.name
                            except Exception as e:
                                error_msg = f"Failed to create {account_name}: {str(e)}"
                                errors.append({
                                    "line": 0,
                                    "error_message": error_msg,
                                    "data": {},
                                    "file": "employeesCsv"
                                })
                                print(error_msg)
                                return None
                        else:
                            print(f"Account {account_name} already exists (name: {existing_account})")
                            return existing_account

                    income_root_name = create_account_if_not_exists(
                        "Income", root_account, "Income Account", "Income", 1
                    )
                    if not income_root_name:
                        continue

                    direct_income_name = create_account_if_not_exists(
                        "Direct Income", income_root_name, "Income Account", "Income", 1
                    )
                    if not direct_income_name:
                        continue

                    expenses_root_name = create_account_if_not_exists(
                        "Expenses", root_account, "Expense Account", "Expense", 1
                    )
                    if not expenses_root_name:
                        continue

                    direct_expenses_name = create_account_if_not_exists(
                        "Direct Expenses", expenses_root_name, "Expense Account", "Expense", 1
                    )
                    if not direct_expenses_name:
                        continue

                    salary_income_name = create_account_if_not_exists(
                        "Salary Income", direct_income_name, "Income Account", "Income", 0
                    )

                    salary_expense_name = create_account_if_not_exists(
                        "Salary Expense", direct_expenses_name, "Expense Account", "Expense", 0
                    )

                except Exception as e:
                    errors.append({
                        "line": 0,
                        "error_message": f"Erreur configuration compagnie {company}: {str(e)}",
                        "data": {},
                        "file": "employeesCsv"
                    })
                    print(f"Company setup error for {company}: {str(e)}")

    except Exception as e:
        errors.append({
            "line": 0,
            "error_message": f"Erreur configuration initiale: {str(e)}",
            "data": {},
            "file": "global"
        })

    return errors

def insert_employees(dto_list: list[EmployeeDTO]):
    """
    Insert Employee records from EmployeeDTO list.
    Args:
        dto_list: List of EmployeeDTO objects.
    Returns:
        Dictionary with created records and errors.
    """
    created = []
    errors = []
    
    for idx, dto in enumerate(dto_list, start=1):
        try:
            # Validate required fields
            if not all([dto.ref, dto.prenom, dto.nom, dto.date_embauche, dto.date_naissance, dto.company]):
                raise ValueError("Champs requis manquants (ref, prenom, nom, date_embauche, date_naissance, company)")
            
            # Get default Holiday List for the company
            holiday_list = get_or_create_holiday_list(dto.company)
            
            # Récupère une désignation aléatoire
            random_designation = get_random_designation()
            if not random_designation:
                raise ValueError("Aucune désignation disponible dans la base de données")
            
            # Récupère un département aléatoire
            random_department = get_random_department()
            if not random_department:
                raise ValueError("Aucun département disponible dans la base de données")
            
            # Prepare Employee data
            employee_data = {
                "doctype": "Employee",
                "ref": str(dto.ref),
                "name": str(dto.ref),
                "employee": str(dto.ref),
                "first_name": dto.prenom,
                "last_name": dto.nom,
                "gender": convert_gender(dto.genre),
                "date_of_joining": convert_date_format(dto.date_embauche),
                "date_of_birth": convert_date_format(dto.date_naissance),
                "company": dto.company,
                "status": "Active",
                "employee_name": f"{dto.prenom} {dto.nom}",
                "department": random_department,  # Département aléatoire
                "designation": random_designation,  # Désignation aléatoire
                "default_holiday_list": holiday_list  # Assign default Holiday List
            }
            
            # Check if employee exists
            if frappe.db.exists("Employee", {"employee": dto.ref}):
                errors.append({
                    "line": idx,
                    "error_message": f"Employé {dto.ref} existe déjà",
                    "data": vars(dto)
                })
                continue
            
            # Insert Employee
            employee_doc = frappe.get_doc(employee_data)
            employee_doc.insert()
            
            frappe.logger().info(f"Inserted employee: {employee_doc.ref} with designation: {random_designation} and department: {random_department}")
            
            created.append({
                "employee_id": employee_doc.name,
                "employee_number": employee_doc.employee,
                "name": employee_doc.employee_name,
                "designation": random_designation,
                "department": random_department
            })
            
        except Exception as e:
            errors.append({
                "line": idx,
                "error_message": f"Erreur employé {dto.ref}: {str(e)}",
                "data": vars(dto)
            })
            frappe.logger().error(f"Employee insertion failed: {dto.ref} - {str(e)}")
    
    return {
        "success": len(errors) == 0,
        "created_count": len(created),
        "created": created,
        "errors": errors
    }




def insert_salary_components(dto_list: list[SalaryStructureDTO]):
    """
    Insert Salary Component records from SalaryStructureDTO list.
    """
    created = []
    errors = []
    processed_components = set()

    for idx, dto in enumerate(dto_list, start=1):
        try:
            component_name = dto.name
            print(f"Processing salary component: {component_name} at line {idx}", "Component Debug")

            # Skip duplicates within the current list
            if component_name in processed_components:
                print(f"Skipping duplicate component in list: {component_name} at line {idx}", "Component Debug")
                continue

            if not component_name or not dto.type:
                errors.append({
                    "line": idx,
                    "error_message": f"Champ 'name' ou 'type' manquant pour composant à la ligne {idx}",
                    "data": vars(dto),
                    "file": "salaryStructureCsv"
                })
                continue

            # Check if component already exists in the database
            if frappe.db.exists("Salary Component", {"salary_component": component_name}):
                print(f"Salary component {component_name} already exists in database, skipping insertion", "Component Debug")
                created.append(component_name)
                processed_components.add(component_name)
                continue

            # Déterminer si le composant doit dépendre des jours de paiement
            # Par défaut, seul le salaire de base dépend des jours de paiement
            base_components = ["salaire base", "base salary", "basic salary", "salary", "salaire"]
            depends_on_payment_days = 1 if component_name.lower() in base_components else 0
            mapped_type = get_salary_component_type(dto.type)
            
            component_data = {
                "doctype": "Salary Component",
                "salary_component": component_name,
                "salary_component_abbr": dto.abbr,
                "type": mapped_type,
                "is_tax_applicable": 1 if mapped_type == "Earning" else 0,
                "amount_based_on_formula": 1 if dto.valeur else 0,
                "depends_on_payment_days": 0 if dto.valeur else 1,
                "formula": dto.valeur or "",
                "is_active": "Yes"
            }

            print(f"Inserting salary component: {component_name} with depends_on_payment_days={depends_on_payment_days}", "Component Debug")
            print(f"salary component : {component_data}")
            component_doc = frappe.get_doc(component_data)
            component_doc.insert()
            
            companies = frappe.get_all("Company", fields=["name", "abbr"])
            for company in companies:

                account = frappe.db.exists("Account", {"account_name": "Payroll Payable", "company": company.name})
                if not account:
                    # Crée le compte s'il n'existe pas
                    account_doc = frappe.get_doc({
                        "doctype": "Account",
                        "account_name": "Payroll Payable",
                        "company": company.name,
                        "parent_account": frappe.db.get_value(
                            "Account",
                            {"is_group": 1, "root_type": "Liability", "company": company.name},
                            "name"),
                        "is_group": 0,
                        "root_type": "Liability"
                    })
                    account_doc.insert()
                    account = account_doc.name

                if not frappe.db.exists("Salary Component Account", {
                    "parent": component_doc.name,
                    "company": company.name
                }):
                    component_doc.append("accounts", {
                        "company": company.name,
                        "account": account
                    })

            component_doc.save()

            created.append(component_name)
            processed_components.add(component_name)

        except Exception as e:
            errors.append({
                "line": idx,
                "error_message": f"Erreur composant {dto.name or 'inconnu'} à la ligne {idx}: {str(e)}",
                "data": vars(dto),
                "file": "salaryStructureCsv"
            })
            print(f"Component insertion error at line {idx}: {str(e)}", "Component Error")

    return {
        "success": len(errors) == 0,
        "created_count": len(created),
        "created": created,
        "errors": errors
    }



def insert_salary_structures(dto_list: list[SalaryStructureDTO], payroll_dtos: list[PayrollDTO]):
    """
    Insert Salary Structure records from SalaryStructureDTO list, using company from matching PayrollDTO.
    Args:
        dto_list: List of SalaryStructureDTO objects.
        payroll_dtos: List of PayrollDTO objects to determine company.
    Returns:
        Dictionary with created records and errors.
    """
    structures_data = {}
    errors = []

    # Group components by structure
    for idx, dto in enumerate(dto_list, start=1):
        try:
            print(f"Processing salary structure DTO at line {idx}: name={dto.name}, structure={dto.salary_structure}", "Structure Debug")
            structure_name = dto.salary_structure
            if not structure_name:
                errors.append({
                    "line": idx,
                    "error_message": f"Champ 'salary_structure' manquant à la ligne {idx}",
                    "data": vars(dto) if hasattr(dto, '__dict__') else dict(dto),
                    "file": "salaryStructureCsv"
                })
                continue

            if structure_name not in structures_data:

                structures_data[structure_name] = {
                    "earnings": [],
                    "deductions": [],
                    "company": dto.company
                }

            # CORRECTION CRITIQUE: Fixer le composant dans la base AVANT de l'utiliser
            fix_component_payment_days(dto.name)
            
            # Get component data from database (après correction)
            component_data_db = frappe.db.get_value(
                "Salary Component", 
                {"salary_component": dto.name}, 
                ["salary_component_abbr", "depends_on_payment_days"],
                as_dict=True
            )
            
            if component_data_db:
                component_abbr = component_data_db.salary_component_abbr
                component_depends_on_payment_days = component_data_db.depends_on_payment_days
            else:
                component_abbr = dto.abbr or dto.name[:3].upper()
                # Par défaut, seul le salaire de base dépend des jours de paiement
                base_components = ["salaire base", "base salary", "basic salary", "salary", "salaire"]
                component_depends_on_payment_days = 1 if dto.name.lower() in base_components else 0

            # Parse formula based on remarque and valeur
            formula = dto.valeur
            
            # La logique depends_on_payment_days est maintenant gérée au niveau du composant lui-même
            final_depends_on_payment_days = component_depends_on_payment_days
            
            component_data = {
                "doctype": "Salary Detail",  # Ajout du doctype requis
                "salary_component": dto.name,
                "abbr": component_abbr,
                "amount": 0,  # Always 0 when using formula
                "depends_on_payment_days": final_depends_on_payment_days,
                "is_tax_applicable": 1 if dto.type.lower() == "earning" else 0,
                "amount_based_on_formula": 1 if formula else 0,
                "formula": formula if formula else ""
            }

            print(f"Component {dto.name}: formula={formula}, depends_on_payment_days={final_depends_on_payment_days}", "Formula Debug")

            if dto.type and dto.type.lower() == "earning":
                structures_data[structure_name]["earnings"].append(component_data)
            elif dto.type:
                structures_data[structure_name]["deductions"].append(component_data)
            else:
                errors.append({
                    "line": idx,
                    "error_message": f"Type manquant pour composant {dto.name} à la ligne {idx}",
                    "data": vars(dto) if hasattr(dto, '__dict__') else dict(dto),
                    "file": "salaryStructureCsv"
                })

        except Exception as e:
            errors.append({
                "line": idx,
                "error_message": f"Erreur traitement structure à la ligne {idx}: {str(e)}",
                "data": vars(dto) if hasattr(dto, '__dict__') else dict(dto),
                "file": "salaryStructureCsv"
            })
            print(f"Structure processing error at line {idx}: {str(e)}", "Structure Error")

    created = []
    for structure_name, components in structures_data.items():
        try:
            if frappe.db.exists("Salary Structure", structure_name):
                errors.append({
                    "line": 0,
                    "error_message": f"Structure salariale {structure_name} existe déjà",
                    "data": components,
                    "file": "salaryStructureCsv"
                })
                continue

            structure_data = {
                "doctype": "Salary Structure",
                "name": structure_name,
                "structure_name": structure_name,
                "company": components["company"],
                "is_active": "Yes",
                "earnings": components["earnings"],
                "deductions": components["deductions"]
            }

            print(f"Inserting salary structure: {structure_data}", "Structure Insertion")

            structure_doc = frappe.get_doc(structure_data)
            structure_doc.flags.ignore_mandatory = True  # Bypass naming series
            structure_doc.insert()
            structure_doc.submit()

            created.append(structure_name)

        except Exception as e:
            errors.append({
                "line": 0,
                "error_message": f"Erreur insertion structure {structure_name}: {str(e)}",
                "data": components,
                "file": "salaryStructureCsv"
            })
            print(f"Structure insertion error for {structure_name}: {str(e)}", "Structure Error")

    return {
        "success": len(errors) == 0,
        "created_count": len(created),
        "created": created,
        "errors": errors
    }




def insert_salary_assignments(dto_list: list[PayrollDTO]):
    """
    Insert Salary Structure Assignment records from PayrollDTO list.
    """
    assignments_data = []  # Changé en liste au lieu de dictionnaire
    errors = []

    # Traiter chaque DTO individuellement
    for idx, dto in enumerate(dto_list, start=1):
        try:
            employee_ref = dto.ref_employe
            converted_date = convert_date_format(dto.mois)
            if not converted_date:
                raise ValueError(f"Invalid date format for mois: {dto.mois}")

            # Ajouter chaque assignment à la liste
            assignments_data.append({
                "employee_ref": employee_ref,
                "salary_structure": dto.salaire,
                "base_salary": dto.salaire_base,
                "start_date": converted_date,
                "line": idx
            })
            
            print(f"parse assignement  {employee_ref}: {str(converted_date)}")
            
        except Exception as e:
            errors.append({
                "line": idx,
                "error_message": f"Erreur traitement données assignation pour employé {dto.ref_employe}: {str(e)}",
                "data": vars(dto),
                "file": "payrollCsv"
            })

    created = []
    # Maintenant on traite chaque assignment individuellement
    for assignment_data in assignments_data:
        employee_ref = assignment_data["employee_ref"]
        print(f"treat assignement  {employee_ref}: {assignment_data}")
        try:
            # Get Employee details
            employee_data = frappe.db.get_value("Employee", {"ref": employee_ref}, ["name", "company", "date_of_joining"], as_dict=True)
            print(employee_data)
            if not employee_data:
                errors.append({
                    "line": assignment_data["line"],
                    "error_message": f"Employé {employee_ref} non trouvé",
                    "data": assignment_data,
                    "file": "payrollCsv"
                })
                continue

            from_date = assignment_data["start_date"]
            

            # Check if Salary Structure exists
            structure_name = frappe.db.get_value("Salary Structure", {"structure_name": assignment_data["salary_structure"]}, "name")
            print(structure_name)
            if not structure_name:
                errors.append({
                    "line": assignment_data["line"],
                    "error_message": f"Structure salariale '{assignment_data['salary_structure']}' non trouvée",
                    "data": assignment_data,
                    "file": "payrollCsv"
                })
                continue

            # Check if assignment exists - Vérification plus précise
            existing = frappe.db.exists("Salary Structure Assignment", {
                "employee": employee_data.name, 
                "salary_structure": structure_name,
                "from_date": from_date,  # Vérification exacte de la date
                "docstatus": 1
            })
            if existing:
                print(f"Assignment already exists for {employee_ref} with structure {assignment_data['salary_structure']} from {from_date}", "Assignment Debug")
                continue

            assignment_doc_data = {
                "doctype": "Salary Structure Assignment",
                "employee": employee_data.name,
                "salary_structure": structure_name,
                "from_date": from_date,
                "base": assignment_data["base_salary"],
                "company": employee_data.company
            }

            print(f"Inserting assignment for {employee_ref}: {assignment_doc_data}", "Assignment Debug")
            assignment_doc = frappe.get_doc(assignment_doc_data)
            assignment_doc.insert()
            assignment_doc.submit()  # Submit to set docstatus=1
            created.append(f"{employee_ref} -> {assignment_data['salary_structure']} from {from_date}")

        except Exception as e:
            errors.append({
                "line": assignment_data["line"],
                "error_message": f"Erreur assignation pour {employee_ref}: {str(e)}",
                "data": assignment_data,
                "file": "payrollCsv"
            })
            print(f"Assignment error for {employee_ref}: {str(e)}", "Assignment Error")

    return {
        "success": len(errors) == 0,
        "created_count": len(created),
        "created": created,
        "errors": errors
    }



def insert_salary_slips(dto_list: list[PayrollDTO]):
    """
    Insert Salary Slip records from PayrollDTO list.
    """
    created = []
    errors = []

    for idx, dto in enumerate(dto_list, start=1):
        try:
            # Get Employee
            employee = frappe.db.get_value("Employee", {"ref": dto.ref_employe}, "name")
            if not employee:
                errors.append({
                    "line": idx,
                    "error_message": f"Employé {dto.ref_employe} non trouvé",
                    "data": vars(dto),
                    "file": "payrollCsv"
                })
                continue

            # Convert date
            converted_date = convert_date_format(dto.mois)
            print(f"Processing salary slip for employee {dto.ref_employe} on date {converted_date}", "Salary Slip Debug")

            # Check if salary structure assignment exists
            assignment = frappe.db.get_value(
                "Salary Structure Assignment",
                {
                    "employee": employee,
                    "from_date": ["<=", converted_date],
                    "docstatus": 1
                },
                ["name", "salary_structure"],
                as_dict=True
            )
            if not assignment:
                errors.append({
                    "line": idx,
                    "error_message": f"Aucune structure salariale assignée à l'employé {dto.ref_employe} pour la date {dto.mois}",
                    "data": vars(dto),
                    "file": "payrollCsv"
                })
                continue

            # Check if Salary Slip exists
            existing = frappe.db.exists("Salary Slip", {
                "employee": employee,
                "start_date": converted_date
            })
            if existing:
                print(f"Salary slip already exists for {dto.ref_employe} on {converted_date}", "Salary Slip Debug")
                continue
            employee_data = frappe.db.get_value("Employee", {"ref": dto.ref_employe}, ["name", "company", "date_of_joining"], as_dict=True)
            
            if not employee_data:
                errors.append({
                    "line":0,
                    "error_message": f"Employé {dto.ref_employe} non trouvé",
                    "data": "",
                    "file": "payrollCsv"
                })
                continue
            
            salary_slip_data = {
                "doctype": "Salary Slip",
                "employee": employee,
                "salary_structure": assignment.salary_structure,
                "start_date": converted_date,
                "end_date": get_month_end_date(dto.mois),
                "posting_date": converted_date,
                "company": employee_data.company
            }

            print(f"Inserting salary slip: {salary_slip_data}", "Salary Slip Debug")
            slip_doc = frappe.get_doc(salary_slip_data)
            slip_doc.insert()
            slip_doc.submit()

            created.append(f"{dto.ref_employe} -> {dto.mois}")

        except Exception as e:
            errors.append({
                "line": idx,
                "error_message": f"Erreur bulletin {dto.ref_employe}: {str(e)}",
                "data": vars(dto),
                "file": "payrollCsv"
            })
            print(f"Salary slip error for {dto.ref_employe}: {str(e)}", "Salary Slip Error")

    return {
        "success": len(errors) == 0,
        "created_count": len(created),
        "created": created,
        "errors": errors
    }



def convert_gender(genre):
    """Convert gender to ERPNext format."""
    genre_lower = genre.lower() if genre else ""
    if genre_lower in ['masculin', 'male', 'm']:
        return 'Male'
    elif genre_lower in ['feminin', 'female', 'f']:
        return 'Female'
    return 'Male'  # Default

def get_or_create_designation(designation_name):
    """Get or create a Designation."""
    if not frappe.db.exists("Designation", designation_name):
        desig_doc = frappe.get_doc({
            "doctype": "Designation",
            "designation_name": designation_name
        })
        desig_doc.insert()
    return designation_name

def parse_salary_value(valeur):
    """Parse salary value (percentage or amount)."""
    if valeur is None:
        print("parse_salary_value: valeur is None", "Value Debug")
        return 0
    try:
        valeur_str = str(valeur).strip()
        if not valeur_str or valeur_str.lower() == "none":
            return 0
        if '%' in valeur_str:
            return 0  # Use formula for percentages
        return flt(valeur_str)
    except Exception as e:
        print(f"parse_salary_value error: {str(e)} for valeur={valeur}", "Value Debug")
        return 0
    
def get_month_end_date(date_str):
    """Get the end date of the month for a given date string."""
    try:
        # Convert DD/MM/YYYY to datetime object
        date_obj = datetime.strptime(date_str, '%d/%m/%Y')
        last_day = monthrange(date_obj.year, date_obj.month)[1]
        return f"{date_obj.year}-{date_obj.month:02d}-{last_day:02d}"
    except ValueError:
        try:
            # If already in YYYY-MM-DD format
            date_obj = datetime.strptime(date_str, '%Y-%m-%d')
            last_day = monthrange(date_obj.year, date_obj.month)[1]
            return f"{date_obj.year}-{date_obj.month:02d}-{last_day:02d}"
        except ValueError:
            raise ValueError(f"Invalid date format for get_month_end_date: {date_str}. Expected DD/MM/YYYY or YYYY-MM-DD.")

def convert_date_format(date_str):
    """Convert date from DD/MM/YYYY to YYYY-MM-DD or return as is if already in correct format."""
    if not date_str:
        return date_str
    try:
        # Try parsing DD/MM/YYYY format
        date_obj = datetime.strptime(date_str, '%d/%m/%Y')
        return date_obj.strftime('%Y-%m-%d')
    except ValueError:
        try:
            # If already in YYYY-MM-DD format, return as is
            datetime.strptime(date_str, '%Y-%m-%d')
            return date_str
        except ValueError:
            raise ValueError(f"Invalid date format: {date_str}. Expected DD/MM/YYYY or YYYY-MM-DD.")
        
        
def get_or_create_holiday_list(company, year=None):
    """
    Get or create a default Holiday List for the given company and year.
    Args:
        company: Name of the company.
        year: Year for the holiday list (default: current year).
    Returns:
        Name of the Holiday List.
    """
    if not year:
        year = datetime.now().year

    holiday_list_name = f"Default Holiday List - {company} - {year}"
    
    if not frappe.db.exists("Holiday List", holiday_list_name):
        try:
            holiday_list_doc = frappe.get_doc({
                "doctype": "Holiday List",
                "holiday_list_name": holiday_list_name,
                "from_date": f"{year}-01-01",
                "to_date": f"{year}-12-31",
                "weekly_off": "Sunday",  # Example: Set Sunday as weekly off
                "company": company
            })
            holiday_list_doc.insert()
            print(f"Created Holiday List: {holiday_list_name} for {company}")
        except Exception as e:
            raise Exception(f"Failed to create Holiday List for {company}: {str(e)}")
    
    return holiday_list_name
    
def fix_component_payment_days(component_name):
    """
    Corrige le paramètre depends_on_payment_days d'un composant spécifique
    """
    try:
        if frappe.db.exists("Salary Component", {"salary_component": component_name}):
            component_doc = frappe.get_doc("Salary Component", {"salary_component": component_name})
            
            # Seuls les composants de base doivent dépendre des jours de paiement
            base_components = ["salaire base", "base salary", "basic salary", "salary", "salaire"]
            should_depend_on_payment_days = component_name.lower() in base_components
            
            if component_doc.depends_on_payment_days != should_depend_on_payment_days:
                print(f"Correcting {component_name}: depends_on_payment_days {component_doc.depends_on_payment_days} -> {should_depend_on_payment_days}")
                
                # Annuler la soumission si nécessaire
                if component_doc.docstatus == 1:
                    component_doc.cancel()
                
                # Modifier et soumettre à nouveau
                component_doc.depends_on_payment_days = 1 if should_depend_on_payment_days else 0
                component_doc.save()
                component_doc.submit()
                
                return True
        return False
    except Exception as e:
        print(f"Error fixing component {component_name}: {str(e)}")
        return False
    



def _process_employee_batch(batch: list, holiday_lists_cache: dict, 
                          designation: str, start_idx: int) -> Dict[str, Any]:
    """Traite un lot d'employés de manière optimisée."""
    batch_created = []
    batch_errors = []
    
    # 2. VALIDATION EN BLOC
    valid_employees = []
    existing_refs = set()
    
    # Vérifier les employés existants en une seule requête
    if batch:
        refs_to_check = [str(dto.ref) for dto in batch if dto.ref]
        if refs_to_check:
            existing_employees = frappe.db.get_list(
                "Employee",
                filters={"employee": ["in", refs_to_check]},
                fields=["employee"]
            )
            existing_refs = set(emp.employee for emp in existing_employees)
    
    # Valider et préparer les données
    for idx, dto in enumerate(batch, start=start_idx):
        try:
            # Validation des champs requis
            if not all([dto.ref, dto.prenom, dto.nom, dto.date_embauche, 
                       dto.date_naissance, dto.company]):
                raise ValueError("Champs requis manquants")
            
            # Vérifier si existe déjà
            if str(dto.ref) in existing_refs:
                batch_errors.append({
                    "line": idx,
                    "error_message": f"Employé {dto.ref} existe déjà",
                    "data": vars(dto)
                })
                continue
            
            # Préparer les données employé
            employee_data = {
                "doctype": "Employee",
                "ref": str(dto.ref),
                "name": str(dto.ref),
                "employee": str(dto.ref),
                "first_name": dto.prenom,
                "last_name": dto.nom,
                "gender": convert_gender(dto.genre),
                "date_of_joining": convert_date_format(dto.date_embauche),
                "date_of_birth": convert_date_format(dto.date_naissance),
                "company": dto.company,
                "status": "Active",
                "employee_name": f"{dto.prenom} {dto.nom}",
                "department": "Ressources humaines",
                "designation": designation,
                "default_holiday_list": holiday_lists_cache.get(dto.company)
            }
            
            valid_employees.append((employee_data, dto, idx))
            
        except Exception as e:
            batch_errors.append({
                "line": idx,
                "error_message": f"Erreur validation {dto.ref}: {str(e)}",
                "data": vars(dto)
            })
    
    # 3. INSERTION OPTIMISÉE
    if valid_employees:
        batch_created.extend(_bulk_insert_employees(valid_employees))
    
    return {"created": batch_created, "errors": batch_errors}

def _bulk_insert_employees(valid_employees: list) -> list:
    """Insertion optimisée des employés validés."""
    created = []
    
    # Option 1: Insertion avec bypass des validations pour la performance
    for employee_data, dto, idx in valid_employees:
        try:
            # Créer le document mais contourner certaines validations
            employee_doc = frappe.get_doc(employee_data)
            
            # Insertion rapide sans déclencher tous les hooks
            employee_doc.insert(ignore_permissions=True, ignore_if_duplicate=True)
            print(f"{employee_doc.employee_name} Créé")
            # Submit seulement si nécessaire (selon votre workflow)
            if hasattr(employee_doc, 'docstatus') and employee_doc.docstatus == 0:
                employee_doc.submit()
            
            created.append({
                "employee_id": employee_doc.name,
                "employee_number": employee_doc.employee,
                "name": employee_doc.employee_name
            })
            
            # Log de succès moins verbeux
            if len(created) % 50 == 0:  # Log tous les 50
                print(f"Créé {len(created)} employés...")
                
        except Exception as e:
            print(f"Erreur insertion employé {dto.ref}: {str(e)}")
            continue
    
    return created


@frappe.whitelist()
def get_or_create_designation_cached(designation_name: str) -> str:
    """Version mise en cache de get_or_create_designation."""
    cache_key = f"designation_{designation_name}"
    
    # Vérifier le cache
    cached = frappe.cache().get_value(cache_key)
    if cached:
        return cached
    
    if not frappe.db.exists("Designation", designation_name):
        try:
            desig_doc = frappe.get_doc({
                "doctype": "Designation",
                "designation_name": designation_name
            })
            desig_doc.insert(ignore_permissions=True)
            frappe.cache().set_value(cache_key, designation_name, expires_in_sec=3600)
        except Exception as e:
            print(f"Erreur création designation: {str(e)}")
            return designation_name
    
    frappe.cache().set_value(cache_key, designation_name, expires_in_sec=3600)
    return designation_name

@frappe.whitelist()
def get_or_create_holiday_list_cached(company: str, year: int = None) -> str:
    """Version optimisée et mise en cache."""
    if not year:
        year = datetime.now().year
    
    holiday_list_name = f"Default Holiday List - {company} - {year}"
    cache_key = f"holiday_list_{company}_{year}"
    
    # Vérifier le cache
    cached = frappe.cache().get_value(cache_key)
    if cached:
        return cached
    
    if not frappe.db.exists("Holiday List", holiday_list_name):
        try:
            holiday_list_doc = frappe.get_doc({
                "doctype": "Holiday List",
                "holiday_list_name": holiday_list_name,
                "from_date": f"{year}-01-01",
                "to_date": f"{year}-12-31",
                "weekly_off": "Sunday",
                "company": company
            })
            holiday_list_doc.insert(ignore_permissions=True)
            frappe.cache().set_value(cache_key, holiday_list_name, expires_in_sec=3600)
        except Exception as e:
            print(f"Erreur création holiday list: {str(e)}")
            return holiday_list_name
    
    frappe.cache().set_value(cache_key, holiday_list_name, expires_in_sec=3600)
    return holiday_list_name

def convert_gender(genre: str) -> str:
    """Convert gender to ERPNext format - inchangé."""
    if not genre:
        return 'Male'
    
    genre_lower = genre.lower()
    if genre_lower in ['masculin', 'male', 'm']:
        return 'Male'
    elif genre_lower in ['feminin', 'female', 'f']:
        return 'Female'
    return 'Male'


def bulk_insert_employees_raw(dto_list: list) -> Dict[str, Any]:
    """
    Version ultra-optimisée utilisant des requêtes SQL directes.
    ATTENTION: Contourne les validations Frappe - à utiliser avec précaution.
    """
    try:
        # Préparer les données pour insertion SQL directe
        values = []
        for dto in dto_list:
            values.append((
                str(dto.ref),  # name
                str(dto.ref),  # employee
                dto.prenom,    # first_name
                dto.nom,       # last_name
                convert_gender(dto.genre),
                convert_date_format(dto.date_embauche),
                convert_date_format(dto.date_naissance),
                dto.company,
                'Active',
                f"{dto.prenom} {dto.nom}",
                'Ressources humaines',
                'Consultant',
                frappe.utils.now(),  # creation
                frappe.utils.now(),  # modified
                frappe.session.user, # owner
                frappe.session.user  # modified_by
            ))
        
        # Insertion SQL directe (très rapide mais sans validations)
        frappe.db.bulk_insert("tabEmployee", [
            "name", "employee", "first_name", "last_name", "gender",
            "date_of_joining", "date_of_birth", "company", "status",
            "employee_name", "department", "designation",
            "creation", "modified", "owner", "modified_by"
        ], values, ignore_duplicates=True)
        
        
        return {
            "success": True,
            "created_count": len(values),
            "created": [{"employee_id": val[0]} for val in values],
            "errors": []
        }
        
    except Exception as e:
        print(f"Erreur bulk insert raw: {str(e)}")
        return {
            "success": False,
            "created_count": 0,
            "created": [],
            "errors": [{"error": str(e)}]
        }


def get_or_create_designation(designation_name: str) -> str:
    """Version optimisée."""
    return get_or_create_designation_cached(designation_name)

def get_or_create_holiday_list(company: str, year: int = None) -> str:
    """Version optimisée."""
    return get_or_create_holiday_list_cached(company, year)


def insert_employees_optimized(dto_list: list) -> Dict[str, Any]:
    """
    Insert Employee records optimized for bulk operations.
    
    Args:
        dto_list: List of EmployeeDTO objects.
        
    Returns:
        Dictionary with created records and errors.
    """
    created = []
    errors = []
    
    if not dto_list:
        return {"success": True, "created_count": 0, "created": [], "errors": []}
    
    # 1. PRÉPARATION DES DONNÉES EN LOTS
    batch_size = 100  # Traiter par lots de 100
    
    # Pré-créer les documents auxiliaires une seule fois
    companies = set(dto.company for dto in dto_list if dto.company)
    holiday_lists_cache = {}
    
    try:
        # Créer les Holiday Lists nécessaires en une fois
        for company in companies:
            holiday_lists_cache[company] = get_or_create_holiday_list(company)
        
        # Créer la désignation une seule fois
        designation = get_or_create_designation("Consultant")
        
        # Traiter par lots
        for i in range(0, len(dto_list), batch_size):
            batch = dto_list[i:i + batch_size]
            batch_result = _process_employee_batch(
                batch, 
                holiday_lists_cache, 
                designation,
                start_idx=i + 1
            )
            
            created.extend(batch_result["created"])
            errors.extend(batch_result["errors"])
            
            
    except Exception as e:
        print(f"Erreur globale insertion employés: {str(e)}")
        errors.append({
            "line": 0,
            "error_message": f"Erreur système: {str(e)}",
            "data": None
        })
    
    return {
        "success": len(errors) == 0,
        "created_count": len(created),
        "created": created,
        "errors": errors
    }

def _process_employee_batch(batch: list, holiday_lists_cache: dict, 
                          designation: str, start_idx: int) -> Dict[str, Any]:
    """Traite un lot d'employés de manière optimisée."""
    batch_created = []
    batch_errors = []
    
    # 2. VALIDATION EN BLOC
    valid_employees = []
    existing_refs = set()
    
    # Vérifier les employés existants en une seule requête
    if batch:
        refs_to_check = [str(dto.ref) for dto in batch if dto.ref]
        if refs_to_check:
            existing_employees = frappe.db.get_list(
                "Employee",
                filters={"employee": ["in", refs_to_check]},
                fields=["employee"]
            )
            existing_refs = set(emp.employee for emp in existing_employees)
    
    # Valider et préparer les données
    for idx, dto in enumerate(batch, start=start_idx):
        try:
            # Validation des champs requis
            if not all([dto.ref, dto.prenom, dto.nom, dto.date_embauche, 
                       dto.date_naissance, dto.company]):
                raise ValueError("Champs requis manquants")
            
            # Vérifier si existe déjà
            if str(dto.ref) in existing_refs:
                batch_errors.append({
                    "line": idx,
                    "error_message": f"Employé {dto.ref} existe déjà",
                    "data": vars(dto)
                })
                continue
            
            random_designation = get_random_designation()
            if not random_designation:
                raise ValueError("Aucune désignation disponible dans la base de données")
            
            # Récupère un département aléatoire
            random_department = get_random_department()
            if not random_department:
                raise ValueError("Aucun département disponible dans la base de données")
            
            # Préparer les données employé
            employee_data = {
                "doctype": "Employee",
                "ref": str(dto.ref),
                "name": str(dto.ref),
                "employee": str(dto.ref),
                "first_name": dto.prenom,
                "last_name": dto.nom,
                "gender": convert_gender(dto.genre),
                "date_of_joining": convert_date_format(dto.date_embauche),
                "date_of_birth": convert_date_format(dto.date_naissance),
                "company": dto.company,
                "status": "Active",
                "employee_name": f"{dto.prenom} {dto.nom}",
                "department": random_department,
                "designation": random_designation,
                "default_holiday_list": holiday_lists_cache.get(dto.company)
            }
            
            valid_employees.append((employee_data, dto, idx))
        except Exception as e:
            batch_errors.append({
                "line": idx,
                "error_message": f"Erreur validation {dto.ref}: {str(e)}",
                "data": vars(dto)
            })
    
    # 3. INSERTION OPTIMISÉE
    if valid_employees:
        batch_created.extend(_bulk_insert_employees(valid_employees))
    
    return {"created": batch_created, "errors": batch_errors}



def get_random_designation():
    """
    Récupère une désignation aléatoire depuis la base de données.
    Returns:
        str: Nom de la désignation ou None si aucune trouvée.
    """
    try:
        designations = frappe.db.sql("SELECT name FROM tabDesignation", as_dict=False)
        if designations:
            # Sélectionne une désignation aléatoire
            random_designation = random.choice(designations)[0]
            return random_designation
        else:
            frappe.log_error("Aucune désignation trouvée dans la base", "Random Designation")
            return None
    except Exception as e:
        frappe.log_error(f"Erreur lors de la récupération des désignations: {str(e)}", "Random Designation Error")
        return None

def get_random_department():
    """
    Récupère un département aléatoire depuis la base de données.
    Returns:
        str: Nom du département ou None si aucun trouvé.
    """
    try:
        departments = frappe.db.sql("SELECT name FROM tabDepartment", as_dict=False)
        if departments:
            # Sélectionne un département aléatoire
            random_department = random.choice(departments)[0]
            return random_department
        else:
            frappe.log_error("Aucun département trouvé dans la base", "Random Department")
            return None
    except Exception as e:
        frappe.log_error(f"Erreur lors de la récupération des départements: {str(e)}", "Random Department Error")
        return None
    
    
    
    
    
def get_salary_component_type(component_type_input):
    """
    Détermine le type de composant de salaire basé sur des listes de mots équivalents
    
    Args:
        component_type_input (str): Type d'entrée à analyser
    
    Returns:
        str: "Earning" ou "Deduction"
    """
    
    # Liste des mots équivalents pour les gains (Earnings)
    earning_keywords = [
        # Français
        "salaire", "gain", "revenu", "prime", "bonus", "allocation", "indemnite", 
        "indemnité", "gratification", "commission", "avantage", "supplement", 
        "supplément", "majoration", "heures_supplementaires", "overtime", 
        "anciennete", "ancienneté", "responsabilite", "responsabilité", 
        "transport", "logement", "repas", "telephone", "téléphone", "voiture",
        "formation", "performance", "objectif", "productivite", "productivité",
        "risque", "penibilite", "pénibilité", "nuit", "weekend", "dimanche",
        "conge", "congé", "vacances", "treizieme", "treizième", "quatorzieme", "quatorzième",
        
        # Anglais
        "earning","earnings", "salary", "wage", "income", "allowance", "bonus", "commission",
        "premium", "overtime", "supplement", "benefit", "incentive", "reward",
        "gratuity", "tip", "perquisite", "perk", "addition", "extra", "plus",
        "house_allowance", "transport_allowance", "medical_allowance", 
        "food_allowance", "phone_allowance", "car_allowance", "fuel_allowance",
        "night_shift", "weekend_allowance", "holiday_pay", "annual_bonus",
        "performance_bonus", "sales_commission", "overtime_pay", "shift_allowance"
    ]
    
    # Liste des mots équivalents pour les déductions (Deductions)
    deduction_keywords = [
        # Français
        "deduction", "déduction", "retenue", "cotisation", "impot", "impôt",
        "taxe", "charge", "prelevement", "prélèvement", "securite_sociale", 
        "sécurité_sociale", "retraite", "chomage", "chômage", "maladie",
        "accident_travail", "mutuelle", "assurance", "pension", "cnss", "cnam",
        "amo", "amc", "family_allowance_contribution", "formation_professionnelle",
        "solidarite", "solidarité", "syndical", "syndicat", "avance", "pret", "prêt",
        "absence", "retard", "penalite", "pénalité", "amende", "sanction",
        "conge_sans_solde", "congé_sans_solde", "arret_maladie", "arrêt_maladie",
        
        # Anglais
        "deduction","deductions", "withholding", "tax", "contribution", "social_security",
        "insurance", "pension", "retirement", "medical", "health", "dental",
        "life_insurance", "unemployment", "disability", "worker_compensation",
        "union", "dues", "loan", "advance", "garnishment", "fine", "penalty",
        "absent", "late", "unpaid_leave", "sick_leave", "personal_loan",
        "income_tax", "federal_tax", "state_tax", "medicare", "medicaid",
        "401k", "provident_fund", "gratuity_deduction", "professional_tax"
    ]
    
    # Normaliser l'entrée (minuscules, sans espaces supplémentaires)
    normalized_input = component_type_input.lower().strip().replace(" ", "_")
    
    # Vérifier d'abord les correspondances exactes
    if normalized_input in [keyword.lower() for keyword in earning_keywords]:
        return "Earning"
    
    if normalized_input in [keyword.lower() for keyword in deduction_keywords]:
        return "Deduction"
    
    # Vérifier les correspondances partielles (contient le mot-clé)
    for keyword in earning_keywords:
        if keyword.lower() in normalized_input or normalized_input in keyword.lower():
            return "Earning"
    
    for keyword in deduction_keywords:
        if keyword.lower() in normalized_input or normalized_input in keyword.lower():
            return "Deduction"
    
    # Par défaut, si aucune correspondance, retourner "Earning"
    # (ou vous pouvez lever une exception pour forcer la spécification)
    return "Earning"
