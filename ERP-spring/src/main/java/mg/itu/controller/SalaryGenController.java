package mg.itu.controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import mg.itu.model.SalaryGen;
import mg.itu.service.SalaryGenService;


@Controller
@RequestMapping("/api/local")
public class SalaryGenController {
    @Autowired
    private SalaryGenService salaryService;

    @GetMapping("/insertSalary")
    public String insertSalary(Model model){
        return "views/hrms/salary-gen"; 
    }   

    @PostMapping("/insertSalary")
    public String insertSalary(@RequestParam(value = "monthYearStart") String monthYear,@RequestParam(value = "montant") double valeur,Model model){
        String uniqueName = "SALARY_" + monthYear + "_" + System.currentTimeMillis();
        SalaryGen salarygen = salaryService.save(new SalaryGen(uniqueName, monthYear, valeur));
        if(salarygen != null){
            model.addAttribute("response","salary inserted sucessfully");
        }
        else{
            model.addAttribute("response","salary insertion failed ");
        }
        return "views/hrms/salary-gen";
    }   
}
