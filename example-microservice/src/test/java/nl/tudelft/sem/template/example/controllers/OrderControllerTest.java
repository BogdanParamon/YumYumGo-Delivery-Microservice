package nl.tudelft.sem.template.example.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import nl.tudelft.sem.template.example.authorization.AuthorizationService;
import nl.tudelft.sem.template.example.domain.order.OrderRepository;
import nl.tudelft.sem.template.example.domain.order.OrderService;
import nl.tudelft.sem.template.example.domain.order.orderstrategy.GeneralOrdersStrategy;
import nl.tudelft.sem.template.example.domain.order.orderstrategy.OrderPerVendorStrategy;
import nl.tudelft.sem.template.example.domain.user.CourierService;
import nl.tudelft.sem.template.example.domain.user.VendorRepository;
import nl.tudelft.sem.template.example.externalservices.NavigationMock;
import nl.tudelft.sem.template.model.Courier;
import nl.tudelft.sem.template.model.Location;
import nl.tudelft.sem.template.model.Order;
import nl.tudelft.sem.template.model.Time;
import nl.tudelft.sem.template.model.Vendor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;


class OrderControllerTest {

    private OrderService orderService;

    private OrderRepository orderRepo;

    private VendorRepository vendorRepo;

    private CourierService courierService;

    private OrderController controller;

    private Order order1;

    private OrderPerVendorStrategy perVendorStrategy;

    private GeneralOrdersStrategy generalStrategy;

    private AuthorizationService authorizationService;
    private OffsetDateTime eta;


    @BeforeEach
    void setUp() {
        this.orderService = Mockito.mock(OrderService.class);
        this.courierService = Mockito.mock(CourierService.class);
        this.authorizationService = Mockito.mock(AuthorizationService.class);

        this.orderRepo = Mockito.mock(OrderRepository.class);
        this.vendorRepo = Mockito.mock(VendorRepository.class);

        this.generalStrategy = Mockito.mock(GeneralOrdersStrategy.class);
        this.perVendorStrategy = Mockito.mock(OrderPerVendorStrategy.class);

        this.order1 = new Order().id(2L).status(Order.StatusEnum.PREPARING).vendorId(44L);
        Mockito.when(authorizationService.checkIfUserIsAuthorized(any(), anyString(), any())).thenReturn(Optional.empty());
        this.controller = new OrderController(orderService, courierService, authorizationService, orderRepo, vendorRepo);

        NavigationMock navigationMock = new NavigationMock();
        this.eta = navigationMock.getEta(1L, new Time());
    }

    @Test
    void getIndependentOrdersWorks() {
        Mockito.when(orderRepo.findByStatus(any())).thenReturn(List.of(order1));
        Mockito.when(vendorRepo.findById(any())).thenReturn(Optional.ofNullable(new Vendor().id(33L).hasCouriers(false)));
        Mockito.when(generalStrategy.availableOrders(any())).thenReturn(Optional.of(List.of(order1)));

        var res = controller.getIndependentOrders(11L);
        assertEquals(new ResponseEntity<>(List.of(order1), HttpStatus.OK), res);
    }

    @Test
    void getIndependentOrdersForbidden() {
        Mockito.when(orderRepo.findByStatus(any())).thenReturn(List.of(order1));
        Mockito.when(vendorRepo.findById(any())).thenReturn(Optional.ofNullable(new Vendor().id(33L).hasCouriers(false)));
        Mockito.when(generalStrategy.availableOrders(any())).thenReturn(Optional.of(List.of(order1)));
        Mockito.when(authorizationService.checkIfUserIsAuthorized(any(), anyString(), any()))
            .thenReturn(Optional.of(new ResponseEntity<>(HttpStatus.FORBIDDEN)));

        var res = controller.getIndependentOrders(11L);
        assertEquals(new ResponseEntity<>(HttpStatus.FORBIDDEN), res);
    }

