package mg.itu.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Locale;

public class DateUtil {
    
    public static String getLastDayOfMonth(String monthYearString) {
        if (monthYearString == null || monthYearString.trim().isEmpty()) {
            return null;
        }
        try {
            YearMonth yearMonth = YearMonth.parse(monthYearString.trim());
            LocalDate lastDayOfMonth = yearMonth.atEndOfMonth();
            return lastDayOfMonth.format(DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Format de date invalide. Utilisez YYYY-MM (ex: 2025-03)", e);
        }
    }

    public static String getFirstDayOfMonth(String monthYearString) {
        if (monthYearString == null || monthYearString.trim().isEmpty()) {
            return null;
        }
        try {
            YearMonth yearMonth = YearMonth.parse(monthYearString.trim());
            LocalDate firstDayOfMonth = yearMonth.atDay(1);
            return firstDayOfMonth.format(DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Format de date invalide. Utilisez YYYY-MM (ex: 2025-03)", e);
        }
    }

    public static String getLastDayOfMonth(String month, String year) {
        if (month == null || year == null) {
            return null;
        }
        String monthYearString = year + "-" + String.format("%02d", Integer.parseInt(month));
        return getLastDayOfMonth(monthYearString);
    }

    public static String getFirstDayOfMonth(String month, String year) {
        if (month == null || year == null) {
            return null;
        }
        String monthYearString = year + "-" + String.format("%02d", Integer.parseInt(month));
        return getFirstDayOfMonth(monthYearString);
    }

    public static Date parseDate(String dateString) {
        try {
            if (dateString != null && !dateString.isEmpty()) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                return sdf.parse(dateString);
            }
        } catch (Exception e) {
            System.out.println("Failed to parse date: " + dateString);
        }
        return null;
    }

    /**
     * Parse une date au format "Day MMM dd HH:mm:ss EAT yyyy" vers le format "ddMMyyyy"
     * Exemple: "Tue Apr 01 00:00:00 EAT 2025" -> "01042025"
     * 
     * @param dateString La chaîne de date à parser
     * @return La date formatée en ddMMyyyy ou null si erreur
     */
    public static String parseToddMMyyyy(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Format d'entrée: "Day MMM dd HH:mm:ss EAT yyyy"
            SimpleDateFormat inputFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
            Date date = inputFormat.parse(dateString.trim());
            
            // Format de sortie: "ddMMyyyy"
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
            return outputFormat.format(date);
            
        } catch (ParseException e) {
            System.out.println("Erreur lors du parsing de la date: " + dateString + " - " + e.getMessage());
            return null;
        }
    }


    public static String parseToyyyyMM(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Format d'entrée: "Day MMM dd HH:mm:ss EAT yyyy"
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            Date date = inputFormat.parse(dateString.trim());
            
            // Format de sortie: "ddMMyyyy"
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM");
            return outputFormat.format(date);
            
        } catch (ParseException e) {
            System.out.println("Erreur lors du parsing de la date: " + dateString + " - " + e.getMessage());
            return null;
        }
    }

    public static boolean getBooleanValue(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() == 1;
        }
        return "1".equals(value.toString()) || "true".equalsIgnoreCase(value.toString());
    }
}