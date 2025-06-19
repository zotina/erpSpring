package mg.itu.model;

import com.fasterxml.jackson.annotation.JsonProperty;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
@JsonIgnoreProperties(ignoreUnknown = true)
public class SalaryComponent {
    @JsonProperty("salary_component")
    private String salaryComponent;  

    @JsonProperty("amount")
    private double amount;

    
    public String getSalaryComponent() { return salaryComponent; }
    public void setSalaryComponent(String salaryComponent) { this.salaryComponent = salaryComponent; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    @Override
    public String toString() {
        return "SalaryComponent{salaryComponent='" + salaryComponent + "', amount=" + amount + "}";
    }
}