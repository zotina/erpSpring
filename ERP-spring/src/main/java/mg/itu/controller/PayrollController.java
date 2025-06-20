package mg.itu.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import mg.itu.model.ApiResponse;
import mg.itu.model.MonthlyPayrollSummary;
import mg.itu.model.PayrollComponents;
import mg.itu.service.HrmsService;

@Controller
@RequestMapping("/api/hrms")
public class PayrollController {

    @Autowired
    private HrmsService hrmsService;
    
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

}