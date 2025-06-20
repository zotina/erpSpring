package mg.itu.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;


@JsonIgnoreProperties(ignoreUnknown = true)
public class SalaryComponentDTO {
    private String name;
    @JsonProperty("salary_component")
    private String salaryComponent;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSalaryComponent() {
        return salaryComponent;
    }

    public void setSalaryComponent(String salaryComponent) {
        this.salaryComponent = salaryComponent;
    }
} 

