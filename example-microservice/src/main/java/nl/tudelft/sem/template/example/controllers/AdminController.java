package nl.tudelft.sem.template.example.controllers;

import static nl.tudelft.sem.template.example.authorization.AuthorizationService.doesNotHaveAuthority;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import nl.tudelft.sem.template.api.AdminApi;
import nl.tudelft.sem.template.example.authorization.AuthorizationService;
import nl.tudelft.sem.template.example.domain.admin.AdminService;
import nl.tudelft.sem.template.model.DeliveryException;
import nl.tudelft.sem.template.model.Order;
import nl.tudelft.sem.template.model.Vendor;
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
@RequestMapping("/admin")
public class AdminController implements AdminApi {
    public AdminService adminService;
    public AuthorizationService authorizationService;

    public AdminController(AdminService adminService, AuthorizationService authorizationService) {
        this.adminService = adminService;
        this.authorizationService = authorizationService;
    }

    /**
     * This is a way where the admin can mark an exception as "resolved" by changing the field
     * PUT /admin/exceptions/{orderId} : Update exception for a specific order.
     * Update exception information for a specific order. To be used by admin.
     *
     * @param orderId           (required)
     * @param authorization     The userId to check if they have the rights to make this request (required)
     * @param deliveryException (optional)
     * @return Successful response, exception for the specific order updated (status code 200)
     *         or Unsuccessful, specific exception cannot be updated because of a bad request (status code 400)
     *         or Unsuccessful, entity does not have access rights to update specific exception (status code 403)
     *         or Unsuccessful, no specific exception was found (status code 404)
     */
    @Override
    @PutMapping("/exceptions/{orderId}")
    public ResponseEntity<Void> updateException(@PathVariable("orderId") Long orderId,
                                                @RequestParam(value = "authorization") Long authorization,
                                                @RequestBody(required = false) DeliveryException deliveryException) {
        var auth = authorizationService.authorizeAdminOnly(authorization);
        if (auth.isPresent()) {
            return auth.get();
        }

        if (!adminService.doesExceptionExist(deliveryException)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        var saved = adminService.updateException(deliveryException, orderId);

        if (saved.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * GET /admin/exceptions : Retrieve all exceptions.
     * Return a list of all exceptions collected from orders. To be used by admin.
     *
     * @param authorization the userId to check if they have the rights to make this request (required)
     * @return Successful response, all exceptions received (status code 200)
     *         or Unsuccessful, exceptions cannot be retrieved because of a bad request (status code 400)
     *         or Unsuccessful, entity does not have access rights to retrieve exceptions (status code 403)
     *         or Unsuccessful, no exceptions were found (status code 404)
     */
    @Override
    @GetMapping("/exceptions")
    public ResponseEntity<List<DeliveryException>> getExceptions(
        @RequestParam(value = "authorization") Long authorization) {
        var auth = authorizationService.authorizeAdminOnly(authorization);
        if (auth.isPresent()) {
            return auth.get();
        }

        List<DeliveryException> all = adminService.getAllExceptions();

        if (all.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(all, HttpStatus.OK);
    }

    /**
     * POST /admin/exceptions/{orderId} : Add exception for a specific order.
     * Add exception information for a specific order. To be used by admin.
     *
     * @param orderId           (required)
     * @param authorization     The userId to check if they have the rights to make this request (required)
     * @param deliveryException (optional)
     * @return Successful response, exception for the specific order added (status code 200)
     *         or Unsuccessful, specific exception cannot be added because of a bad request (status code 400)
     *         or Unsuccessful, entity does not have access rights to add specific exception (status code 403)
     *         or Unsuccessful, no specific exception was found (status code 404)
     */
    @Override
    @PostMapping("/exceptions/{orderId}")
    public ResponseEntity<Void> makeException(@PathVariable("orderId") Long orderId,
                                              @RequestParam(value = "authorization") Long authorization,
                                              @RequestBody(required = false) DeliveryException deliveryException) {

        var auth = authorizationService.authorizeAdminOnly(authorization);
        if (auth.isPresent()) {
            return auth.get();
        }


        Optional<DeliveryException> res = adminService.makeException(deliveryException, orderId);

        if (res.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * GET /admin/exceptions/{orderId} : Retrieve exception for a specific order.
     * Return exception information for a specific order. To be used by admin.
     *
     * @param orderId       (required)
     * @param authorization the userId to check if they have the rights to make this request (required)
     * @return Successful response, exception for the specific order received (status code 200)
     *         or Unsuccessful, specific exception cannot be retrieved because of a bad request (status code 400)
     *         or Unsuccessful, entity does not have access rights to retrieve specific exception (status code 403)
     *         or Unsuccessful, no specific exception was found (status code 404)
     */
    @Override
    @GetMapping("/exceptions/{orderId}")
    public ResponseEntity<DeliveryException> getExceptionForOrder(@PathVariable("orderId") Long orderId,
                                                                  @RequestParam(value = "authorization")
                                                                  Long authorization) {
        var auth = authorizationService.authorizeAdminOnly(authorization);
        if (auth.isPresent()) {
            return auth.get();
        }

        Optional<DeliveryException> exception = adminService.getExceptionByOrder(orderId);

        return exception.map(e -> new ResponseEntity<>(e, HttpStatus.OK))
            .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));

    }

    /**
     * PUT /admin/vendor/radius : Update the default radius for vendors that don&#39;t have their own couriers
     * (independent vendors)
     * Update the default radius of all independent vendors. To be used by admin.
     *
     * @param authorization The userId to check if they have the rights to make this request (required)
     * @param body          Radius in meters to be set as a new value (required)
     * @return Successful response, default radius of vendors with independent couriers updated (status code 200)
     *         or Unsuccessful, default radius cannot be updated because of a bad request (status code 400)
     *         or Unsuccessful, entity does not have access rights to update radius. Only admins have this right.
     *         (status code 403)
     *         or Unsuccessful, no default radius was found (status code 404)
     */
    @Override
    @PutMapping("/vendor/radius")
    public ResponseEntity<Void> updateDefaultRadius(
        @RequestParam(name = "authorization") Long authorization,
        @RequestBody Double body) {

        var auth = authorizationService.authorizeAdminOnly(authorization);
        if (auth.isPresent()) {
            return auth.get();
        }
        Optional<List<Vendor>> res = adminService.updateDefaultRadius(body);

        if (res.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * GET /admin/vendor/radius : Retrieve the default radius for vendors that don&#39;t have their own couriers
     * (independent vendors)
     * Return the default radius of all independent vendors. To be used by admin.
     *
     * @param authorization the userId to check if they have the rights to make this request (required)
     * @return Successful response, default radius of vendors with independent couriers received (status code 200)
     *         or Unsuccessful, default radius cannot be retrieved because of a bad request (status code 400)
     *         or Unsuccessful, entity does not have access rights to retrieve specific radius (status code 403)
     *         or Unsuccessful, no specific radius was found (status code 404)
     */
    @Override
    @GetMapping("/vendor/radius")
    public ResponseEntity<Double> getDefaultRadius(
        @RequestParam(name = "authorization") Long authorization) {
        var auth = authorizationService.authorizeAdminOnly(authorization);
        if (auth.isPresent()) {
            return auth.get();
        }

        Optional<Double> res = adminService.getDefaultRadius();

        return res.map(dub -> new ResponseEntity<>(dub, HttpStatus.OK))
            .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));

    }


    /**
     * GET /admin/orders/status/delivered : Retrieve the orders that have been delivered.
     * Return a list of orders with a status of delivered. To be used by admin.
     *
     * @param authorization The userId to check if they have the rights to make this request (required)
     * @return Successful response, delivered orders received by admin (status code 200)
     *         or Unsuccessful, delivered orders cannot be retrieved because of a bad request (status code 400)
     *         or Unsuccessful, entity does not have access rights to retrieve delivered orders (status code 403)
     *         or Unsuccessful, no delivered orders were found (status code 404)
     */
    @Override
    @GetMapping("/orders/status/delivered")
    public ResponseEntity<List<Order>> getDeliveredOrders(
        @RequestParam(name = "authorization") Long authorization
    ) {
        var auth = authorizationService.authorizeAdminOnly(authorization);
        if (doesNotHaveAuthority(auth)) {
            return auth.get();
        }

        Optional<List<Order>> res = adminService.getDelivered();

        return res.map(orders -> new ResponseEntity<>(orders, HttpStatus.OK))
            .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));

    }

    /**
     * GET /admin/analytics/courier-efficiency : Retrieve the efficiencies of all couriers.
     * Return the couriers efficiencies
     *
     * @param authorization The userId to check if they have the rights to make this request (required)
     * @return Successful response, courier efficiencies received by admin (status code 200)
     *         or Unsuccessful, courier efficiencies cannot be retrieved because of a bad request (status code 400)
     *         or Unsuccessful, entity does not have access rights to retrieve courier efficiencies (status code 403)
     *         or Unsuccessful, no courier efficiencies were found (status code 404)
     */
    @Override
    @GetMapping("/analytics/courier-efficiency")
    public ResponseEntity<Map<String, Double>> getCourierEfficiencies(
        @RequestParam(name = "authorization") Long authorization
    ) {
        var auth = authorizationService.authorizeAdminOnly(authorization);
        if (doesNotHaveAuthority(auth)) {
            return auth.get();
        }

        Optional<Map<String, Double>> res = adminService.getCouriersEfficiencies();

        return res.map(stringDoubleMap -> new ResponseEntity<>(stringDoubleMap, HttpStatus.OK))
            .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));

    }

