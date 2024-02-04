package nl.tudelft.sem.template.example.controllers;

import static nl.tudelft.sem.template.example.authorization.AuthorizationService.doesNotHaveAuthority;

import java.util.Optional;
import javax.validation.Valid;
import nl.tudelft.sem.template.api.StatusApi;
import nl.tudelft.sem.template.example.authorization.AuthorizationService;
import nl.tudelft.sem.template.example.domain.order.OrderService;
import nl.tudelft.sem.template.example.domain.order.StatusService;
import nl.tudelft.sem.template.model.DeliveryException;
import nl.tudelft.sem.template.model.Order;
import nl.tudelft.sem.template.model.UpdateToDeliveredRequest;
import nl.tudelft.sem.template.model.UpdateToGivenToCourierRequest;
import nl.tudelft.sem.template.model.UpdateToPreparingRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/status")
public class StatusController implements StatusApi {

    private final AuthorizationService authorizationService;
    public StatusService statusService;
    public OrderService orderService;

    /**
     * Status Controller constructor.
     *
     * @param statusService service for status
     * @param orderService service for orders
     * @param authorizationService authorization
     */
    public StatusController(StatusService statusService, OrderService orderService,
                            AuthorizationService authorizationService) {
        this.statusService = statusService;
        this.orderService = orderService;
        this.authorizationService = authorizationService;
    }

