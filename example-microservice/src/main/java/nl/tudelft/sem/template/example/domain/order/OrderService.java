package nl.tudelft.sem.template.example.domain.order;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import nl.tudelft.sem.template.example.domain.user.CourierRepository;
import nl.tudelft.sem.template.example.domain.user.VendorRepository;
import nl.tudelft.sem.template.example.externalservices.NavigationMock;
import nl.tudelft.sem.template.model.Courier;
import nl.tudelft.sem.template.model.Location;
import nl.tudelft.sem.template.model.Order;
import nl.tudelft.sem.template.model.Time;
import nl.tudelft.sem.template.model.Vendor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final OrderRepository orderRepo;
    private final VendorRepository vendorRepo;
    private final NavigationMock navigationMock;
    private final CourierRepository courierRepo;


    /**
     * Order Service Constructor.
     *
     * @param orderRepo repo for orders
     * @param vendorRepo repo for vendors
     * @param courierRepo repo for couriers
     */
    @Autowired
    public OrderService(OrderRepository orderRepo, VendorRepository vendorRepo, CourierRepository courierRepo) {
        this.vendorRepo = vendorRepo;
        this.orderRepo = orderRepo;
        this.courierRepo = courierRepo;
        this.navigationMock = new NavigationMock();
    }

    /**
     * Attempts to return an optional of final delivery destination location of the given order with the order id.
     * Couriers use this.
     *
     * @param orderId the id of the order
     * @return the optional of location object, empty if the order was not found
     */
    public Optional<Location> getFinalDestinationOfOrder(Long orderId) {
        Optional<Order> order = orderRepo.findById(orderId);

        return order.map(Order::getDeliveryDestination);
    }

    /**
     * Gets the pickup destination per order, uses the vendor id to get the location of the vendor,
     * which is the pickup destination.
     *
     * @param orderId the id of the order
     * @return empty optional if either order, vendor, or location DNE, optional of location otherwise
     */
    public Optional<Location> getPickupDestination(Long orderId) {
        Optional<Order> order = orderRepo.findById(orderId);

        // if there is no found order
        if (order.isEmpty()) {
            return Optional.empty();
        }

        Optional<Vendor> vendor = vendorRepo.findById(order.get().getVendorId());
        // if the vendor does not exist
        if (vendor.isEmpty()) {
            return Optional.empty();
        }

        Location vendorLocation = vendor.get().getLocation();
        // if there is no location for whatever reason
        if (vendorLocation == null) {
            return Optional.empty();
        }

        return Optional.of(vendorLocation);
    }

    /**
     * Returns the time value object of an order.
     *
     * @param orderId the id of the order to get the values from
     * @return optional of time object, empty if the order does not exist or values not found
     */
    public Optional<Time> getTimeValuesForOrder(Long orderId) {
        Optional<Order> order = orderRepo.findById(orderId);

        // oh no order is not found
        if (order.isEmpty()) {
            return Optional.empty();
        }

        Time timeValues = order.get().getTimeValues();

        // oh no something went wrong and there is no object
        if (timeValues == null) {
            return Optional.empty();
        }

        return Optional.of(order.get().getTimeValues());
    }


    /**
     * Returns true if an order if it exists.
     *
     * @param orderId the id of the order to retrieve
     * @return boolean
     */
    public Boolean orderExists(Long orderId) {
        return orderRepo.existsById(orderId);
    }

    /**
     * Gets the order based on id.
     *
     * @param orderId the id of the order
     * @return empty optional if order  DNE, optional of order otherwise
     */
    public Optional<Order> getOrderById(Long orderId) {
        return orderRepo.findById(orderId);
    }

    /**
     * Updated the order based on id and updated object.
     *
     * @param orderId the id of the order
     * @param order   the updated order object
     * @return empty optional if order  DNE, optional of order otherwise
     */
    public Optional<Order> updateOrderById(Long orderId, Order order) {
        Optional<Order> o = orderRepo.findById(orderId);
        if (o.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(orderRepo.saveAndFlush(order));
    }

    /**
     * Gets all orders.
     *
     * @return empty optional if no order exists, optional of list of order otherwise
     */
    public Optional<List<Order>> getOrders() {
        List<Order> o = orderRepo.findAll();
        if (o.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(o);
    }

    /**
     * Creates new order.
     *
     * @return optional of order
     */
    public Optional<Order> createOrder(Order order) {
        return Optional.of(orderRepo.saveAndFlush(order));
    }

    /**
     * Gets the rating per order, uses the order id to get the rating of the order.
     *
     * @param orderId the id of the order
     * @return empty optional if either order DNE, optional of rating otherwise
     */
    public Optional<BigDecimal> getRating(Long orderId) {
        Optional<Order> order = orderRepo.findById(orderId);

        if (order.isEmpty()) {
            return Optional.empty();
        }

        if (order.get().getRatingNumber() == null) {
            return Optional.empty();
        }

        return Optional.of(order.get().getRatingNumber());
    }

    /**
     * Update the rating per order, uses the order id to get the rating of the order and
     * updates it using the body parameter provided in the signature.
     *
     * @param orderId the id of the order
     * @param body    the new rating that the order will have
     * @return empty optional if order DNE, optional of rating otherwise
     */
    public Optional<BigDecimal> updateRating(Long orderId, BigDecimal body) {
        Optional<Order> order = orderRepo.findById(orderId);

        if (order.isEmpty()) {
            return Optional.empty();
        }

        Order newOrder = order.get();
        newOrder.setRatingNumber(body);
        orderRepo.saveAndFlush(newOrder);

        return Optional.of(body);
    }

    /**
     * Update the rating per order, uses the order id to get the rating of the order and
     * updates it using the body parameter provided in the signature.
     *
     * @param orderId the id of the order
     * @param body    the new preparation time that the order will have
     * @return empty optional if the order DNE, optional of prepTime otherwise
     */
    public Optional<String> updatePrepTime(Long orderId, String body) {
        Optional<Order> order = orderRepo.findById(orderId);

        if (order.isEmpty()) {
            return Optional.empty();
        }

        Order o = order.get();
        if (o.getTimeValues() == null) {
            o.setTimeValues(new Time());
        }
        Time timeOfOrder = o.getTimeValues();
        timeOfOrder.setPrepTime(body);

        orderRepo.saveAndFlush(o);

        return Optional.of(body);
    }

    /**
     * Gets the ETA.
     *
     * @param orderId id of the order
     * @return Updated Order
     */
    public Optional<OffsetDateTime> getEta(Long orderId) {
        Optional<Order> order = orderRepo.findById(orderId);

        if (order.isEmpty()) {
            return Optional.empty();
        }

        Order orderObject = order.get();
        Time time = orderObject.getTimeValues();

        // if we cannot calculate ETA, return empty
        if (time == null || time.getPrepTime() == null) {
            return Optional.empty();
        }

        // if ETA did not exist, calculate it and persist it
        if (time.getExpectedDeliveryTime() == null) {
            OffsetDateTime eta = navigationMock.getEta(orderId, time);

            time.setExpectedDeliveryTime(eta);
            orderObject.setTimeValues(time);
            orderRepo.saveAndFlush(orderObject);
        }

        return Optional.of(orderObject.getTimeValues().getExpectedDeliveryTime());
    }

    /**
     * Gets the distance.
     *
     * @param orderId id of the order
     * @return the distance
     */
    public Optional<Float> getDistance(Long orderId) {
        Optional<Order> order = orderRepo.findById(orderId);

        // does order have a delivery destination and a courier id?
        if (order.isEmpty() || order.get().getDeliveryDestination() == null || order.get().getCourierId() == null) {
            return Optional.empty();
        }

        Long courierId = order.get().getCourierId();
        Optional<Courier> courier = courierRepo.findById(courierId);

        // id there a courier with a location?
        if (courier.isEmpty() || courier.get().getCurrentLocation() == null) {
            return Optional.empty();
        }

        Location courierLocation = courier.get().getCurrentLocation();
        Location deliveryLocation = order.get().getDeliveryDestination();

        return Optional.of(navigationMock.getDistance(courierLocation, deliveryLocation));
    }

    /**
     * Update the courier of the order.
     *
     * @param orderId   the id of the order
     * @param courierId the new courier of the order
     * @return empty optional if either order DNE, optional of updated order otherwise
     */
    public Optional<Order> updateCourier(Long orderId, Long courierId) {
        Optional<Order> order = orderRepo.findById(orderId);

        if (order.isEmpty()) {
            return Optional.empty();
        }

        Order o = order.get();
        o.setCourierId(courierId);

        return Optional.of(orderRepo.saveAndFlush(o));
    }

    /**
     * gets order location.
     *
     * @param order order to retrieve location from
     * @return Location of order
     */
    public Optional<Location> getOrderLocation(Order order) {
        Order.StatusEnum status = order.getStatus();

        Optional<Vendor> v = vendorRepo.findById(order.getVendorId());
        if (v.isEmpty()) {
            return Optional.empty();
        }
        Location vendorLocation = v.get().getLocation();

        Optional<Courier> c = courierRepo.findById(order.getCourierId());
        Location courierLocation = null;
        if (c.isPresent()) {
            courierLocation = c.get().getCurrentLocation();
        }
        switch (status) {
            case ACCEPTED, PREPARING -> {
                return Optional.of(vendorLocation);
            }
            case GIVEN_TO_COURIER, IN_TRANSIT -> {
                if (courierLocation == null) {
                    return Optional.empty();
                }
                return Optional.of(courierLocation);
            }
            case DELIVERED -> {
                return Optional.of(order.getDeliveryDestination());
            }
            default -> {
                return Optional.empty();
            }
        }
    }

    /**
     * Updates the location of an order.
     *
     * @param order order to update
     * @param location location to be updated to
     * @return the new location as an optional
     */
    public Optional<Location> updateLocation(Order order, Location location) {
        Order.StatusEnum status = order.getStatus();

        Optional<Courier> c = courierRepo.findById(order.getCourierId());
        if (c.isEmpty()) {
            return Optional.empty();
        }
        Courier courier = c.get();
        switch (status) {
            case GIVEN_TO_COURIER, IN_TRANSIT -> {
                courier.setCurrentLocation(location);
                courierRepo.saveAndFlush(courier);
                return Optional.of(courier.getCurrentLocation());
            }
            default -> {
                return Optional.empty();
            }
        }
    }
}
