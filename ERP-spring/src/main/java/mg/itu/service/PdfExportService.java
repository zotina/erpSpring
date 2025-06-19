package mg.itu.service;

import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import mg.itu.model.EmployeeDTO;
import mg.itu.model.PayrollSlipDTO;
import mg.itu.util.DateUtil;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Service
public class PdfExportService {
        
    private static final Logger logger = LoggerFactory.getLogger(PdfExportService.class);
    
    
    private final DecimalFormat currencyFormatter;
    
    public PdfExportService() {
        
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRANCE);
        symbols.setGroupingSeparator(',');  
        symbols.setDecimalSeparator('.');   
        
        currencyFormatter = new DecimalFormat("#,##0", symbols);
    }
    
    public byte[] generatePayrollSlipPdf(EmployeeDTO employee, PayrollSlipDTO payrollSlip, String monthYear) throws Exception {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDocument = new PdfDocument(writer);
            Document document = new Document(pdfDocument, PageSize.A4);
            
            document.setMargins(30, 30, 30, 30);
            
            Color primaryColor = new DeviceRgb(46, 134, 171); 
            Color lightGray = new DeviceRgb(248, 249, 250);
            Color borderColor = new DeviceRgb(68, 68, 68);
            
            Paragraph title = new Paragraph("📄 FICHE DE PAIE")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(20)
                    .setBold()
                    .setFontColor(primaryColor)
                    .setMarginBottom(20);
            document.add(title);
            
            addCompanyInfo(document, payrollSlip, lightGray, borderColor);
            addEmployeeInfo(document, employee, payrollSlip, lightGray, borderColor);
            addPeriodInfo(document, payrollSlip, lightGray, borderColor);
            addEarningsSection(document, payrollSlip, borderColor);
            addDeductionsSection(document, payrollSlip, borderColor);
            addSummarySection(document, payrollSlip, primaryColor, lightGray);
            addLegalInfo(document, payrollSlip);
            
            document.close();
            return baos.toByteArray();
            
        } catch (Exception e) {
            logger.error("Error generating PDF", e);
            throw new Exception("Erreur lors de la génération du PDF: " + e.getMessage());
        }
    }
    
    private void addCompanyInfo(Document document, PayrollSlipDTO payrollSlip, Color lightGray, Color borderColor) {
        Table companyTable = new Table(1).setWidth(UnitValue.createPercentValue(100));
        companyTable.setBorder(new SolidBorder(borderColor, 1));
        companyTable.setBackgroundColor(lightGray);
        
        Cell companyCell = new Cell()
                .add(new Paragraph(payrollSlip.getCompany() != null ? payrollSlip.getCompany() : "ENTREPRISE").setBold())
                .add(new Paragraph("Adresse: Rue de l'Indépendance, Antananarivo"))
                .add(new Paragraph("SIRET: 12345678901234 | Code APE: 6201Z"))
                .add(new Paragraph("Tél: +261 20 22 123 45"))
                .setBorder(Border.NO_BORDER)
                .setPadding(10);
        
        companyTable.addCell(companyCell);
        document.add(companyTable);
        document.add(new Paragraph("\n"));
    }
    
    private void addEmployeeInfo(Document document, EmployeeDTO employee, PayrollSlipDTO payrollSlip, Color lightGray, Color borderColor) {
        Table empTable = new Table(1).setWidth(UnitValue.createPercentValue(100));
        empTable.setBorder(new SolidBorder(borderColor, 1));
        empTable.setBackgroundColor(lightGray);
        
        Cell empCell = new Cell()
                .add(new Paragraph("👤 SALARIÉ").setBold())
                .add(new Paragraph("Nom: " + (payrollSlip.getEmployeeName() != null ? payrollSlip.getEmployeeName() : employee.getFullName())))
                .add(new Paragraph("Matricule: " + (payrollSlip.getEmployee() != null ? payrollSlip.getEmployee() : employee.getEmployeeId())))
                .add(new Paragraph("Poste: " + (payrollSlip.getDesignation() != null ? payrollSlip.getDesignation() : employee.getDesignation())))
                .add(new Paragraph("Département: " + (payrollSlip.getDepartment() != null ? payrollSlip.getDepartment() : employee.getDepartment())))
                .add(new Paragraph("Fréquence de Paie: " + (payrollSlip.getPayrollFrequency() != null ? payrollSlip.getPayrollFrequency() : "Mensuelle")))
                .add(new Paragraph("Devise: " + (payrollSlip.getCurrency() != null ? payrollSlip.getCurrency() : "Ar")))
                .setBorder(Border.NO_BORDER)
                .setPadding(10);
        
        empTable.addCell(empCell);
        document.add(empTable);
        document.add(new Paragraph("\n"));
    }
    
    private void addPeriodInfo(Document document, PayrollSlipDTO payrollSlip, Color lightGray, Color borderColor) {
        String startFormatted = payrollSlip.getStartDate() != null ? 
                DateUtil.parseToddMMyyyy(payrollSlip.getStartDate().toString()) : "N/A";
        String endFormatted = payrollSlip.getEndDate() != null ? 
                DateUtil.parseToddMMyyyy(payrollSlip.getEndDate().toString()) : "N/A";
        
        Paragraph periodInfo = new Paragraph()
                .add(new Text("PÉRIODE: ").setBold())
                .add("Du " + startFormatted + " au " + endFormatted + "\n")
                .add(new Text("TEMPS DE TRAVAIL: ").setBold())
                .add(payrollSlip.getPaymentDays() + " jours payés sur " + payrollSlip.getTotalWorkingDays() + " jours ouvrés");
        
        if (payrollSlip.getTotalWorkingHours() > 0) {
            periodInfo.add(" - " + payrollSlip.getTotalWorkingHours() + " heures");
        }
        
        document.add(periodInfo);
        document.add(new Paragraph("\n"));
    }
    
    private void addEarningsSection(Document document, PayrollSlipDTO payrollSlip, Color borderColor) {
        if (payrollSlip.getEarnings() != null && !payrollSlip.getEarnings().isEmpty()) {
            Paragraph earningsTitle = new Paragraph("💰 Éléments de Rémunération")
                    .setBold()
                    .setFontSize(14)
                    .setMarginBottom(10);
            document.add(earningsTitle);
            
            Table earningsTable = new Table(new float[]{3, 1, 1.5f, 2});
            earningsTable.setWidth(UnitValue.createPercentValue(100));
            
            addTableHeader(earningsTable, "COMPOSANT", borderColor);
            addTableHeader(earningsTable, "ABRÉV.", borderColor);
            addTableHeader(earningsTable, "MONTANT", borderColor);
            addTableHeader(earningsTable, "STATUT", borderColor);
            
            for (PayrollSlipDTO.SalaryDetail earning : payrollSlip.getEarnings()) {
                addTableCell(earningsTable, earning.getSalaryComponent());
                addTableCell(earningsTable, earning.getAbbr() != null ? earning.getAbbr() : "");
                addTableCell(earningsTable, formatCurrency(earning.getAmount(), payrollSlip.getCurrency()));
                addTableCell(earningsTable, getStatusText(earning));
            }
            
            addTotalRow(earningsTable, "TOTAL BRUT", formatCurrency(payrollSlip.getGrossPay(), payrollSlip.getCurrency()), borderColor);
            
            document.add(earningsTable);
            document.add(new Paragraph("\n"));
        }
    }
    
    private void addDeductionsSection(Document document, PayrollSlipDTO payrollSlip, Color borderColor) {
        if (payrollSlip.getDeductions() != null && !payrollSlip.getDeductions().isEmpty()) {
            Paragraph deductionsTitle = new Paragraph("➖ Déductions")
                    .setBold()
                    .setFontSize(14)
                    .setMarginBottom(10);
            document.add(deductionsTitle);
            
            Table deductionsTable = new Table(new float[]{3, 1, 1.5f, 2});
            deductionsTable.setWidth(UnitValue.createPercentValue(100));
            
            addTableHeader(deductionsTable, "COMPOSANT", borderColor);
            addTableHeader(deductionsTable, "ABRÉV.", borderColor);
            addTableHeader(deductionsTable, "MONTANT", borderColor);
            addTableHeader(deductionsTable, "STATUT", borderColor);
            
            for (PayrollSlipDTO.SalaryDetail deduction : payrollSlip.getDeductions()) {
                addTableCell(deductionsTable, deduction.getSalaryComponent());
                addTableCell(deductionsTable, deduction.getAbbr() != null ? deduction.getAbbr() : "");
                addTableCell(deductionsTable, formatCurrency(deduction.getAmount(), payrollSlip.getCurrency()));
                addTableCell(deductionsTable, getStatusText(deduction));
            }
            
            addTotalRow(deductionsTable, "TOTAL DÉDUCTIONS", formatCurrency(payrollSlip.getTotalDeduction(), payrollSlip.getCurrency()), borderColor);
            
            document.add(deductionsTable);
            document.add(new Paragraph("\n"));
        }
    }
    
    private void addSummarySection(Document document, PayrollSlipDTO payrollSlip, Color primaryColor, Color lightGray) {
        Paragraph summaryTitle = new Paragraph("📊 RÉSUMÉ")
                .setBold()
                .setFontSize(14)
                .setMarginBottom(10);
        document.add(summaryTitle);
        
        Table summaryTable = new Table(2).setWidth(UnitValue.createPercentValue(100));
        summaryTable.setBorder(new SolidBorder(primaryColor, 2));
        summaryTable.setBackgroundColor(lightGray);
        
        addSummaryRow(summaryTable, "💰 Total Brut:", formatCurrency(payrollSlip.getGrossPay(), payrollSlip.getCurrency()));
        addSummaryRow(summaryTable, "➖ Total Déductions:", formatCurrency(payrollSlip.getTotalDeduction(), payrollSlip.getCurrency()));
        
        
        if (payrollSlip.getRoundedTotal() != 0 && 
            payrollSlip.getRoundedTotal() != payrollSlip.getNetPay() && 
            payrollSlip.getRoundedTotal() > 0) {
            double rounding = payrollSlip.getRoundedTotal() - payrollSlip.getNetPay();
            addSummaryRow(summaryTable, "🔄 Arrondi:", formatCurrency(rounding, payrollSlip.getCurrency()));
        }
        
        
        Cell netPayLabelCell = new Cell().add(new Paragraph("💵 NET À PAYER:").setBold().setFontSize(12))
                .setBackgroundColor(primaryColor)
                .setFontColor(ColorConstants.WHITE)
                .setBorder(Border.NO_BORDER)
                .setPadding(8);
        
        Cell netPayValueCell = new Cell().add(new Paragraph(formatCurrency(payrollSlip.getNetPay(), payrollSlip.getCurrency())).setBold().setFontSize(12))
                .setBackgroundColor(primaryColor)
                .setFontColor(ColorConstants.WHITE)
                .setBorder(Border.NO_BORDER)
                .setPadding(8)
                .setTextAlignment(TextAlignment.RIGHT);
        
        summaryTable.addCell(netPayLabelCell);
        summaryTable.addCell(netPayValueCell);
        
        document.add(summaryTable);
        document.add(new Paragraph("\n"));
    }
    
    private void addLegalInfo(Document document, PayrollSlipDTO payrollSlip) {
        String postingDate = payrollSlip.getPostingDate() != null ? 
                new SimpleDateFormat("dd/MM/yyyy").format(payrollSlip.getPostingDate()) : 
                new SimpleDateFormat("dd/MM/yyyy").format(new Date());
        
        Paragraph dateInfo = new Paragraph("📅 Date d'édition: " + postingDate)
                .setFontSize(10)
                .setFontColor(ColorConstants.GRAY);
        document.add(dateInfo);
        
        Paragraph note = new Paragraph("ℹ️ Note: Ce bulletin de paie est généré automatiquement par le système HRMS.")
                .setFontSize(10)
                .setFontColor(ColorConstants.GRAY)
                .setMarginBottom(20);
        document.add(note);
        
        Table signatureTable = new Table(2).setWidth(UnitValue.createPercentValue(100));
        
        Cell employerSignature = new Cell()
                .add(new Paragraph("✍️ Signature Employeur").setBold())
                .add(new Paragraph("\n\n\n").setHeight(60))
                .add(new Paragraph("_____________________").setTextAlignment(TextAlignment.CENTER))
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.CENTER);
        
        Cell employeeSignature = new Cell()
                .add(new Paragraph("✍️ Signature Employé").setBold())
                .add(new Paragraph("\n\n\n").setHeight(60))
                .add(new Paragraph("_____________________").setTextAlignment(TextAlignment.CENTER))
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.CENTER);
        
        signatureTable.addCell(employerSignature);
        signatureTable.addCell(employeeSignature);
        
        document.add(signatureTable);
    }
    
    private void addTableHeader(Table table, String text, Color borderColor) {
        Cell cell = new Cell()
                .add(new Paragraph(text).setBold())
                .setBackgroundColor(new DeviceRgb(245, 245, 245))
                .setBorder(new SolidBorder(borderColor, 1))
                .setPadding(8);
        table.addCell(cell);
    }
    
    private void addTableCell(Table table, String text) {
        Cell cell = new Cell()
                .add(new Paragraph(text != null ? text : "N/A"))
                .setBorder(new SolidBorder(ColorConstants.GRAY, 1))
                .setPadding(8);
        table.addCell(cell);
    }
    
    private void addTotalRow(Table table, String label, String value, Color borderColor) {
        Cell labelCell = new Cell(1, 3)
                .add(new Paragraph(label).setBold())
                .setBackgroundColor(new DeviceRgb(245, 245, 245))
                .setBorder(new SolidBorder(borderColor, 1))
                .setPadding(8);
        
        Cell valueCell = new Cell()
                .add(new Paragraph(value).setBold())
                .setBackgroundColor(new DeviceRgb(245, 245, 245))
                .setBorder(new SolidBorder(borderColor, 1))
                .setPadding(8)
                .setTextAlignment(TextAlignment.RIGHT);
        
        table.addCell(labelCell);
        table.addCell(valueCell);
    }
    
    private void addSummaryRow(Table table, String label, String value) {
        Cell labelCell = new Cell()
                .add(new Paragraph(label).setBold())
                .setBorder(Border.NO_BORDER)
                .setPadding(8);
        
        Cell valueCell = new Cell()
                .add(new Paragraph(value))
                .setBorder(Border.NO_BORDER)
                .setPadding(8)
                .setTextAlignment(TextAlignment.RIGHT);
        
        table.addCell(labelCell);
        table.addCell(valueCell);
    }
    
    
    private String formatCurrency(double amount, String currency) {
        String formattedAmount = currencyFormatter.format(amount);
        return (currency != null ? currency : "Ar") + " " + formattedAmount;
    }
    
    
    private String formatAmount(double amount) {
        return currencyFormatter.format(amount);
    }
    
    
    private String formatCurrencyCustom(double amount, String currency, boolean showDecimals) {
        DecimalFormat formatter;
        if (showDecimals) {
            formatter = currencyFormatter;
        } else {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRANCE);
            symbols.setGroupingSeparator(',');
            formatter = new DecimalFormat("#,##0", symbols);
        }
        
        String formattedAmount = formatter.format(amount);
        return (currency != null ? currency : "Ar") + " " + formattedAmount;
    }
    
    private String getStatusText(PayrollSlipDTO.SalaryDetail component) {
        List<String> statuses = new ArrayList<>();
        
        if (component.isStatisticalComponent()) statuses.add("Statistique");
        if (component.isDoNotIncludeInTotal() && !component.isStatisticalComponent()) statuses.add("Hors Total");
        if (component.isExemptFromPayrollTax()) statuses.add("Exonéré Taxe");
        if (component.isDependsOnPaymentDays()) statuses.add("Dépend Jours");
        
        return statuses.isEmpty() ? "" : String.join(", ", statuses);
    }
}