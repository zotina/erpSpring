"""
Frappe HR API CRUD - Employee Management
Ce fichier contient les endpoints API pour gérer les employés dans Frappe HR
"""

import frappe
from frappe import _
from frappe.utils import cstr, validate_email_address
from frappe.model.document import Document
import json

# Employee CRUD Operations

@frappe.whitelist()
def create_employee(employee_data):
    """
    Créer un nouvel employé
    
    Args:
        employee_data (dict): Données de l'employé
        
    Returns:
        dict: Résultat de la création
    """
    try:
        # Validation des données requises
        required_fields = ['employee_name', 'company']
        for field in required_fields:
            if not employee_data.get(field):
                frappe.throw(_("Le champ {0} est requis").format(field))
        
        # Validation de l'email si fourni
        if employee_data.get('personal_email'):
            validate_email_address(employee_data.get('personal_email'))
        
        # Créer le document Employee
        employee = frappe.get_doc({
            "doctype": "Employee",
            "employee_name": employee_data.get('employee_name'),
            "company": employee_data.get('company'),
            "personal_email": employee_data.get('personal_email'),
            "cell_number": employee_data.get('cell_number'),
            "gender": employee_data.get('gender'),
            "date_of_birth": employee_data.get('date_of_birth'),
            "date_of_joining": employee_data.get('date_of_joining'),
            "department": employee_data.get('department'),
            "designation": employee_data.get('designation'),
            "employment_type": employee_data.get('employment_type'),
            "status": employee_data.get('status', 'Active'),
            "employee_number": employee_data.get('employee_number'),
            "first_name": employee_data.get('first_name'),
            "last_name": employee_data.get('last_name')
        })
        
        employee.insert()
        frappe.db.commit()
        
        return {
            "success": True,
            "message": "Employé créé avec succès",
            "data": {
                "name": employee.name,
                "employee_name": employee.employee_name,
                "employee_number": employee.employee_number
            }
        }
        
    except Exception as e:
        frappe.db.rollback()
        return {
            "success": False,
            "message": str(e),
            "data": None
        }

@frappe.whitelist()
def get_employee(employee_id):
    """
    Récupérer les détails d'un employé
    
    Args:
        employee_id (str): ID de l'employé
        
    Returns:
        dict: Détails de l'employé
    """
    try:
        if not employee_id:
            frappe.throw(_("ID employé requis"))
        
        employee = frappe.get_doc("Employee", employee_id)
        
        return {
            "success": True,
            "message": "Employé trouvé",
            "data": {
                "name": employee.name,
                "employee_name": employee.employee_name,
                "employee_number": employee.employee_number,
                "company": employee.company,
                "personal_email": employee.personal_email,
                "cell_number": employee.cell_number,
                "gender": employee.gender,
                "date_of_birth": employee.date_of_birth,
                "date_of_joining": employee.date_of_joining,
                "department": employee.department,
                "designation": employee.designation,
                "employment_type": employee.employment_type,
                "status": employee.status,
                "first_name": employee.first_name,
                "last_name": employee.last_name,
                "creation": employee.creation,
                "modified": employee.modified
            }
        }
        
    except Exception as e:
        return {
            "success": False,
            "message": str(e),
            "data": None
        }

@frappe.whitelist()
def update_employee(employee_id, employee_data):
    """
    Mettre à jour un employé
    
    Args:
        employee_id (str): ID de l'employé
        employee_data (dict): Nouvelles données
        
    Returns:
        dict: Résultat de la mise à jour
    """
    try:
        if not employee_id:
            frappe.throw(_("ID employé requis"))
        
        employee = frappe.get_doc("Employee", employee_id)
        
        # Validation de l'email si fourni
        if employee_data.get('personal_email'):
            validate_email_address(employee_data.get('personal_email'))
        
        # Mettre à jour les champs
        updateable_fields = [
            'employee_name', 'personal_email', 'cell_number', 'gender',
            'date_of_birth', 'department', 'designation', 'employment_type',
            'status', 'first_name', 'last_name'
        ]
        
        for field in updateable_fields:
            if field in employee_data:
                setattr(employee, field, employee_data[field])
        
        employee.save()
        frappe.db.commit()
        
        return {
            "success": True,
            "message": "Employé mis à jour avec succès",
            "data": {
                "name": employee.name,
                "employee_name": employee.employee_name,
                "modified": employee.modified
            }
        }
        
    except Exception as e:
        frappe.db.rollback()
        return {
            "success": False,
            "message": str(e),
            "data": None
        }

@frappe.whitelist()
def delete_employee(employee_id):
    """
    Supprimer un employé
    
    Args:
        employee_id (str): ID de l'employé
        
    Returns:
        dict: Résultat de la suppression
    """
    try:
        if not employee_id:
            frappe.throw(_("ID employé requis"))
        
        # Vérifier si l'employé existe
        employee = frappe.get_doc("Employee", employee_id)
        employee_name = employee.employee_name
        
        # Supprimer l'employé
        frappe.delete_doc("Employee", employee_id)
        frappe.db.commit()
        
        return {
            "success": True,
            "message": f"Employé {employee_name} supprimé avec succès",
            "data": {
                "deleted_id": employee_id,
                "deleted_name": employee_name
            }
        }
        
    except Exception as e:
        frappe.db.rollback()
        return {
            "success": False,
            "message": str(e),
            "data": None
        }

