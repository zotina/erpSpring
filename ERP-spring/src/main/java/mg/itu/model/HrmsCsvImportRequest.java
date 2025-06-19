package mg.itu.model;

public class HrmsCsvImportRequest {
    private String employeesCsv;
    private String salaryStructureCsv;
    private String payrollCsv;

    
    public String getEmployeesCsv() {
        return employeesCsv;
    }

    public void setEmployeesCsv(String employeesCsv) {
        this.employeesCsv = employeesCsv;
    }

    public String getSalaryStructureCsv() {
        return salaryStructureCsv;
    }

    public void setSalaryStructureCsv(String salaryStructureCsv) {
        this.salaryStructureCsv = salaryStructureCsv;
    }

    public String getPayrollCsv() {
        return payrollCsv;
    }

    public void setPayrollCsv(String payrollCsv) {
        this.payrollCsv = payrollCsv;
    }
}