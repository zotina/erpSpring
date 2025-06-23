package mg.itu.util;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;

public class SeniorityUtil {
    
    
    public static String calculateSeniority(Date dateOfJoining) {
        if (dateOfJoining == null) {
            return "-";
        }
        
        try {
             
            LocalDate joinDate = dateOfJoining.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
            
            LocalDate currentDate = LocalDate.now();
            
            
            Period period = Period.between(joinDate, currentDate);
            
            int years = period.getYears();
            int months = period.getMonths();
            
            
            if (years > 0) {
                if (months > 6) {
                    return (years + 1) + " ans"; 
                } else {
                    return years + " ans";
                }
            } else if (months > 0) {
                return months + " mois";
            } else {
                return "Moins d'un mois";
            }
            
        } catch (Exception e) {
            return "-";
        }
    }
    
    
    public static String calculateDetailedSeniority(Date dateOfJoining) {
        if (dateOfJoining == null) {
            return "-";
        }
        
        try {
            LocalDate joinDate = dateOfJoining.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
            
            LocalDate currentDate = LocalDate.now();
            Period period = Period.between(joinDate, currentDate);
            
            int years = period.getYears();
            int months = period.getMonths();
            
            StringBuilder result = new StringBuilder();
            
            if (years > 0) {
                result.append(years).append(" ans");
                if (months > 0) {
                    result.append(" ").append(months).append(" mois");
                }
            } else if (months > 0) {
                result.append(months).append(" mois");
            } else {
                result.append("Moins d'un mois");
            }
            
            return result.toString();
            
        } catch (Exception e) {
            return "-";
        }
    }
}