    @Test
    void getIndependentOrders404() {
        Mockito.when(orderRepo.findByStatus(any())).thenReturn(List.of());
        Mockito.when(vendorRepo.findById(any())).thenReturn(Optional.ofNullable(new Vendor().id(33L).hasCouriers(false)));
        Mockito.when(generalStrategy.availableOrders(any())).thenReturn(Optional.of(List.of(order1)));

        var res = controller.getIndependentOrders(11L);
        assertEquals(new ResponseEntity<>(HttpStatus.NOT_FOUND), res);
    }

    @Test
    void getNextOrderForVendorWorks() {
        Mockito.when(courierService.getCourierById(anyLong())).thenReturn(Optional.of(new Courier().bossId(4L)));
        Mockito.when(orderRepo.findByVendorIdAndStatusAndCourierId(anyLong(), any(), any())).thenReturn(List.of(order1));
        Mockito.when(perVendorStrategy.availableOrders(any())).thenReturn(Optional.of(List.of(order1)));

        var res = controller.getNextOrderForVendor(11L, 1L);
        assertEquals(new ResponseEntity<>(order1, HttpStatus.OK), res);
    }

    @Test
    void getNextOrderForVendorForbidden() {
        Mockito.when(courierService.getCourierById(anyLong())).thenReturn(Optional.of(new Courier().bossId(4L)));
        Mockito.when(orderRepo.findByVendorIdAndStatusAndCourierId(anyLong(), any(), any())).thenReturn(List.of(order1));
        Mockito.when(perVendorStrategy.availableOrders(any())).thenReturn(Optional.of(List.of(order1)));
        Mockito.when(authorizationService.checkIfUserIsAuthorized(any(), anyString(), any()))
            .thenReturn(Optional.of(new ResponseEntity<>(HttpStatus.FORBIDDEN)));

        var res = controller.getNextOrderForVendor(11L, 1L);
        assertEquals(new ResponseEntity<>(HttpStatus.FORBIDDEN), res);
    }

    @Test
    void getNextOrderForVendorForbiddenNotCourier() {
        Mockito.when(courierService.getCourierById(anyLong())).thenReturn(Optional.empty());
        Mockito.when(orderRepo.findByVendorIdAndStatusAndCourierId(anyLong(), any(), any())).thenReturn(List.of(order1));
        Mockito.when(perVendorStrategy.availableOrders(any())).thenReturn(Optional.of(List.of(order1)));

        var res = controller.getNextOrderForVendor(11L, 1L);
        assertEquals(new ResponseEntity<>(HttpStatus.FORBIDDEN), res);
    }

    @Test
    void getNextOrderForVendorWorksMultipleOrders() {
        Mockito.when(courierService.getCourierById(anyLong())).thenReturn(Optional.of(new Courier().bossId(4L)));
        Mockito.when(orderRepo.findByVendorIdAndStatusAndCourierId(anyLong(), any(), any()))
            .thenReturn(List.of(order1, new Order()));
        Mockito.when(perVendorStrategy.availableOrders(any())).thenReturn(Optional.of(List.of(order1)));

        var res = controller.getNextOrderForVendor(11L, 1L);
        assertEquals(new ResponseEntity<>(order1, HttpStatus.OK), res);
    }

    @Test
    void getNextOrderForVendorNotFound() {
        Mockito.when(courierService.getCourierById(anyLong())).thenReturn(Optional.of(new Courier().bossId(4L)));
        Mockito.when(orderRepo.findByVendorIdAndStatusAndCourierId(anyLong(), any(), any())).thenReturn(List.of());
        Mockito.when(perVendorStrategy.availableOrders(any())).thenReturn(Optional.of(List.of()));

        var res = controller.getNextOrderForVendor(11L, 1L);
        assertEquals(new ResponseEntity<>(HttpStatus.NOT_FOUND), res);
    }

    @Test
    void getNextOrderForVendorBadRequest() {
        Mockito.when(courierService.getCourierById(anyLong())).thenReturn(Optional.of(new Courier().bossId(4L)));
        Mockito.when(orderRepo.findByVendorIdAndStatusAndCourierId(anyLong(), any(), any())).thenReturn(null);
        Mockito.when(perVendorStrategy.availableOrders(any())).thenReturn(Optional.empty());

        var res = controller.getNextOrderForVendor(11L, 1L);
        assertEquals(new ResponseEntity<>(HttpStatus.BAD_REQUEST), res);
    }

