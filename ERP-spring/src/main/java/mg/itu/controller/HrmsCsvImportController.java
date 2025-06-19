package mg.itu.controller;

import jakarta.servlet.http.HttpSession;
import mg.itu.model.HrmsCsvImportResponse;
import mg.itu.model.HrmsResetResponse;
import mg.itu.service.HrmsCsvImportService;
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

@Controller
@RequestMapping("/api/hrms-csv-import")
public class HrmsCsvImportController {

    private static final Logger logger = LoggerFactory.getLogger(HrmsCsvImportController.class);

    @Autowired
    private HrmsCsvImportService hrmsCsvImportService;

    @GetMapping("/")
    public String getImportForm(Model model) {
        return "views/hrms-csv-import/hrms-csv-import";
    }

    @PostMapping("/import")
    public String importCsvFiles(
            @RequestParam("employeesCsv") MultipartFile employeesCsv,
            @RequestParam("salaryStructureCsv") MultipartFile salaryStructureCsv,
            @RequestParam("payrollCsv") MultipartFile payrollCsv,
            Model model,
            HttpSession session) {
        String accessToken = (String) session.getAttribute("sid") ;

        if (accessToken == null) {
            model.addAttribute("error", "Please log in to access the dashboard");
            return "views/auth/login";
        } 
        try {
            HrmsCsvImportResponse response = hrmsCsvImportService.importCsvFiles(
                    employeesCsv, salaryStructureCsv, payrollCsv, session); 
 
            logger.debug("Response: status={}, success={}, message={}, validation_errors={}",
                    response.getStatus(), response.isSuccess(), response.getMessage(), response.getValidation_errors());
            logger.info("Model attributes: error={}, validationErrors={}",
                    model.getAttribute("error"), model.getAttribute("validationErrors"));

            if ("success".equals(response.getStatus()) || response.isSuccess()) {
                model.addAttribute("success", response.getMessageAsString());
                model.addAttribute("insertedRecords", response.getInserted_records());
            } else {
                model.addAttribute("error", response.getMessageAsString());
                model.addAttribute("validationErrors", response.getValidation_errors() != null ? response.getValidation_errors() : new java.util.ArrayList<>());
            }
        } catch (IllegalStateException e) {
            logger.warn("Authentication error: {}", e.getMessage());
            return "redirect:/api/auth/";
        } catch (Exception e) {  
            logger.error("Error importing CSV files", e );
            model.addAttribute("error", "An error occurred while importing CSV files: " + e.getMessage());
            model.addAttribute("validationErrors", new java.util.ArrayList<>());
        }
        return "views/hrms-csv-import/hrms-csv-import";
    }  
    
    @PostMapping("/reset") 
    public String resetHrmsData(Model model, HttpSession session) {
        String accessToken = (String) session.getAttribute("sid") ;

        if (accessToken == null) {
            model.addAttribute("error", "Please log in to access the dashboard");
            return "views/auth/login";
        } 
        try {
            HrmsResetResponse response = hrmsCsvImportService.resetHrmsData(session);

            logger.debug("Reset Response: success={}, message={}, errors={}, deleted_records={}",
                    response.isSuccess(), response.getMessage(), response.getErrors(), response.getDeleted_records());
 
            if (response.isSuccess()) {
                model.addAttribute("success", response.getMessageAsString());
                model.addAttribute("deletedRecords", response.getDeleted_records());
            } else {
                model.addAttribute("error", response.getMessageAsString());
                model.addAttribute("resetErrors", response.getErrors() != null ? response.getErrors() : new java.util.ArrayList<>());
            }
        } catch (IllegalStateException e) {
            logger.warn("Authentication error: {}", e.getMessage());
            return "redirect:/api/auth/";
        } catch (Exception e) {
            logger.error("Error resetting HRMS data", e);
            model.addAttribute("error", "An error occurred while resetting HRMS data: " + e.getMessage());
            model.addAttribute("resetErrors", new java.util.ArrayList<>());
        }
        return "views/hrms-csv-import/hrms-csv-import";
    }
}