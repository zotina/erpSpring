package mg.itu.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PayrollDTO {
    private String mois;
    private String refEmploye;
    private Double salaireBase;
    private String salaire;

    
    public String getMois() {
        return mois;
    }

    public void setMois(String mois) {
        this.mois = mois;
    }

    public String getRefEmploye() {
        return refEmploye;
    }

    public void setRefEmploye(String refEmploye) {
        this.refEmploye = refEmploye;
    }

    public Double getSalaireBase() {
        return salaireBase;
    }

    public void setSalaireBase(Double salaireBase) {
        this.salaireBase = salaireBase;
    }

    public String getSalaire() {
        return salaire;
    }

    public void setSalaire(String salaire) {
        this.salaire = salaire;
    }
}