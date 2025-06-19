import frappe
from frappe import _
from frappe.utils import get_last_day
@frappe.whitelist(allow_guest=False)
def get_monthly_payroll_summary(year):
    """
    Endpoint: /api/method/hrms.controllers.payroll_controller.get_monthly_payroll_summary?year=2024
    Method: GET
    Retourne les totaux brut, net et déductions pour chaque mois de l'année ayant des Salary Slips
    """
    try:
        start_date = f"{year}-01-01"
        end_date = f"{year}-12-31"
        
        # Fetch Salary Slips within the year
        slips = frappe.get_all("Salary Slip",
            fields=["posting_date", "net_pay", "total_deduction", "employee", "currency", "gross_pay"],
            filters={"posting_date": ["between", [start_date, end_date]]},
            order_by="posting_date"
        )
        
        # Check if any Salary Slips exist
        if not slips:
            frappe.local.response["status"] = "error"
            frappe.local.response["message"] = f"Aucun bulletin de paie trouvé pour l'année {year}."
            frappe.local.response["validation_errors"] = ["No salary slips found"]
            return
        
        # Initialize monthly data only for months with data
        monthly_data = {}
        months = ["Janvier", "Février", "Mars", "Avril", "Mai", "Juin", 
                  "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"]
        
        # Group and calculate totals by month
        for slip in slips:
            month_key = slip.posting_date.strftime("%Y-%m")
            month_number = int(slip.posting_date.strftime("%m"))
            
            if month_key not in monthly_data:
                monthly_data[month_key] = {
                    "month": months[month_number - 1],
                    "year": int(year),
                    "month_number": month_number,
                    "monthyear": f"{year}-{month_number:02d}",
                    "total_gross_pay": 0.0,
                    "total_net_pay": 0.0,
                    "total_deductions": 0.0,
                    "employee_count": 0,
                    "currency": slip.currency or "EUR"
                }
            
            monthly_data[month_key]["total_gross_pay"] += slip.gross_pay or 0.0
            monthly_data[month_key]["total_net_pay"] += slip.net_pay or 0.0
            monthly_data[month_key]["total_deductions"] += slip.total_deduction or 0.0
            monthly_data[month_key]["employee_count"] += 1
        
        # Convert to list and sort by month_number
        result = sorted(monthly_data.values(), key=lambda x: x["month_number"])
        
        # Return success response
        frappe.local.response["status"] = "success"
        frappe.local.response["message"] = f"Récapitulatif mensuel des salaires pour l'année {year} récupéré avec succès."
        frappe.local.response["data"] = result

    except Exception as e:
        # Handle any unexpected errors
        frappe.local.response["status"] = "error"
        frappe.local.response["message"] = "Une erreur s'est produite lors de la récupération du récapitulatif mensuel des salaires."
        frappe.local.response["validation_errors"] = [str(e)]

