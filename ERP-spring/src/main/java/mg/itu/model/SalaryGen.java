package mg.itu.model;

import jakarta.persistence.*;

@Entity
@Table(name = "salary_gen")
public class SalaryGen {
    @Id
    @Column(name = "name", length = 140)
    private String name;
    
    @Column(name = "month")
    private String month;
    
    @Column(name = "valeur")
    private double valeur;
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public double getValeur() {
        return valeur;
    }

    public void setValeur(double valeur) {
        this.valeur = valeur;
    }

    public SalaryGen(String month, double valeur) {
        this.month = month;
        this.valeur = valeur;
    }
    
    
}