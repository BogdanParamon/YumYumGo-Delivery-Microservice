package nl.tudelft.sem.template.example.domain.user;

import nl.tudelft.sem.template.model.Courier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourierRepository extends JpaRepository<Courier, Long> {
    boolean existsByIdAndBossId(Long courierId, Long vendorId);
}
