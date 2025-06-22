package mg.itu.controller;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;

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
import mg.itu.service.HrmsService;

  
@Controller
@RequestMapping("/api/hrms")
public class HrmsController {

    private static final Logger logger = LoggerFactory.getLogger(HrmsController.class);

    @Autowired
    private HrmsService hrmsService;
    
    @GetMapping("/insert")
    public String insertSlipForm(Model model, HttpSession session, @ModelAttribute("selectedEmployeeId") String selectedEmployeeId) {
        String accessToken = (String) session.getAttribute("sid");
        if (accessToken == null) {
            model.addAttribute("error", "Please log in to access the dashboard");
            return "views/auth/login";
        }

        try {
            
            ApiResponse<EmployeeDTO> employeesResponse = hrmsService.getAllEmployees(session);
            if ("success".equals(employeesResponse.getStatus())) {
                model.addAttribute("employees", employeesResponse.getData());
            } else {
                model.addAttribute("error", "Erreur lors du chargement des employés: " + employeesResponse.getMessage());
                model.addAttribute("employees", new ArrayList<>());
            }

            
            String empId = selectedEmployeeId != null && !selectedEmployeeId.isEmpty() ? selectedEmployeeId : (String) session.getAttribute("employeID");
            if (empId != null && !empId.isEmpty()) {
                try {
                    ApiResponse<EmployeeDTO> empResponse = hrmsService.getEmployeeDetails(empId, session);
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
                    return "redirect:/api/hrms/insert";
                }
            } catch (DateTimeParseException e) {
                redirectAttributes.addFlashAttribute("error", "Format de date invalide");
                redirectAttributes.addFlashAttribute("monthYearStart", monthDebut);
                redirectAttributes.addFlashAttribute("monthYearEnd", monthFin);
                redirectAttributes.addFlashAttribute("montant", montant);
                redirectAttributes.addFlashAttribute("selectedEmployeeId", empId);
                return "redirect:/api/hrms/insert";
            }

            
            ApiResponse<EmployeeDTO> empResponse = hrmsService.getEmployeeDetails(empId, session);
            if (!"success".equals(empResponse.getStatus())) {
                redirectAttributes.addFlashAttribute("error", empResponse.getMessage());
                redirectAttributes.addFlashAttribute("monthYearStart", monthDebut);
                redirectAttributes.addFlashAttribute("monthYearEnd", monthFin);
                redirectAttributes.addFlashAttribute("montant", montant);
                redirectAttributes.addFlashAttribute("selectedEmployeeId", empId);
                return "redirect:/api/hrms/insert";
            }

            
            ApiResponse<PayrollDTO> insertResponse = hrmsService.insertSalarySlip(empId, monthDebut, monthFin, montant, session);

            
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
}