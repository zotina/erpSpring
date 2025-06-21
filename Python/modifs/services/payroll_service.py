# hrms/services/payroll_service.py

import frappe
from frappe import _
from frappe.utils import get_last_day
from hrms.models.payroll_data import PayrollData
from hrms.utils.date_formatter import DateFormatter


class PayrollService:
    """Service pour gérer la logique métier des données de paie"""
    
    def __init__(self):
        self.payroll_data = PayrollData()
        self.date_formatter = DateFormatter()
    
    def get_monthly_summary(self, year):
        """
        Récupère le résumé mensuel des salaires pour une année donnée
        
        Args:
            year (str): Année au format YYYY
            
        Returns:
            list: Liste des données mensuelles ou None si aucune donnée
        """
        start_date = f"{year}-01-01"
        end_date = f"{year}-12-31"
        
        # Récupération des bulletins de salaire
        slips = self.payroll_data.get_salary_slips_for_period(start_date, end_date)
        
        if not slips:
            return None
        
        # Groupement et calcul des totaux par mois
        monthly_data = self._group_slips_by_month(slips, year)
        
        # Tri par numéro de mois
        return sorted(monthly_data.values(), key=lambda x: x["month_number"])
    
    def get_payroll_components_for_month(self, year_month, employee_id=None):
        """
        Récupère les composants de salaire pour un mois donné
        
        Args:
            year_month (str): Mois au format YYYY-MM
            employee_id (str, optional): ID de l'employé spécifique
            
        Returns:
            dict: Données des composants de salaire
        """
        start_date, end_date = self._get_month_date_range(year_month)
        filters = {"posting_date": ["between", [start_date, end_date]]}
        
        if employee_id:
            filters["employee"] = employee_id
        
        # Récupération des bulletins
        slips = self.payroll_data.get_salary_slips_with_details(filters)
        
        if not slips:
            return self._create_empty_component_response(year_month, employee_id)
        
        # Traitement des données
        return self._process_payroll_components(slips, year_month, employee_id)
    
    def get_salary_evolution(self, year):
        """
        Récupère l'évolution des salaires mois par mois pour une année
        
        Args:
            year (str): Année au format YYYY
            
        Returns:
            dict: Données d'évolution mensuelle
        """
        monthly_data = []
        
        for month in range(1, 13):
            month_data = self._get_month_salary_data(year, month)
            monthly_data.append(month_data)
        
        return {"monthly_data": monthly_data}
    
    def _group_slips_by_month(self, slips, year):
        """Groupe les bulletins de salaire par mois"""
        monthly_data = {}
        
        for slip in slips:
            month_key = slip.posting_date.strftime("%Y-%m")
            month_number = int(slip.posting_date.strftime("%m"))
            
            if month_key not in monthly_data:
                monthly_data[month_key] = self._create_monthly_summary_structure(
                    year, month_number, slip.currency
                )
            
            # Accumulation des totaux
            monthly_data[month_key]["total_gross_pay"] += slip.gross_pay or 0.0
            monthly_data[month_key]["total_net_pay"] += slip.net_pay or 0.0
            monthly_data[month_key]["total_deductions"] += slip.total_deduction or 0.0
            monthly_data[month_key]["employee_count"] += 1
        
        return monthly_data
    
    def _create_monthly_summary_structure(self, year, month_number, currency):
        """Crée la structure de base pour un résumé mensuel"""
        return {
            "month": self.date_formatter.get_french_month_name(month_number),
            "year": int(year),
            "month_number": month_number,
            "monthyear": f"{year}-{month_number:02d}",
            "total_gross_pay": 0.0,
            "total_net_pay": 0.0,
            "total_deductions": 0.0,
            "employee_count": 0,
            "currency": currency or "EUR"
        }
    
    def _get_month_date_range(self, year_month):
        """Calcule la plage de dates pour un mois donné"""
        start_date = f"{year_month}-01"
        end_date = get_last_day(year_month).strftime("%Y-%m-%d")
        return start_date, end_date
    
    def _process_payroll_components(self, slips, year_month, employee_id):
        """Traite les composants de salaire des bulletins"""
        earnings_map = {}
        deductions_map = {}
        totals = {"gross": 0.0, "deduction": 0.0, "net": 0.0}
        currency = "EUR"
        employee_info = None
        
        for slip in slips:
            # Accumulation des totaux
            totals["gross"] += slip.gross_pay or 0.0
            totals["deduction"] += slip.total_deduction or 0.0
            totals["net"] += slip.net_pay or 0.0
            currency = slip.currency or "EUR"
            
            # Information de l'employé (si mode spécifique)
            if employee_id and not employee_info:
                employee_info = {
                    "employee_id": slip.employee,
                    "employee_name": slip.employee_name
                }
            
            # Traitement des composants
            self._aggregate_salary_components(slip.name, earnings_map, deductions_map)
        
        return self._build_component_response(
            year_month, earnings_map, deductions_map, totals, 
            currency, len(slips), employee_info, employee_id
        )
    
    def _aggregate_salary_components(self, slip_name, earnings_map, deductions_map):
        """Agrège les composants de salaire d'un bulletin"""
        # Gains
        earnings = self.payroll_data.get_salary_details(slip_name, "earnings")
        for earning in earnings:
            component = earning.salary_component
            earnings_map[component] = earnings_map.get(component, 0.0) + (earning.amount or 0.0)
        
        # Déductions
        deductions = self.payroll_data.get_salary_details(slip_name, "deductions")
        for deduction in deductions:
            component = deduction.salary_component
            deductions_map[component] = deductions_map.get(component, 0.0) + (deduction.amount or 0.0)
    
    def _build_component_response(self, year_month, earnings_map, deductions_map, 
                                 totals, currency, slip_count, employee_info, employee_id):
        """Construit la réponse pour les composants de salaire"""
        response = {
            "month": year_month,
            "month_name": self.date_formatter.get_french_month_name_caps(year_month),
            "earnings": [{"salary_component": k, "amount": v} for k, v in earnings_map.items()],
            "deductions": [{"salary_component": k, "amount": v} for k, v in deductions_map.items()],
            "total_gross": totals["gross"],
            "total_deduction": totals["deduction"],
            "total_net": totals["net"],
            "currency": currency,
            "employee_count": slip_count
        }
        
        # Mode employé spécifique ou tous les employés
        if employee_id and employee_info:
            response["employee"] = employee_info
            response["mode"] = "single_employee"
        else:
            response["mode"] = "all_employees"
        
        return response
    
    def _create_empty_component_response(self, year_month, employee_id):
        """Crée une réponse vide pour les composants"""
        return {
            "month": year_month,
            "month_name": self.date_formatter.get_french_month_name_caps(year_month),
            "earnings": [],
            "deductions": [],
            "total_gross": 0.0,
            "total_deduction": 0.0,
            "total_net": 0.0,
            "currency": "EUR",
            "employee_count": 0,
            "mode": "single_employee" if employee_id else "all_employees"
        }
    
    def _get_month_salary_data(self, year, month):
        """Récupère les données de salaire pour un mois spécifique"""
        start_date = f"{year}-{month:02d}-01"
        end_date = get_last_day(start_date).strftime("%Y-%m-%d")
        
        slips = self.payroll_data.get_salary_slips_with_names_for_period(start_date, end_date)
        
        total_net = 0.0
        earnings_components = {}
        deductions_components = {}
        
        for slip in slips:
            total_net += slip.net_pay or 0.0
            
            # Traitement des gains et déductions
            self._process_salary_components_for_evolution(
                slip.name, earnings_components, deductions_components
            )
        
        return {
            "month": self.date_formatter.get_french_month_name(month),
            "month_short": self.date_formatter.get_french_month_name(month)[:3],
            "total_net": round(total_net, 2),
            "earnings": {k: round(v, 2) for k, v in earnings_components.items()},
            "deductions": {k: round(v, 2) for k, v in deductions_components.items()}
        }
    
    def _process_salary_components_for_evolution(self, slip_name, earnings_components, deductions_components):
        """Traite les composants de salaire pour l'évolution"""
        # Gains
        earnings = self.payroll_data.get_salary_details(slip_name, "earnings")
        for earning in earnings:
            component = earning.salary_component
            if component not in earnings_components:
                earnings_components[component] = 0.0
            earnings_components[component] += earning.amount or 0.0
        
        # Déductions
        deductions = self.payroll_data.get_salary_details(slip_name, "deductions")
        for deduction in deductions:
            component = deduction.salary_component
            if component not in deductions_components:
                deductions_components[component] = 0.0
            deductions_components[component] += deduction.amount or 0.0