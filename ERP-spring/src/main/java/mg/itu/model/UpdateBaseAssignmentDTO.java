package mg.itu.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateBaseAssignmentDTO {
    @JsonProperty("old_slip")
    private String oldSlip;
    @JsonProperty("new_slip")
    private String newSlip;
    @JsonProperty("employee")
    private String employee;
    @JsonProperty("period")
    private String period;
    @JsonProperty("structure_name")
    private String structureName;
    @JsonProperty("old_base")
    private Double oldBase;
    @JsonProperty("new_base")
    private Double newBase;
    @JsonProperty("adjustment_percentage")
    private Double adjustmentPercentage;
    @JsonProperty("adjustment_type")
    private String adjustmentType;

    public String getOldSlip() {
        return oldSlip;
    }

    public void setOldSlip(String oldSlip) {
        this.oldSlip = oldSlip;
    }

    public String getNewSlip() {
        return newSlip;
    }

    public void setNewSlip(String newSlip) {
        this.newSlip = newSlip;
    }

    public String getEmployee() {
        return employee;
    }

    public void setEmployee(String employee) {
        this.employee = employee;
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
}