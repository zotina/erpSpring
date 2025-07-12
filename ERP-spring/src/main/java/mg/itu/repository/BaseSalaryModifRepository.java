package mg.itu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import mg.itu.model.BaseSalaryModif;

@Repository
public interface BaseSalaryModifRepository extends JpaRepository<BaseSalaryModif, Long> {

}