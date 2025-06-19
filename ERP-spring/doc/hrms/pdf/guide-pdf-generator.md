# Guide du Générateur PDF de Bulletins de Paie

## 📋 Vue d'ensemble

Ce code Java utilise la bibliothèque **iText** pour générer automatiquement des bulletins de paie au format PDF. Il s'agit d'un service Spring Boot qui transforme les données d'employés et de paie en documents PDF professionnels.

## 🏗️ Structure du Code

### Classe Principale
```java
@Service
public class PdfExportService
```
- **@Service**: Annotation Spring qui indique que cette classe est un service
- **PdfExportService**: La classe principale qui contient toute la logique de génération PDF

### Méthode Principale
```java
public byte[] generatePayrollSlipPdf(EmployeeDTO employee, PayrollSlipDTO payrollSlip, String monthYear)
```
- Génère le PDF complet et retourne un tableau d'octets
- Prend en paramètres les données de l'employé et du bulletin de paie

## 🎨 Sections du Bulletin de Paie

Le bulletin est divisé en plusieurs sections, chacune gérée par une méthode spécifique :

### 1. **Informations de l'Entreprise** (`addCompanyInfo`)
- Nom de l'entreprise
- Adresse
- SIRET et Code APE
- Téléphone

### 2. **Informations de l'Employé** (`addEmployeeInfo`)
- Nom complet
- Matricule
- Poste
- Département
- Fréquence de paie
- Devise

### 3. **Période de Paie** (`addPeriodInfo`)
- Dates de début et fin
- Jours travaillés
- Heures travaillées

### 4. **Éléments de Rémunération** (`addEarningsSection`)
- Salaire de base
- Primes
- Autres revenus

### 5. **Déductions** (`addDeductionsSection`)
- Cotisations sociales
- Impôts
- Autres retenues

### 6. **Résumé** (`addSummarySection`)
- Total brut
- Total déductions
- **Net à payer** (en surbrillance)

### 7. **Informations Légales** (`addLegalInfo`)
- Date d'édition
- Signatures
- Notes légales

## 🔧 Comment Modifier le PDF

### ✅ Ajouter un Nouveau Paragraphe

**Exemple 1: Ajouter une note après le titre**
```java
// Dans generatePayrollSlipPdf(), après le titre
Paragraph note = new Paragraph("⚠️ DOCUMENT CONFIDENTIEL")
    .setTextAlignment(TextAlignment.CENTER)
    .setFontSize(12)
    .setFontColor(ColorConstants.RED)
    .setBold()
    .setMarginBottom(10);
document.add(note);
```

**Exemple 2: Ajouter des informations bancaires**
```java
// Créer une nouvelle méthode
private void addBankInfo(Document document, PayrollSlipDTO payrollSlip) {
    Paragraph bankTitle = new Paragraph("🏦 INFORMATIONS BANCAIRES")
        .setBold()
        .setFontSize(14)
        .setMarginBottom(10);
    document.add(bankTitle);
    
    Paragraph bankDetails = new Paragraph()
        .add("Banque: BNI Madagascar\n")
        .add("IBAN: MG12 3456 7890 1234 5678 90\n")
        .add("Mode de paiement: Virement bancaire");
    document.add(bankDetails);
    document.add(new Paragraph("\n")); // Espacement
}

// Puis l'appeler dans generatePayrollSlipPdf()
addBankInfo(document, payrollSlip);
```

### ✅ Modifier les Couleurs

```java
// Changer les couleurs existantes
Color primaryColor = new DeviceRgb(220, 20, 60);    // Rouge
Color lightGray = new DeviceRgb(240, 248, 255);     // Bleu très clair
Color borderColor = new DeviceRgb(25, 25, 112);     // Bleu foncé
```

### ✅ Ajouter un Tableau Personnalisé

```java
private void addCustomTable(Document document) {
    // Créer un tableau avec 3 colonnes
    Table customTable = new Table(new float[]{2, 2, 2});
    customTable.setWidth(UnitValue.createPercentValue(100));
    
    // Ajouter les en-têtes
    addTableHeader(customTable, "COLONNE 1", ColorConstants.BLUE);
    addTableHeader(customTable, "COLONNE 2", ColorConstants.BLUE);
    addTableHeader(customTable, "COLONNE 3", ColorConstants.BLUE);
    
    // Ajouter les données
    addTableCell(customTable, "Données 1");
    addTableCell(customTable, "Données 2");
    addTableCell(customTable, "Données 3");
    
    document.add(customTable);
    document.add(new Paragraph("\n"));
}
```

### ✅ Modifier le Formatage des Montants

```java
// Pour changer le format des devises (actuellement "Ar 1,000")
private String formatCurrency(double amount, String currency) {
    String formattedAmount = currencyFormatter.format(amount);
    return formattedAmount + " " + (currency != null ? currency : "Ar"); // "1,000 Ar"
}
```

### ✅ Ajouter des Images/Logos

