package mg.itu.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PayrollDTO {
    @JsonProperty("mois")
    private String mois;
    @JsonProperty("ref_employe")
    private String refEmploye;
    @JsonProperty("salaire_base")
    private Double salaireBase;
    @JsonProperty("salaire")
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