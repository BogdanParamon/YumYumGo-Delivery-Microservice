package nl.tudelft.sem.template.example.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import nl.tudelft.sem.template.example.authorization.AuthorizationService;
import nl.tudelft.sem.template.example.domain.order.OrderService;
import nl.tudelft.sem.template.example.domain.order.StatusService;
import nl.tudelft.sem.template.model.Order;
import nl.tudelft.sem.template.model.Time;
import nl.tudelft.sem.template.model.UpdateToDeliveredRequest;
import nl.tudelft.sem.template.model.UpdateToGivenToCourierRequest;
import nl.tudelft.sem.template.model.UpdateToPreparingRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;


public class StatusControllerTest {

    private StatusService statusService;
    private AuthorizationService authorizationService;
    private OrderService orderService;
    private StatusController controller;


    @BeforeEach
    void setUp() {
        this.statusService = Mockito.mock(StatusService.class);
        this.authorizationService = Mockito.mock(AuthorizationService.class);
        this.orderService = Mockito.mock(OrderService.class);
        this.controller = new StatusController(statusService, orderService, authorizationService);
    }

    @Test
    void updateStatusToAccepted200() {
        Mockito.when(statusService.getOrderStatus(11L)).thenReturn(
            Optional.of(Order.StatusEnum.PENDING));
        Mockito.when(statusService.updateStatusToAccepted(11L)).thenReturn(
            Optional.of(new Order().id(11L).status(Order.StatusEnum.ACCEPTED)));

        var res = controller.updateToAccepted(11L, 1L);
        assertEquals(new ResponseEntity<>(HttpStatus.OK), res);
    }

    @Test
    void updateStatusToRejected200() {
        Mockito.when(statusService.getOrderStatus(11L)).thenReturn(
            Optional.of(Order.StatusEnum.PENDING));
        Mockito.when(statusService.updateStatusToRejected(11L)).thenReturn(
            Optional.of(new Order().id(11L).status(Order.StatusEnum.REJECTED)));

        var res = controller.updateToRejected(11L, 1L);
        assertEquals(new ResponseEntity<>(HttpStatus.OK), res);
    }

    @Test
    void updateStatusToAccepted404() {
        Mockito.when(statusService.updateStatusToAccepted(anyLong())).thenReturn(Optional.empty());

        var res = controller.updateToAccepted(11L, 1L);
        assertEquals(new ResponseEntity<>(HttpStatus.NOT_FOUND), res);
    }

    @Test
    void updateStatusToAccepted404second() {
        Mockito.when(authorizationService.checkIfUserIsAuthorized(1L, "updateToAccepted", 11L))
            .thenReturn(Optional.empty());
        Mockito.when(statusService.getOrderStatus(11L)).thenReturn(
            Optional.of(Order.StatusEnum.PENDING));
        Mockito.when(statusService.updateStatusToAccepted(11L)).thenReturn(Optional.empty());

        var res = controller.updateToAccepted(11L, 1L);
        assertEquals(new ResponseEntity<>(HttpStatus.NOT_FOUND), res);
    }

    @Test
    void updateStatusToRejected404() {
        Mockito.when(statusService.updateStatusToRejected(anyLong())).thenReturn(Optional.empty());

        var res = controller.updateToRejected(1L, 11L);
        assertEquals(new ResponseEntity<>(HttpStatus.NOT_FOUND), res);
    }

    @Test
    void updateStatusToRejected404second() {
        Mockito.when(authorizationService.checkIfUserIsAuthorized(1L, "updateToRejected", 11L))
            .thenReturn(Optional.empty());
        Mockito.when(statusService.getOrderStatus(11L)).thenReturn(
            Optional.of(Order.StatusEnum.PENDING));
        Mockito.when(statusService.updateStatusToRejected(11L)).thenReturn(Optional.empty());

        var res = controller.updateToRejected(11L, 1L);
        assertEquals(new ResponseEntity<>(HttpStatus.NOT_FOUND), res);
    }

