package nl.tudelft.sem.template.example.controllers;

import static nl.tudelft.sem.template.example.authorization.AuthorizationService.doesNotHaveAuthority;

import io.swagger.v3.oas.annotations.Parameter;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import lombok.Getter;
import lombok.Setter;
import nl.tudelft.sem.template.api.OrderApi;
import nl.tudelft.sem.template.example.authorization.AuthorizationService;
import nl.tudelft.sem.template.example.domain.order.OrderRepository;
import nl.tudelft.sem.template.example.domain.order.OrderService;
import nl.tudelft.sem.template.example.domain.order.orderstrategy.GeneralOrdersStrategy;
import nl.tudelft.sem.template.example.domain.order.orderstrategy.NextOrderStrategy;
import nl.tudelft.sem.template.example.domain.order.orderstrategy.OrderPerVendorStrategy;
import nl.tudelft.sem.template.example.domain.user.CourierService;
import nl.tudelft.sem.template.example.domain.user.VendorRepository;
import nl.tudelft.sem.template.model.Courier;
import nl.tudelft.sem.template.model.Location;
import nl.tudelft.sem.template.model.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/order")
public class OrderController implements OrderApi {

    private final OrderService orderService;
    private final AuthorizationService authorizationService;
    private final OrderRepository orderRepository;
    private final VendorRepository vendorRepository;
    private final CourierService courierService;
    @Getter
    @Setter
    private NextOrderStrategy strategy;

    /**
     * OrderController constructor.
     *
     * @param orderService service for orders
     * @param courierService service for couriers
     * @param authorizationService authorization
     * @param orderRepository Repo for orders
     * @param vendorRepository Repo for Vendors
     */
    @Autowired
    public OrderController(OrderService orderService, CourierService courierService,
                           AuthorizationService authorizationService, OrderRepository orderRepository,
                           VendorRepository vendorRepository) {
        this.orderService = orderService;
        this.courierService = courierService;
        this.authorizationService = authorizationService;
        this.orderRepository = orderRepository;
        this.vendorRepository = vendorRepository;
    }


