# hrms/utils/date_formatter.py


class DateFormatter:
    """Utilitaire pour le formatage des dates et mois en français"""
    
    MONTHS_FR = [
        "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
        "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"
    ]
    
    MONTHS_FR_CAPS = {
        "01": "JANVIER", "02": "FÉVRIER", "03": "MARS", "04": "AVRIL",
        "05": "MAI", "06": "JUIN", "07": "JUILLET", "08": "AOÛT",
        "09": "SEPTEMBRE", "10": "OCTOBRE", "11": "NOVEMBRE", "12": "DÉCEMBRE"
    }
    
    def get_french_month_name(self, month_number):
        """
        Retourne le nom du mois en français
        
        Args:
            month_number (int): Numéro du mois (1-12)
            
        Returns:
            str: Nom du mois en français
        """
        if 1 <= month_number <= 12:
            return self.MONTHS_FR[month_number - 1]
        return "MOIS"
    
    def get_french_month_name_caps(self, year_month):
        """
        Retourne le nom du mois en français en majuscules
        
        Args:
            year_month (str): Mois au format YYYY-MM
            
        Returns:
            str: Nom du mois en français en majuscules
        """
        month_num = year_month.split('-')[1]
        return self.MONTHS_FR_CAPS.get(month_num, "MOIS")