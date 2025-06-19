package mg.itu.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Date;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PayrollSlipDTO {
    private String name;
    private String employee;
    private String employeeName;
    private String department;
    private String designation;
    private Date startDate;
    private Date endDate;
    private Date postingDate;
    private String payrollFrequency;
    private String company;
    private String currency;
    private double totalWorkingDays;
    private double paymentDays;
    private double hourRate;
    private double totalWorkingHours;
    
    
    private List<SalaryDetail> earnings;
    private List<SalaryDetail> deductions;
    
    
    private double grossPay;
    private double totalDeduction;
    private double netPay;
    private double roundedTotal;
    
    
    private List<LoanRepaymentDetail> loanRepayment;
    
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SalaryDetail {
        private String salaryComponent;
        private String abbr;
        private double amount;
        private String formula;
        private boolean dependsOnPaymentDays;
        private boolean exemptFromPayrollTax;
        private boolean doNotIncludeInTotal;
        private boolean statisticalComponent;
        private String condition;
        
        
        public String getSalaryComponent() { return salaryComponent; }
        public void setSalaryComponent(String salaryComponent) { this.salaryComponent = salaryComponent; }
        
        public String getAbbr() { return abbr; }
        public void setAbbr(String abbr) { this.abbr = abbr; }
        
        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }
        
        public String getFormula() { return formula; }
        public void setFormula(String formula) { this.formula = formula; }
        
        public boolean isDependsOnPaymentDays() { return dependsOnPaymentDays; }
        public void setDependsOnPaymentDays(boolean dependsOnPaymentDays) { this.dependsOnPaymentDays = dependsOnPaymentDays; }
        
        public boolean isExemptFromPayrollTax() { return exemptFromPayrollTax; }
        public void setExemptFromPayrollTax(boolean exemptFromPayrollTax) { this.exemptFromPayrollTax = exemptFromPayrollTax; }
        
        public boolean isDoNotIncludeInTotal() { return doNotIncludeInTotal; }
        public void setDoNotIncludeInTotal(boolean doNotIncludeInTotal) { this.doNotIncludeInTotal = doNotIncludeInTotal; }
        
        public boolean isStatisticalComponent() { return statisticalComponent; }
        public void setStatisticalComponent(boolean statisticalComponent) { this.statisticalComponent = statisticalComponent; }
        
        public String getCondition() { return condition; }
        public void setCondition(String condition) { this.condition = condition; }
    }
    
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LoanRepaymentDetail {
        private String loan;
        private String loanType;
        private double principalAmount;
        private double interestAmount;
        private double totalPayment;
        
        
        public String getLoan() { return loan; }
        public void setLoan(String loan) { this.loan = loan; }
        
        public String getLoanType() { return loanType; }
        public void setLoanType(String loanType) { this.loanType = loanType; }
        
        public double getPrincipalAmount() { return principalAmount; }
        public void setPrincipalAmount(double principalAmount) { this.principalAmount = principalAmount; }
        
        public double getInterestAmount() { return interestAmount; }
        public void setInterestAmount(double interestAmount) { this.interestAmount = interestAmount; }
        
        public double getTotalPayment() { return totalPayment; }
        public void setTotalPayment(double totalPayment) { this.totalPayment = totalPayment; }
    }
    
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getEmployee() { return employee; }
    public void setEmployee(String employee) { this.employee = employee; }
    
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    
    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }
    
    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }
    
    public Date getPostingDate() { return postingDate; }
    public void setPostingDate(Date postingDate) { this.postingDate = postingDate; }
    
    public String getPayrollFrequency() { return payrollFrequency; }
    public void setPayrollFrequency(String payrollFrequency) { this.payrollFrequency = payrollFrequency; }
    
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public double getTotalWorkingDays() { return totalWorkingDays; }
    public void setTotalWorkingDays(double totalWorkingDays) { this.totalWorkingDays = totalWorkingDays; }
    
    public double getPaymentDays() { return paymentDays; }
    public void setPaymentDays(double paymentDays) { this.paymentDays = paymentDays; }
    
    public double getHourRate() { return hourRate; }
    public void setHourRate(double hourRate) { this.hourRate = hourRate; }
    
    public double getTotalWorkingHours() { return totalWorkingHours; }
    public void setTotalWorkingHours(double totalWorkingHours) { this.totalWorkingHours = totalWorkingHours; }
    
    public List<SalaryDetail> getEarnings() { return earnings; }
    public void setEarnings(List<SalaryDetail> earnings) { this.earnings = earnings; }
    
    public List<SalaryDetail> getDeductions() { return deductions; }
    public void setDeductions(List<SalaryDetail> deductions) { this.deductions = deductions; }
    
    public double getGrossPay() { return grossPay; }
    public void setGrossPay(double grossPay) { this.grossPay = grossPay; }
    
    public double getTotalDeduction() { return totalDeduction; }
    public void setTotalDeduction(double totalDeduction) { this.totalDeduction = totalDeduction; }
    
    public double getNetPay() { return netPay; }
    public void setNetPay(double netPay) { this.netPay = netPay; }
    
    public double getRoundedTotal() { return roundedTotal; }
    public void setRoundedTotal(double roundedTotal) { this.roundedTotal = roundedTotal; }
    
    public List<LoanRepaymentDetail> getLoanRepayment() { return loanRepayment; }
    public void setLoanRepayment(List<LoanRepaymentDetail> loanRepayment) { this.loanRepayment = loanRepayment; }
}