    /**
     * Handles put request for (/status/{orderId}/accepted).
     *
     * @param authorization The userId to check if they have the rights to make this request (required)
     * @param orderId       id of the order to update its status to accepted (required)
     * @return a response entity with nothing,
     *         400 if previous status doesn't match method,
     *         404 if not found,
     *         403 if not authorized,
     *         500 if server error,
     *         only for vendors
     */
    @Override
    @PutMapping("/{orderId}/accepted")
    public ResponseEntity<Void> updateToAccepted(
        @PathVariable(name = "orderId") Long orderId,
        @RequestParam(name = "authorization") Long authorization) {

        var auth = authorizationService.checkIfUserIsAuthorized(authorization, "updateToAccepted", orderId);
        if (doesNotHaveAuthority(auth)) {
            return auth.get();
        }

        Optional<ResponseEntity<Void>> checkPrev = checkPrevStatus(orderId, Order.StatusEnum.PENDING);
        // if present, then the previous status was not right and we cannot accept order
        if (checkPrev.isPresent()) {
            return checkPrev.get();
        }

        Optional<Order> order = statusService.updateStatusToAccepted(orderId);

        if (order.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Handles put request for (/status/{orderId}/rejected).
     *
     * @param authorization The userId to check if they have the rights to make this request (required)
     * @param orderId       id of the order to update its status to accepted (required)
     * @return a response entity with nothing, 404 if not found  403 if not authorized, only for vendors
     */
    @Override
    @PutMapping("/{orderId}/rejected")
    public ResponseEntity<Void> updateToRejected(
        @PathVariable(name = "orderId") Long orderId,
        @RequestParam(name = "authorization") Long authorization
    ) {

        var auth = authorizationService.checkIfUserIsAuthorized(authorization, "updateToRejected", orderId);
        if (doesNotHaveAuthority(auth)) {
            return auth.get();
        }

        Optional<Order.StatusEnum> currentStatus = statusService.getOrderStatus(orderId);

        if (currentStatus.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        if (currentStatus.get() != Order.StatusEnum.PENDING) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        Optional<Order> order = statusService.updateStatusToRejected(orderId);
        if (order.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        DeliveryException e = new DeliveryException().isResolved(false)
            .exceptionType(DeliveryException.ExceptionTypeEnum.REJECTED)
            .order(order.get()).message("Order was rejected by the vendor");

        statusService.addDeliveryException(e);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Handles put request for (/status/{orderId}/giventocourier).
     *
     * @param orderId       id of the order to update its status to given_to_courier (required)
     * @param authorization The userId to check if they have the rights to make this request (required)
     * @return a response entity with nothing,
     *         400 if previous status doesn't match method,
     *         404 if not found,
     *         403 if not authorized,
     *         500 if server error,
     *         only for vendors
     */
    @Override
    @PutMapping("/{orderId}/giventocourier")
    public ResponseEntity<Void> updateToGivenToCourier(
        @PathVariable(name = "orderId") Long orderId,
        @RequestParam(name = "authorization") Long authorization,
        @RequestBody UpdateToGivenToCourierRequest updateToGivenToCourierRequest) {

        var auth = authorizationService.checkIfUserIsAuthorized(authorization,
                "updateToGivenToCourier", orderId);
        if (doesNotHaveAuthority(auth)) {
            return auth.get();
        }

        Optional<ResponseEntity<Void>> checkValue = checkPrevStatus(orderId, Order.StatusEnum.PREPARING);
        // if the checkValue is present, the previous status does not match
        if (checkValue.isPresent()) {
            return checkValue.get();
        }

        Optional<Order> order = statusService.updateStatusToGivenToCourier(orderId, updateToGivenToCourierRequest);

        if (order.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Helper method for updating status objects, checks if the previous status is the expected one.
     *
     * @param orderId          the id of the order
     * @param expectedPrevious the expected status value
     * @return Optional of empty if everything is fine,
     *         NOT_FOUND if there is no previous order,
     *         BAD_REQUEST if the previous status does not match the expected value
     */
    private Optional<ResponseEntity<Void>> checkPrevStatus(Long orderId, Order.StatusEnum expectedPrevious) {
        Optional<Order.StatusEnum> currentStatus = statusService.getOrderStatus(orderId);

        if (currentStatus.isEmpty()) {
            return Optional.of(new ResponseEntity<>(HttpStatus.NOT_FOUND));
        }

        if (currentStatus.get() != expectedPrevious) {
            return Optional.of(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
        }
        return Optional.empty();
    }


    /**
     * Handles put request for (/status/{orderId}/intransit).
     *
     * @param authorization The userId to check if they have the rights to make this request (required)
     * @param orderId       id of the order to update its status to in_transit (required)
     * @return a response entity with nothing,
     *         400 if previous status doesn't match method,
     *         404 if not found,
     *         403 if not authorized,
     *         500 if server error,
     *         only for couriers
     */
    @Override
    @PutMapping("/{orderId}/intransit")
    public ResponseEntity<Void> updateToInTransit(
        @PathVariable(name = "orderId") Long orderId,
        @RequestParam(name = "authorization") Long authorization) {

        var auth = authorizationService.checkIfUserIsAuthorized(authorization, "updateToInTransit", orderId);
        if (doesNotHaveAuthority(auth)) {
            return auth.get();
        }

        Optional<ResponseEntity<Void>> checkValue = checkPrevStatus(orderId, Order.StatusEnum.GIVEN_TO_COURIER);
        // if the checkValue is present, the previous status does not match
        if (checkValue.isPresent()) {
            return checkValue.get();
        }

        Optional<Order> order = statusService.updateStatusToInTransit(orderId);

        if (order.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * PUT /status/{orderId}/preparing : Change the order status from accepted to preparing
     * Update the order status to preparing.
     *
     * @param orderId                  Id of order to change status (required)
     * @param authorization            The userId to check if they have the rights to make a request (required)
     * @param updateToPreparingRequest Request body for status change from accepted to preparing (required)
     * @return Successful update of status to preparing (status code 200)
     *         or Invalid arguments (status code 400)
     *         or Order not found (status code 404)
     *         or Unauthorized (status code 403)
     */
    @Override
    @PutMapping("/{orderId}/preparing")
    public ResponseEntity<Void> updateToPreparing(
            @PathVariable(name = "orderId") Long orderId,
            @RequestParam(name = "authorization") Long authorization,
            @Valid @RequestBody UpdateToPreparingRequest updateToPreparingRequest) {

        var auth = authorizationService.checkIfUserIsAuthorized(authorization,
            "updateToPreparing", orderId);
        if (doesNotHaveAuthority(auth)) {
            return auth.get();
        }

        Optional<ResponseEntity<Void>> checkValue = checkPrevStatus(orderId, Order.StatusEnum.ACCEPTED);
        // if the checkValue is present, the previous status does not match
        if (checkValue.isPresent()) {
            return checkValue.get();
        }


        Optional<Order> order = statusService.updateStatusToPreparing(orderId, updateToPreparingRequest);

        if (order.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * PUT for /status/{orderId}/delivered.
     *
     * @param orderId                  Id of the order to change status (required)
     * @param authorization            the UserId to check if they have the rights to make this request (required)
     * @param updateToDeliveredRequest Request body for status change from in-transit to delivered (required)
     * @return a response entity with nothing,
     *         400 if previous status doesn't match method or the given variables are wrong,
     *         404 if not found,
     *         403 if not authorized,
     *         500 if server error,
     *         only for couriers
     */
    @Override
    @PutMapping("/{orderId}/delivered")
    public ResponseEntity<Void> updateToDelivered(
            @PathVariable("orderId") Long orderId,
            @RequestParam(value = "authorization") Long authorization,
            @Valid @RequestBody UpdateToDeliveredRequest updateToDeliveredRequest) {
        var auth = authorizationService.checkIfUserIsAuthorized(authorization, "updateToDelivered", orderId);
        if (doesNotHaveAuthority(auth)) {
            return auth.get();
        }

        if (!orderService.orderExists(orderId)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Optional<ResponseEntity<Void>> checkValue = checkPrevStatus(orderId, Order.StatusEnum.IN_TRANSIT);
        // if the checkValue is present, the previous status does not match
        if (checkValue.isPresent()) {
            return checkValue.get();
        }

        Optional<Order> updatedTime = statusService.updateStatusToDelivered(orderId, updateToDeliveredRequest);

        if (updatedTime.isEmpty()) {
            // something went wrong with the specific fields and the logic inside the service method :(
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Handles get request for (/status/{orderId}).
     *
     * @param authorization The userId to check if they have the rights to make this request (required)
     * @param orderId       id of the order to update its status to accepted (required)
     * @return a response entity with a status String,
     *         404 if not found,
     *         403 if not authorized,
     *         500 if server error,
     *         for customers, vendors, couriers
     */

    @Override
    @GetMapping("/{orderId}")
    public ResponseEntity<String> getStatus(
        @PathVariable(name = "orderId") Long orderId,
        @RequestParam(name = "authorization") Long authorization
    ) {

        var auth = authorizationService.checkIfUserIsAuthorized(authorization, "getStatus", orderId);
        if (doesNotHaveAuthority(auth)) {
            return auth.get();
        }

        Optional<Order.StatusEnum> currentStatus = statusService.getOrderStatus(orderId);

        return currentStatus.map(statusEnum -> new ResponseEntity<>(statusEnum.toString(), HttpStatus.OK))
            .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));

    }


}
