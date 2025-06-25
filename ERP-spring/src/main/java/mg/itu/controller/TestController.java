package mg.itu.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import mg.itu.model.Employee;
import mg.itu.repository.EmployeeRepository;
@Controller
public class TestController {
    
    @Autowired
    private EmployeeRepository employeeRepository;
    @GetMapping("/test")
    public ResponseEntity<List<Employee>> testConnection() {
        
        try {
            List<Employee> employees = employeeRepository.findAll();
            System.out.println("employe size "+employees.size());
            return ResponseEntity.ok(employees);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }
    
}