    @Test
    void updateStatusToRejected400() {
        Mockito.when(statusService.updateStatusToRejected(anyLong())).thenReturn(Optional.empty());
        Mockito.when(statusService.getOrderStatus(anyLong()))
            .thenReturn(Optional.of(Order.StatusEnum.GIVEN_TO_COURIER));

        var res = controller.updateToRejected(1L, 11L);
        assertEquals(new ResponseEntity<>(HttpStatus.BAD_REQUEST), res);
    }

    @Test
    void updateStatusToRejected500() {
        Mockito.when(authorizationService.checkIfUserIsAuthorized(1L, "updateToRejected", 11L))
            .thenReturn(Optional.of(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR)));
        var res = controller.updateToRejected(11L, 1L);
        assertEquals(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR), res);
    }

    @Test
    void updateStatusToRejected403() {
        Mockito.when(authorizationService.checkIfUserIsAuthorized(1L, "updateToRejected", 11L))
            .thenReturn(Optional.of(new ResponseEntity<>(HttpStatus.FORBIDDEN)));
        var res = controller.updateToRejected(11L, 1L);
        assertEquals(new ResponseEntity<>(HttpStatus.FORBIDDEN), res);
    }

    @Test
    void updateStatusToAccepted500() {
        Mockito.when(authorizationService.checkIfUserIsAuthorized(1L, "updateToAccepted", 11L))
            .thenReturn(Optional.of(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR)));
        var res = controller.updateToAccepted(11L, 1L);
        assertEquals(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR), res);
    }

    @Test
    void updateStatusToAccepted403() {
        Mockito.when(authorizationService.checkIfUserIsAuthorized(1L, "updateToAccepted", 11L))
            .thenReturn(Optional.of(new ResponseEntity<>(HttpStatus.FORBIDDEN)));
        var res = controller.updateToAccepted(11L, 1L);
        assertEquals(new ResponseEntity<>(HttpStatus.FORBIDDEN), res);
    }

    @Test
    void updateStatusToAccepted400() {
        Mockito.when(statusService.getOrderStatus(11L))
            .thenReturn(Optional.of(Order.StatusEnum.DELIVERED));
        var res = controller.updateToAccepted(11L, 1L);
        assertEquals(new ResponseEntity<>(HttpStatus.BAD_REQUEST), res);
    }

    @Test
    void getStatus404() {
        Mockito.when(statusService.getOrderStatus(anyLong())).thenReturn(Optional.empty());
        var res = controller.getStatus(1L, 1L);
        assertEquals(new ResponseEntity<>(HttpStatus.NOT_FOUND), res);
    }

    @Test
    void getStatus200() {
        Mockito.when(statusService.getOrderStatus(1L)).thenReturn(Optional.of(Order.StatusEnum.ACCEPTED));
        var res = controller.getStatus(1L, 1L);
        assertEquals(new ResponseEntity<>("accepted", HttpStatus.OK), res);

    }

    @Test
    void getStatus500() {
        Mockito.when(authorizationService.checkIfUserIsAuthorized(1L, "getStatus", 11L))
            .thenReturn(Optional.of(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR)));
        Mockito.when(statusService.getOrderStatus(11L)).thenReturn(Optional.of(Order.StatusEnum.ACCEPTED));
        var res = controller.getStatus(11L, 1L);
        assertEquals(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR), res);
    }

    @Test
    void getStatus403() {
        Mockito.when(authorizationService.checkIfUserIsAuthorized(1L, "getStatus", 11L))
            .thenReturn(Optional.of(new ResponseEntity<>(HttpStatus.FORBIDDEN)));
        Mockito.when(statusService.getOrderStatus(11L)).thenReturn(Optional.of(Order.StatusEnum.ACCEPTED));
        var res = controller.getStatus(11L, 1L);
        assertEquals(new ResponseEntity<>(HttpStatus.FORBIDDEN), res);
    }

    @Test
    void updateStatusToGivenToCourier200() {
        UpdateToGivenToCourierRequest req = new UpdateToGivenToCourierRequest();
        req.courierId(3L);

        Mockito.when(statusService.getOrderStatus(2L)).thenReturn(
            Optional.of(Order.StatusEnum.PREPARING));
        Mockito.when(statusService.updateStatusToGivenToCourier(2L, req)).thenReturn(
            Optional.of(new Order().id(2L).status(Order.StatusEnum.GIVEN_TO_COURIER)));

        var res = controller.updateToGivenToCourier(2L, 1L, req);
        assertEquals(new ResponseEntity<>(HttpStatus.OK), res);
    }

    @Test
    void updateStatusToGivenToCourier404() {
        Mockito.when(statusService.updateStatusToGivenToCourier(anyLong(), any())).thenReturn(Optional.empty());

        UpdateToGivenToCourierRequest req = new UpdateToGivenToCourierRequest();
        req.courierId(3L);
        var res = controller.updateToGivenToCourier(2L, 1L, req);
        assertEquals(new ResponseEntity<>(HttpStatus.NOT_FOUND), res);
    }

    @Test
    void updateStatusToGivenToCourier404second() {
        UpdateToGivenToCourierRequest req = new UpdateToGivenToCourierRequest();
        req.courierId(3L);

        Mockito.when(authorizationService.checkIfUserIsAuthorized(1L, "updateToGivenToCourier", 2L))
            .thenReturn(Optional.empty());
        Mockito.when(statusService.getOrderStatus(2L)).thenReturn(
            Optional.of(Order.StatusEnum.PREPARING));
        Mockito.when(statusService.updateStatusToGivenToCourier(2L, req)).thenReturn(Optional.empty());

        var res = controller.updateToGivenToCourier(2L, 1L, req);
        assertEquals(new ResponseEntity<>(HttpStatus.NOT_FOUND), res);
    }

    @Test
    void updateStatusToGivenToCourier500() {
        Mockito.when(authorizationService.checkIfUserIsAuthorized(1L, "updateToGivenToCourier", 2L))
            .thenReturn(Optional.of(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR)));
        Mockito.when(statusService.updateStatusToGivenToCourier(anyLong(), any())).thenReturn(Optional.empty());

        UpdateToGivenToCourierRequest req = new UpdateToGivenToCourierRequest();
        req.courierId(3L);
        var res = controller.updateToGivenToCourier(2L, 1L, req);
        assertEquals(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR), res);
    }

    @Test
    void updateStatusToGivenToCourier403() {
        Mockito.when(authorizationService.checkIfUserIsAuthorized(1L, "updateToGivenToCourier", 2L))
            .thenReturn(Optional.of(new ResponseEntity<>(HttpStatus.FORBIDDEN)));
        Mockito.when(statusService.updateStatusToGivenToCourier(anyLong(), any())).thenReturn(Optional.empty());

        UpdateToGivenToCourierRequest req = new UpdateToGivenToCourierRequest();
        req.courierId(3L);
        var res = controller.updateToGivenToCourier(2L, 1L, req);
        assertEquals(new ResponseEntity<>(HttpStatus.FORBIDDEN), res);
    }

    @Test
    void updateStatusToGivenToCourier400() {
        UpdateToGivenToCourierRequest req = new UpdateToGivenToCourierRequest();
        req.courierId(3L);

        Mockito.when(statusService.getOrderStatus(11L))
            .thenReturn(Optional.of(Order.StatusEnum.DELIVERED));

        var res = controller.updateToGivenToCourier(11L, 1L, req);
        assertEquals(new ResponseEntity<>(HttpStatus.BAD_REQUEST), res);
    }


    @Test
    void updateStatusToInTransit200() {
        Mockito.when(statusService.getOrderStatus(11L)).thenReturn(
            Optional.of(Order.StatusEnum.GIVEN_TO_COURIER));
        Mockito.when(statusService.updateStatusToInTransit(11L)).thenReturn(
            Optional.of(new Order().id(11L).status(Order.StatusEnum.IN_TRANSIT)));

        var res = controller.updateToInTransit(11L, 1L);
        assertEquals(new ResponseEntity<>(HttpStatus.OK), res);
    }

    @Test
    void updateStatusToInTransit404() {
        Mockito.when(statusService.updateStatusToInTransit(anyLong())).thenReturn(Optional.empty());

        var res = controller.updateToInTransit(11L, 1L);
        assertEquals(new ResponseEntity<>(HttpStatus.NOT_FOUND), res);
    }

    @Test
    void updateStatusToInTransit404second() {
        Mockito.when(authorizationService.checkIfUserIsAuthorized(1L, "updateToInTransit", 11L))
            .thenReturn(Optional.empty());
        Mockito.when(statusService.getOrderStatus(11L)).thenReturn(
            Optional.of(Order.StatusEnum.GIVEN_TO_COURIER));
        Mockito.when(statusService.updateStatusToInTransit(11L)).thenReturn(Optional.empty());

        var res = controller.updateToInTransit(11L, 1L);
        assertEquals(new ResponseEntity<>(HttpStatus.NOT_FOUND), res);
    }

    @Test
    void updateStatusToDelivered200() {
        Mockito.when(statusService.getOrderStatus(11L)).thenReturn(
            Optional.of(Order.StatusEnum.IN_TRANSIT));

        OffsetDateTime time = OffsetDateTime.of(2023, 12, 17, 12, 30, 0, 0, ZoneOffset.UTC);
        UpdateToDeliveredRequest req =
            new UpdateToDeliveredRequest().actualDeliveryTime(time);
        Mockito.when(orderService.orderExists(anyLong())).thenReturn(true);
        Mockito.when(statusService.updateStatusToDelivered(11L, req)).thenReturn(
            Optional.of(
                new Order().id(11L).timeValues(new Time().actualDeliveryTime(time)).status(Order.StatusEnum.DELIVERED)));

        var res = controller.updateToDelivered(11L, 1L, req);
        assertEquals(new ResponseEntity<>(HttpStatus.OK), res);
    }

    @Test
    void updateStatusToDelivered403NotInTransit() {
        Mockito.when(statusService.getOrderStatus(11L)).thenReturn(
            Optional.of(Order.StatusEnum.REJECTED));

        OffsetDateTime time = OffsetDateTime.of(2023, 12, 17, 12, 30, 0, 0, ZoneOffset.UTC);
        UpdateToDeliveredRequest req =
            new UpdateToDeliveredRequest().actualDeliveryTime(time);
        Mockito.when(orderService.orderExists(anyLong())).thenReturn(true);
        Mockito.when(statusService.updateStatusToDelivered(11L, req)).thenReturn(
            Optional.of(
                new Order().id(11L).timeValues(new Time().actualDeliveryTime(time)).status(Order.StatusEnum.DELIVERED)));

        var res = controller.updateToDelivered(11L, 1L, req);
        assertEquals(new ResponseEntity<>(HttpStatus.BAD_REQUEST), res);
    }

    @Test
    void updateStatusToDelivered404() {
        Mockito.when(statusService.updateStatusToDelivered(anyLong(), any()))
            .thenReturn(Optional.of(new Order().id(11L)));
        Mockito.when(orderService.orderExists(anyLong())).thenReturn(
            false);

        UpdateToDeliveredRequest update =
            new UpdateToDeliveredRequest().actualDeliveryTime(OffsetDateTime.of(2023, 12, 17, 12, 30, 0, 0, ZoneOffset.UTC));
        var res = controller.updateToDelivered(11L, 1L, update);
        assertEquals(new ResponseEntity<>(HttpStatus.NOT_FOUND), res);
    }

    @Test
    void updateStatusToDelivered404NoStatus() {
        Mockito.when(statusService.updateStatusToDelivered(anyLong(), any()))
            .thenReturn(Optional.of(new Order().id(11L)));
        Mockito.when(orderService.orderExists(anyLong())).thenReturn(
            true);

        UpdateToDeliveredRequest update =
            new UpdateToDeliveredRequest().actualDeliveryTime(OffsetDateTime.of(2023, 12, 17, 12, 30, 0, 0, ZoneOffset.UTC));
        var res = controller.updateToDelivered(11L, 1L, update);
        assertEquals(new ResponseEntity<>(HttpStatus.NOT_FOUND), res);
    }

    @Test
    void updateStatusToDelivered400() {
        Mockito.when(statusService.getOrderStatus(any())).thenReturn(Optional.of(Order.StatusEnum.IN_TRANSIT));
        Mockito.when(statusService.updateStatusToDelivered(anyLong(), any()))
            .thenReturn(Optional.empty());
        Mockito.when(orderService.orderExists(anyLong())).thenReturn(
            true);

        UpdateToDeliveredRequest update =
            new UpdateToDeliveredRequest().actualDeliveryTime(OffsetDateTime.of(2023, 12, 17, 12, 30, 0, 0, ZoneOffset.UTC));
        var res = controller.updateToDelivered(11L, 1L, update);
        assertEquals(new ResponseEntity<>(HttpStatus.BAD_REQUEST), res);
    }

    @Test
    void updateStatusToDelivered403() {
        Mockito.when(authorizationService.checkIfUserIsAuthorized(1L, "updateToDelivered", 11L))
            .thenReturn(Optional.of(new ResponseEntity<>(HttpStatus.FORBIDDEN)));

        UpdateToDeliveredRequest update =
            new UpdateToDeliveredRequest().actualDeliveryTime(OffsetDateTime.of(2023, 12, 17, 12, 30, 0, 0, ZoneOffset.UTC));
        var res = controller.updateToDelivered(11L, 1L, update);
        assertEquals(new ResponseEntity<>(HttpStatus.FORBIDDEN), res);
    }

    @Test
    void updateStatusToDelivered500() {
        Mockito.when(authorizationService.checkIfUserIsAuthorized(1L, "updateToDelivered", 11L))
            .thenReturn(Optional.of(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR)));
        Mockito.when(statusService.updateStatusToDelivered(anyLong(), any()))
            .thenReturn(Optional.empty());
        Mockito.when(orderService.orderExists(anyLong())).thenReturn(
            true);

        UpdateToDeliveredRequest update =
            new UpdateToDeliveredRequest().actualDeliveryTime(OffsetDateTime.of(2023, 12, 17, 12, 30, 0, 0, ZoneOffset.UTC));
        var res = controller.updateToDelivered(11L, 1L, update);
        assertEquals(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR), res);
    }

    @Test
    void updateStatusToInTransit500() {
        Mockito.when(authorizationService.checkIfUserIsAuthorized(1L, "updateToInTransit", 2L))
            .thenReturn(Optional.of(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR)));
        Mockito.when(statusService.updateStatusToInTransit(anyLong())).thenReturn(Optional.empty());

        var res = controller.updateToInTransit(2L, 1L);
        assertEquals(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR), res);
    }

    @Test
    void updateStatusToInTransit403() {
        Mockito.when(authorizationService.checkIfUserIsAuthorized(1L, "updateToInTransit", 2L))
            .thenReturn(Optional.of(new ResponseEntity<>(HttpStatus.FORBIDDEN)));
        Mockito.when(statusService.updateStatusToInTransit(anyLong())).thenReturn(Optional.empty());

        var res = controller.updateToInTransit(2L, 1L);
        assertEquals(new ResponseEntity<>(HttpStatus.FORBIDDEN), res);
    }

    @Test
    void updateStatusToInTransit400() {
        Mockito.when(statusService.getOrderStatus(11L)).thenReturn(
            Optional.of(Order.StatusEnum.DELIVERED));

        var res = controller.updateToInTransit(11L, 1L);
        assertEquals(new ResponseEntity<>(HttpStatus.BAD_REQUEST), res);
    }

    @Test
    void updateToPreparingWorks200() {
        OffsetDateTime time1 = OffsetDateTime.of(2023, 01, 01,
            12, 30, 00, 0, ZoneOffset.ofHours(2));
        OffsetDateTime time2 = OffsetDateTime.of(2024, 01, 01,
            14, 30, 00, 0, ZoneOffset.ofHours(2));
        UpdateToPreparingRequest req = new UpdateToPreparingRequest().expectedDeliveryTime(time2).prepTime("02:30:00");
        Time tv = new Time().prepTime("01:30:00").expectedDeliveryTime(time1);
        Time tv2 = new Time().prepTime("02:30:00").expectedDeliveryTime(time2);
        Order order1 = new Order().id(11L).status(Order.StatusEnum.PREPARING).timeValues(tv);
        Order order2 = new Order().id(11L).status(Order.StatusEnum.ACCEPTED).timeValues(tv2);

        Mockito.when(statusService.getOrderStatus(11L)).thenReturn(
            Optional.of(Order.StatusEnum.ACCEPTED));
        Mockito.when(statusService.updateStatusToPreparing(11L, req)).thenReturn(
            Optional.of(order2));

        var res = controller.updateToPreparing(11L, 1L, req);
        assertEquals(new ResponseEntity<>(HttpStatus.OK), res);
    }

    @Test
    void updateToPreparingGives404() {
        OffsetDateTime time2 = OffsetDateTime.of(2024, 01, 01,
            14, 30, 00, 0, ZoneOffset.ofHours(2));
        UpdateToPreparingRequest req = new UpdateToPreparingRequest().expectedDeliveryTime(time2).prepTime("02:30:00");

        Mockito.when(statusService.getOrderStatus(anyLong())).thenReturn(
            Optional.empty());

        var res = controller.updateToPreparing(11L, 1L, req);
        assertEquals(new ResponseEntity<>(HttpStatus.NOT_FOUND), res);
    }

    @Test
    void updateToPreparingGives404Variant() {
        OffsetDateTime time2 = OffsetDateTime.of(2024, 01, 01,
            14, 30, 00, 0, ZoneOffset.ofHours(2));
        UpdateToPreparingRequest req = new UpdateToPreparingRequest().expectedDeliveryTime(time2).prepTime("02:30:00");

        Mockito.when(statusService.getOrderStatus(anyLong())).thenReturn(
            Optional.of(Order.StatusEnum.ACCEPTED));
        Mockito.when(statusService.updateStatusToPreparing(11L, req)).thenReturn(
            Optional.empty());

        var res = controller.updateToPreparing(11L, 1L, req);
        assertEquals(new ResponseEntity<>(HttpStatus.NOT_FOUND), res);
    }

    @Test
    void updateToPreparingGives400() {
        OffsetDateTime time2 = OffsetDateTime.of(2024, 01, 01,
            14, 30, 00, 0, ZoneOffset.ofHours(2));
        UpdateToPreparingRequest req = new UpdateToPreparingRequest().expectedDeliveryTime(time2).prepTime("02:30:00");

        Mockito.when(statusService.getOrderStatus(anyLong())).thenReturn(
            Optional.of(Order.StatusEnum.DELIVERED));

        var res = controller.updateToPreparing(11L, 1L, req);
        assertEquals(new ResponseEntity<>(HttpStatus.BAD_REQUEST), res);
    }

    @Test
    void updateToPreparingGives403() {
        OffsetDateTime time2 = OffsetDateTime.of(2024, 01, 01,
            14, 30, 00, 0, ZoneOffset.ofHours(2));
        UpdateToPreparingRequest req = new UpdateToPreparingRequest().expectedDeliveryTime(time2).prepTime("02:30:00");

        Mockito.when(authorizationService.checkIfUserIsAuthorized(1L, "updateToPreparing", 11L))
            .thenReturn(Optional.of(new ResponseEntity<>(HttpStatus.FORBIDDEN)));
        Mockito.when(statusService.getOrderStatus(anyLong())).thenReturn(
            Optional.empty());


        var res = controller.updateToPreparing(11L, 1L, req);
        assertEquals(new ResponseEntity<>(HttpStatus.FORBIDDEN), res);
    }

}
