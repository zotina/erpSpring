package mg.itu.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MonthlyPayrollSummary {
    @JsonProperty("month")
    private String month;

    @JsonProperty("year")
    private int year;

    @JsonProperty("month_number")
    private int monthNumber;

    @JsonProperty("total_gross_pay")
    private double totalGrossPay;

    @JsonProperty("total_net_pay")
    private double totalNetPay;

    @JsonProperty("total_deductions")
    private double totalDeductions;

    @JsonProperty("employee_count")
    private int employeeCount;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("monthyear")
    private String monthYear;

    
    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public int getMonthNumber() { return monthNumber; }
    public void setMonthNumber(int monthNumber) { this.monthNumber = monthNumber; }

    public double getTotalGrossPay() { return totalGrossPay; }
    public void setTotalGrossPay(double totalGrossPay) { this.totalGrossPay = totalGrossPay; }

    public double getTotalNetPay() { return totalNetPay; }
    public void setTotalNetPay(double totalNetPay) { this.totalNetPay = totalNetPay; }

    public double getTotalDeductions() { return totalDeductions; }
    public void setTotalDeductions(double totalDeductions) { this.totalDeductions = totalDeductions; }

    public int getEmployeeCount() { return employeeCount; }
    public void setEmployeeCount(int employeeCount) { this.employeeCount = employeeCount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getMonthYear() { return monthYear; }
    public void setMonthYear(String monthYear) { this.monthYear = monthYear; }
}