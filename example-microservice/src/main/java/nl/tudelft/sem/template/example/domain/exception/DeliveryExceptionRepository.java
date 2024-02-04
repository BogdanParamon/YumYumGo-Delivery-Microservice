package nl.tudelft.sem.template.example.domain.exception;

import java.util.List;
import nl.tudelft.sem.template.model.DeliveryException;
import nl.tudelft.sem.template.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryExceptionRepository extends JpaRepository<DeliveryException, Long> {
    List<DeliveryException> findByOrder(Order o);

    Boolean existsByOrder(Order o);
}
