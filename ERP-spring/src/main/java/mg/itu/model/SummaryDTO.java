package mg.itu.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import mg.itu.util.DateUtil;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SummaryDTO {
    @JsonProperty("employee")
    private String employeeId;

    @JsonProperty("employee_name")
    private String employeeName;

    @JsonProperty("department") 
    private String department; 


    @JsonProperty("gross_pay")
    private double totalGross;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("net_pay")
    private double netPay;

    private double total_deduction;

    @JsonProperty("posting_date")
    private String  postingDate;

    private String monthYear;

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public double getTotalGross() {
        return totalGross;
    }

    public void setTotalGross(double totalGross) {
        this.totalGross = totalGross;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public double getNetPay() {
        return netPay;
    }

    public void setNetPay(double netPay) {
        this.netPay = netPay;
    }

    public String getPostingDate() {
        return postingDate;
    }

    public void setPostingDate(String postingDate) {
        this.postingDate = postingDate;
    }

    public double getTotal_deduction() {
        return total_deduction;
    }

    public void setTotal_deduction(double total_deduction) {
        this.total_deduction = total_deduction;
    }

    public String getMonthYear() {
        return monthYear;
    }

    public void setMonthYear(String monthYear) {
        
        this.monthYear = DateUtil.parseToyyyyMM(monthYear);
    }
    
}