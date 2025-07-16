package mg.itu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import mg.itu.model.SalaryChangeHistory;

@Repository
public interface SalaryChangeHistoryRepository extends JpaRepository<SalaryChangeHistory, Long> {
}