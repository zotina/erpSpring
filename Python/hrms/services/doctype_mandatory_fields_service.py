#!/usr/bin/env python3
"""
Service Python pour afficher les colonnes obligatoires d'un DocType Frappe/ERPNext
Basé sur la documentation Frappe Framework
"""

import frappe
from frappe import _
import json
from typing import List, Dict, Any


class DocTypeMandatoryFieldsService:
    """Service pour récupérer et afficher les champs obligatoires d'un DocType"""
    
    def __init__(self):
        """Initialise le service"""
        self.mandatory_fields = []
        self.doctype_name = None
    
    def get_mandatory_fields(self, doctype_name: str) -> List[Dict[str, Any]]:
        """
        Récupère tous les champs obligatoires d'un DocType
        
        Args:
            doctype_name (str): Nom du DocType à analyser
            
        Returns:
            List[Dict]: Liste des champs obligatoires avec leurs métadonnées
        """
        try:
            # Vérifier si le DocType existe
            if not frappe.db.exists("DocType", doctype_name):
                frappe.throw(_("DocType '{0}' n'existe pas").format(doctype_name))
            
            self.doctype_name = doctype_name
            
            # Récupérer les métadonnées du DocType
            doctype_meta = frappe.get_meta(doctype_name)
            
            # Filtrer les champs obligatoires
            mandatory_fields = []
            
            for field in doctype_meta.fields:
                if field.reqd:  # reqd = required (obligatoire)
                    field_info = {
                        'fieldname': field.fieldname,
                        'fieldtype': field.fieldtype,
                        'sqltype': self._get_sql_type(field.fieldtype, field.length, field.precision)
                    }
                    mandatory_fields.append(field_info)
            
            self.mandatory_fields = mandatory_fields
            return mandatory_fields
            
        except Exception as e:
            frappe.log_error(f"Erreur lors de la récupération des champs obligatoires: {str(e)}")
            frappe.throw(_("Erreur lors de la récupération des champs obligatoires: {0}").format(str(e)))
    
    def display_mandatory_fields(self, doctype_name: str, output_format: str = 'table') -> str:
        """
        Affiche les champs obligatoires dans le format spécifié
        
        Args:
            doctype_name (str): Nom du DocType
            output_format (str): Format d'affichage ('table', 'json', 'list')
            
        Returns:
            str: Représentation formatée des champs obligatoires
        """
        mandatory_fields = self.get_mandatory_fields(doctype_name)
        
        if not mandatory_fields:
            return f"Aucun champ obligatoire trouvé pour le DocType '{doctype_name}'"
        
        if output_format == 'json':
            return self._format_as_json(mandatory_fields)
        elif output_format == 'list':
            return self._format_as_list(mandatory_fields)
        else:
            return self._format_as_table(mandatory_fields)
    
    def _get_sql_type(self, fieldtype: str, length: int = None, precision: int = None) -> str:
        """
        Convertit un fieldtype Frappe en type SQL correspondant
        
        Args:
            fieldtype (str): Type de champ Frappe
            length (int): Longueur du champ (pour varchar, etc.)
            precision (int): Précision pour les nombres décimaux
            
        Returns:
            str: Type SQL équivalent
        """
        # Mapping des types Frappe vers SQL
        frappe_to_sql_mapping = {
            # Types texte
            'Data': f'VARCHAR({length or 140})',
            'Small Text': 'TEXT',
            'Text': 'TEXT',
            'Long Text': 'LONGTEXT',
            'Text Editor': 'LONGTEXT',
            'HTML Editor': 'LONGTEXT',
            'Markdown Editor': 'LONGTEXT',
            'Code': 'LONGTEXT',
            
            # Types numériques
            'Int': 'INT(11)',
            'Float': f'DECIMAL(18,{precision or 6})',
            'Currency': f'DECIMAL(18,{precision or 6})',
            'Percent': f'DECIMAL(18,{precision or 6})',
            
            # Types date/heure
            'Date': 'DATE',
            'Datetime': 'DATETIME(6)',
            'Time': 'TIME(6)',
            
            # Types spéciaux
            'Check': 'INT(1)',
            'Select': f'VARCHAR({length or 140})',
            'Link': f'VARCHAR({length or 140})',
            'Dynamic Link': f'VARCHAR({length or 140})',
            'Password': f'TEXT',
            'Read Only': f'VARCHAR({length or 140})',
            'Attach': 'TEXT',
            'Attach Image': 'TEXT',
            'Signature': 'LONGTEXT',
            'Color': 'VARCHAR(140)',
            'Barcode': 'TEXT',
            'Geolocation': 'TEXT',
            'Duration': 'DECIMAL(18,6)',
            'Rating': 'INT(1)',
            'JSON': 'JSON',
            
            # Types de table
            'Table': 'TEXT',
            'Table MultiSelect': 'TEXT',
            
            # Types de section
            'Section Break': None,
            'Column Break': None,
            'Page Break': None,
            'Fold': None,
            'Heading': None,
            'HTML': None,
            'Button': None,
            'Image': None,
            'Icon': None,
            'Tab Break': None
        }
        
        sql_type = frappe_to_sql_mapping.get(fieldtype, f'VARCHAR({length or 140})')
        
        # Retourner None pour les types qui ne correspondent pas à des colonnes DB
        if sql_type is None:
            return 'N/A (UI Element)'
            
        return sql_type

    def _format_as_table(self, fields: List[Dict]) -> str:
        """Formate les champs en tableau"""
        output = f"\n{'='*80}\n"
        output += f"CHAMPS OBLIGATOIRES DU DOCTYPE: {self.doctype_name}\n"
        output += f"{'='*80}\n\n"
        
        # En-têtes
        output += f"{'Nom du champ':<20} {'Label':<20} {'Type Frappe':<15} {'Type SQL':<20} {'Options':<15}\n"
        output += f"{'-'*20} {'-'*20} {'-'*15} {'-'*20} {'-'*15}\n"
        
        # Données
        for field in fields:
            fieldname = field['fieldname'][:19]
            label = (field['label'] or '')[:19]
            fieldtype = field['fieldtype'][:14]
            sqltype = field['sqltype'][:19]
            options = (field['options'] or '')[:14]
            
            output += f"{fieldname:<20} {label:<20} {fieldtype:<15} {sqltype:<20} {options:<15}\n"
        
        output += f"\nTotal: {len(fields)} champs obligatoires\n"
        return output
    
    def _format_as_json(self, fields: List[Dict]) -> str:
        """Formate les champs en JSON"""
        return json.dumps({
            'doctype': self.doctype_name,
            'mandatory_fields_count': len(fields),
            'mandatory_fields': fields
        }, indent=2, ensure_ascii=False)
    
    def _format_as_list(self, fields: List[Dict]) -> str:
        """Formate les champs en liste simple"""
        output = f"\nChamps obligatoires du DocType '{self.doctype_name}':\n"
        output += f"{'-'*50}\n"
        
        for i, field in enumerate(fields, 1):
            output += f"{i}. {field['fieldname']} ({field['label']}) - {field['fieldtype']} -> {field['sqltype']}\n"
            if field['description']:
                output += f"   Description: {field['description']}\n"
            if field['default']:
                output += f"   Valeur par défaut: {field['default']}\n"
            output += "\n"
        
        return output
    
    def get_field_details(self, doctype_name: str, fieldname: str) -> Dict[str, Any]:
        """
        Récupère les détails complets d'un champ spécifique
        
        Args:
            doctype_name (str): Nom du DocType
            fieldname (str): Nom du champ
            
        Returns:
            Dict: Détails complets du champ
        """
        try:
            doctype_meta = frappe.get_meta(doctype_name)
            field_meta = doctype_meta.get_field(fieldname)
            
            if not field_meta:
                return {}
            
            return {
                'fieldname': field_meta.fieldname,
                'label': field_meta.label,
                'fieldtype': field_meta.fieldtype,
                'sqltype': self._get_sql_type(field_meta.fieldtype, field_meta.length, field_meta.precision)
            }
            
        except Exception as e:
            frappe.log_error(f"Erreur lors de la récupération des détails du champ: {str(e)}")
            return {}


# Fonction utilitaire pour utilisation directe
def show_mandatory_fields(doctype_name: str, output_format: str = 'table'):
    """
    Fonction utilitaire pour afficher les champs obligatoires
    
    Args:
        doctype_name (str): Nom du DocType
        output_format (str): Format d'affichage ('table', 'json', 'list')
    """
    service = DocTypeMandatoryFieldsService()
    result = service.display_mandatory_fields(doctype_name, output_format)
    print(result)
    return result


# API Hooks pour intégration avec Frappe
@frappe.whitelist()
def get_doctype_mandatory_fields(doctype_name):
    """
    API endpoint pour récupérer les champs obligatoires via REST API
    
    Usage: /api/method/your_app.your_module.get_doctype_mandatory_fields?doctype_name=Customer
    """
    service = DocTypeMandatoryFieldsService()
    return service.get_mandatory_fields(doctype_name)


@frappe.whitelist()
def display_doctype_mandatory_fields(doctype_name, output_format='table'):
    """
    API endpoint pour afficher les champs obligatoires formatés
    """
    service = DocTypeMandatoryFieldsService()
    return service.display_mandatory_fields(doctype_name, output_format)