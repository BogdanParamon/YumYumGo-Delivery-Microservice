package nl.tudelft.sem.template.example.domain.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import nl.tudelft.sem.template.example.domain.order.orderstrategy.GeneralOrdersStrategy;
import nl.tudelft.sem.template.example.domain.user.VendorRepository;
import nl.tudelft.sem.template.model.Location;
import nl.tudelft.sem.template.model.Order;
import nl.tudelft.sem.template.model.Vendor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GeneralOrdersStrategyTest {

    private OrderRepository orderRepo;
    private VendorRepository vendorRepo;

    private GeneralOrdersStrategy generalStrategy;

    private Order order1;
    private Order order2;
    private Order order3;

    private Order order4;

    private Vendor vendor1;

    private Vendor vendor2;

    @BeforeEach
    void setUp() {
        this.orderRepo = mock(OrderRepository.class);
        this.vendorRepo = mock(VendorRepository.class);

        this.order1 = new Order().id(1L).vendorId(2L).deliveryDestination(new Location().latitude(11F).longitude(22F))
            .status(Order.StatusEnum.PREPARING);
        this.order2 = new Order().id(22L).vendorId(2L).deliveryDestination(new Location().latitude(11F).longitude(22F))
            .status(Order.StatusEnum.PENDING);
        this.order3 = new Order().id(44L).vendorId(3L).deliveryDestination(new Location().latitude(11F).longitude(22F))
            .status(Order.StatusEnum.PREPARING);
        this.order4 = new Order().id(33L).vendorId(3L).deliveryDestination(new Location().latitude(11F).longitude(22F))
            .status(Order.StatusEnum.PREPARING);

        this.vendor1 = new Vendor().id(2L).location(new Location().latitude(22F).longitude(33F)).hasCouriers(false);
        this.vendor2 = new Vendor().id(3L).location(new Location().latitude(22F).longitude(33F)).hasCouriers(true);
        this.generalStrategy = new GeneralOrdersStrategy(orderRepo, vendorRepo);
    }

    @Test
    void availableOrdersWrongParameters() {
        assertEquals(Optional.empty(), generalStrategy.availableOrders(Optional.of(31L)));
    }

    @Test
    void availableOrdersFiltersVendor() {
        when(orderRepo.findByStatus(any())).thenReturn(List.of(order1, order3));
        when(vendorRepo.findById(2L)).thenReturn(Optional.of(vendor1));
        when(vendorRepo.findById(3L)).thenReturn(Optional.of(vendor2));
        // this checks filtering out both for invalid vendor and status values
        assertEquals(Optional.of(List.of(order1)), generalStrategy.availableOrders(Optional.empty()));
    }

    @Test
    void availableOrdersFilters2() {
        when(orderRepo.findByStatus(any())).thenReturn(List.of(order1, order2.status(Order.StatusEnum.PREPARING)));
        when(vendorRepo.findById(2L)).thenReturn(Optional.of(vendor1));
        when(vendorRepo.findById(3L)).thenReturn(Optional.of(vendor2));
        // this checks filtering out both for invalid vendor and status values
        assertEquals(Optional.of(List.of(order1, order2.status(Order.StatusEnum.PREPARING))),
            generalStrategy.availableOrders(Optional.empty()));
    }

    @Test
    void availableOrdersNoOutput() {
        when(orderRepo.findByStatus(any())).thenReturn(List.of());
        when(vendorRepo.findById(2L)).thenReturn(Optional.of(vendor1));
        when(vendorRepo.findById(3L)).thenReturn(Optional.of(vendor2));
        // this checks filtering out both for invalid vendor and status values
        assertEquals(Optional.of(List.of()), generalStrategy.availableOrders(Optional.empty()));
    }

    @Test
    void availableOrdersFiltersNoResults() {
        when(orderRepo.findByStatus(any())).thenReturn(List.of(order4));
        when(vendorRepo.findById(2L)).thenReturn(Optional.of(vendor1));
        when(vendorRepo.findById(3L)).thenReturn(Optional.of(vendor2));
        // this checks filtering out both for invalid vendor and status values
        assertEquals(Optional.of(List.of()), generalStrategy.availableOrders(Optional.empty()));
    }

    @Test
    void availableOrdersFiltersCorrectly() {
        when(orderRepo.findByStatus(any())).thenReturn(List.of(order1, order3));
        when(vendorRepo.findById(2L)).thenReturn(Optional.of(vendor1));
        when(vendorRepo.findById(3L)).thenReturn(Optional.of(vendor2));
        // this checks filtering out both for invalid vendor and status values
        assertEquals(Optional.of(List.of(order1)), generalStrategy.availableOrders(Optional.empty()));
    }

    @Test
    void availableOrdersFilters() {
        when(orderRepo.findByStatus(any())).thenReturn(List.of(order1, order3));
        when(vendorRepo.findById(2L)).thenReturn(Optional.of(vendor1));
        when(vendorRepo.findById(3L)).thenReturn(Optional.of(vendor2));
        // this checks filtering out both for invalid vendor and status values
        assertEquals(Optional.of(List.of(order1)), generalStrategy.availableOrders(Optional.empty()));
    }

    @Test
    void availableOrdersFiltersNull() {
        when(orderRepo.findByStatus(any())).thenReturn(List.of(order3.courierId(5L), order1));
        when(vendorRepo.findById(2L)).thenReturn(Optional.of(vendor1));
        when(vendorRepo.findById(3L)).thenReturn(Optional.of(vendor2));
        // this checks filtering out both for invalid vendor and status values
        assertEquals(Optional.of(List.of(order1)), generalStrategy.availableOrders(Optional.empty()));
    }


    @Test
    void vendorHasCouriersTrue() {
        when(vendorRepo.findById(anyLong())).thenReturn(Optional.of(vendor2));
        assertTrue(generalStrategy.vendorHasCouriers(2L));
    }

    @Test
    void vendorHasCouriersFalse() {
        when(vendorRepo.findById(anyLong())).thenReturn(Optional.of(vendor1));
        assertFalse(generalStrategy.vendorHasCouriers(2L));
    }

    @Test
    void vendorHasCouriersNoVendor() {
        when(vendorRepo.findById(anyLong())).thenReturn(Optional.empty());
        assertFalse(generalStrategy.vendorHasCouriers(2L));
    }


}