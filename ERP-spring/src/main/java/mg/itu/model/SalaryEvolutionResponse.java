package mg.itu.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SalaryEvolutionResponse {
    
    @JsonProperty("monthly_data")
    private List<SalaryEvolutionDTO> monthlyData;

    
    public SalaryEvolutionResponse() {} 

    public SalaryEvolutionResponse(List<SalaryEvolutionDTO> monthlyData) {
        this.monthlyData = monthlyData;
    }

    
    public List<SalaryEvolutionDTO> getMonthlyData() {
        return monthlyData;
    }

    public void setMonthlyData(List<SalaryEvolutionDTO> monthlyData) {
        this.monthlyData = monthlyData;
    }
}