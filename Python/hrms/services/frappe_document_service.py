import frappe
from frappe import _
from typing import Dict, List, Any, Optional

class FrappeDocumentService:
    """Service généralisé pour les opérations CRUD sur les documents Frappe/ERPNext"""
    
    def extract_info(self, doctype: str, name: str, fields: List[str] = None) -> Dict[str, Any]:
        """
        Extrait les informations d'un document selon le doctype et les champs spécifiés.
        
        Args:
            doctype (str): Le nom du doctype (ex: "Salary Slip", "Employee", "Company")
            name (str): L'ID/nom du document
            fields (List[str], optional): Liste des champs à récupérer. Si None, récupère tous les champs
            
        Returns:
            Dict[str, Any]: Dictionnaire contenant les informations du document
            
        Raises:
            frappe.DoesNotExistError: Si le document n'existe pas
            frappe.PermissionError: Si l'utilisateur n'a pas les permissions
        """
        try:
            # Vérifier si le document existe
            if not frappe.db.exists(doctype, name):
                frappe.throw(_("Document {0} {1} does not exist").format(doctype, name))
            
            # Récupérer le document
            if fields:
                # Récupérer seulement les champs spécifiés
                doc_data = frappe.db.get_value(doctype, name, fields, as_dict=True)
                if not doc_data:
                    frappe.throw(_("Failed to retrieve document {0} {1}").format(doctype, name))
                
                # Ajouter le nom du document
                doc_data['name'] = name
                return doc_data
            else:
                # Récupérer tous les champs
                doc = frappe.get_doc(doctype, name)
                return doc.as_dict()
                
        except frappe.PermissionError:
            frappe.throw(_("No permission to read {0} {1}").format(doctype, name))
        except Exception as e:
            frappe.log_error(f"Error extracting info for {doctype} {name}: {str(e)}")
            frappe.throw(_("Error retrieving document: {0}").format(str(e)))
    
    def cancel_and_delete(self, doctype: str, name: str, ignore_permissions: bool = False) -> bool:
        """
        Annule et supprime un document.
        
        Args:
            doctype (str): Le nom du doctype
            name (str): L'ID/nom du document
            ignore_permissions (bool): Ignorer les vérifications de permissions
            
        Returns:
            bool: True si l'opération a réussi
            
        Raises:
            frappe.DoesNotExistError: Si le document n'existe pas
            frappe.PermissionError: Si l'utilisateur n'a pas les permissions
        """
        try:
            # Vérifier si le document existe
            if not frappe.db.exists(doctype, name):
                frappe.throw(_("Document {0} {1} does not exist").format(doctype, name))
            
            # Récupérer le document
            doc = frappe.get_doc(doctype, name)
            
            # Annuler le document s'il est soumis (docstatus = 1)
            if doc.docstatus == 1:
                doc.cancel()
                frappe.msgprint(_("Document {0} {1} has been cancelled").format(doctype, name))
            
            # Supprimer le document
            frappe.delete_doc(doctype, name, ignore_permissions=ignore_permissions)
            
            # Valider les changements
            frappe.db.commit()
            
            frappe.msgprint(_("Document {0} {1} has been deleted successfully").format(doctype, name))
            print(f"Cancelled and deleted {doctype} {name}")
            
            return True
            
        except frappe.PermissionError:
            frappe.throw(_("No permission to delete {0} {1}").format(doctype, name))
        except Exception as e:
            frappe.log_error(f"Error cancelling/deleting {doctype} {name}: {str(e)}")
            frappe.throw(_("Error deleting document: {0}").format(str(e)))
    
    def update_document(self, doctype: str, name: str, field_values: Dict[str, Any], 
                       submit_after_update: bool = False, force_update: bool = False) -> bool:
        """
        Met à jour un document avec les valeurs spécifiées.
        
        Args:
            doctype (str): Le nom du doctype
            name (str): L'ID/nom du document
            field_values (Dict[str, Any]): Dictionnaire des champs à mettre à jour {champ: nouvelle_valeur}
            submit_after_update (bool): Soumettre le document après la mise à jour
            force_update (bool): Forcer la mise à jour même pour les documents soumis (utilise db_set)
            
        Returns:
            bool: True si l'opération a réussi
            
        Raises:
            frappe.DoesNotExistError: Si le document n'existe pas
            frappe.PermissionError: Si l'utilisateur n'a pas les permissions
        """
        try:
            # Vérifier si le document existe
            if not frappe.db.exists(doctype, name):
                frappe.throw(_("Document {0} {1} does not exist").format(doctype, name))
            
            # Récupérer le document
            doc = frappe.get_doc(doctype, name)
            
            # Vérifier si le document peut être modifié
            if doc.docstatus == 2:  # Cancelled
                frappe.throw(_("Cannot update cancelled document {0} {1}").format(doctype, name))
            
            # Gestion des documents soumis
            if doc.docstatus == 1:  # Submitted
                if force_update:
                    # Mise à jour directe en base pour les documents soumis
                    for field, value in field_values.items():
                        frappe.db.set_value(doctype, name, field, value)
                    
                    frappe.db.commit()
                    frappe.msgprint(_("Document {0} {1} updated directly (submitted document)").format(doctype, name))
                    print(f"Force updated submitted {doctype} {name} with values: {field_values}")
                    return True
                else:
                    # Créer un amendement pour les documents soumis
                    return self._create_amendment(doc, field_values, submit_after_update)
            
            # Document en brouillon - mise à jour normale
            for field, value in field_values.items():
                if hasattr(doc, field):
                    setattr(doc, field, value)
                else:
                    frappe.msgprint(_("Field {0} does not exist in {1}").format(field, doctype))
            
            # Sauvegarder les modifications
            doc.save()
            
            # Soumettre si demandé
            if submit_after_update:
                doc.submit()
            
            frappe.db.commit()
            
            frappe.msgprint(_("Document {0} {1} updated successfully").format(doctype, name))
            print(f"Updated {doctype} {name} with values: {field_values}")
            
            return True
            
        except frappe.PermissionError:
            frappe.throw(_("No permission to update {0} {1}").format(doctype, name))
        except Exception as e:
            frappe.log_error(f"Error updating {doctype} {name}: {str(e)}")
            frappe.throw(_("Error updating document: {0}").format(str(e)))
    
    def _create_amendment(self, doc, field_values: Dict[str, Any], submit_after_update: bool = False):
        """
        Crée un amendement pour un document soumis.
        """
        try:
            # Vérifier si le doctype supporte les amendements
            meta = frappe.get_meta(doc.doctype)
            if not meta.is_submittable:
                frappe.throw(_("Document type {0} does not support amendments").format(doc.doctype))
            
            # Annuler le document original
            doc.cancel()
            
            # Créer un amendement
            amended_doc = frappe.copy_doc(doc)
            amended_doc.amended_from = doc.name
            
            # Appliquer les modifications
            for field, value in field_values.items():
                if hasattr(amended_doc, field):
                    setattr(amended_doc, field, value)
            
            # Sauvegarder l'amendement
            amended_doc.insert()
            
            # Soumettre si demandé
            if submit_after_update:
                amended_doc.submit()
            
            frappe.db.commit()
            
            frappe.msgprint(_("Created amendment {0} for {1} {2}").format(
                amended_doc.name, doc.doctype, doc.name))
            print(f"Created amendment {amended_doc.name} for {doc.doctype} {doc.name}")
            
            return True
            
        except Exception as e:
            frappe.log_error(f"Error creating amendment for {doc.doctype} {doc.name}: {str(e)}")
            frappe.throw(_("Error creating amendment: {0}").format(str(e)))
    
    def insert_document(self, doctype: str, field_values: Dict[str, Any], 
                       submit_after_insert: bool = False) -> str:
        """
        Insère un nouveau document avec les valeurs spécifiées.
        
        Args:
            doctype (str): Le nom du doctype
            field_values (Dict[str, Any]): Dictionnaire des champs à insérer {champ: valeur}
            submit_after_insert (bool): Soumettre le document après l'insertion
            
        Returns:
            str: Nom/ID du document créé
            
        Raises:
            frappe.ValidationError: Si les données ne sont pas valides
            frappe.PermissionError: Si l'utilisateur n'a pas les permissions
        """
        try:
            # Créer un nouveau document
            doc = frappe.new_doc(doctype)
            
            # Définir les valeurs des champs
            for field, value in field_values.items():
                if hasattr(doc, field):
                    setattr(doc, field, value)
                else:
                    frappe.msgprint(_("Warning: Field {0} does not exist in {1}").format(field, doctype))
            
            # Insérer le document
            doc.insert()
            
            # Soumettre si demandé
            if submit_after_insert:
                doc.submit()
            
            frappe.db.commit()
            
            frappe.msgprint(_("Document {0} {1} created successfully").format(doctype, doc.name))
            print(f"Created {doctype} {doc.name} with values: {field_values}")
            
            return doc.name
            
        except frappe.PermissionError:
            frappe.throw(_("No permission to create {0}").format(doctype))
        except frappe.ValidationError as e:
            frappe.throw(_("Validation error while creating {0}: {1}").format(doctype, str(e)))
        except Exception as e:
            frappe.log_error(f"Error creating {doctype}: {str(e)}")
            frappe.throw(_("Error creating document: {0}").format(str(e)))


