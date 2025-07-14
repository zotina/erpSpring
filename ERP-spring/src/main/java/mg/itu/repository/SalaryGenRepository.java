package mg.itu.repository;

import mg.itu.model.SalaryGen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface SalaryGenRepository extends JpaRepository<SalaryGen, String> {
}