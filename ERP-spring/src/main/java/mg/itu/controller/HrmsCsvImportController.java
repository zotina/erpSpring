package mg.itu.controller;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import mg.itu.model.HrmsCsvImportResponse;
import mg.itu.model.HrmsResetResponse;
import mg.itu.service.HrmsCsvImportService;

@Controller
@RequestMapping("/api/hrms-csv-import")
public class HrmsCsvImportController {

    private static final Logger logger = LoggerFactory.getLogger(HrmsCsvImportController.class);
    private static final String CSV_FILES_SESSION_KEY = "csvImportFiles";

    @Autowired
    private HrmsCsvImportService hrmsCsvImportService;

    @GetMapping("/")
    public String getImportForm(Model model) {
        return "views/hrms-csv-import/hrms-csv-import";
    }

    @PostMapping("/upload-for-preview")
    public String uploadForPreview(
            @RequestParam("employeesCsv") MultipartFile employeesCsv,
            @RequestParam("salaryStructureCsv") MultipartFile salaryStructureCsv,
            @RequestParam("payrollCsv") MultipartFile payrollCsv,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (session.getAttribute("sid") == null) {
            redirectAttributes.addFlashAttribute("error", "Please log in to proceed.");
            return "redirect:/api/auth/login";
        }

        try {
            Map<String, byte[]> fileDataMap = new HashMap<>();
            Map<String, Long> lineCounts = new HashMap<>();

            byte[] employeesCsvBytes = processFile(employeesCsv, "employeesCsv", fileDataMap, lineCounts);
            processFile(salaryStructureCsv, "salaryStructureCsv", fileDataMap, lineCounts);
            byte[] payrollCsvBytes = processFile(payrollCsv, "payrollCsv", fileDataMap, lineCounts);
            
            if (fileDataMap.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Veuillez sélectionner au moins un fichier CSV à importer.");
                return "redirect:/api/hrms-csv-import/";
            }

            session.setAttribute(CSV_FILES_SESSION_KEY, fileDataMap);
            redirectAttributes.addFlashAttribute("lineCounts", lineCounts);

            Map<String, Long> salaryCountsByEmployee = parseAndCountSalariesFromBytes(employeesCsvBytes, payrollCsvBytes);
            if (!salaryCountsByEmployee.isEmpty()) {
                redirectAttributes.addFlashAttribute("salaryCountsByEmployee", salaryCountsByEmployee);
            }

            return "redirect:/api/hrms-csv-import/confirm";

        } catch (Exception e) {
            logger.error("Error during file preview generation", e);
            redirectAttributes.addFlashAttribute("error", "An error occurred while reading the files: " + e.getMessage());
            return "redirect:/api/hrms-csv-import/";
        }
    }

