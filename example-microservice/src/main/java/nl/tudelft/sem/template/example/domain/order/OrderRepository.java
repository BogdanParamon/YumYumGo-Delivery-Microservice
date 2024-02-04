package nl.tudelft.sem.template.example.domain.order;

import java.util.List;
import nl.tudelft.sem.template.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByVendorIdAndStatusAndCourierId(Long vendorId, Order.StatusEnum status, Long courierId);

    List<Order> findByStatus(Order.StatusEnum status);

    boolean existsByIdAndVendorId(Long orderId, Long vendorId);

    boolean existsByIdAndCourierId(Long orderId, Long courierId);

    List<Order> findByCourierIdAndStatus(Long courierId, Order.StatusEnum status);

}
