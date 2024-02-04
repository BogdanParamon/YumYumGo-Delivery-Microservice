package nl.tudelft.sem.template.example.domain.order.orderstrategy;

import java.util.List;
import java.util.Optional;
import nl.tudelft.sem.template.model.Order;

public interface NextOrderStrategy {
    Optional<List<Order>> availableOrders(Optional<Long> vendorId);
}