```java
// Ajouter cette méthode pour inclure un logo
private void addCompanyLogo(Document document) {
    try {
        // Remplacer "path/to/logo.png" par le chemin réel
        ImageData imageData = ImageDataFactory.create("path/to/logo.png");
        Image logo = new Image(imageData);
        logo.setWidth(100);
        logo.setTextAlignment(TextAlignment.CENTER);
        document.add(logo);
    } catch (Exception e) {
        // Si le logo n'est pas trouvé, ignorer
        logger.warn("Logo non trouvé");
    }
}
```

## 🛠️ Exemples de Modifications Courantes

### 1. **Changer la Taille de Police du Titre**
```java
// Dans generatePayrollSlipPdf()
Paragraph title = new Paragraph("📄 FICHE DE PAIE")
    .setTextAlignment(TextAlignment.CENTER)
    .setFontSize(24) // Changé de 20 à 24
    .setBold()
    .setFontColor(primaryColor)
    .setMarginBottom(20);
```

### 2. **Ajouter une Section de Congés**
```java
private void addLeaveSection(Document document, PayrollSlipDTO payrollSlip) {
    Paragraph leaveTitle = new Paragraph("🏖️ CONGÉS")
        .setBold()
        .setFontSize(14)
        .setMarginBottom(10);
    document.add(leaveTitle);
    
    Table leaveTable = new Table(2).setWidth(UnitValue.createPercentValue(100));
    
    addSummaryRow(leaveTable, "Congés acquis:", "25 jours");
    addSummaryRow(leaveTable, "Congés pris:", "5 jours");
    addSummaryRow(leaveTable, "Solde restant:", "20 jours");
    
    document.add(leaveTable);
    document.add(new Paragraph("\n"));
}
```

### 3. **Personnaliser l'En-tête d'Entreprise**
```java
// Modifier addCompanyInfo() pour ajouter plus d'informations
Cell companyCell = new Cell()
    .add(new Paragraph(payrollSlip.getCompany() != null ? payrollSlip.getCompany() : "ENTREPRISE").setBold().setFontSize(16))
    .add(new Paragraph("Adresse: Rue de l'Indépendance, Antananarivo"))
    .add(new Paragraph("SIRET: 12345678901234 | Code APE: 6201Z"))
    .add(new Paragraph("Tél: +261 20 22 123 45"))
    .add(new Paragraph("Email: contact@entreprise.mg")) // Nouveau
    .add(new Paragraph("Site web: www.entreprise.mg"))  // Nouveau
    .setBorder(Border.NO_BORDER)
    .setPadding(10);
```

## 📋 Points à Retenir

### ✅ **Bonnes Pratiques**
- Toujours ajouter `document.add(new Paragraph("\n"));` après chaque section pour l'espacement
- Utiliser les méthodes helper existantes (`addTableHeader`, `addTableCell`, etc.)
- Respecter les couleurs définies pour la cohérence visuelle
- Tester avec différentes données pour s'assurer que le format reste correct

### ⚠️ **Points d'Attention**
- Les modifications des couleurs affectent tout le document
- Vérifier que les nouvelles sections ne cassent pas la mise en page
- S'assurer que les données nécessaires sont disponibles dans les DTOs
- Tester avec des noms longs pour éviter les débordements

### 🔧 **Pour Déboguer**
- Utiliser `logger.info()` pour tracer l'exécution
- Tester la génération avec des données minimales d'abord
- Vérifier les exceptions dans les logs

## 🚀 Exemple Complet d'Ajout

Voici comment ajouter une section complète "Informations Bancaires" :

```java
// 1. Ajouter la méthode
private void addBankingSection(Document document, PayrollSlipDTO payrollSlip, Color lightGray, Color borderColor) {
    Paragraph bankTitle = new Paragraph("🏦 INFORMATIONS BANCAIRES")
        .setBold()
        .setFontSize(14)
        .setMarginBottom(10);
    document.add(bankTitle);
    
    Table bankTable = new Table(1).setWidth(UnitValue.createPercentValue(100));
    bankTable.setBorder(new SolidBorder(borderColor, 1));
    bankTable.setBackgroundColor(lightGray);
    
    Cell bankCell = new Cell()
        .add(new Paragraph("Établissement: BNI Madagascar"))
        .add(new Paragraph("IBAN: MG12 3456 7890 1234 5678 90"))
        .add(new Paragraph("Code BIC: BMOIMGMG"))
        .add(new Paragraph("Mode de paiement: Virement automatique"))
        .setBorder(Border.NO_BORDER)
        .setPadding(10);
    
    bankTable.addCell(bankCell);
    document.add(bankTable);
    document.add(new Paragraph("\n"));
}

// 2. L'appeler dans generatePayrollSlipPdf()
addBankingSection(document, payrollSlip, lightGray, borderColor);
```

Ce guide vous donne toutes les bases pour personnaliser votre générateur de bulletins de paie PDF ! 🎯