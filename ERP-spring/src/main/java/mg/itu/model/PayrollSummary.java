package mg.itu.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PayrollSummary {
    private String employeeId;
    private String employeeName;
    private String department;
    private double baseSalary;
    private double primes;
    private double overtime;
    private double grossPay;
    private double netPay;

    
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public double getBaseSalary() { return baseSalary; }
    public void setBaseSalary(double baseSalary) { this.baseSalary = baseSalary; }
    public double getPrimes() { return primes; }
    public void setPrimes(double primes) { this.primes = primes; }
    public double getOvertime() { return overtime; }
    public void setOvertime(double overtime) { this.overtime = overtime; }
    public double getGrossPay() { return grossPay; }
    public void setGrossPay(double grossPay) { this.grossPay = grossPay; }
    public double getNetPay() { return netPay; }
    public void setNetPay(double netPay) { this.netPay = netPay; }
}