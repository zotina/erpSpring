package mg.itu.data;

import java.util.Arrays;
import java.util.List;

public class Init {
    
    /**
     * Liste des désignations disponibles dans le système
     */
    public static final List<String> DESIGNATIONS = Arrays.asList(
        "Accountant",
        "Administrative Assistant",
        "Administrative Officer",
        "Analyst",
        "Associate",
        "Business Analyst",
        "Business Development Manager",
        "Consultant",
        "Chief Executive Officer",
        "Chief Financial Officer",
        "Chief Operating Officer",
        "Chief Technology Officer",
        "Customer Service Representative",
        "Designer",
        "Engineer",
        "Executive Assistant",
        "Finance Manager",
        "HR Manager",
        "Head of Marketing and Sales",
        "Manager",
        "Managing Director",
        "Marketing Manager",
        "Marketing Specialist",
        "President",
        "Product Manager",
        "Project Manager",
        "Researcher",
        "Sales Representative",
        "Secretary",
        "Software Developer",
        "Vice President",
        "Employee"
    );
    
    /**
     * Liste des départements disponibles dans le système
     */
    public static final List<String> DEPARTMENTS = Arrays.asList(
        "Tous les départements",
        "achat",
        "All Departments - NS",
        "Comptes",
        "Envoi",
        "Gestion",
        "Gestion de la qualité",
        "Legal",
        "Marketing",
        "Opérations",
        "Production",
        "Recherche & Développement",
        "Ressources humaines",
        "Service Client",
        "Ventes"
    );
    
    /**
     * Méthode utilitaire pour obtenir la liste des désignations
     * @return Liste des désignations
     */
    public static List<String> getDesignations() {
        return DESIGNATIONS;
    }
    
    /**
     * Méthode utilitaire pour obtenir la liste des départements
     * @return Liste des départements
     */
    public static List<String> getDepartments() {
        return DEPARTMENTS;
    }
}