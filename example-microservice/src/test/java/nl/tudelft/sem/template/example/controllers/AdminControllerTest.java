package nl.tudelft.sem.template.example.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import nl.tudelft.sem.template.example.authorization.AuthorizationService;
import nl.tudelft.sem.template.example.domain.admin.AdminService;
import nl.tudelft.sem.template.model.DeliveryException;
import nl.tudelft.sem.template.model.Order;
import nl.tudelft.sem.template.model.Vendor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class AdminControllerTest {

    private AdminService adminService;
    private AdminController controller;
    private AuthorizationService authorizationService;

    private DeliveryException exception1;

    private DeliveryException exception2;


    @BeforeEach
    void setUp() {
        Order order1 = new Order().status(Order.StatusEnum.DELIVERED).id(3L);
        this.exception1 = new DeliveryException().id(2L)
            .exceptionType(DeliveryException.ExceptionTypeEnum.LATEDELIVERY)
            .isResolved(false)
            .order(order1);
        this.exception2 = new DeliveryException().id(2L)
            .exceptionType(DeliveryException.ExceptionTypeEnum.LATEDELIVERY)
            .isResolved(false)
            .order(new Order().id(6L));
        this.adminService = Mockito.mock(AdminService.class);
        this.authorizationService = Mockito.mock(AuthorizationService.class);
        this.controller = new AdminController(adminService, authorizationService);
    }

    @Test
    void updateDefaultRadius404() {
        Mockito.when(adminService.updateDefaultRadius(5D)).thenReturn(Optional.empty());
        var res = controller.updateDefaultRadius(1L, 5D);
        assertEquals(new ResponseEntity<>(HttpStatus.NOT_FOUND), res);
    }

    @Test
    void updateDefaultRadius200() {
        List<Vendor> listVendors = new ArrayList<>();
        listVendors.add(new Vendor());
        Mockito.when(adminService.updateDefaultRadius(5D)).thenReturn(Optional.of(listVendors));
        var res = controller.updateDefaultRadius(1L, 5D);
        assertEquals(new ResponseEntity<>(HttpStatus.OK), res);
    }

    @Test
    void updateDefaultRadius403() {
        Mockito.when(authorizationService.authorizeAdminOnly(1L))
            .thenReturn(Optional.of(new ResponseEntity<>(HttpStatus.FORBIDDEN)));

        var res = controller.updateDefaultRadius(1L, 5D);
        assertEquals(new ResponseEntity<>(HttpStatus.FORBIDDEN), res);
    }

    @Test
    void getDefaultRadius200() {
        Mockito.when(adminService.getDefaultRadius()).thenReturn(Optional.of(5D));
        var res = controller.getDefaultRadius(1L);
        assertEquals(new ResponseEntity<>(5D, HttpStatus.OK), res);
    }

    @Test
    void getDefaultRadius404() {
        Mockito.when(adminService.getDefaultRadius()).thenReturn(Optional.empty());
        var res = controller.getDefaultRadius(1L);
        assertEquals(new ResponseEntity<>(HttpStatus.NOT_FOUND), res);
    }

    @Test
    void getDefaultRadius403() {
        Mockito.when(authorizationService.authorizeAdminOnly(1L))
            .thenReturn(Optional.of(new ResponseEntity<>(HttpStatus.FORBIDDEN)));

        var res = controller.getDefaultRadius(1L);
        assertEquals(new ResponseEntity<>(HttpStatus.FORBIDDEN), res);
    }

    @Test
    void getDeliveredOrdersWorks200() {
        List<Order> orders = List.of(new Order().id(1L).status(Order.StatusEnum.DELIVERED),
            new Order().id(2L).status(Order.StatusEnum.DELIVERED),
            new Order().id(3L).status(Order.StatusEnum.DELIVERED));
        Mockito.when(adminService.getDelivered()).thenReturn(Optional.of(orders));

        var res = controller.getDeliveredOrders(1L);
        assertEquals(new ResponseEntity<>(orders, HttpStatus.OK), res);
    }

    @Test
    void getDeliveredOrders404() {
        Mockito.when(adminService.getDelivered()).thenReturn(Optional.empty());

        var res = controller.getDeliveredOrders(1L);
        assertEquals(new ResponseEntity<>(HttpStatus.NOT_FOUND), res);
    }

    @Test
    void getException200() {
        Mockito.when(adminService.getExceptionByOrder(anyLong())).thenReturn(Optional.of(exception1));
        var res = controller.getExceptionForOrder(1L, 5L);
        assertEquals(new ResponseEntity<>(exception1, HttpStatus.OK), res);
    }

    @Test
    void getException404() {
        Mockito.when(adminService.getExceptionByOrder(anyLong())).thenReturn(Optional.empty());
        var res = controller.getExceptionForOrder(1L, 5L);
        assertEquals(new ResponseEntity<>(HttpStatus.NOT_FOUND), res);
    }

    @Test
    void getDeliveredOrders403() {
        Mockito.when(authorizationService.authorizeAdminOnly(1L))
            .thenReturn(Optional.of(new ResponseEntity<>(HttpStatus.FORBIDDEN)));

        var res = controller.getDeliveredOrders(1L);
        assertEquals(new ResponseEntity<>(HttpStatus.FORBIDDEN), res);
    }

    @Test
    void makeException400() {
        Mockito.when(adminService.makeException(any(), anyLong())).thenReturn(Optional.empty());
        var res = controller.makeException(1L, 5L, exception1);
        assertEquals(new ResponseEntity<>(HttpStatus.BAD_REQUEST), res);
    }

    @Test
    void makeException200() {
        Mockito.when(adminService.makeException(any(), anyLong())).thenReturn(Optional.of(exception1));
        var res = controller.makeException(1L, 5L, exception1);
        assertEquals(new ResponseEntity<>(HttpStatus.OK), res);
    }

    @Test
    void getAllExceptions200() {
        Mockito.when(adminService.getAllExceptions()).thenReturn(List.of(exception1, exception2));
        var res = controller.getExceptions(1L);
        assertEquals(new ResponseEntity<>(List.of(exception1, exception2), HttpStatus.OK), res);
    }

    @Test
    void getAllExceptions404() {
        Mockito.when(adminService.getAllExceptions()).thenReturn(List.of());
        var res = controller.getExceptions(1L);
        assertEquals(new ResponseEntity<>(HttpStatus.NOT_FOUND), res);
    }

    @Test
    void updateException200() {
        Mockito.when(adminService.doesExceptionExist(any())).thenReturn(true);
        Mockito.when(adminService.updateException(any(), anyLong())).thenReturn(Optional.of(exception1));
        var res = controller.updateException(1L, 3L, exception1);
        assertEquals(new ResponseEntity<>(HttpStatus.OK), res);
    }

    @Test
    void updateException404() {
        Mockito.when(adminService.doesExceptionExist(any())).thenReturn(false);
        Mockito.when(adminService.updateException(any(), anyLong())).thenReturn(Optional.of(exception1));
        var res = controller.updateException(1L, 3L, exception1);
        assertEquals(new ResponseEntity<>(HttpStatus.NOT_FOUND), res);
    }

    @Test
    void updateException400() {
        Mockito.when(adminService.doesExceptionExist(any())).thenReturn(true);
        Mockito.when(adminService.updateException(any(), anyLong())).thenReturn(Optional.empty());
        var res = controller.updateException(1L, 3L, exception1);
        assertEquals(new ResponseEntity<>(HttpStatus.BAD_REQUEST), res);
    }

    @Test
    void getCourierEfficienciesWorks200() {
        Mockito.when(adminService.getCouriersEfficiencies()).thenReturn(
            Optional.of(Map.of("22", 60.0D, "23", 120.0D)));

        var res = controller.getCourierEfficiencies(1L);
        assertEquals(new ResponseEntity<>(Map.of("22", 60.0D, "23", 120.0D), HttpStatus.OK), res);
    }

    @Test
    void getCourierEfficienciesWorks404() {
        Mockito.when(adminService.getCouriersEfficiencies()).thenReturn(
            Optional.empty());

        var res = controller.getCourierEfficiencies(1L);
        assertEquals(new ResponseEntity<>(HttpStatus.NOT_FOUND), res);
    }

    @Test
    void getCourierEfficienciesWorks403() {
        Mockito.when(authorizationService.authorizeAdminOnly(anyLong())).thenReturn(
            Optional.of(new ResponseEntity<>(HttpStatus.FORBIDDEN)));

        var res = controller.getCourierEfficiencies(1L);
        assertEquals(new ResponseEntity<>(HttpStatus.FORBIDDEN), res);
    }

    @Test
    void updateException403() {
        Mockito.when(authorizationService.authorizeAdminOnly(1L))
            .thenReturn(Optional.of(new ResponseEntity<>(HttpStatus.FORBIDDEN)));

        var res = controller.updateException(1L, 1L, null);
        assertEquals(new ResponseEntity<>(HttpStatus.FORBIDDEN), res);
    }

    @Test
    void getExceptions403() {
        Mockito.when(authorizationService.authorizeAdminOnly(1L))
            .thenReturn(Optional.of(new ResponseEntity<>(HttpStatus.FORBIDDEN)));

        var res = controller.getExceptions(1L);
        assertEquals(new ResponseEntity<>(HttpStatus.FORBIDDEN), res);
    }

    @Test
    void makeException403() {
        Mockito.when(authorizationService.authorizeAdminOnly(1L))
            .thenReturn(Optional.of(new ResponseEntity<>(HttpStatus.FORBIDDEN)));

        var res = controller.makeException(1L, 1L, null);
        assertEquals(new ResponseEntity<>(HttpStatus.FORBIDDEN), res);
    }

    @Test
    void getExceptionForOrder403() {
        Mockito.when(authorizationService.authorizeAdminOnly(1L))
            .thenReturn(Optional.of(new ResponseEntity<>(HttpStatus.FORBIDDEN)));

        var res = controller.getExceptionForOrder(1L, 1L);
        assertEquals(new ResponseEntity<>(HttpStatus.FORBIDDEN), res);
    }

    @Test
    void getAllRatings200() {
        List<BigDecimal> ratings = Arrays.asList(
            new BigDecimal("4.5"),
            new BigDecimal("3.2"),
            new BigDecimal("5.0"),
            new BigDecimal("2.8"),
            new BigDecimal("4.0")
        );
        Mockito.when(adminService.getAllRatings()).thenReturn(Optional.of(ratings));
        var res = controller.getAllRatings(0L);
        assertEquals(new ResponseEntity<>(ratings, HttpStatus.OK), res);
    }

    @Test
    void getAllRatings404() {
        Mockito.when(adminService.getAllRatings()).thenReturn(Optional.empty());

        var res = controller.getAllRatings(0L);
        assertEquals(new ResponseEntity<>(HttpStatus.NOT_FOUND), res);
    }

    @Test
    void getAllOrders403() {
        Mockito.when(authorizationService.authorizeAdminOnly(0L))
            .thenReturn(Optional.of(new ResponseEntity<>(HttpStatus.FORBIDDEN)));

        var res = controller.getAllRatings(0L);
        assertEquals(new ResponseEntity<>(HttpStatus.FORBIDDEN), res);
    }

    @Test
    void getAllDeliveryTimes200() {
        List<String> strings = Arrays.asList("4.5", "3.2", "5.0", "2.8", "4.0");

        Mockito.when(adminService.getAllDeliveryTimes()).thenReturn(Optional.of(strings));

        var res = controller.getAllDeliveryTimes(0L);
        assertEquals(new ResponseEntity<>(strings, HttpStatus.OK), res);
    }

    @Test
    void getAllDeliveryTimes404() {
        Mockito.when(adminService.getAllDeliveryTimes()).thenReturn(Optional.empty());

        var res = controller.getAllDeliveryTimes(0L);
        assertEquals(new ResponseEntity<>(HttpStatus.NOT_FOUND), res);
    }

    @Test
    void getAllDeliveryTimes403() {
        Mockito.when(authorizationService.authorizeAdminOnly(0L))
            .thenReturn(Optional.of(new ResponseEntity<>(HttpStatus.FORBIDDEN)));

        var res = controller.getAllDeliveryTimes(0L);
        assertEquals(new ResponseEntity<>(HttpStatus.FORBIDDEN), res);
    }
}