    @Test
    void getFinalDestinationWorks() {
        Optional<Location> proper = Optional.of(new Location().latitude(1F).longitude(2F));
        Mockito.when(orderService.getFinalDestinationOfOrder(anyLong())).thenReturn(proper);
        var res = controller.getFinalDestination(11L, 1L);
        assertEquals(new ResponseEntity<>(proper.get(), HttpStatus.OK), res);
    }


    @Test
    void getFinalDestinationGives404() {
        Optional<Location> proper = Optional.empty();
        Mockito.when(orderService.getFinalDestinationOfOrder(anyLong())).thenReturn(proper);
        var res = controller.getFinalDestination(11L, 1L);
        assertEquals(new ResponseEntity<>(HttpStatus.NOT_FOUND), res);
    }

    @Test
    void getFinalDestinationGives403() {
        Mockito.when(authorizationService.checkIfUserIsAuthorized(1L, "getFinalDestination", 11L))
            .thenReturn(Optional.of(new ResponseEntity<>(HttpStatus.FORBIDDEN)));

        var res = controller.getFinalDestination(1L, 11L);
        assertEquals(new ResponseEntity<>(HttpStatus.FORBIDDEN), res);
    }

    @Test
    void getPickUpDestinationWorks() {
        var proper = Optional.of(new Location().longitude(11f).latitude(22f));
        Mockito.when(orderService.getPickupDestination(anyLong())).thenReturn(proper);
        var res = controller.getPickupDestination(1L, 22L);
        assertEquals(new ResponseEntity<>(proper.get(), HttpStatus.OK), res);
    }

    @Test
    void getPickUpDestinationGives404() {
        Optional<Location> proper = Optional.empty();
        Mockito.when(orderService.getPickupDestination(anyLong())).thenReturn(proper);
        var res = controller.getPickupDestination(1L, 22L);
        assertEquals(new ResponseEntity<>(HttpStatus.NOT_FOUND), res);
    }

    @Test
    void getPickUpDestinationGives403() {
        Mockito.when(authorizationService.checkIfUserIsAuthorized(1L, "getPickupDestination", 11L))
            .thenReturn(Optional.of(new ResponseEntity<>(HttpStatus.FORBIDDEN)));

        var res = controller.getPickupDestination(11L, 1L);
        assertEquals(new ResponseEntity<>(HttpStatus.FORBIDDEN), res);
    }

    @Test
    void getOrder200() {
        Optional<Order> proper = Optional.of(new Order().id(11L).status(Order.StatusEnum.ACCEPTED));
        Mockito.when(orderService.getOrderById(11L)).thenReturn(proper);
        var res = controller.getOrder(11L, 1L);
        assertEquals(new ResponseEntity<>(proper.get(), HttpStatus.OK), res);
    }

    @Test
    void getOrder404() {
        Mockito.when(orderService.getOrderById(11L)).thenReturn(Optional.empty());
        var res = controller.getOrder(1L, 11L);
        assertEquals(new ResponseEntity<>(HttpStatus.NOT_FOUND), res);
    }

    @Test
    void getOrder403() {
        Mockito.when(orderService.getOrderById(11L)).thenReturn(Optional.empty());
        Mockito.when(authorizationService.checkIfUserIsAuthorized(1L, "getOrder", 11L))
            .thenReturn(Optional.of(new ResponseEntity<>(HttpStatus.FORBIDDEN)));

        var res = controller.getOrder(11L, 1L);
        assertEquals(new ResponseEntity<>(HttpStatus.FORBIDDEN), res);
    }

    @Test
    void getOrderRating200() {
        Mockito.when(authorizationService.authorizeAdminOnly(1L))
            .thenReturn(Optional.empty());
        Optional<BigDecimal> proper = Optional.of(new BigDecimal("5.0"));
        Mockito.when(orderService.getRating(1L)).thenReturn(proper);
        var res = controller.getOrderRating(1L, 1L);
        assertEquals(new ResponseEntity<>(proper.get(), HttpStatus.OK), res);
    }

