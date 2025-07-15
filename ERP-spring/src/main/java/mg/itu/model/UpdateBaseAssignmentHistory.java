package mg.itu.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "update_base_assignment_history")
public class UpdateBaseAssignmentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "update_date", nullable = false)
    private LocalDateTime updateDate;

    @Column(name = "salary_component", nullable = false)
    private String salaryComponent;

    @Column(name = "old_base", nullable = false)
    private Double oldBase;

    @Column(name = "new_base", nullable = false)
    private Double newBase;

    @Column(name = "employee_name", nullable = false)
    private String employeeName;

    @Column(name = "period", nullable = false)
    private String period;

    @Column(name = "structure_name", nullable = false)
    private String structureName;

    @Column(name = "adjustment_type", nullable = false)
    private String adjustmentType;

    @Column(name = "adjustment_percentage", nullable = false)
    private Double adjustmentPercentage;

    
    public UpdateBaseAssignmentHistory() {
        this.updateDate = LocalDateTime.now();
    }

    public UpdateBaseAssignmentHistory(String salaryComponent, Double oldBase, Double newBase, 
                                     String employeeName, String period, String structureName,
                                     String adjustmentType, Double adjustmentPercentage) {
        this.updateDate = LocalDateTime.now();
        this.salaryComponent = salaryComponent;
        this.oldBase = oldBase;
        this.newBase = newBase;
        this.employeeName = employeeName;
        this.period = period;
        this.structureName = structureName;
        this.adjustmentType = adjustmentType;
        this.adjustmentPercentage = adjustmentPercentage;
    }

    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(LocalDateTime updateDate) {
        this.updateDate = updateDate;
    }

    public String getSalaryComponent() {
        return salaryComponent;
    }

    public void setSalaryComponent(String salaryComponent) {
        this.salaryComponent = salaryComponent;
    }

    public Double getOldBase() {
        return oldBase;
    }

    public void setOldBase(Double oldBase) {
        this.oldBase = oldBase;
    }

    public Double getNewBase() {
        return newBase;
    }

    public void setNewBase(Double newBase) {
        this.newBase = newBase;
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

    public String getStructureName() {
        return structureName;
    }

    public void setStructureName(String structureName) {
        this.structureName = structureName;
    }

    public String getAdjustmentType() {
        return adjustmentType;
    }

    public void setAdjustmentType(String adjustmentType) {
        this.adjustmentType = adjustmentType;
    }

    public Double getAdjustmentPercentage() {
        return adjustmentPercentage;
    }

    public void setAdjustmentPercentage(Double adjustmentPercentage) {
        this.adjustmentPercentage = adjustmentPercentage;
    }
}