    private byte[] processFile(MultipartFile file, String fileKey, Map<String, byte[]> fileDataMap, Map<String, Long> lineCounts) throws java.io.IOException {
        if (file != null && !file.isEmpty()) {
            byte[] fileBytes = file.getBytes();
            fileDataMap.put(fileKey, fileBytes);
            long count = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(fileBytes), StandardCharsets.UTF_8)).lines().count();
            lineCounts.put(file.getOriginalFilename(), Math.max(0, count - 1));
            return fileBytes;
        }
        return null;
    }

    private Map<String, Long> parseAndCountSalariesFromBytes(byte[] employeesCsvBytes, byte[] payrollCsvBytes) {
        Map<String, Long> sortedSalaryCounts = new LinkedHashMap<>();
        if (employeesCsvBytes == null || payrollCsvBytes == null) {
            return sortedSalaryCounts; 
        }

        Map<String, String> employeeRefToNameMap = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(employeesCsvBytes), StandardCharsets.UTF_8))) {
            reader.lines().skip(1).forEach(line -> {
                String[] columns = line.split(",");
                if (columns.length >= 3) {
                    String ref = columns[0].trim();
                    String fullName = columns[1].trim() + " " + columns[2].trim();
                    employeeRefToNameMap.put(ref, fullName);
                }
            });
        } catch (java.io.IOException e) {
            logger.error("Error reading employees.csv for salary count", e);
            return sortedSalaryCounts; 
        }

        Map<String, Long> refCounts = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(payrollCsvBytes), StandardCharsets.UTF_8))) {
            reader.lines().skip(1).forEach(line -> {
                String[] columns = line.split(",");
                if (columns.length >= 2) {
                    String refEmploye = columns[1].trim();
                    refCounts.put(refEmploye, refCounts.getOrDefault(refEmploye, 0L) + 1);
                }
            });
        } catch (java.io.IOException e) {
            logger.error("Error reading payroll.csv for salary count", e);
            return sortedSalaryCounts; 
        }

        Map<String, Long> finalSalaryCounts = new HashMap<>();
        refCounts.forEach((ref, count) -> {
            String employeeName = employeeRefToNameMap.getOrDefault(ref, "Unknown Employee (Ref: " + ref + ")");
            finalSalaryCounts.put(employeeName, count);
        });
        
        finalSalaryCounts.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEachOrdered(x -> sortedSalaryCounts.put(x.getKey(), x.getValue()));

        return sortedSalaryCounts;
    }
    private void processAndStoreFile(MultipartFile file, String fileKey, Map<String, byte[]> fileDataMap, Map<String, Long> lineCounts) throws java.io.IOException {
        if (file != null && !file.isEmpty()) {
            fileDataMap.put(fileKey, file.getBytes());
            long count = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)).lines().count();
            lineCounts.put(file.getOriginalFilename(), Math.max(0, count - 1));
        }
    }


    @GetMapping("/confirm")
    public String showConfirmationPage(Model model) {
        if (!model.containsAttribute("lineCounts")) {
            return "redirect:/api/hrms-csv-import/";
        }
        return "views/hrms-csv-import/import-confirmation";
    }

    @PostMapping("/confirm-import")
    public String confirmAndImport(HttpSession session, RedirectAttributes redirectAttributes) {
        if (session.getAttribute("sid") == null) {
            redirectAttributes.addFlashAttribute("error", "Your session has expired. Please log in again.");
            return "redirect:/api/auth/login";
        }

        @SuppressWarnings("unchecked")
        Map<String, byte[]> fileData = (Map<String, byte[]>) session.getAttribute(CSV_FILES_SESSION_KEY);

        if (fileData == null || fileData.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "No file data found in session. Please start the import process again.");
            return "redirect:/api/hrms-csv-import/";
        }
        
        try {
            HrmsCsvImportResponse response = hrmsCsvImportService.importCsvData(fileData, session);

            if ("success".equals(response.getStatus()) || response.isSuccess()) {
                redirectAttributes.addFlashAttribute("success", response.getMessageAsString());
                redirectAttributes.addFlashAttribute("insertedRecords", response.getInserted_records());
            } else {
                redirectAttributes.addFlashAttribute("error", response.getMessageAsString());
                redirectAttributes.addFlashAttribute("validationErrors", response.getValidation_errors() != null ? response.getValidation_errors() : new java.util.ArrayList<>());
            }
        } catch (Exception e) {
            logger.error("Error confirming CSV import", e);
            redirectAttributes.addFlashAttribute("error", "A critical error occurred during the import process: " + e.getMessage());
        } finally {
            session.removeAttribute(CSV_FILES_SESSION_KEY);
        } 

        return "redirect:/api/hrms-csv-import/";
    }

    @PostMapping("/reset") 
    public String resetHrmsData(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        String accessToken = (String) session.getAttribute("sid") ;
        if (accessToken == null) {
            model.addAttribute("error", "Please log in to access the dashboard");
            return "views/auth/login";
        }
        try {
            HrmsResetResponse response = hrmsCsvImportService.resetHrmsData(session);
            if (response.isSuccess()) {
                redirectAttributes.addFlashAttribute("success", response.getMessageAsString());
                redirectAttributes.addFlashAttribute("deletedRecords", response.getDeleted_records());
            } else {
                redirectAttributes.addFlashAttribute("error", response.getMessageAsString());
                redirectAttributes.addFlashAttribute("resetErrors", response.getErrors() != null ? response.getErrors() : new java.util.ArrayList<>());
            }
        } catch (Exception e) {
            logger.error("Error resetting HRMS data", e);
            redirectAttributes.addFlashAttribute("error", "An error occurred while resetting HRMS data: " + e.getMessage());
        }
        return "redirect:/api/hrms-csv-import/";
    }
}