    @Test
    void getOrderRating403() {
        Mockito.when(authorizationService.checkIfUserIsAuthorized(1L, "getOrderRating", 1L))
            .thenReturn(Optional.of(new ResponseEntity<>(HttpStatus.FORBIDDEN)));

        var res = controller.getOrderRating(1L, 1L);
        assertEquals(new ResponseEntity<>(HttpStatus.FORBIDDEN), res);
    }

    @Test
    void getOrderRating404() {
        Mockito.when(authorizationService.authorizeAdminOnly(1L))
            .thenReturn(Optional.empty());
        Mockito.when(orderService.getRating(1L)).thenReturn(Optional.empty());
        var res = controller.getOrderRating(1L, 1L);
        assertEquals(new ResponseEntity<>(HttpStatus.NOT_FOUND), res);
    }

    @Test
    void updateOrder200() {
        Order updated = new Order().id(11L).status(Order.StatusEnum.PENDING);
        Mockito.when(orderService.updateOrderById(11L, updated)).thenReturn(Optional.of(updated));
        var res = controller.updateOrder(11L, 1L, updated);
        assertEquals(new ResponseEntity<>(HttpStatus.OK), res);
    }

    @Test
    void updateOrder404() {
        Order updated = new Order().id(11L).status(Order.StatusEnum.PENDING);
        Mockito.when(orderService.updateOrderById(11L, updated)).thenReturn(Optional.empty());
        var res = controller.updateOrder(11L, 1L, updated);
        assertEquals(new ResponseEntity<>(HttpStatus.NOT_FOUND), res);
    }

    @Test
    void updateOrder403() {
        Order updated = new Order().id(11L).status(Order.StatusEnum.PENDING);
        Mockito.when(authorizationService.checkIfUserIsAuthorized(1L, "updateOrder", 11L))
            .thenReturn(Optional.of(new ResponseEntity<>(HttpStatus.FORBIDDEN)));

        var res = controller.updateOrder(11L, 1L, updated);
        assertEquals(new ResponseEntity<>(HttpStatus.FORBIDDEN), res);
    }

    @Test
    void getOrders200() {
        List<Order> proper = List.of(new Order().id(11L), new Order().id(22L), new Order().id(33L));
        Mockito.when(orderService.getOrders()).thenReturn(Optional.of(proper));
        var res = controller.getOrders(1L);
        assertEquals(new ResponseEntity<>(proper, HttpStatus.OK), res);
    }

    @Test
    void getOrders403() {
        List<Order> proper = List.of(new Order().id(11L), new Order().id(22L), new Order().id(33L));
        Mockito.when(orderService.getOrders()).thenReturn(Optional.of(proper));
        Mockito.when(authorizationService.authorizeAdminOnly(1L))
            .thenReturn(Optional.of(new ResponseEntity<>(HttpStatus.FORBIDDEN)));

        var res = controller.getOrders(1L);
        assertEquals(new ResponseEntity<>(HttpStatus.FORBIDDEN), res);
    }

    @Test
    void getOrders404() {
        Mockito.when(orderService.getOrders()).thenReturn(Optional.empty());
        var res = controller.getOrders(1L);
        assertEquals(new ResponseEntity<>(HttpStatus.NOT_FOUND), res);
    }

    @Test
    void makeOrder200() {
        Order o = new Order().id(11L);
        Mockito.when(orderService.createOrder(o)).thenReturn(Optional.of(o));
        Mockito.when(orderService.getOrderById(11L)).thenReturn(Optional.empty());

        var res = controller.makeOrder(11L, 1L, o);
        assertEquals(new ResponseEntity<>(HttpStatus.OK), res);
    }

    @Test
    void updateOrderRating200() {
        Optional<BigDecimal> rating1 = Optional.of(new BigDecimal("5.0"));
        Optional<BigDecimal> rating2 = Optional.of(new BigDecimal("2.0"));

        Mockito.when(authorizationService.authorizeAdminOnly(1L))
            .thenReturn(Optional.empty());
        Mockito.when(orderService.getRating(1L)).thenReturn(rating1);
        Mockito.when(orderService.updateRating(1L, rating2.get())).thenReturn(rating2);

        var res = controller.putOrderRating(1L, 1L, rating2.get());
        assertEquals(new ResponseEntity<>(HttpStatus.OK), res);
    }

