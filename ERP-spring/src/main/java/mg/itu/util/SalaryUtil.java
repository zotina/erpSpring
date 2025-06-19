package mg.itu.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SalaryUtil {
    
    public static class SalarySummaryCalculation {
        private double totalGross;
        private double totalNet;
        private double totalDeduction;
        private double averageGross;
        private double medianGross;
        private int totalEmployees;
        private String currency;
        
        public SalarySummaryCalculation() {}
        
        public SalarySummaryCalculation(double totalGross, double totalNet, double totalDeduction, 
                                      double averageGross, double medianGross, int totalEmployees, String currency) {
            this.totalGross = totalGross;
            this.totalNet = totalNet;
            this.totalDeduction = totalDeduction;
            this.averageGross = averageGross;
            this.medianGross = medianGross;
            this.totalEmployees = totalEmployees;
            this.currency = currency;
        }
        
        public double getTotalGross() { return totalGross; }
        public void setTotalGross(double totalGross) { this.totalGross = totalGross; }
        
        public double getTotalNet() { return totalNet; }
        public void setTotalNet(double totalNet) { this.totalNet = totalNet; }
        
        public double getTotalDeduction() { return totalDeduction; }
        public void setTotalDeduction(double totalDeduction) { this.totalDeduction = totalDeduction; }
        
        public double getAverageGross() { return averageGross; }
        public void setAverageGross(double averageGross) { this.averageGross = averageGross; }
        
        public double getMedianGross() { return medianGross; }
        public void setMedianGross(double medianGross) { this.medianGross = medianGross; }
        
        public int getTotalEmployees() { return totalEmployees; }
        public void setTotalEmployees(int totalEmployees) { this.totalEmployees = totalEmployees; }
        
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        
        public Map<String, Object> toModelAttributes() {
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("totalGross", this.totalGross);
            attributes.put("totalNet", this.totalNet);
            attributes.put("totalDeduction", this.totalDeduction);
            attributes.put("averageGross", this.averageGross);
            attributes.put("medianGross", this.medianGross);
            attributes.put("totalEmployees", this.totalEmployees);
            attributes.put("currency", this.currency);
            return attributes;
        }
    }
    
    
    public interface SalaryExtractor<T> {
        double getTotalGross(T item);
        default double getNetPay(T item) { return 0.0; }
        default double getTotalDeduction(T item) { return 0.0; }
        default String getCurrency(T item) { return "EUR"; }
    }
    
    public static <T> SalarySummaryCalculation calculateSalarySummary(List<T> dataList, SalaryExtractor<T> extractor) {
        if (dataList == null || dataList.isEmpty()) {
            return new SalarySummaryCalculation(0.0, 0.0, 0.0, 0.0, 0.0, 0, "EUR");
        }
        
        double totalGross = 0.0;
        double totalNet = 0.0;
        double totalDeduction = 0.0;
        List<Double> grossList = new ArrayList<>();
        
        String currency = extractor.getCurrency(dataList.get(0));
        
        for (T item : dataList) {
            double gross = extractor.getTotalGross(item);
            totalGross += gross;
            totalNet += extractor.getNetPay(item);
            totalDeduction += extractor.getTotalDeduction(item);
            grossList.add(gross);
        }
        
        double averageGross = totalGross / dataList.size();
        double medianGross = calculateMedian(grossList);
        
        return new SalarySummaryCalculation(
            totalGross, totalNet, totalDeduction, 
            averageGross, medianGross, dataList.size(), currency
        );
    }
    
    
    public static SalarySummaryCalculation calculateSummaryStatistics(List<?> summaryList) {
        if (summaryList == null || summaryList.isEmpty()) {
            return new SalarySummaryCalculation(0.0, 0.0, 0.0, 0.0, 0.0, 0, "EUR");
        }
        
        
        @SuppressWarnings("unchecked")
        List<Object> objectList = (List<Object>) summaryList;
        
        return calculateSalarySummary(objectList, new SalaryExtractor<Object>() {
            @Override
            public double getTotalGross(Object item) {
                try {
                    return (Double) item.getClass().getMethod("getTotalGross").invoke(item);
                } catch (Exception e) {
                    return 0.0;
                }
            }
            
            @Override
            public double getNetPay(Object item) {
                try {
                    return (Double) item.getClass().getMethod("getNetPay").invoke(item);
                } catch (Exception e) {
                    return 0.0;
                }
            }
            
            @Override
            public double getTotalDeduction(Object item) {
                try {
                    return (Double) item.getClass().getMethod("getTotal_deduction").invoke(item);
                } catch (Exception e) {
                    return 0.0;
                }
            }
            
            @Override
            public String getCurrency(Object item) {
                try {
                    String curr = (String) item.getClass().getMethod("getCurrency").invoke(item);
                    return curr != null ? curr : "EUR";
                } catch (Exception e) {
                    return "EUR";
                }
            }
        });
    }
    
    public static double calculateMedian(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        
        List<Double> sortedValues = new ArrayList<>(values);
        Collections.sort(sortedValues);
        
        int size = sortedValues.size();
        if (size % 2 == 0) {
            return (sortedValues.get(size / 2 - 1) + sortedValues.get(size / 2)) / 2.0;
        } else {
            return sortedValues.get(size / 2);
        }
    }
    
    public static double calculateAverage(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        
        double sum = values.stream().mapToDouble(Double::doubleValue).sum();
        return sum / values.size();
    }
    
    public static String formatAmount(double amount, String currency) {
        return String.format("%.2f %s", amount, currency != null ? currency : "EUR");
    }
    
    public static double roundToTwoDecimals(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }
}