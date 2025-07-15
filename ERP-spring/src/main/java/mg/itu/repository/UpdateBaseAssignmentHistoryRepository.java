package mg.itu.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import mg.itu.model.UpdateBaseAssignmentHistory;

@Repository
public interface UpdateBaseAssignmentHistoryRepository extends JpaRepository<UpdateBaseAssignmentHistory, Long> {
}