    @Test
    void makeOrder403() {
        Order o = new Order().id(11L);
        Mockito.when(authorizationService.authorizeAdminOnly(1L))
            .thenReturn(Optional.of(new ResponseEntity<>(HttpStatus.FORBIDDEN)));
        Mockito.when(orderService.createOrder(o)).thenReturn(Optional.of(o));
        Mockito.when(orderService.getOrderById(11L)).thenReturn(Optional.empty());

        var res = controller.makeOrder(11L, 1L, o);
        assertEquals(new ResponseEntity<>(HttpStatus.FORBIDDEN), res);
    }

    @Test
    void makeOrder400() {
        Order o = new Order().id(11L);
        Mockito.when(orderService.createOrder(o)).thenReturn(Optional.of(o));
        Mockito.when(orderService.getOrderById(11L)).thenReturn(Optional.of(o));
        var res = controller.makeOrder(11L, 1L, o);
        assertEquals(new ResponseEntity<>(HttpStatus.BAD_REQUEST), res);
    }

    @Test
    void updateOrderRating404() {
        Mockito.when(authorizationService.authorizeAdminOnly(1L))
            .thenReturn(Optional.empty());
        Mockito.when(orderService.getRating(1L)).thenReturn(Optional.empty());
        var res = controller.putOrderRating(1L, 1L, new BigDecimal("1.0"));

        assertEquals(new ResponseEntity<>(HttpStatus.NOT_FOUND), res);
    }

    @Test
    void updateOrderRating403() {
        Mockito.when(authorizationService.checkIfUserIsAuthorized(1L, "putOrderRating", 1L))
            .thenReturn(Optional.of(new ResponseEntity<>(HttpStatus.FORBIDDEN)));

        var res = controller.putOrderRating(1L, 1L, new BigDecimal("1.0"));
        assertEquals(new ResponseEntity<>(HttpStatus.FORBIDDEN), res);
    }

    @Test
    void updateOrderRating404second() {
        Optional<BigDecimal> rating1 = Optional.of(new BigDecimal("5.0"));
        Optional<BigDecimal> rating2 = Optional.of(new BigDecimal("2.0"));

        Mockito.when(orderService.getRating(1L)).thenReturn(rating1);
        Mockito.when(orderService.updateRating(1L, rating2.get())).thenReturn(Optional.empty());

        var res = controller.putOrderRating(1L, 1L, rating2.get());
        assertEquals(new ResponseEntity<>(HttpStatus.NOT_FOUND), res);
    }

    @Test
    void setCourierId200() {
        Courier c = new Courier().id(2L);
        Mockito.when(courierService.getCourierById(2L)).thenReturn(Optional.of(c));
        Order o = new Order().id(11L).courierId(3L);
        Order o1 = new Order().id(11L).courierId(2L);
        Mockito.when(orderService.updateCourier(11L, 2L)).thenReturn(Optional.of(o1));

        ResponseEntity<Void> res = controller.setCourierId(11L, 2L, 1L, o);
        assertEquals(new ResponseEntity<>(HttpStatus.OK), res);
    }

    @Test
    void setCourierId403() {
        Mockito.when(authorizationService.authorizeAdminOnly(1L))
            .thenReturn(Optional.of(new ResponseEntity<>(HttpStatus.FORBIDDEN)));

        Order o = new Order().id(11L).courierId(3L);

        ResponseEntity<Void> res = controller.setCourierId(11L, 2L, 1L, o);
        assertEquals(new ResponseEntity<>(HttpStatus.FORBIDDEN), res);
    }

    @Test
    void setCourierId404noCourier() {
        Mockito.when(courierService.getCourierById(2L)).thenReturn(Optional.empty());
        Order o = new Order().id(11L).courierId(3L);

        ResponseEntity<Void> res = controller.setCourierId(11L, 2L, 1L, o);
        assertEquals(new ResponseEntity<>(HttpStatus.NOT_FOUND), res);
    }

