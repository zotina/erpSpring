package mg.itu.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SalaryDetailDTO {
    
    @JsonProperty("employee")
    private String employeeId;
    
    @JsonProperty("employee_name")
    private String employeeName;
    
    @JsonProperty("department")
    private String department;
    
    @JsonProperty("designation")
    private String designation;
    
    @JsonProperty("start_date")
    private Date startDate;
    
    @JsonProperty("end_date")
    private Date endDate;
    
    @JsonProperty("posting_date")
    private Date postingDate;
    
    @JsonProperty("total_working_days")
    private double totalWorkingDays;
    
    @JsonProperty("payment_days")
    private double paymentDays;
    
    @JsonProperty("absent_days")
    private double absentDays;
    
    @JsonProperty("leave_without_pay")
    private double leaveWithoutPay;
    
    @JsonProperty("gross_pay")
    private double grossPay;
    
    @JsonProperty("total_deduction")
    private double totalDeduction;
    
    @JsonProperty("net_pay")
    private double netPay;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("salary_structure")
    private String salaryStructure;
    
    @JsonProperty("company")
    private String company;

    
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
    
    public String getDesignation() { 
        return designation; 
    }
    
    public void setDesignation(String designation) { 
        this.designation = designation; 
    }
    
    public Date getStartDate() { 
        return startDate; 
    }
    
    public void setStartDate(Date startDate) { 
        this.startDate = startDate; 
    }
    
    public Date getEndDate() { 
        return endDate; 
    }
    
    public void setEndDate(Date endDate) { 
        this.endDate = endDate; 
    }
    
    public Date getPostingDate() { 
        return postingDate; 
    }
    
    public void setPostingDate(Date postingDate) { 
        this.postingDate = postingDate; 
    }
    
    public double getTotalWorkingDays() { 
        return totalWorkingDays; 
    }
    
    public void setTotalWorkingDays(double totalWorkingDays) { 
        this.totalWorkingDays = totalWorkingDays; 
    }
    
    public double getPaymentDays() { 
        return paymentDays; 
    }
    
    public void setPaymentDays(double paymentDays) { 
        this.paymentDays = paymentDays; 
    }
    
    public double getAbsentDays() { 
        return absentDays; 
    }
    
    public void setAbsentDays(double absentDays) { 
        this.absentDays = absentDays; 
    }
    
    public double getLeaveWithoutPay() { 
        return leaveWithoutPay; 
    }
    
    public void setLeaveWithoutPay(double leaveWithoutPay) { 
        this.leaveWithoutPay = leaveWithoutPay; 
    }
    
    public double getGrossPay() { 
        return grossPay; 
    }
    
    public void setGrossPay(double grossPay) { 
        this.grossPay = grossPay; 
    }
    
    public double getTotalDeduction() { 
        return totalDeduction; 
    }
    
    public void setTotalDeduction(double totalDeduction) { 
        this.totalDeduction = totalDeduction; 
    }
    
    public double getNetPay() { 
        return netPay; 
    }
    
    public void setNetPay(double netPay) { 
        this.netPay = netPay; 
    }
    
    public String getCurrency() { 
        return currency; 
    }
    
    public void setCurrency(String currency) { 
        this.currency = currency; 
    }
    
    public String getStatus() { 
        return status; 
    }
    
    public void setStatus(String status) { 
        this.status = status; 
    }
    
    public String getSalaryStructure() { 
        return salaryStructure; 
    }
    
    public void setSalaryStructure(String salaryStructure) { 
        this.salaryStructure = salaryStructure; 
    }
    
    public String getCompany() { 
        return company; 
    }
    
    public void setCompany(String company) { 
        this.company = company; 
    }
}