@frappe.whitelist()
def get_employees(page=1, page_size=20, filters=None, search=None):
    """
    Récupérer la liste des employés avec pagination et filtres
    
    Args:
        page (int): Numéro de page
        page_size (int): Taille de la page
        filters (dict): Filtres à appliquer
        search (str): Terme de recherche
        
    Returns:
        dict: Liste des employés avec métadonnées
    """
    try:
        # Paramètres de pagination
        start = (int(page) - 1) * int(page_size)
        limit = int(page_size)
        
        # Construction des filtres
        filter_conditions = {}
        if filters:
            if isinstance(filters, str):
                filters = json.loads(filters)
            filter_conditions.update(filters)
        
        # Construction de la requête
        fields = [
            "name", "employee_name", "employee_number", "company",
            "personal_email", "cell_number", "department", "designation",
            "status", "date_of_joining", "creation", "modified"
        ]
        
        # Recherche textuelle
        or_filters = []
        if search:
            or_filters = [
                {"employee_name": ["like", f"%{search}%"]},
                {"employee_number": ["like", f"%{search}%"]},
                {"personal_email": ["like", f"%{search}%"]},
                {"department": ["like", f"%{search}%"]},
                {"designation": ["like", f"%{search}%"]}
            ]
        
        # Exécuter la requête
        employees = frappe.get_list(
            "Employee",
            fields=fields,
            filters=filter_conditions,
            or_filters=or_filters if or_filters else None,
            start=start,
            page_length=limit,
            order_by="creation desc"
        )
        
        # Compter le total
        total_count = frappe.db.count(
            "Employee",
            filters=filter_conditions
        )
        
        # Calculer les métadonnées de pagination
        total_pages = (total_count + page_size - 1) // page_size
        
        return {
            "success": True,
            "message": "Liste des employés récupérée",
            "data": {
                "employees": employees,
                "pagination": {
                    "current_page": int(page),
                    "page_size": int(page_size),
                    "total_records": total_count,
                    "total_pages": total_pages,
                    "has_next": int(page) < total_pages,
                    "has_previous": int(page) > 1
                }
            }
        }
        
    except Exception as e:
        return {
            "success": False,
            "message": str(e),
            "data": None
        }

# Department CRUD Operations

@frappe.whitelist()
def create_department(department_data):
    """Créer un nouveau département"""
    try:
        required_fields = ['department_name', 'company']
        for field in required_fields:
            if not department_data.get(field):
                frappe.throw(_("Le champ {0} est requis").format(field))
        
        department = frappe.get_doc({
            "doctype": "Department",
            "department_name": department_data.get('department_name'),
            "company": department_data.get('company'),
            "parent_department": department_data.get('parent_department'),
            "is_group": department_data.get('is_group', 0)
        })
        
        department.insert()
        frappe.db.commit()
        
        return {
            "success": True,
            "message": "Département créé avec succès",
            "data": {
                "name": department.name,
                "department_name": department.department_name
            }
        }
        
    except Exception as e:
        frappe.db.rollback()
        return {
            "success": False,
            "message": str(e),
            "data": None
        }

@frappe.whitelist()
def get_departments():
    """Récupérer la liste des départements"""
    try:
        departments = frappe.get_list(
            "Department",
            fields=["name", "department_name", "company", "parent_department", "is_group"],
            order_by="department_name"
        )
        
        return {
            "success": True,
            "message": "Liste des départements récupérée",
            "data": departments
        }
        
    except Exception as e:
        return {
            "success": False,
            "message": str(e),
            "data": None
        }

# Leave Application CRUD Operations

@frappe.whitelist()
def create_leave_application(leave_data):
    """Créer une demande de congé"""
    try:
        required_fields = ['employee', 'leave_type', 'from_date', 'to_date']
        for field in required_fields:
            if not leave_data.get(field):
                frappe.throw(_("Le champ {0} est requis").format(field))
        
        leave_app = frappe.get_doc({
            "doctype": "Leave Application",
            "employee": leave_data.get('employee'),
            "leave_type": leave_data.get('leave_type'),
            "from_date": leave_data.get('from_date'),
            "to_date": leave_data.get('to_date'),
            "half_day": leave_data.get('half_day', 0),
            "description": leave_data.get('description'),
            "leave_approver": leave_data.get('leave_approver')
        })
        
        leave_app.insert()
        frappe.db.commit()
        
        return {
            "success": True,
            "message": "Demande de congé créée avec succès",
            "data": {
                "name": leave_app.name,
                "employee": leave_app.employee,
                "leave_type": leave_app.leave_type,
                "status": leave_app.status
            }
        }
        
    except Exception as e:
        frappe.db.rollback()
        return {
            "success": False,
            "message": str(e),
            "data": None
        }

@frappe.whitelist()
def get_leave_applications(employee=None):
    """Récupérer les demandes de congé"""
    try:
        filters = {}
        if employee:
            filters["employee"] = employee
        
        leave_apps = frappe.get_list(
            "Leave Application",
            fields=["name", "employee", "employee_name", "leave_type", 
                   "from_date", "to_date", "total_leave_days", "status"],
            filters=filters,
            order_by="creation desc"
        )
        
        return {
            "success": True,
            "message": "Demandes de congé récupérées",
            "data": leave_apps
        }
        
    except Exception as e:
        return {
            "success": False,
            "message": str(e),
            "data": None
        }