    @Test
    void setCourierId404noOrder() {
        Courier c = new Courier().id(2L);
        Mockito.when(courierService.getCourierById(2L)).thenReturn(Optional.of(c));
        Order o = new Order().id(11L).courierId(3L);
        Mockito.when(orderService.updateCourier(11L, 2L)).thenReturn(Optional.empty());

        ResponseEntity<Void> res = controller.setCourierId(11L, 2L, 1L, o);
        assertEquals(new ResponseEntity<>(HttpStatus.NOT_FOUND), res);
    }

    @Test
    void setDeliverTime200() {
        Mockito.when(authorizationService.checkIfUserIsAuthorized(1L, "setDeliverTime", 11L))
            .thenReturn(Optional.empty());
        Mockito.when(orderService.updatePrepTime(11L, "03:30:00")).thenReturn(Optional.of("03:30:00"));
        var ret = controller.setPreparationTime(11L, 1L, "03:30:00");

        assertEquals(new ResponseEntity<>(HttpStatus.OK), ret);
    }

    @Test
    void setDeliverTime403() {
        Mockito.when(authorizationService.checkIfUserIsAuthorized(1L, "setDeliverTime", 11L))
            .thenReturn(Optional.of(new ResponseEntity<>(HttpStatus.FORBIDDEN)));
        Mockito.when(orderService.updatePrepTime(11L, "03:30:00")).thenReturn(Optional.of("03:30:00"));
        var ret = controller.setPreparationTime(11L, 1L, "03:30:00");

        assertEquals(new ResponseEntity<>(HttpStatus.FORBIDDEN), ret);
    }

    @Test
    void setDeliverTime404() {
        Mockito.when(authorizationService.checkIfUserIsAuthorized(1L, "setDeliverTime", 11L))
            .thenReturn(Optional.empty());
        Mockito.when(orderService.updatePrepTime(11L, "03:30:00")).thenReturn(Optional.empty());
        var ret = controller.setPreparationTime(11L, 1L, "03:30:00");

        assertEquals(new ResponseEntity<>(HttpStatus.NOT_FOUND), ret);
    }

    @Test
    void getOrderLocation404() {
        Mockito.when(orderService.getOrderById(anyLong())).thenReturn(Optional.empty());

        var res = controller.getOrderLocation(0L, 1L);
        assertEquals(new ResponseEntity<>(HttpStatus.NOT_FOUND), res);
    }

    @Test
    void getOrderLocation200() {
        Order order = new Order().id(2L);
        Location loc = new Location().latitude(1F).longitude(1F);
        Mockito.when(orderService.getOrderById(anyLong())).thenReturn(Optional.of(order));
        Mockito.when(orderService.getOrderLocation(order)).thenReturn(Optional.of(loc));
        var res = controller.getOrderLocation(2L, 1L);
        assertEquals(new ResponseEntity<>(loc, HttpStatus.OK), res);
    }

    @Test
    void getOrderLocation400() {
        Order order = new Order().id(2L);
        Mockito.when(orderService.getOrderById(anyLong())).thenReturn(Optional.of(order));
        Mockito.when(orderService.getOrderLocation(order)).thenReturn(Optional.empty());
        var res = controller.getOrderLocation(2L, 1L);
        assertEquals(new ResponseEntity<>(HttpStatus.BAD_REQUEST), res);
    }

    @Test
    void updateLocation404() {
        Location newLocation = new Location().longitude(23F).latitude(32F);
        Mockito.when(orderService.getOrderById(anyLong())).thenReturn(Optional.empty());

        var res = controller.updateLocation(0L, 1L, newLocation);
        assertEquals(new ResponseEntity<>(HttpStatus.NOT_FOUND), res);
    }

    @Test
    void updateLocation400() {
        Order order = new Order().id(2L);
        Location newLocation = new Location().longitude(23F).latitude(32F);
        Mockito.when(orderService.getOrderById(anyLong())).thenReturn(Optional.of(order));
        Mockito.when(orderService.updateLocation(order, newLocation)).thenReturn(Optional.empty());

        var res = controller.updateLocation(0L, 1L, newLocation);
        assertEquals(new ResponseEntity<>(HttpStatus.BAD_REQUEST), res);
    }