    /**
     * GET /admin/analytics/delivery-times : Retrieve a list of all delivery times.
     * Return delivery times of all of the orders. The difference in time between ETA and actual time arrival.
     *
     * @param authorization the userId to check if they have the rights to make this request, only admin is allowed.
     *                     (required)
     * @return Successful response, delivery times received (status code 200)
     *         or Unsuccessful, delivery times cannot be retrieved because of a bad request (status code 400)
     *         or Unsuccessful, entity does not have access rights to retrieve delivery times (status code 403)
     *         or Unsuccessful, delivery times were not found (status code 404)
     */
    @Override
    @GetMapping("/analytics/delivery-times")
    public ResponseEntity<List<String>> getAllDeliveryTimes(
        @RequestParam(name = "authorization") Long authorization) {

        var auth = authorizationService.authorizeAdminOnly(authorization);
        if (doesNotHaveAuthority(auth)) {
            return auth.get();
        }

        Optional<List<String>> deliveryTimes = adminService.getAllDeliveryTimes();

        return deliveryTimes.map(strings -> new ResponseEntity<>(strings, HttpStatus.OK))
            .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * GET /admin/analytics/all-ratings : Retrieve a list of all ratings.
     * Return rating of all the orders.
     *
     * @param authorization the userId to check if they have the rights to make this request, only admin is allowed.
     *                     (required)
     * @return Successful response, ratings received (status code 200)
     *         or Unsuccessful, ratings cannot be retrieved because of a bad request (status code 400)
     *         or Unsuccessful, entity does not have access rights to retrieve ratings (status code 403)
     *         or Unsuccessful, ratings were not found (status code 404)
     */
    @Override
    @GetMapping("/analytics/all-ratings")
    public ResponseEntity<List<BigDecimal>> getAllRatings(
        @RequestParam(name = "authorization") Long authorization) {

        var auth = authorizationService.authorizeAdminOnly(authorization);
        if (doesNotHaveAuthority(auth)) {
            return auth.get();
        }

        Optional<List<BigDecimal>> ratingsList = adminService.getAllRatings();

        return ratingsList.map(ratings -> new ResponseEntity<>(ratings, HttpStatus.OK))
            .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
}
