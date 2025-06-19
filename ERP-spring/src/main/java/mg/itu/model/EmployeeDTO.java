package mg.itu.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EmployeeDTO {
    @JsonProperty("name") 
    private String employeeId;
    @JsonProperty("employee_name")
    private String fullName;
    @JsonProperty("company_email")
    private String email;
    @JsonProperty("cell_number")
    private String phone;
    @JsonProperty("current_address")
    private String address;
    @JsonProperty("date_of_birth")
    private Date dateOfBirth;
    private String department;
    private String designation;
    @JsonProperty("date_of_joining")
    private Date dateOfJoining;
    @JsonProperty("employment_type")
    private String contractType;
    @JsonProperty("reports_to") 
    private String managerId; 
    private String status;

    
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; } 
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Date getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(Date dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public Date getDateOfJoining() { return dateOfJoining; }
    public void setDateOfJoining(Date dateOfJoining) { this.dateOfJoining = dateOfJoining; }
    public String getContractType() { return contractType; }
    public void setContractType(String contractType) { this.contractType = contractType; }
    public String getManagerId() { return managerId; }
    public void setManagerId(String managerId) { this.managerId = managerId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}