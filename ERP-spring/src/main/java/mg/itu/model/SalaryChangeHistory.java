package mg.itu.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "salary_change_history")
public class SalaryChangeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private String employeeId;

    @Column(name = "employee_name")
    private String employeeName;
    
    @Column(name = "period")
    private String period;

    @Column(name = "old_base_salary", nullable = false)
    private BigDecimal oldBaseSalary;

    @Column(name = "new_base_salary", nullable = false)
    private BigDecimal newBaseSalary;
    
    @Column(name = "adjustment_percentage")
    private Double adjustmentPercentage;

    @Column(name = "adjustment_type")
    private String adjustmentType;

    @Column(name = "change_timestamp")
    private LocalDateTime changeTimestamp;
    
    @Column(name = "old_slip_id")
    private String oldSlipId;

    @Column(name = "new_slip_id")
    private String newSlipId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public BigDecimal getOldBaseSalary() {
        return oldBaseSalary;
    }

    public void setOldBaseSalary(BigDecimal oldBaseSalary) {
        this.oldBaseSalary = oldBaseSalary;
    }

    public BigDecimal getNewBaseSalary() {
        return newBaseSalary;
    }

    public void setNewBaseSalary(BigDecimal newBaseSalary) {
        this.newBaseSalary = newBaseSalary;
    }

    public Double getAdjustmentPercentage() {
        return adjustmentPercentage;
    }

    public void setAdjustmentPercentage(Double adjustmentPercentage) {
        this.adjustmentPercentage = adjustmentPercentage;
    }

    public String getAdjustmentType() {
        return adjustmentType;
    }

    public void setAdjustmentType(String adjustmentType) {
        this.adjustmentType = adjustmentType;
    }

    public LocalDateTime getChangeTimestamp() {
        return changeTimestamp;
    }

    public void setChangeTimestamp(LocalDateTime changeTimestamp) {
        this.changeTimestamp = changeTimestamp;
    }

    public String getOldSlipId() {
        return oldSlipId;
    }

    public void setOldSlipId(String oldSlipId) {
        this.oldSlipId = oldSlipId;
    }

    public String getNewSlipId() {
        return newSlipId;
    }

    public void setNewSlipId(String newSlipId) {
        this.newSlipId = newSlipId;
    }

    
}