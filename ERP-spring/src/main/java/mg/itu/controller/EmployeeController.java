package mg.itu.controller;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import mg.itu.model.ApiResponse;
import mg.itu.model.EmployeeDTO;
import mg.itu.model.PaginatedResponse;
import mg.itu.model.PayrollSlipDTO;
import mg.itu.model.SalaryDetailDTO;
import mg.itu.service.HrmsService;
import mg.itu.service.PdfExportService;
import mg.itu.util.DateUtil;

@Controller
@RequestMapping("/api/hrms")
public class EmployeeController {
    
    private static final Logger logger = LoggerFactory.getLogger(HrmsController.class);

    @Autowired
    private HrmsService hrmsService;

    @Autowired
    private PdfExportService pdfExportService;

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
}