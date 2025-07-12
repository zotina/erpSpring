package mg.itu.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mg.itu.model.BaseSalaryModif;
import mg.itu.repository.BaseSalaryModifRepository;

@Service
public class BaseSalaryModifService {

    @Autowired
    private BaseSalaryModifRepository repository;

    /**
     * Fetches all records and filters in memory to find reductions within a period.
     * @param startMonth The start of the period (e.g., "2025-01")
     * @param endMonth The end of the period (e.g., "2025-03")
     * @return A list of matching BaseSalaryModif objects.
     */
    public List<BaseSalaryModif> findReductionsInPeriod(String startMonth, String endMonth) {
        // 1. Fetch ALL records from the database
        List<BaseSalaryModif> allMods = repository.findAll();

        // 2. Filter the list using Java streams
        return allMods.stream()
                .filter(mod -> {
                    // Condition 1: Must be a reduction
                    boolean isReduction = mod.getPercentageValue() < 0;
                    
                    // Condition 2: Must be within the date range (using string comparison)
                    boolean isInRange = mod.getMonthYear().compareTo(startMonth) >= 0 &&
                                        mod.getMonthYear().compareTo(endMonth) <= 0;
                    
                    return isReduction && isInRange;
                })
                .collect(Collectors.toList());
    }

    /**
     * Fetches all records and filters in memory to find increases within a period.
     * @param startMonth The start of the period (e.g., "2025-01")
     * @param endMonth The end of the period (e.g., "2025-03")
     * @return A list of matching BaseSalaryModif objects.
     */
    public List<BaseSalaryModif> findIncreasesInPeriod(String startMonth, String endMonth) {
        // 1. Fetch ALL records
        List<BaseSalaryModif> allMods = repository.findAll();

        // 2. Filter in memory
        return allMods.stream()
                .filter(mod -> {
                    // Condition 1: Must be an increase
                    boolean isIncrease = mod.getPercentageValue() > 0;

                    // Condition 2: Must be within the date range
                    boolean isInRange = mod.getMonthYear().compareTo(startMonth) >= 0 &&
                                        mod.getMonthYear().compareTo(endMonth) <= 0;

                    return isIncrease && isInRange;
                })
                .collect(Collectors.toList());
    }
}