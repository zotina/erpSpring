package mg.itu.controller;

import jakarta.servlet.http.HttpSession;
import mg.itu.model.ApiResponse;
import mg.itu.model.EmployeeDTO;
import mg.itu.model.MonthlyPayrollSummary;
import mg.itu.model.PaginatedResponse;
import mg.itu.model.PayrollComponents;
import mg.itu.model.PayrollDTO;
import mg.itu.model.PayrollSlipDTO;
import mg.itu.model.SalaryComponentDTO;
import mg.itu.model.SalaryDetailDTO;
import mg.itu.model.SummaryDTO;
import mg.itu.model.UpdateBaseAssignmentDTO;
import mg.itu.service.HrmsService;
import mg.itu.service.PdfExportService;
import mg.itu.util.DateUtil;
import mg.itu.util.SalaryUtil;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

  
@Controller
@RequestMapping("/api/hrms")
public class HrmsController {

    private static final Logger logger = LoggerFactory.getLogger(HrmsController.class);

    @Autowired
    private HrmsService hrmsService;

    @Autowired
    private PdfExportService pdfExportService;

    @Autowired
    private HrmsService salaryEvolutionService;
    
   
    @GetMapping("/employees")
    public String getEmployeeList(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "department", required = false) String department,
            @RequestParam(value = "designation", required = false) String designation,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "5") int size,
            Model model, HttpSession session) {

        String accessToken = (String) session.getAttribute("sid") ;

        if (accessToken == null) {
            model.addAttribute("error", "Please log in to access the dashboard");
            return "views/auth/login";
        } 
        
        try {
            if (page < 1) page = 1;
            if (size < 1) size = 20;
            if (size > 100) size = 100; 
            
            PaginatedResponse<EmployeeDTO> response = hrmsService.getEmployeeListPaginated(
                    search, department, designation, startDate, endDate, page, size, session);

            model.addAttribute("employees", response.getData());
            model.addAttribute("currentPage", response.getCurrentPage());
            model.addAttribute("totalPages", response.getTotalPages());
            model.addAttribute("totalRecords", response.getTotalRecords());
            model.addAttribute("pageSize", response.getPageSize());
            model.addAttribute("hasNext", response.isHasNext());
            model.addAttribute("hasPrevious", response.isHasPrevious());
            
            int startPage = Math.max(1, page - 2);
            int endPage = Math.min(response.getTotalPages(), page + 2);
            model.addAttribute("startPage", startPage);
            model.addAttribute("endPage", endPage);
            
        } catch (IllegalStateException e) {
            logger.warn("Authentication error: {}", e.getMessage());
            return "redirect:/api/auth/";
        } catch (Exception e) {
            logger.error("Error fetching employee list", e);
            model.addAttribute("error", "Error fetching employee list: " + e.getMessage());
            
            model.addAttribute("employees", java.util.Collections.emptyList());
            model.addAttribute("currentPage", 1);
            model.addAttribute("totalPages", 0);
            model.addAttribute("totalRecords", 0);
            model.addAttribute("pageSize", size);
            model.addAttribute("hasNext", false);
            model.addAttribute("hasPrevious", false);
        }
        
        model.addAttribute("availableDepartments", mg.itu.data.Init.getDepartments());
        model.addAttribute("availableDesignations", mg.itu.data.Init.getDesignations());
        
        model.addAttribute("search", search);
        model.addAttribute("selectedDepartment", department);
        model.addAttribute("selectedDesignation", designation);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        
        return "views/hrms/employee-list";
    }
    
    @GetMapping("/employee/{id}")
    public String getEmployeeDetails(@PathVariable String id, Model model, HttpSession session) {
        String accessToken = (String) session.getAttribute("sid") ;

        if (accessToken == null) {
            model.addAttribute("error", "Please log in to access the dashboard");
            return "views/auth/login";
        } 

        try {
            ApiResponse<EmployeeDTO> empResponse = hrmsService.getEmployeeDetails(id, session);
            ApiResponse<SalaryDetailDTO> salaryResponse = hrmsService.getSalaryHistory(id, session);
            session.setAttribute("employeID", id); 
            if ("success".equals(empResponse.getStatus()) && "success".equals(salaryResponse.getStatus())) {
                model.addAttribute("employee", empResponse.getData().get(0));
                model.addAttribute("salaryHistory", salaryResponse.getData());
            } else {
                model.addAttribute("error", empResponse.getMessage() + " | " + salaryResponse.getMessage());
            }
        } catch (IllegalStateException e) {
            logger.warn("Authentication error: {}", e.getMessage());
            return "redirect:/api/auth/";
        } catch (Exception e) {
            logger.error("Error fetching employee details", e);
            model.addAttribute("error", "Error fetching employee details: " + e.getMessage());
        }
        return "views/hrms/employee-details";
    }

    
    @GetMapping("/employee/{id}/payroll")
    public String getPayrollForm(@PathVariable String id, Model model, HttpSession session) {
        String accessToken = (String) session.getAttribute("sid") ;

        if (accessToken == null) {
            model.addAttribute("error", "Please log in to access the dashboard");
            return "views/auth/login";
        } 

        try {
            
            String empId = (String) session.getAttribute("employeID");
            if (empId == null) {
                empId = id;
                session.setAttribute("employeID", empId); 
            }
            
            ApiResponse<EmployeeDTO> empResponse = hrmsService.getEmployeeDetails(empId, session);
            if ("success".equals(empResponse.getStatus())) {
                model.addAttribute("employee", empResponse.getData().get(0));
            } else {
                model.addAttribute("error", empResponse.getMessage()); 
            }
        } catch (IllegalStateException e) {
            logger.warn("Authentication error: {}", e.getMessage());
            return "redirect:/api/auth/";
        } catch (Exception e) {
            logger.error("Error fetching payroll form", e);
            model.addAttribute("error", "Error fetching payroll form: " + e.getMessage());
        }
        return "views/hrms/payroll-form";
    }
   @PostMapping("/employee/{id}/payroll/generate")  
    public String generatePayrollSlip(
            @PathVariable String id, 
            @RequestParam("monthYear") String monthYear, 
            Model model, HttpSession session) {
        String accessToken = (String) session.getAttribute("sid") ;

        if (accessToken == null) {
            model.addAttribute("error", "Please log in to access the dashboard");
            return "views/auth/login";
        } 
        try {
            session.setAttribute("monthYear", monthYear); 
            String emp = (String) session.getAttribute("employeID");
            System.out.println("employe ID "+ emp);

            ApiResponse<EmployeeDTO> empResponse = hrmsService.getEmployeeDetails(emp, session);
            
            String startDate = DateUtil.getFirstDayOfMonth(monthYear);
            String endDate = DateUtil.getLastDayOfMonth(monthYear);
            
            ApiResponse<PayrollSlipDTO> response = hrmsService.generatePayrollSlip(emp, startDate,endDate, session);
            model.addAttribute("employee", empResponse.getData().get(0));
            
            if ("success".equals(empResponse.getStatus()) && "success".equals(response.getStatus())) {
                model.addAttribute("payrollSlip", response.getData().get(0));
                model.addAttribute("monthYear", monthYear);
            } else {
                model.addAttribute("error", response.getMessage());
            }
        } catch (IllegalArgumentException e) {
            logger.error("Invalid date format: {}", e.getMessage());
            model.addAttribute("error", "Format de date invalide: " + e.getMessage());
        } catch (IllegalStateException e) {
            logger.warn("Authentication error: {}", e.getMessage());
            return "redirect:/api/auth/";
        } catch (Exception e) {
            logger.error("Error generating payroll slip", e); 
            model.addAttribute("error", "Error generating payroll slip: " + e.getMessage());
        }
        return "views/hrms/payroll-form";
    }
    
    @GetMapping("/employee/{id}/payroll/export")
    public ResponseEntity<byte[]> exportPayrollSlipToPdf(
            @PathVariable String id,
            @RequestParam("monthYear") String monthYear,
            HttpSession session,Model model) {
        
        try {
            String emp = (String) session.getAttribute("employeID");
            String month = (String) session.getAttribute("monthYear");
            ApiResponse<EmployeeDTO> empResponse = hrmsService.getEmployeeDetails(emp, session);
            String startDate = DateUtil.getFirstDayOfMonth(month);
            String endDate = DateUtil.getLastDayOfMonth(month);
            ApiResponse<PayrollSlipDTO> response = hrmsService.generatePayrollSlip(emp, startDate, endDate, session);
            
            if (!"success".equals(empResponse.getStatus()) || !"success".equals(response.getStatus())) {
                logger.error("Error retrieving data for PDF export: {}", response.getMessage());
                return ResponseEntity.badRequest().build();
            }
            
            EmployeeDTO employee = empResponse.getData().get(0);
            PayrollSlipDTO payrollSlip = response.getData().get(0);
            
            
            byte[] pdfBytes = pdfExportService.generatePayrollSlipPdf(employee, payrollSlip, month);
            
            
            String filename = String.format("Fiche_Paie_%s_%s.pdf", 
                employee.getFullName().replaceAll("\s+", "_"), 
                monthYear);
                
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
            headers.setContentLength(pdfBytes.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
                    
        } catch (IllegalArgumentException e) {
            logger.error("Invalid date format: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            logger.warn("Authentication error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            logger.error("Error exporting payroll slip to PDF", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    
    @GetMapping("/summary")
    public String getMonthlySummary(
            @RequestParam(value = "monthYear", required = false) String monthYear,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "5") int size,
            Model model, HttpSession session) {
        
        String accessToken = (String) session.getAttribute("sid") ;

        if (accessToken == null) {
            model.addAttribute("error", "Please log in to access the dashboard");
            return "views/auth/login";
        } 
        
        try {
            if (page < 1) page = 1;
            if (size < 1) size = 20;
            if (size > 100) size = 100;
            
            model.addAttribute("monthYear", monthYear);
            
            String startDate = DateUtil.getFirstDayOfMonth(monthYear);
            String endDate = DateUtil.getLastDayOfMonth(monthYear);
            
            PaginatedResponse<SummaryDTO> response = hrmsService.getMonthlySummaryPaginated(
                    startDate, endDate, page, size, session);
            
            model.addAttribute("summary", response.getData());
            model.addAttribute("currentPage", response.getCurrentPage());
            model.addAttribute("totalPages", response.getTotalPages());
            model.addAttribute("totalRecords", response.getTotalRecords());
            model.addAttribute("pageSize", response.getPageSize());
            model.addAttribute("hasNext", response.isHasNext());
            model.addAttribute("hasPrevious", response.isHasPrevious());
            
            int startPage = Math.max(1, page - 2);
            int endPage = Math.min(response.getTotalPages(), page + 2);
            model.addAttribute("startPage", startPage);
            model.addAttribute("endPage", endPage);
            
            
            logger.info("Start Page: {}, End Page: {}", startPage, endPage);
            logger.info("Condition pagination (totalPages > 1): {}", response.getTotalPages() > 1);
            
            
            ApiResponse<SummaryDTO> allDataResponse = hrmsService.getAllMonthlySummary(startDate, endDate, session);
            
            if ("success".equals(allDataResponse.getStatus())) {
                SalaryUtil.SalarySummaryCalculation calculations = 
                    SalaryUtil.calculateSummaryStatistics(allDataResponse.getData());
                
                model.addAllAttributes(calculations.toModelAttributes());
            }
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid date format: {}", e.getMessage());
            model.addAttribute("error", "Format de date invalide: " + e.getMessage());
            
        } catch (IllegalStateException e) {
            logger.warn("Authentication error: {}", e.getMessage());
            return "redirect:/api/auth/";
            
        } catch (Exception e) {
            logger.error("Error fetching monthly summary", e);
            model.addAttribute("error", "Erreur lors de la récupération du récapitulatif mensuel: " + e.getMessage());
            
            model.addAttribute("summary", java.util.Collections.emptyList());
            model.addAttribute("currentPage", 1);
            model.addAttribute("totalPages", 0);
            model.addAttribute("totalRecords", 0);
            model.addAttribute("pageSize", size);
            model.addAttribute("hasNext", false);
            model.addAttribute("hasPrevious", false);
        }
        
        return "views/hrms/monthly-summary";
    }
    
    
    @GetMapping("/payroll-summary")
    public String getPayrollSummary(@RequestParam(value = "year", required = false) String year, Model model, HttpSession session) {
        if (year == null || year.isEmpty()) {
            year = "2024";
        }
        
        String accessToken = (String) session.getAttribute("sid");
        if (accessToken == null) {
            model.addAttribute("error", "Please log in to access the dashboard");
            return "views/auth/login";
        }
        
        try {
            ApiResponse<MonthlyPayrollSummary> response = hrmsService.getMonthlyPayrollSummary(year, session);
            if ("success".equals(response.getStatus())) {
                List<MonthlyPayrollSummary> summaries = response.getData();
                model.addAttribute("summaries", summaries);
                model.addAttribute("year", year);
                
                
                if (summaries != null && !summaries.isEmpty()) {
                    double totalAnnual = summaries.stream()
                        .mapToDouble(MonthlyPayrollSummary::getTotalNetPay)
                        .sum();
                    
                    model.addAttribute("totalAnnual", totalAnnual);
                    
                    
                    String currency = summaries.get(0).getCurrency();
                    model.addAttribute("currency", currency);
                } else {
                    model.addAttribute("totalAnnual", 0.0);
                    model.addAttribute("currency", "");
                }
            } else {
                model.addAttribute("error", response.getMessage());
            }
        } catch (IllegalStateException e) {
            return "redirect:/api/auth/";
        } catch (Exception e) {
            model.addAttribute("error", "Erreur lors de la récupération du tableau mensuel des salaires : " + e.getMessage());
        }
        
        return "views/hrms/payroll-summary";
    }

    @GetMapping("/payroll-components")
    public String getPayrollComponents(@RequestParam("monthYear") String monthYear,
    @RequestParam(value = "employeeId", required = false)String employeeId ,@RequestParam(value = "empname", required = false) String employeeName, Model model, HttpSession session) {
        String accessToken = (String) session.getAttribute("sid") ;

        if (accessToken == null) {
            model.addAttribute("error", "Please log in to access the dashboard");
            return "views/auth/login";
        } 
        try {
            ApiResponse<PayrollComponents> response = hrmsService.getPayrollComponents(monthYear,employeeId, session);
            if ("success".equals(response.getStatus())) {
                model.addAttribute("components", response.getData().get(0));
                model.addAttribute("empname", employeeName);
            } else {
                model.addAttribute("error", response.getMessage());
            }
        } catch (IllegalStateException e) {
            return "redirect:/api/auth/";
        } catch (Exception e) {
            model.addAttribute("error", "Erreur lors de la récupération des détails par éléments de salaire : " + e.getMessage());
        }
        return "views/hrms/payroll-components";
    }

    @GetMapping("/salary-evolution-data")
    public ResponseEntity<String> getSalaryEvolutionData(
        @RequestParam(value = "year", required = false)
        String year,HttpSession session) {

        if(year == null || year == "")
            year = "2025";
        try {
            String data = salaryEvolutionService.fetchSalaryEvolutionData(year,session);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/salary-evolution")
    public String getSalaryEvolution(Model model, HttpSession session) {
        String accessToken = (String) session.getAttribute("sid") ;

        if (accessToken == null) {
            model.addAttribute("error", "Please log in to access the dashboard");
            return "views/auth/login";
        } 
        String sid = (String) session.getAttribute("sid");
        model.addAttribute("sid", sid);
        return "views/hrms/salary-evolution";
    }

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

            
            ApiResponse<UpdateBaseAssignmentDTO> response = hrmsService.updateBaseAssignment(
                    salaryComponent, montant, infOrSup, minusOrPlus, taux, session);

            
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