@frappe.whitelist(allow_guest=False)
def get_payroll_components(year_month, employee_id=None):
    """
    Endpoint: /api/method/hrms.controllers.payroll_controller.get_payroll_components?year_month=2024-01&employee_id=EMP-001
    Method: GET
    Retourne le détail des composants de salaire pour un mois donné avec formatage visuel
    - Si employee_id est fourni : données pour cet employé spécifique
    - Si employee_id est None : agrégation de tous les employés (comportement actuel)
    """
    start_date = f"{year_month}-01"
    end_date = frappe.utils.get_last_day(year_month).strftime("%Y-%m-%d")
    
    # Construction des filtres
    filters = {"posting_date": ["between", [start_date, end_date]]}
    if employee_id:
        filters["employee"] = employee_id
    
    # Récupération des salary slips
    slips = frappe.get_all("Salary Slip",
        fields=["name", "employee", "employee_name", "gross_pay", "total_deduction", "net_pay", "currency"],
        filters=filters
    )
    
    print(f"Slips trouvés: {len(slips)} pour {year_month}" + (f" - Employé: {employee_id}" if employee_id else " - Tous employés"))
    
    earnings_map = {}
    deductions_map = {}
    total_gross = 0.0
    total_deduction = 0.0
    total_net = 0.0
    currency = "EUR"
    employee_info = None
    
    for slip in slips:
        total_gross += slip.gross_pay or 0.0
        total_deduction += slip.total_deduction or 0.0
        total_net += slip.net_pay or 0.0
        currency = slip.currency or "EUR"
        
        # Stocker les infos employé si mode employé spécifique
        if employee_id and not employee_info:
            employee_info = {
                "employee_id": slip.employee,
                "employee_name": slip.employee_name
            }
        
        # Récupérer les gains
        earnings = frappe.get_all("Salary Detail",
            fields=["salary_component", "amount"],
            filters={"parent": slip.name, "parentfield": "earnings"}
        )
        
        # Récupérer les déductions
        deductions = frappe.get_all("Salary Detail",
            fields=["salary_component", "amount"],
            filters={"parent": slip.name, "parentfield": "deductions"}
        )
        
        # Agrégation des gains
        for earning in earnings:
            component = earning.salary_component
            earnings_map[component] = earnings_map.get(component, 0.0) + (earning.amount or 0.0)
        
        # Agrégation des déductions
        for deduction in deductions:
            component = deduction.salary_component
            deductions_map[component] = deductions_map.get(component, 0.0) + (deduction.amount or 0.0)
    
    # Formatage du mois en français
    months_fr = {
        "01": "JANVIER", "02": "FÉVRIER", "03": "MARS", "04": "AVRIL",
        "05": "MAI", "06": "JUIN", "07": "JUILLET", "08": "AOÛT",
        "09": "SEPTEMBRE", "10": "OCTOBRE", "11": "NOVEMBRE", "12": "DÉCEMBRE"
    }
    month_num = year_month.split('-')[1]
    month_name = months_fr.get(month_num, "MOIS")
    
    # Construction de la réponse
    response = {
        "month": year_month,
        "month_name": month_name,
        "earnings": [{"salary_component": k, "amount": v} for k, v in earnings_map.items()],
        "deductions": [{"salary_component": k, "amount": v} for k, v in deductions_map.items()],
        "total_gross": total_gross,
        "total_deduction": total_deduction,
        "total_net": total_net,
        "currency": currency,
        "employee_count": len(slips)
    }
    
    # Ajouter les infos employé si mode spécifique
    if employee_id and employee_info:
        response["employee"] = employee_info
        response["mode"] = "single_employee"
    else:
        response["mode"] = "all_employees"
    
    return response

@frappe.whitelist(allow_guest=False)
def get_salary_evolution_data(year):
    """
    Endpoint: /api/method/hrms.controllers.payroll_controller.get_salary_evolution_data?year=2024
    Method: GET
    Retourne les données mensuelles de salaire pour l'année spécifiée, incluant le total net
    et les composants de salaire récupérés dynamiquement depuis ERPNext.
    """
    if not year:
        frappe.throw(_("L'année doit être spécifiée."))

    months_fr = [
        "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
        "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"
    ]

    monthly_data = []

    for month in range(1, 13):
        start_date = f"{year}-{month:02d}-01"
        end_date = get_last_day(start_date).strftime("%Y-%m-%d")
        print(start_date)
        print(end_date)
        slips = frappe.get_all("Salary Slip",
            fields=["posting_date", "net_pay", "total_deduction", "employee", "currency", "gross_pay"],
            filters={"posting_date": ["between", [start_date, end_date]]},
            order_by="posting_date"
        )
        print(slips)
        total_net = 0.0
        earnings_components = {}  # Dictionnaire vide pour les gains
        deductions_components = {}  # Dictionnaire vide pour les déductions

        for slip in slips:
            total_net += slip.net_pay or 0.0

            # Récupérer les gains (earnings)
            earnings = frappe.get_all("Salary Detail",
                fields=["salary_component", "amount"],
                filters={"parent": slip.name, "parentfield": "earnings"}
            )
            
            for earning in earnings:
                component = earning.salary_component
                if component not in earnings_components:
                    earnings_components[component] = 0.0
                earnings_components[component] += earning.amount or 0.0

            # Récupérer les déductions (deductions)
            deductions = frappe.get_all("Salary Detail",
                fields=["salary_component", "amount"],
                filters={"parent": slip.name, "parentfield": "deductions"}
            )
            for deduction in deductions:
                component = deduction.salary_component
                if component not in deductions_components:
                    deductions_components[component] = 0.0
                deductions_components[component] += deduction.amount or 0.0

        month_data = {
            "month": months_fr[month - 1],
            "month_short": months_fr[month - 1][:3],
            "total_net": round(total_net, 2),
            "earnings": {k: round(v, 2) for k, v in earnings_components.items()},
            "deductions": {k: round(v, 2) for k, v in deductions_components.items()}
        }
        monthly_data.append(month_data)

    if not any(m["total_net"] > 0 for m in monthly_data):
        print(_("Aucune donnée de salaire trouvée pour l'année {year}.").format(year=year))

    return {
        "monthly_data": monthly_data
    }