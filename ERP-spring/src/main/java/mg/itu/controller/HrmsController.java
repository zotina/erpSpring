package mg.itu.controller;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import mg.itu.model.ApiResponse;
import mg.itu.model.EmployeeDTO;
import mg.itu.model.PayrollDTO;
import mg.itu.model.SalaryComponentDTO;
import mg.itu.model.UpdateBaseAssignmentDTO;
import mg.itu.service.EmployeeService;
import mg.itu.service.HrmsService;

  
@Controller
@RequestMapping("/api/hrms")
public class HrmsController {

    private static final Logger logger = LoggerFactory.getLogger(HrmsController.class);

    @Autowired
    private HrmsService hrmsService;

    @Autowired
    private EmployeeService employeeService;
    
    @GetMapping("/insert")
    public String insertSlipForm(Model model, HttpSession session, @ModelAttribute("selectedEmployeeId") String selectedEmployeeId) {
        String accessToken = (String) session.getAttribute("sid");
        if (accessToken == null) {
            model.addAttribute("error", "Please log in to access the dashboard");
            return "views/auth/login";
        }

        try {
            
            ApiResponse<EmployeeDTO> employeesResponse = employeeService.getAllEmployees(session);
            if ("success".equals(employeesResponse.getStatus())) {
                model.addAttribute("employees", employeesResponse.getData());
            } else {
                model.addAttribute("error", "Erreur lors du chargement des employés: " + employeesResponse.getMessage());
                model.addAttribute("employees", new ArrayList<>());
            }

            
            String empId = selectedEmployeeId != null && !selectedEmployeeId.isEmpty() ? selectedEmployeeId : (String) session.getAttribute("employeID");
            if (empId != null && !empId.isEmpty()) {
                try {
                    ApiResponse<EmployeeDTO> empResponse = employeeService.getEmployeeDetails(empId, session);
                    if ("success".equals(empResponse.getStatus())) {
                        model.addAttribute("selectedEmployee", empResponse.getData().get(0));
                        model.addAttribute("selectedEmployeeId", empId);
                    }
                } catch (Exception e) {
                    logger.warn("Employee from session or redirect not found: {}", empId);
                }
            }

            
            if (!model.containsAttribute("monthYearStart")) {
                model.addAttribute("monthYearStart", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")));
            }
            if (!model.containsAttribute("monthYearEnd")) {
                model.addAttribute("monthYearEnd", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")));
            }
            if (!model.containsAttribute("montant")) {
                model.addAttribute("montant", 0.0);
            }

        } catch (IllegalStateException e) {
            logger.warn("Authentication error: {}", e.getMessage());
            return "redirect:/api/auth/";
        } catch (Exception e) {
            logger.error("Error fetching payroll form", e);
            model.addAttribute("error", "Erreur lors du chargement du formulaire: " + e.getMessage());
            model.addAttribute("employees", new ArrayList<>());
        }

        return "views/hrms/salary_form";
    }
    
    @PostMapping("/insert")
    public String insertSlipFormSubmit(
            @RequestParam("monthYearStart") String monthDebut,
            @RequestParam("monthYearEnd") String monthFin,
            @RequestParam("emp") String empId,
            @RequestParam(value = "montant", defaultValue = "0") Double montant,
            @RequestParam("ecraser") int ecraser,
            @RequestParam("moyen") int moyen,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        String accessToken = (String) session.getAttribute("sid");

        if (accessToken == null) {
            redirectAttributes.addFlashAttribute("error", "Please log in to access the dashboard");
            return "redirect:/api/auth/login";
        }

        try {
            
            if (monthDebut == null || monthFin == null || monthDebut.isEmpty() || monthFin.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Les dates de début et de fin sont obligatoires");
                redirectAttributes.addFlashAttribute("monthYearStart", monthDebut);
                redirectAttributes.addFlashAttribute("monthYearEnd", monthFin);
                redirectAttributes.addFlashAttribute("montant", montant);
                redirectAttributes.addFlashAttribute("selectedEmployeeId", empId);
                redirectAttributes.addFlashAttribute("ecraser", ecraser);
                redirectAttributes.addFlashAttribute("moyen", moyen);
                return "redirect:/api/hrms/insert";
            }

            
            try {
                YearMonth startMonth = YearMonth.parse(monthDebut);
                YearMonth endMonth = YearMonth.parse(monthFin);
                if (startMonth.isAfter(endMonth)) {
                    redirectAttributes.addFlashAttribute("error", "La date de début ne peut pas être postérieure à la date de fin");
                    redirectAttributes.addFlashAttribute("monthYearStart", monthDebut);
                    redirectAttributes.addFlashAttribute("monthYearEnd", monthFin);
                    redirectAttributes.addFlashAttribute("montant", montant);
                    redirectAttributes.addFlashAttribute("selectedEmployeeId", empId);
                    redirectAttributes.addFlashAttribute("ecraser", ecraser);
                  redirectAttributes.addFlashAttribute("moyen", moyen);
                    return "redirect:/api/hrms/insert";
                }
            } catch (DateTimeParseException e) {
                redirectAttributes.addFlashAttribute("error", "Format de date invalide");
                redirectAttributes.addFlashAttribute("monthYearStart", monthDebut);
                redirectAttributes.addFlashAttribute("monthYearEnd", monthFin);
                redirectAttributes.addFlashAttribute("montant", montant);
                redirectAttributes.addFlashAttribute("selectedEmployeeId", empId);
                redirectAttributes.addFlashAttribute("ecraser", ecraser);
                redirectAttributes.addFlashAttribute("moyen", moyen);
                return "redirect:/api/hrms/insert";
            }

            
            ApiResponse<EmployeeDTO> empResponse = employeeService.getEmployeeDetails(empId, session);
            if (!"success".equals(empResponse.getStatus())) {
                redirectAttributes.addFlashAttribute("error", empResponse.getMessage());
                redirectAttributes.addFlashAttribute("monthYearStart", monthDebut);
                redirectAttributes.addFlashAttribute("monthYearEnd", monthFin);
                redirectAttributes.addFlashAttribute("montant", montant);
                redirectAttributes.addFlashAttribute("selectedEmployeeId", empId);
                redirectAttributes.addFlashAttribute("ecraser", ecraser);
                redirectAttributes.addFlashAttribute("moyen", moyen);
                return "redirect:/api/hrms/insert";
            } 

             
            ApiResponse<PayrollDTO> insertResponse = hrmsService.insertSalarySlip(empId, monthDebut, monthFin, montant,ecraser,moyen, session);

             
            if ("success".equals(insertResponse.getStatus())) { 
                redirectAttributes.addFlashAttribute("success", insertResponse.getMessage());
                redirectAttributes.addFlashAttribute("salarySlips", insertResponse.getData());
                redirectAttributes.addFlashAttribute("employee", empResponse.getData().get(0));
            } else if ("warning".equals(insertResponse.getStatus())) {
                redirectAttributes.addFlashAttribute("warning", insertResponse.getMessage());
            } else {
                redirectAttributes.addFlashAttribute("error", insertResponse.getMessage());
            }

            
            redirectAttributes.addFlashAttribute("monthYearStart", monthDebut);
            redirectAttributes.addFlashAttribute("monthYearEnd", monthFin);
            redirectAttributes.addFlashAttribute("montant", montant);
            redirectAttributes.addFlashAttribute("selectedEmployeeId", empId);
            redirectAttributes.addFlashAttribute("ecraser", ecraser);
redirectAttributes.addFlashAttribute("moyen", moyen);

            
            return "redirect:/api/hrms/insert";

        } catch (IllegalStateException e) {
            logger.warn("Authentication error: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Authentication error: " + e.getMessage());
            return "redirect:/api/auth/";
        } catch (Exception e) {
            logger.error("Error creating salary slip", e);
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la création des fiches de paie: " + e.getMessage());
            redirectAttributes.addFlashAttribute("monthYearStart", monthDebut);
            redirectAttributes.addFlashAttribute("monthYearEnd", monthFin);
            redirectAttributes.addFlashAttribute("montant", montant);
            redirectAttributes.addFlashAttribute("selectedEmployeeId", empId);
            redirectAttributes.addFlashAttribute("ecraser", ecraser);
redirectAttributes.addFlashAttribute("moyen", moyen);
            return "redirect:/api/hrms/insert";
        } 
    }
    
    @GetMapping("/update-base-assignment")
    public String updateBaseAssignmentForm(Model model, HttpSession session, 
            @ModelAttribute("selectedSalaryComponent") String selectedSalaryComponent) {
        String accessToken = (String) session.getAttribute("sid");
        if (accessToken == null) {
            model.addAttribute("error", "Please log in to access the dashboard");
            return "views/auth/login";
        }
 
        try {
            
            ApiResponse<SalaryComponentDTO> componentsResponse = hrmsService.getAllSalaryComponents(session);
            if ("success".equals(componentsResponse.getStatus())) {
                model.addAttribute("salaryComponents", componentsResponse.getData());
            } else {
                model.addAttribute("error", "Erreur lors du chargement des composants de salaire: " + componentsResponse.getMessage());
                model.addAttribute("salaryComponents", new ArrayList<>());
            }

            
            if (!model.containsAttribute("montant")) {
                model.addAttribute("montant", 0.0);
            }
            if (!model.containsAttribute("taux")) {
                model.addAttribute("taux", 0.0);
            }
            if (!model.containsAttribute("infOrSup")) {
                model.addAttribute("infOrSup", 0);
            }
            if (!model.containsAttribute("minusOrPlus")) {
                model.addAttribute("minusOrPlus", 0);
            }

        } catch (IllegalStateException e) {
            logger.warn("Authentication error: {}", e.getMessage());
            return "redirect:/api/auth/";
        } catch (Exception e) {
            logger.error("Error fetching update base assignment form", e);
            model.addAttribute("error", "Erreur lors du chargement du formulaire: " + e.getMessage());
            model.addAttribute("salaryComponents", new ArrayList<>());
        }

        return "views/hrms/update_base_assignment_form";
    }

    @PostMapping("/update-base-assignment")
    public String updateBaseAssignmentSubmit(
            @RequestParam("salaryComponent") String salaryComponent,
            @RequestParam("montant") Double montant,
            @RequestParam("infOrSup") Integer infOrSup,
            @RequestParam("minusOrPlus") Integer minusOrPlus,
            @RequestParam("taux") Double taux,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        String accessToken = (String) session.getAttribute("sid");
        if (accessToken == null) {
            redirectAttributes.addFlashAttribute("error", "Please log in to access the dashboard");
            return "redirect:/api/auth/login";
        }

        try { 
            
            if (salaryComponent == null || salaryComponent.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Le composant de salaire est obligatoire");
                redirectAttributes.addFlashAttribute("montant", montant);
                redirectAttributes.addFlashAttribute("infOrSup", infOrSup);
                redirectAttributes.addFlashAttribute("minusOrPlus", minusOrPlus);
                redirectAttributes.addFlashAttribute("taux", taux);
                redirectAttributes.addFlashAttribute("selectedSalaryComponent", salaryComponent);
                return "redirect:/api/hrms/update-base-assignment";
            }

            if (taux < 0) {
                redirectAttributes.addFlashAttribute("error", "Le taux ne peut pas être négatif");
                redirectAttributes.addFlashAttribute("montant", montant);
                redirectAttributes.addFlashAttribute("infOrSup", infOrSup);
                redirectAttributes.addFlashAttribute("minusOrPlus", minusOrPlus);
                redirectAttributes.addFlashAttribute("taux", taux);
                redirectAttributes.addFlashAttribute("selectedSalaryComponent", salaryComponent);
                return "redirect:/api/hrms/update-base-assignment";
            }

            
            ApiResponse<UpdateBaseAssignmentDTO> response = hrmsService.updateBaseAssignment(salaryComponent, montant, infOrSup, minusOrPlus, taux, session);

            if ("success".equals(response.getStatus())) {
                redirectAttributes.addFlashAttribute("success", response.getMessage());
                redirectAttributes.addFlashAttribute("updatedSlips", response.getData());
            } else {
                redirectAttributes.addFlashAttribute("error", response.getMessage());
            }

            
            redirectAttributes.addFlashAttribute("montant", montant);
            redirectAttributes.addFlashAttribute("infOrSup", infOrSup);
            redirectAttributes.addFlashAttribute("minusOrPlus", minusOrPlus);
            redirectAttributes.addFlashAttribute("taux", taux);
            redirectAttributes.addFlashAttribute("selectedSalaryComponent", salaryComponent);

            return "redirect:/api/hrms/update-base-assignment";

        } catch (IllegalStateException e) {
            logger.warn("Authentication error: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Authentication error: " + e.getMessage());
            return "redirect:/api/auth/";
        } catch (Exception e) {
            logger.error("Error updating base assignment", e);
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la mise à jour: " + e.getMessage());
            redirectAttributes.addFlashAttribute("montant", montant);
            redirectAttributes.addFlashAttribute("infOrSup", infOrSup);
            redirectAttributes.addFlashAttribute("minusOrPlus", minusOrPlus);
            redirectAttributes.addFlashAttribute("taux", taux);
            redirectAttributes.addFlashAttribute("selectedSalaryComponent", salaryComponent);
            return "redirect:/api/hrms/update-base-assignment";
        }
    }

    @GetMapping("/base-salary-modification")
    public String showBaseSalaryModificationForm(Model model, HttpSession session) {
        String accessToken = (String) session.getAttribute("sid");
        if (accessToken == null) {
            return "redirect:/api/auth/login";
        }
        
        // Set default month-year to the current month for the form
        if (!model.containsAttribute("monthYear")) {
            model.addAttribute("monthYear", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")));
        }
        
        return "views/hrms/base_salary_modification_form";
    }

    @PostMapping("/base-salary-modification")
    public String handleBaseSalaryModification(
            @RequestParam("percentageValue") Double percentageValue,
            @RequestParam("monthYear") String monthYear,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        String accessToken = (String) session.getAttribute("sid");
        if (accessToken == null) {
            redirectAttributes.addFlashAttribute("error", "Your session has expired. Please log in again.");
            return "redirect:/api/auth/login";
        }
        
        try {
            // Basic validation
            if (percentageValue == null || monthYear == null || monthYear.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Percentage value and month-year are required.");
                redirectAttributes.addFlashAttribute("percentageValue", percentageValue);
                redirectAttributes.addFlashAttribute("monthYear", monthYear);
                return "redirect:/api/hrms/base-salary-modification";
            }

            ApiResponse<Map<String, Object>> response = hrmsService.applyBaseSalaryModification(monthYear, percentageValue, session);

            if ("success".equals(response.getStatus()) || "partial_success".equals(response.getStatus())) {
                redirectAttributes.addFlashAttribute("success", response.getMessage());
                // Optionally pass the detailed result to the view
                redirectAttributes.addFlashAttribute("resultData", response.getData().get(0));
            } else {
                redirectAttributes.addFlashAttribute("error", response.getMessage());
            }

        } catch (Exception e) {
            logger.error("Error handling base salary modification", e);
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred: " + e.getMessage());
        }

        // Retain form values on redirect
        redirectAttributes.addFlashAttribute("percentageValue", percentageValue);
        redirectAttributes.addFlashAttribute("monthYear", monthYear);
        
        return "redirect:/api/hrms/base-salary-modification";
    }

     @GetMapping("/reapply-adjustments")
    public String showReapplyAdjustmentsForm(Model model) {
        if (!model.containsAttribute("form")) {
            // Set default values for the form
            model.addAttribute("form", Map.of(
                "monthYearMin", LocalDate.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM")),
                "monthYearMax", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                "adjustmentType", "increase"
            ));
        }
        return "views/hrms/reapply_adjustments_form";
    }

    @PostMapping("/reapply-adjustments")
    public String handleReapplyAdjustments(
            @RequestParam("monthYearMin") String monthYearMin,
            @RequestParam("monthYearMax") String monthYearMax,
            @RequestParam("adjustmentType") String adjustmentType,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        try {
            if (YearMonth.parse(monthYearMin).isAfter(YearMonth.parse(monthYearMax))) {
                redirectAttributes.addFlashAttribute("error", "The start period cannot be after the end period.");
                redirectAttributes.addFlashAttribute("form", Map.of("monthYearMin", monthYearMin, "monthYearMax", monthYearMax, "adjustmentType", adjustmentType));
                return "redirect:/api/hrms/reapply-adjustments";
            }

            ApiResponse<Map<String, Object>> response = hrmsService.reapplyHistoricalAdjustments(monthYearMin, monthYearMax, adjustmentType, session);
            
            if ("success".equals(response.getStatus())) {
                redirectAttributes.addFlashAttribute("success", response.getMessage());
            } else if ("warning".equals(response.getStatus())) {
                redirectAttributes.addFlashAttribute("warning", response.getMessage());
            } else {
                redirectAttributes.addFlashAttribute("error", response.getMessage());
            }
            
            // Pass the detailed response data to the view for display
            redirectAttributes.addFlashAttribute("resultData", response.getData());

        } catch (Exception e) {
            logger.error("Error handling historical adjustment re-application", e);
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred: " + e.getMessage());
        }

        // Retain user input on redirect
        redirectAttributes.addFlashAttribute("form", Map.of("monthYearMin", monthYearMin, "monthYearMax", monthYearMax, "adjustmentType", adjustmentType));
        return "redirect:/api/hrms/reapply-adjustments";
    }

}