package nl.tudelft.sem.template.example.utils;

import nl.tudelft.sem.template.example.domain.order.OrderRepository;
import nl.tudelft.sem.template.example.domain.user.CourierRepository;
import nl.tudelft.sem.template.example.domain.user.VendorRepository;
import nl.tudelft.sem.template.example.externalservices.OrderExternalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DbUtils {

    private final OrderRepository orderRepo;
    private final VendorRepository vendorRepo;
    private final CourierRepository courierRepo;

    private final OrderExternalService orderExternalService;

    /**
     * Constructor for DBUtils.
     *
     * @param orderRepo            The order repository.
     * @param vendorRepo           The vendor repository.
     * @param courierRepo          The courier repository.
     * @param orderExternalService The External Service for order
     */
    @Autowired
    public DbUtils(OrderRepository orderRepo, VendorRepository vendorRepo, CourierRepository courierRepo,
                   OrderExternalService orderExternalService) {
        this.orderRepo = orderRepo;
        this.vendorRepo = vendorRepo;
        this.courierRepo = courierRepo;
        this.orderExternalService = orderExternalService;
    }

    /**
     * Checks if a user belongs to an order.
     *
     * @param userId  The ID of the user.
     * @param orderId The ID of the order.
     * @return True if the user belongs to the order, false otherwise.
     */
    public boolean userBelongsToOrder(Long userId, Long orderId) {
        return orderRepo.existsByIdAndCourierId(orderId, userId)
            || orderRepo.existsByIdAndVendorId(orderId, userId)
            || customerBelongsToOrder(orderId, userId);
    }

    /**
     * Checks if a courier belongs to a vendor.
     *
     * @param courierId The ID of the courier.
     * @param vendorId  The ID of the vendor.
     * @return True if the courier belongs to the vendor, false otherwise.
     */
    public boolean courierBelongsToVendor(Long courierId, Long vendorId) {
        return courierRepo.existsByIdAndBossId(courierId, vendorId);
    }

    /**
     * Checks if a customer belongs to an order.
     *
     * @param customerId The ID of the customer.
     * @param orderId    The ID of the order.
     * @return True if the customer belongs to the order, false otherwise.
     */
    public boolean customerBelongsToOrder(Long orderId, Long customerId) {
        try {
            orderExternalService.getOrder(customerId, orderId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
