package mg.itu.service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mg.itu.model.SalaryGen;
import mg.itu.repository.SalaryGenRepository;

@Service
public class SalaryGenService {
    
    @Autowired
    private SalaryGenRepository salaryService;

    public SalaryGen save(SalaryGen salary){
        try {
            return salaryService.save(salary);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'enregistrement du salaire : " + e.getMessage(), e);
        }
    }
}
