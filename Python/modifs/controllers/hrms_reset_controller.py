import frappe
from frappe import _

@frappe.whitelist(allow_guest=False)
def reset_hrms_data():
    """
    Reset HRMS-related tables by deleting all records.
    Excludes Account table as it is conditionally created.
    Returns:
        Dictionary with success status, message, and deleted tables.
    """
    try:
        table_names = [
            "Salary Slip",
            "Salary Structure Assignment",
            "Salary Detail",
            "Salary Structure",
            "Salary Component",
            "Employee",
            "Holiday List"
        ]

        deleted_tables = []
        errors = []

        for table in table_names:
            try:
                if frappe.db.exists("DocType", table):
                    frappe.db.delete(table)
                    frappe.db.commit()
                    deleted_tables.append(table)
                    frappe.log_error(
                        message=f"Données de la table {table} réinitialisées par {frappe.session.user}",
                        title="Réinitialisation de table"
                    )
                else:
                    errors.append({
                        "table": table,
                        "error_message": f"Table {table} does not exist"
                    })
            except Exception as e:
                frappe.db.rollback()
                errors.append({
                    "table": table,
                    "error_message": str(e)
                })
                frappe.log_error(
                    message=f"Erreur lors de la réinitialisation de la table {table}: {str(e)}",
                    title="Erreur de réinitialisation"
                )

        response = {
            "success": len(errors) == 0,
            "message": _("Les données des tables {0} ont été réinitialisées avec succès.").format(", ".join(deleted_tables)) if deleted_tables else _("Aucune table n'a été réinitialisée."),
            "deleted_tables": deleted_tables,
            "errors": errors
        }

        frappe.log_error("HRMS Tables Reset", f"Response: {response}")
        return response

    except Exception as e:
        frappe.db.rollback()
        frappe.log_error(
            message=f"Erreur globale lors de la réinitialisation: {str(e)}",
            title="Erreur de réinitialisation globale"
        )
        return {
            "success": False,
            "message": _("Erreur lors de la réinitialisation des tables: {0}").format(str(e)),
            "deleted_tables": [],
            "errors": [{"table": "Global", "error_message": str(e)}]
        }