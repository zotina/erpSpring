package mg.itu.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import mg.itu.model.ApiResponse;
import mg.itu.model.PaginatedResponse;
import mg.itu.model.SummaryDTO;
import mg.itu.service.HrmsService;
import mg.itu.util.DateUtil;
import mg.itu.util.SalaryUtil;

@Controller
@RequestMapping("/api/hrms")
public class SalaryController {

    private static final Logger logger = LoggerFactory.getLogger(HrmsController.class);

    @Autowired
    private HrmsService hrmsService;

    @Autowired
    private HrmsService salaryEvolutionService;
    
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
}
