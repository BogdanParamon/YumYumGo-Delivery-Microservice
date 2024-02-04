package nl.tudelft.sem.template.example.domain.order;

import java.time.OffsetDateTime;
import java.util.Optional;
import nl.tudelft.sem.template.example.domain.exception.DeliveryExceptionRepository;
import nl.tudelft.sem.template.model.DeliveryException;
import nl.tudelft.sem.template.model.Order;
import nl.tudelft.sem.template.model.Time;
import nl.tudelft.sem.template.model.UpdateToDeliveredRequest;
import nl.tudelft.sem.template.model.UpdateToGivenToCourierRequest;
import nl.tudelft.sem.template.model.UpdateToPreparingRequest;
import org.springframework.stereotype.Service;

@Service
public class StatusService {

    private final DeliveryExceptionRepository exceptionRepo;
    private final OrderRepository orderRepo;


    public StatusService(OrderRepository orderRepo, DeliveryExceptionRepository exceptionRepo) {
        this.orderRepo = orderRepo;
        this.exceptionRepo = exceptionRepo;
    }

    /**
     * Attempts to get the status of order.
     * Everyone can use this.
     *
     * @param orderId the id of the order
     * @return the optional of updated order object, empty if the order was not found
     */
    public Optional<Order.StatusEnum> getOrderStatus(Long orderId) {
        Optional<Order> o = orderRepo.findById(orderId);
        return o.map(Order::getStatus);
    }

    /**
     * Attempts to update the status of order to "accepted".
     * Vendors use this.
     *
     * @param orderId the id of the order
     * @return the optional of updated order object, empty if the order was not found
     */
    public Optional<Order> updateStatusToAccepted(Long orderId) {
        Optional<Order> o = orderRepo.findById(orderId);

        if (o.isEmpty()) {
            return Optional.empty();
        }

        Order order = o.get();
        order.setStatus(Order.StatusEnum.ACCEPTED);
        return Optional.of(orderRepo.saveAndFlush(order));
    }


    /**
     * Attempts to update the status of order to "rejected".
     * Vendors use this.
     *
     * @param orderId the id of the order
     * @return the optional of updated order object, empty if the order was not found
     */
    public Optional<Order> updateStatusToRejected(Long orderId) {
        Optional<Order> o = orderRepo.findById(orderId);

        if (o.isEmpty()) {
            return Optional.empty();
        }

        Order order = o.get();
        order.setStatus(Order.StatusEnum.REJECTED);
        return Optional.of(orderRepo.saveAndFlush(order));
    }


    /**
     * Attempts to update the status of order to "given_to_courier".
     * Vendors use this.
     *
     * @param orderId the id of the order
     * @return the optional of updated order object, empty if the order was not found
     */
    public Optional<Order> updateStatusToGivenToCourier(Long orderId, UpdateToGivenToCourierRequest req) {
        Optional<Order> o = orderRepo.findById(orderId);

        if (o.isEmpty()) {
            return Optional.empty();
        }

        Order order = o.get();
        order.setStatus(Order.StatusEnum.GIVEN_TO_COURIER);
        order.courierId(req.getCourierId());
        return Optional.of(orderRepo.saveAndFlush(order));
    }


    /**
     * Attempts to update the status of order to "in_transit".
     * Couriers use this.
     *
     * @param orderId the id of the order
     * @return the optional of updated order object, empty if the order was not found
     */
    public Optional<Order> updateStatusToInTransit(Long orderId) {
        Optional<Order> o = orderRepo.findById(orderId);

        if (o.isEmpty()) {
            return Optional.empty();
        }

        Order order = o.get();
        order.setStatus(Order.StatusEnum.IN_TRANSIT);
        return Optional.of(orderRepo.saveAndFlush(order));
    }

    /**
     * updates the status to preparing.
     *
     * @param orderId id of the order
     * @param req     request
     * @return the updated order
     */
    public Optional<Order> updateStatusToPreparing(Long orderId, UpdateToPreparingRequest req) {
        Optional<Order> o = orderRepo.findById(orderId);

        if (o.isEmpty()) {
            return Optional.empty();
        }

        Order order = o.get();
        order.setStatus(Order.StatusEnum.PREPARING);
        if (order.getTimeValues() == null) {
            order.setTimeValues(new Time());
        }
        Time timeValues = order.getTimeValues();
        timeValues.setPrepTime(req.getPrepTime());
        timeValues.setExpectedDeliveryTime(req.getExpectedDeliveryTime());

        return Optional.of(orderRepo.saveAndFlush(order));
    }

    /**
     * Sets the actual delivery time of an object and sets its status to delivered.
     *
     * @param orderId                  the id of the object to be updated
     * @param updateToDeliveredRequest object thst has the actual delivery time
     * @return the updated version optional of order, empty if order was not found or the actual delivery time
     *         was already set, also if required fields were missing or if the order was already delivered
     */
    public Optional<Order> updateStatusToDelivered(Long orderId, UpdateToDeliveredRequest updateToDeliveredRequest) {
        Optional<Order> ret = orderRepo.findById(orderId);
        // we now the order exists as it is checked in the controller
        if (ret.isEmpty()) {
            return Optional.empty();
        }
        Order order = ret.get();
        Time timeValues = order.getTimeValues();

        // Something is wrong if there is no timeValues,
        // if there is no time to set to, and if there is already an actual delivery time
        if (timeValues == null || updateToDeliveredRequest.getActualDeliveryTime() == null
            || timeValues.getActualDeliveryTime() != null) {
            return Optional.empty();
        }

        // set the timeValues of the order object
        OffsetDateTime deliveredTime = updateToDeliveredRequest.getActualDeliveryTime();
        Time newTimeValues = timeValues.actualDeliveryTime(deliveredTime);

        order.setStatus(Order.StatusEnum.DELIVERED);
        order.setTimeValues(newTimeValues);
        orderRepo.saveAndFlush(order);
        return Optional.of(order);
    }

    /**
     * Adds an exception to the DeliveryExceptionRepo.
     *
     * @return the HttpStatus of the request
     */
    public Optional<DeliveryException> addDeliveryException(DeliveryException e) {

        if (e == null) {
            return Optional.empty();
        }

        return Optional.of(exceptionRepo.saveAndFlush(e));
    }
}