    @Test
    void updateLocation200() {
        Order order = new Order().id(2L);
        Location newLocation = new Location().longitude(23F).latitude(32F);
        Mockito.when(orderService.getOrderById(anyLong())).thenReturn(Optional.of(order));
        Mockito.when(orderService.updateLocation(order, newLocation)).thenReturn(Optional.of(newLocation));

        var res = controller.updateLocation(0L, 1L, newLocation);
        assertEquals(new ResponseEntity<>(HttpStatus.OK), res);
    }

    @Test
    void getOrderLocation403() {
        Order order = new Order().id(2L);
        Location newLocation = new Location().longitude(23F).latitude(32F);

        Mockito.when(authorizationService.checkIfUserIsAuthorized(11L, "getOrderLocation", 2L))
            .thenReturn(Optional.of(new ResponseEntity<>(HttpStatus.FORBIDDEN)));
        Mockito.when(orderService.getOrderById(anyLong())).thenReturn(Optional.of(order));
        Mockito.when(orderService.getOrderLocation(order)).thenReturn(Optional.of(newLocation));

        var ret = controller.getOrderLocation(2L, 11L);
        assertEquals(new ResponseEntity<>(HttpStatus.FORBIDDEN), ret);
    }

    @Test
    void updateLocation403() {
        Order order = new Order().id(2L);
        Location newLocation = new Location().longitude(23F).latitude(32F);

        Mockito.when(authorizationService.checkIfUserIsAuthorized(11L, "updateLocation", 2L))
            .thenReturn(Optional.of(new ResponseEntity<>(HttpStatus.FORBIDDEN)));
        Mockito.when(orderService.getOrderById(anyLong())).thenReturn(Optional.of(order));
        Mockito.when(orderService.updateLocation(order, newLocation)).thenReturn(Optional.of(newLocation));

        var ret = controller.updateLocation(2L, 11L, newLocation);
        assertEquals(new ResponseEntity<>(HttpStatus.FORBIDDEN), ret);
    }

    @Test
    void getEta200() {
        Mockito.when(orderService.getEta(1L)).thenReturn(Optional.of(eta));
        var res = controller.getETA(2L, 1L);
        assertEquals(new ResponseEntity<>(eta, HttpStatus.OK), res);
    }

    @Test
    void getEta403() {
        Mockito.when(orderService.getEta(1L)).thenReturn(Optional.of(eta));
        Mockito.when(authorizationService.checkIfUserIsAuthorized(anyLong(), anyString(), any()))
            .thenReturn(Optional.of(new ResponseEntity<>(HttpStatus.FORBIDDEN)));

        var res = controller.getETA(2L, 1L);
        assertEquals(new ResponseEntity<>(HttpStatus.FORBIDDEN), res);
    }

    @Test
    void getEtaA404() {
        Mockito.when(orderService.getEta(1L)).thenReturn(Optional.empty());
        var res = controller.getETA(2L, 1L);
        assertEquals(new ResponseEntity<>(HttpStatus.NOT_FOUND), res);
    }

    @Test
    void getDistance200() {
        Mockito.when(orderService.getDistance(1L)).thenReturn(Optional.of(5.0F));
        var res = controller.getOrderDistance(1L, 2L);
        assertEquals(new ResponseEntity<>(5.0F, HttpStatus.OK), res);
    }

    @Test
    void getDistance403() {
        Mockito.when(orderService.getDistance(1L)).thenReturn(Optional.of(5.0F));
        Mockito.when(authorizationService.checkIfUserIsAuthorized(anyLong(), anyString(), any()))
            .thenReturn(Optional.of(new ResponseEntity<>(HttpStatus.FORBIDDEN)));

        var res = controller.getOrderDistance(1L, 2L);
        assertEquals(new ResponseEntity<>(HttpStatus.FORBIDDEN), res);
    }

    @Test
    void getDistance404() {
        Mockito.when(orderService.getDistance(1L)).thenReturn(Optional.empty());
        var res = controller.getOrderDistance(1L, 2L);
        assertEquals(new ResponseEntity<>(HttpStatus.NOT_FOUND), res);
    }


}