    /**
     * GET /order/{vendorId} : Retrieve the next order that belongs to a given vendor.
     * return the next order object that is assigned to specific vendor.
     *
     * @param vendorId      id of the vendor to retrieve the order from (required)
     * @param authorization The userId to check if they have the rights to make this request (required)
     * @return Successful response, order received (status code 200)
     *         or Unsuccessful, order cannot be retrieved because of bad request (status code 400)
     *         or Unsuccessful, entity does not have access rights to retrieve vendor order (status code 403)
     *         or Unsuccessful, no order was found (status code 404)
     */
    @Override
    @GetMapping("/{vendorId}/get-next-order")
    public ResponseEntity<Order> getNextOrderForVendor(
        @PathVariable("vendorId") Long vendorId,
        @RequestParam(value = "authorization") Long authorization) {

        Optional<Courier> courier = courierService.getCourierById(authorization);

        // extra check outside of authorization added to be able to get the vendor id
        if (courier.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        var auth = authorizationService.checkIfUserIsAuthorized(authorization, "getNextOrderForVendor",
                    courier.get().getBossId());
        if (doesNotHaveAuthority(auth)) {
            return auth.get();
        }

        this.setStrategy(new OrderPerVendorStrategy(orderRepository));
        Optional<List<Order>> orders = strategy.availableOrders(Optional.of(vendorId));

        return getOrderResponseEntity(orders);
    }

    /**
     * Helper method for getNextOrderForVendor,
     * used to evaluate the appropriate response type for given available list of orders.
     *
     * @param orders the optional list of available order gotten from service class
     * @return OK, if there is an available order for that courier,
     *         NOT_FOUND if there were none,
     *         BAD_REQUEST if mismatches between vendor-courier and attributes
     */
    private ResponseEntity<Order> getOrderResponseEntity(Optional<List<Order>> orders) {
        if (orders.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        if (orders.get().isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(orders.get().get(0), HttpStatus.OK);
    }

    /**
     * GET /order/unassigned : Retrieve all independent and unassigned orders.
     * Return a list of all independent and unassigned orders. Independent orders are orders that belong
     * to a vendor without own couriers.
     *
     * @param authorization The userId to check if they have the rights to make this request (required)
     * @return Successful response, independent and unassigned orders received (status code 200)
     *         or Unsuccessful, independent and unassigned orders cannot be retrieved because of a bad request
     *         (status code 400)
     *         or Unsuccessful, entity does not have access rights to retrieve independent and unassigned orders
     *         (status code 403)
     *         or Unsuccessful, no independent and unassigned orders were found (status code 404)
     */
    @Override
    @GetMapping("/unassigned")
    public ResponseEntity<List<Order>> getIndependentOrders(
        @RequestParam(value = "authorization") Long authorization) {
        var auth = authorizationService.checkIfUserIsAuthorized(authorization,
                "getIndependentOrders", authorization);
        if (doesNotHaveAuthority(auth)) {
            return auth.get();
        }

        this.setStrategy(new GeneralOrdersStrategy(orderRepository, vendorRepository));
        Optional<List<Order>> orders = strategy.availableOrders(Optional.empty());

        if (orders.isEmpty() || orders.get().isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(orders.get(), HttpStatus.OK);
    }

    /**
     * GET /order/{orderId}/final-destination : Get the final destination of a specific order.
     * Retrieve the final destination of an order.
     *
     * @param authorization The userId to check if they have the rights to make this request (required)
     * @param orderId       Id of the order to get its final destination (required)
     * @return Successful response, order found and final destination can be retrieved (status code 200)
     *         or Unsuccessful, entity does not have access rights to retrieve final destination (status code 403)
     *         or Unsuccessful, order not found by id (status code 404)
     */
    @Override
    @GetMapping("/{orderId}/final-destination")
    public ResponseEntity<Location> getFinalDestination(
        @RequestParam(name = "authorization") Long authorization,
        @PathVariable(name = "orderId") Long orderId
    ) {
        var auth = authorizationService.checkIfUserIsAuthorized(authorization,
                "getFinalDestination", orderId);
        if (doesNotHaveAuthority(auth)) {
            return auth.get();
        }
        Optional<Location> location = orderService.getFinalDestinationOfOrder(orderId);

        // 404 not found if not found :(
        return location.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
            .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * GET /order/{orderId} : Retrieve order based on orderId.
     * Return the Order object.
     *
     * @param orderId       (required)
     * @param authorization The userId to check if they have the rights to make this request (required)
     * @return Successful response, order object received (status code 200)
     *         or Unsuccessful, order cannot be retrieved because of bad request (status code 400)
     *         or Unsuccessful, entity does not have access rights to retrieve order (status code 403)
     *         or Unsuccessful, no order was found (status code 404)
     */
    @Override
    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(
        @PathVariable(name = "orderId") Long orderId,
        @RequestParam(name = "authorization") Long authorization
    ) {
        var auth = authorizationService.checkIfUserIsAuthorized(authorization, "getOrder", orderId);
        if (doesNotHaveAuthority(auth)) {
            return auth.get();
        }

        Optional<Order> o = orderService.getOrderById(orderId);
        return o.map(order -> new ResponseEntity<>(order, HttpStatus.OK))
            .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * GET /order : Retrieve all orders.
     * Return list of all Order objects.
     *
     * @param authorization The userId to check if they have the rights to make this request (required)
     * @return Successful response, order objects received (status code 200)
     *         or Unsuccessful, orders cannot be retrieved because of bad request (status code 400)
     *         or Unsuccessful, entity does not have access rights to retrieve all orders (status code 403)
     *         or Unsuccessful, no orders were found (status code 404)
     */
    @Override
    @GetMapping("")
    public ResponseEntity<List<Order>> getOrders(@RequestParam(name = "authorization") Long authorization) {
        var auth = authorizationService.authorizeAdminOnly(authorization);
        if (doesNotHaveAuthority(auth)) {
            return auth.get();
        }

        Optional<List<Order>> o = orderService.getOrders();
        return o.map(orders -> new ResponseEntity<>(orders, HttpStatus.OK))
            .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * GET /order/{orderId}/pickup-destination : Retrieve vendor location of the order.
     * Return the vendor location of the specified order.
     *
     * @param orderId       (required)
     * @param authorization The userId to check if they have the rights to make this request (required)
     * @return Successful response, vendor location of the order received (status code 200)
     *         or Unsuccessful, vendor location of the order cannot be retrieved because of a bad request
     *         (status code 400)
     *         or Unsuccessful, entity does not have access rights to retrieve vendor location (status code 403)
     *         or Unsuccessful, vendor location for the order not found (status code 404)
     */
    @Override
    @GetMapping("/{orderId}/pickup-destination")
    public ResponseEntity<Location> getPickupDestination(
        @PathVariable(name = "orderId") Long orderId,
        @RequestParam(name = "authorization") Long authorization
    ) {
        var auth = authorizationService.checkIfUserIsAuthorized(authorization,
                "getPickupDestination", orderId);
        if (doesNotHaveAuthority(auth)) {
            return auth.get();
        }

        Optional<Location> pickup = orderService.getPickupDestination(orderId);

        return pickup.map(location -> new ResponseEntity<>(location, HttpStatus.OK))
            .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * POST /order/{orderId} : Create new order with specified id.
     * Return the response of POST operation.
     *
     * @param orderId       (required)
     * @param authorization The userId to check if they have the rights to make this request (required)
     * @param order         Order object to create
     * @return Successful response, order created (status code 200)
     *         or Unsuccessful, order cannot be added because of bad request (status code 400)
     *         - order with this id already exists
     *         or Unsuccessful, entity does not have access rights to add order (status code 403)
     *         or Unsuccessful, no order was found (status code 404)
     */
    @Override
    @PostMapping("/{orderId}")
    public ResponseEntity<Void> makeOrder(
        @PathVariable(name = "orderId") Long orderId,
        @RequestParam(name = "authorization") Long authorization,
        @Parameter(name = "Order") @RequestBody @Valid Order order
    ) {
        var auth = authorizationService.authorizeAdminOnly(authorization);
        if (doesNotHaveAuthority(auth)) {
            return auth.get();
        }

        Optional<Order> o = orderService.getOrderById(orderId);
        if (o.isPresent()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        orderService.createOrder(order);

        return new ResponseEntity<>(HttpStatus.OK);

    }

    /**
     * PUT /order/{orderId} : Retrieve order based on orderId.
     * Return the status of the update.
     *
     * @param orderId       (required)
     * @param authorization The userId to check if they have the rights to make this request (required)
     * @param order         The updated object of the order
     * @return Successful response, order updated (status code 200)
     *         or Unsuccessful, order cannot be updated because of bad request (status code 400)
     *         or Unsuccessful, entity does not have access rights to update order (status code 403)
     *         or Unsuccessful, no order was found" (status code 404)
     */
    @Override
    @PutMapping("/{orderId}")
    public ResponseEntity<Void> updateOrder(
        @PathVariable(name = "orderId") Long orderId,
        @RequestParam(name = "authorization") Long authorization,
        @Parameter(name = "Order") @RequestBody @Valid Order order) {
        var auth = authorizationService.checkIfUserIsAuthorized(authorization, "updateOrder", orderId);
        if (doesNotHaveAuthority(auth)) {
            return auth.get();
        }

        Optional<Order> response = orderService.updateOrderById(orderId, order);

        if (response.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }


    /**
     * GET /order/{orderId}/rating : Retrieve rating of the order.
     * Return the rating of the specified order.
     *
     * @param authorization Id of the order to update rating (required)
     * @param orderId       The userId to check if they have the rights to make this request (required)
     * @return Successful response, rating of the order received (status code 200)
     *         or Unsuccessful, rating of the order cannot be retrieved because of a bad request (status code 400)
     *         or Unsuccessful, entity does not have access rights to retrieve rating (status code 403)
     *         or Unsuccessful, rating for the order not found (status code 404)
     */
    @Override
    @GetMapping("/{orderId}/rating")
    public ResponseEntity<BigDecimal> getOrderRating(
        @PathVariable(name = "orderId") Long orderId,
        @RequestParam(name = "authorization") Long authorization
    ) {
        var auth = authorizationService.checkIfUserIsAuthorized(authorization, "getOrderRating", orderId);
        if (doesNotHaveAuthority(auth)) {
            return auth.get();
        }

        Optional<BigDecimal> currentRating = orderService.getRating(orderId);

        return currentRating.map(bigDecimal -> new ResponseEntity<>(bigDecimal, HttpStatus.OK))
            .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * PUT /order/{orderId}/rating : Update rating of the order.
     * Update the rating of the specified order and return response.
     *
     * @param authorization Id of the order to update rating (required)
     * @param orderId       The userId to check if they have the rights to make this request (required)
     * @param body          Order object where ratingNumber is updated (required)
     * @return Successful response, rating of the order received (status code 200)
     *         or Unsuccessful, rating of the order cannot be retrieved because of a bad request (status code 400)
     *         or Unsuccessful, entity does not have access rights to retrieve rating (status code 403)
     *         or Unsuccessful, rating for the order not found (status code 404)
     *         only for customers
     */
    @Override
    @PutMapping("/{orderId}/rating")
    public ResponseEntity<Void> putOrderRating(
        @PathVariable(name = "orderId") Long orderId,
        @RequestParam(name = "authorization") Long authorization,
        @RequestBody @Valid BigDecimal body
    ) {
        var auth = authorizationService.checkIfUserIsAuthorized(authorization, "putOrderRating", orderId);
        if (doesNotHaveAuthority(auth)) {
            return auth.get();
        }

        Optional<BigDecimal> newRating = orderService.updateRating(orderId, body);

        if (newRating.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * PUT /order/{orderId}/courier/{courierId} : Update courierId of the order.
     * Update the courier of the order and return response. *
     *
     * @param orderId       id of the order to update rating (required)
     * @param authorization The userId to check if they have the rights to make this request (required)
     * @param courierId     The courierId to update (required)
     * @param order         Order object where courierId is updated (required)
     * @return Successful response, courier id of order set (status code 200)
     *         or Unsuccessful, courier id cannot be updated because of bad request (status code 400)
     *         or Unsuccessful, entity does not have access rights to update courier id (status code 403)
     *         or Unsuccessful, no order or courier id was found (status code 404)
     */
    @Override
    @PutMapping("/{orderId}/courier/{courierId}")
    public ResponseEntity<Void> setCourierId(

        @PathVariable(name = "orderId") Long orderId,
        @PathVariable(name = "courierId") Long courierId,
        @RequestParam(name = "authorization") Long authorization,
        @RequestBody @Valid Order order
    ) {
        var auth = authorizationService.authorizeAdminOnly(authorization);
        if (doesNotHaveAuthority(auth)) {
            return auth.get();
        }

        Optional<Courier> c = courierService.getCourierById(courierId);
        if (c.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Optional<Order> updated = orderService.updateCourier(orderId, courierId);
        if (updated.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * PUT /order/{orderId}/preparation-time : Update prepTime of the order.
     * Update the preparation time of the specified order and return response.
     *
     * @param orderId       id of the order to update (required)
     * @param authorization The userId to check if they have the rights to make this request (required)
     * @param body          New preparation time to be replaced (required)
     * @return Successful response, preparation time of the order updated (status code 200)
     *         or Unsuccessful, preparation time of the order cannot be updated
     *         because of a bad request (status code 400)
     *         or Unsuccessful, entity does not have access rights to update
     *         preparation time (status code 403)
     *         or Unsuccessful, preparation time for the order not found (status code 404)
     */
    @Override
    @PutMapping("/{orderId}/preparation-time")
    public ResponseEntity<Void> setPreparationTime(
        @PathVariable(name = "orderId") Long orderId,
        @RequestParam(name = "authorization") Long authorization,
        @RequestBody @Valid String body
    ) {
        var auth =
            authorizationService.checkIfUserIsAuthorized(authorization, "setDeliverTime", orderId);
        if (doesNotHaveAuthority(auth)) {
            return auth.get();
        }

        Optional<String> newPrepTime = orderService.updatePrepTime(orderId, body);

        if (newPrepTime.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * GET /order/{orderId}/current : Retrieve the current location of the courier with this order.
     * return the location of the courier who has the order corresponding to the id
     *
     * @param orderId       id of the order with the location to retrieve (required)
     * @param authorization The userId to check if they have the rights to make this request (required)
     * @return Successful response, location received (status code 200)
     *         or Unsuccessful, location cannot be retrieved because of bad request (status code 400)
     *         or Unsuccessful, entity does not have access rights to retrieve order location (status code 403)
     *         or Unsuccessful, no location was found (status code 404)
     */
    @Override
    @GetMapping("/{orderId}/current")
    public ResponseEntity<Location> getOrderLocation(
        @PathVariable(name = "orderId") Long orderId,
        @RequestParam(name = "authorization") Long authorization) {
        var auth = authorizationService
            .checkIfUserIsAuthorized(authorization, "getOrderLocation", orderId);
        if (doesNotHaveAuthority(auth)) {
            return auth.get();
        }

        Optional<Order> order = orderService.getOrderById(orderId);
        if (order.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Optional<Location> res = orderService.getOrderLocation(order.get());

        return res.map(location -> new ResponseEntity<>(location, HttpStatus.OK))
            .orElseGet(() -> new ResponseEntity<>(HttpStatus.BAD_REQUEST));
    }

    /**
     * PUT /order/{orderId}/current : Update the current location of the courier with this order.
     * update the location of the courier who has the order corresponding to the id
     *
     * @param orderId       id of the order with the location to update (required)
     * @param authorization The userId to check if they have the rights to make this request (required)
     * @param location      Location object (required)
     * @return Successful response, location updated (status code 200)
     *         or Unsuccessful, location cannot be updated because of bad request (status code 400)
     *         or Unsuccessful, entity does not have access rights to update order location (status code 403)
     *         or Unsuccessful, no location was found (status code 404)
     */
    @Override
    @PutMapping("/{orderId}/current")
    public ResponseEntity<Void> updateLocation(
        @PathVariable(name = "orderId") Long orderId,
        @RequestParam(name = "authorization") Long authorization,
        @RequestBody Location location) {
        var auth = authorizationService
            .checkIfUserIsAuthorized(authorization, "updateLocation", orderId);
        if (doesNotHaveAuthority(auth)) {
            return auth.get();
        }
        Optional<Order> order = orderService.getOrderById(orderId);
        if (order.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Optional<Location> res = orderService.updateLocation(order.get(), location);

        if (res.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(HttpStatus.OK);

    }

    /**
     * GET /order/{orderId}/ETA : Get the ETA of an order.
     * return the ETA of an order
     *
     * @param authorization The userId to check if they have the rights to make this request (required)
     * @param orderId       Id of the order to get it&#39;s ETA (required)
     * @return Successful response, order found (status code 200)
     *         or Invalid arguments (status code 400)
     *         or Unsuccessful, entity does not have access rights to retrieve estimated time of arrival
     *         (status code 403)
     *         or Unsuccessful, order not found (status code 404)
     */
    @Override
    @GetMapping("/{orderId}/ETA")
    public ResponseEntity<OffsetDateTime> getETA(
        @RequestParam(value = "authorization", required = true) Long authorization,
        @PathVariable(name = "orderId") Long orderId) {

        var auth =
            authorizationService.checkIfUserIsAuthorized(authorization, "getETA", orderId);
        if (doesNotHaveAuthority(auth)) {
            return auth.get();
        }

        Optional<OffsetDateTime> eta = orderService.getEta(orderId);
        return eta.map(offsetDateTime -> new ResponseEntity<>(offsetDateTime, HttpStatus.OK))
            .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));

    }

    /**
     * GET /order/{orderId}/distance : Retrieve the current distance to the courier with this order.
     * return the distance (given in meters) between final delivery location and the courier  who has the order
     * corresponding to the id.
     *
     * @param orderId       id of the order with the distance to retrieve (required)
     * @param authorization The userId to check if they have the rights to make this request (required)
     * @return Successful response, distance received (status code 200)
     *         or Unsuccessful, distance cannot be retrieved because of bad request (status code 400)
     *         or Unsuccessful, no distance was found (status code 404)
     *         or Unsuccessful, entity does not have access rights to retrieve order distance (status code 403)
     */
    @Override
    @GetMapping("/{orderId}/distance")
    public ResponseEntity<Float> getOrderDistance(
        @PathVariable(name = "orderId") Long orderId,
        @RequestParam(value = "authorization") Long authorization
    ) {
        var auth =
            authorizationService.checkIfUserIsAuthorized(authorization, "getOrderDistance", orderId);
        if (doesNotHaveAuthority(auth)) {
            return auth.get();
        }

        Optional<Float> distance = orderService.getDistance(orderId);
        return distance.map(flt -> new ResponseEntity<>(flt, HttpStatus.OK))
            .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

}
