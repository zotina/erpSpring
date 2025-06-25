package mg.itu.model;

import jakarta.persistence.*;

@Entity
@Table(name = "tabEmployee")
public class Employee {
    @Id
    @Column(name = "name", length = 140)
    private String name;
    
    @Column(name = "creation")
    private String creation;
    
    @Column(name = "modified")
    private String modified;
    
    @Column(name = "modified_by", length = 140)
    private String modifiedBy;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCreation() {
        return creation;
    }

    public void setCreation(String creation) {
        this.creation = creation;
    }

    public String getModified() {
        return modified;
    }

    public void setModified(String modified) {
        this.modified = modified;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }
    
    
}