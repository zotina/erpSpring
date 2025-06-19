package mg.itu.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PayrollComponents {
    @JsonProperty("month")
    private String month;

    @JsonProperty("month_name")
    private String monthName;

    @JsonProperty("earnings")
    private List<SalaryComponent> earnings;

    @JsonProperty("deductions")
    private List<SalaryComponent> deductions;

    @JsonProperty("total_gross")
    private double totalGross;

    @JsonProperty("total_deduction")
    private double totalDeduction;

    @JsonProperty("total_net")
    private double totalNet;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("employee_count")
    private int employeeCount;

    
    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public String getMonthName() { return monthName; }
    public void setMonthName(String monthName) { this.monthName = monthName; }

    public List<SalaryComponent> getEarnings() { return earnings; }
    public void setEarnings(List<SalaryComponent> earnings) { this.earnings = earnings; }

    public List<SalaryComponent> getDeductions() { return deductions; }
    public void setDeductions(List<SalaryComponent> deductions) { this.deductions = deductions; }

    public double getTotalGross() { return totalGross; }
    public void setTotalGross(double totalGross) { this.totalGross = totalGross; }

    public double getTotalDeduction() { return totalDeduction; }
    public void setTotalDeduction(double totalDeduction) { this.totalDeduction = totalDeduction; }

    public double getTotalNet() { return totalNet; }
    public void setTotalNet(double totalNet) { this.totalNet = totalNet; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public int getEmployeeCount() { return employeeCount; }
    public void setEmployeeCount(int employeeCount) { this.employeeCount = employeeCount; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PayrollComponents{\n");
        sb.append("  month='").append(month).append("',\n");
        sb.append("  monthName='").append(monthName).append("',\n");
        sb.append("  totalGross=").append(totalGross).append(",\n");
        sb.append("  totalDeduction=").append(totalDeduction).append(",\n");
        sb.append("  totalNet=").append(totalNet).append(",\n");
        sb.append("  currency='").append(currency).append("',\n");
        sb.append("  employeeCount=").append(employeeCount).append(",\n");

        sb.append("  earnings=[\n");
        if (earnings != null) {
            for (SalaryComponent earning : earnings) {
                sb.append("    ").append(earning.toString()).append(",\n");
            }
        }
        sb.append("  ],\n");

        sb.append("  deductions=[\n");
        if (deductions != null) {
            for (SalaryComponent deduction : deductions) {
                sb.append("    ").append(deduction.toString()).append(",\n");
            }
        }
        sb.append("  ]\n");
        sb.append("}");
        return sb.toString();
    }
}