# Exemple d'utilisation du service
def exemple_utilisation():
    """Exemples d'utilisation du service généralisé"""
    
    service = FrappeDocumentService()
    
    # 1. Extraire des informations spécifiques d'une fiche de paie
    salary_slip_info = service.extract_info(
        doctype="Salary Slip",
        name="HR-SLI-2024-00001",
        fields=["employee", "employee_name", "start_date", "end_date", "posting_date", "company"]
    )
    print("Salary Slip Info:", salary_slip_info)
    
    # 2. Extraire toutes les informations d'un employé
    employee_info = service.extract_info(
        doctype="Employee",
        name="HR-EMP-00001"
    )
    print("Employee Info:", employee_info)
    
    # 3. Insérer un nouveau document
    new_employee_name = service.insert_document(
        doctype="Employee",
        field_values={
            "first_name": "Jean",
            "last_name": "Rakoto",
            "employee_name": "Jean Rakoto",
            "company": "Ma Société",
            "department": "IT",
            "designation": "Développeur",
            "date_of_joining": "2024-01-15",
            "personal_email": "jean.rakoto@example.com",
            "cell_number": "+261 32 12 345 67"
        },
        submit_after_insert=False
    )
    print("Nouvel employé créé:", new_employee_name)
    
    # 4. Mettre à jour un document (brouillon)
    service.update_document(
        doctype="Employee",
        name="HR-EMP-00001",
        field_values={
            "cell_number": "+261 32 12 345 67",
            "personal_email": "employee@example.com"
        }
    )
    
    # 4b. Mettre à jour un document soumis (force update)
    service.update_document(
        doctype="Employee",
        name="HR-EMP-00001",
        field_values={
            "cell_number": "+261 32 99 888 77"
        },
        force_update=False  # Pour les documents soumis
    )
    
    # 5. Annuler et supprimer un document
    service.cancel_and_delete(
        doctype="Salary Slip",
        name="HR-SLI-2024-00001"
    )



@frappe.whitelist()
def api_insert_document(doctype, field_values, submit_after_insert=False):
    """API endpoint pour insérer un nouveau document"""
    service = FrappeDocumentService()
    if isinstance(field_values, str):
        import json
        field_values = json.loads(field_values)
    return service.insert_document(doctype, field_values, submit_after_insert)

@frappe.whitelist()
def api_extract_info(doctype, name, fields=None):
    """API endpoint pour extraire les informations d'un document"""
    service = FrappeDocumentService()
    if fields and isinstance(fields, str):
        fields = fields.split(',')
    return service.extract_info(doctype, name, fields)

@frappe.whitelist()
def api_update_document(doctype, name, field_values, submit_after_update=False, force_update=False):
    """API endpoint pour mettre à jour un document"""
    service = FrappeDocumentService()
    if isinstance(field_values, str):
        import json
        field_values = json.loads(field_values)
    return service.update_document(doctype, name, field_values, submit_after_update, force_update)

@frappe.whitelist()
def api_cancel_and_delete(doctype, name, ignore_permissions=False):
    """API endpoint pour annuler et supprimer un document"""
    service = FrappeDocumentService()
    return service.cancel_and_delete(doctype, name, ignore_permissions)
