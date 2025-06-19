package mg.itu.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SalaryEvolutionDTO {
    
    @JsonProperty("month")
    private String month;
    
    @JsonProperty("month_short")
    private String monthShort;
    
    @JsonProperty("total_net")
    private double totalNet;
    
    @JsonProperty("earnings")
    private Map<String, Double> earnings;
    
    @JsonProperty("deductions")
    private Map<String, Double> deductions;

    
    public SalaryEvolutionDTO() {}

    public SalaryEvolutionDTO(String month, String monthShort, double totalNet, 
                            Map<String, Double> earnings, Map<String, Double> deductions) {
        this.month = month;
        this.monthShort = monthShort;
        this.totalNet = totalNet;
        this.earnings = earnings;
        this.deductions = deductions;
    }

    
    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public String getMonthShort() {
        return monthShort;
    }

    public void setMonthShort(String monthShort) {
        this.monthShort = monthShort;
    }

    public double getTotalNet() {
        return totalNet;
    }

    public void setTotalNet(double totalNet) {
        this.totalNet = totalNet;
    }

    public Map<String, Double> getEarnings() {
        return earnings;
    }

    public void setEarnings(Map<String, Double> earnings) {
        this.earnings = earnings;
    }

    public Map<String, Double> getDeductions() {
        return deductions;
    }

    public void setDeductions(Map<String, Double> deductions) {
        this.deductions = deductions;
    }
}