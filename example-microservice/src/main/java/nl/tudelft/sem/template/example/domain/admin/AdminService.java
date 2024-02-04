package nl.tudelft.sem.template.example.domain.admin;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import nl.tudelft.sem.template.example.domain.exception.DeliveryExceptionRepository;
import nl.tudelft.sem.template.example.domain.order.OrderRepository;
import nl.tudelft.sem.template.example.domain.user.VendorRepository;
import nl.tudelft.sem.template.model.DeliveryException;
import nl.tudelft.sem.template.model.Order;
import nl.tudelft.sem.template.model.Vendor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

    OrderRepository orderRepo;

    DeliveryExceptionRepository exceptionRepo;

    VendorRepository vendorRepo;

    /**
     * Admin Service constructor.
     *
     * @param vendorRepo repo for vendors
     * @param orderRepo repo for orders
     * @param exceptionRepo repo for exceptions
     */
    @Autowired
    public AdminService(VendorRepository vendorRepo, OrderRepository orderRepo, DeliveryExceptionRepository exceptionRepo) {
        this.exceptionRepo = exceptionRepo;
        this.orderRepo = orderRepo;
        this.vendorRepo = vendorRepo;
    }

    /**
     * updates the Default Radius.
     *
     * @param body new Radius
     * @return changed list of Vendors
     */
    public Optional<List<Vendor>> updateDefaultRadius(Double body) {

        List<Vendor> vendors = vendorRepo.findVendorsByHasCouriers(false);

        if (vendors.isEmpty()) {
            return Optional.empty();
        }
        for (Vendor v : vendors) {
            v.setRadius(body);
            vendorRepo.saveAndFlush(v);
        }

        return Optional.of(vendors);
    }

    /**
     * gets the default radius.
     *
     * @return the default Radius
     */
    public Optional<Double> getDefaultRadius() {
        List<Vendor> vendors = vendorRepo.findVendorsByHasCouriers(false);

        if (vendors.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(vendors.get(0).getRadius());
    }

    /**
     * Get all delivered orders.
     *
     * @return an optional list of all delivered orders
     */
    public Optional<List<Order>> getDelivered() {
        List<Order> orders = orderRepo.findByStatus(Order.StatusEnum.DELIVERED);

        if (orders.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(orders);
    }

    /**
     * Get the exception of an order.
     *
     * @param orderId id of the order
     * @return the optional of th exception, empty if order or exception not found
     */
    public Optional<DeliveryException> getExceptionByOrder(Long orderId) {
        Optional<Order> order = orderRepo.findById(orderId);

        if (order.isEmpty()) {
            return Optional.empty();
        }

        List<DeliveryException> exceptionList = exceptionRepo.findByOrder(order.get());

        if (exceptionList.isEmpty()) {
            return Optional.empty();
        }
        // only get the first exception as there only should be one
        return Optional.of(exceptionList.get(0));
    }

    /**
     * Saves the exception in the database.
     *
     * @param deliveryException the exception to save
     * @return the optional object saved, empty if there is an exception for that order
     */
    public Optional<DeliveryException> makeException(DeliveryException deliveryException, Long orderId) {

        Optional<DeliveryException> empty = validateException(deliveryException, orderId);
        // the order is not valid
        if (empty.isEmpty()) {
            return Optional.empty();
        }

        // can not add an exception if that order already has an exception
        if (exceptionRepo.existsByOrder(deliveryException.getOrder())) {
            return Optional.empty();
        }

        return Optional.of(exceptionRepo.saveAndFlush(deliveryException));
    }

    /**
     * Checks if the exception has valid fields to continue performing operations.
     *
     * @param deliveryException the exception to be checked
     * @param orderId           the id of the related order gathered form the path of the request
     * @return an empty optional if it is not valid
     */
    private Optional<DeliveryException> validateException(DeliveryException deliveryException, Long orderId) {
        if (deliveryException == null) {
            return Optional.empty();
        }

        // can not make an exception that is not linked to an order
        if (deliveryException.getOrder() == null) {
            return Optional.empty();
        }

        // order id on the path does not match order id in the object
        if (!Objects.equals(orderId, deliveryException.getOrder().getId())) {
            return Optional.empty();
        }
        return Optional.of(deliveryException);
    }

    /**
     * Updates an exception.
     *
     * @param deliveryException the exception to be checked
     * @param orderId           the id of the related order gathered form the path of the request
     */
    public Optional<DeliveryException> updateException(DeliveryException deliveryException, Long orderId) {

        Optional<DeliveryException> empty = validateException(deliveryException, orderId);
        // the order is not valid
        if (empty.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(exceptionRepo.saveAndFlush(deliveryException));
    }


    /**
     * Returns all the exceptions stored in the database.
     *
     * @return the list of exceptions, empty if there are none
     */
    public List<DeliveryException> getAllExceptions() {
        return exceptionRepo.findAll();
    }


    /**
     * Checks if the current exception exists by id.
     *
     * @param exception the exception to check
     * @return the boolean value
     */
    public Boolean doesExceptionExist(DeliveryException exception) {
        if (exception == null) {
            return false;
        }

        if (exception.getOrder() == null) {
            return false;
        }
        return exceptionRepo.existsById(exception.getId());
    }

    /**
     * Get all courier efficiencies.
     * The way the method is designed, the couriers that had no orders delivered will
     * not have an efficiency rating.
     * As for the efficiency rating it is based on the difference between the expected time
     * of delivery and the actual time of delivery, thus penalizing a late delivery and
     * rewarding an early one
     *
     * @return map of couriers and their efficiencies
     */
    public Optional<Map<String, Double>> getCouriersEfficiencies() {
        List<Order> orders = orderRepo.findByStatus(Order.StatusEnum.DELIVERED);

        if (orders.isEmpty()) {
            return Optional.empty();
        }

        Set<Long> couriers = orders.stream()
            .map(Order::getCourierId)
            .collect(Collectors.toSet());

        Map<String, Double> res = new HashMap<>();

        for (Long courier : couriers) {
            List<Order> courierOrders = orderRepo.findByCourierIdAndStatus(courier, Order.StatusEnum.DELIVERED);

            double value = 0.0;
            int size = courierOrders.size();

            for (Order o : courierOrders) {
                value += Duration.between(o.getTimeValues().getActualDeliveryTime(),
                    o.getTimeValues().getExpectedDeliveryTime()).getSeconds();
            }

            double result = value / size;

            res.put(courier.toString(), result);
        }

        return Optional.of(res);
    }

    /**
     * gets all the Delivery Times.
     *
     * @return Optional list of delivery Times
     */
    public Optional<List<String>> getAllDeliveryTimes() {
        List<Order> orders = orderRepo.findAll();
        if (orders.isEmpty()) {
            return Optional.empty();
        }

        List<String> collect = orders.stream()
            .filter(order -> order.getTimeValues() != null)
            .filter(order -> order.getTimeValues().getOrderTime() != null
                && order.getTimeValues().getActualDeliveryTime() != null)
            .map(order -> Duration.between(order.getTimeValues().getOrderTime(),
                order.getTimeValues().getActualDeliveryTime()))
            .map(order -> String.format("%d Hours, %d Minutes, %d Seconds",
                order.toHours(), order.minusHours(order.toHours()).toMinutes(),
                order.minusMinutes(order.minusHours(order.toHours()).toMinutes() + 60 * order.toHours())
                    .toSeconds()))
            .collect(Collectors.toList());
        return Optional.of(collect);
    }

    /**
     * gets all the ratings.
     *
     * @return Optional List of Ratings
     */
    public Optional<List<BigDecimal>> getAllRatings() {
        List<Order> orders = orderRepo.findAll();

        if (orders.isEmpty()) {
            return Optional.empty();
        }

        List<BigDecimal> collect = orders.stream()
            .map(Order::getRatingNumber).collect(Collectors.toList());
        return Optional.of(collect);
    }
}
