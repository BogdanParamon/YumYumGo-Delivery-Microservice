package nl.tudelft.sem.template.example.domain.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import nl.tudelft.sem.template.example.domain.order.orderstrategy.OrderPerVendorStrategy;
import nl.tudelft.sem.template.model.Location;
import nl.tudelft.sem.template.model.Order;
import nl.tudelft.sem.template.model.Vendor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderPerVendorStrategyTest {

    private OrderRepository orderRepo;

    private OrderPerVendorStrategy strategy;

    private Order order1;
    private Order order2;

    private Vendor vendor1;


    @BeforeEach
    void setUp() {
        this.orderRepo = mock(OrderRepository.class);

        this.order1 = new Order().id(1L).vendorId(2L).deliveryDestination(new Location().latitude(11F).longitude(22F))
            .status(Order.StatusEnum.PREPARING);
        this.order2 = new Order().id(22L).vendorId(2L).deliveryDestination(new Location().latitude(11F).longitude(22F))
            .status(Order.StatusEnum.PREPARING);

        this.vendor1 = new Vendor().id(2L).location(new Location().latitude(22F).longitude(33F)).hasCouriers(false);
        this.strategy = new OrderPerVendorStrategy(orderRepo);
    }

    @Test
    void availableOrdersFiltersNull() {
        when(orderRepo.findByVendorIdAndStatusAndCourierId(anyLong(), any(), any())).thenReturn(null);
        assertEquals(Optional.empty(), strategy.availableOrders(Optional.of(2L)));
    }

    @Test
    void availableOrdersGetsFirstOne() {
        when(orderRepo.findByVendorIdAndStatusAndCourierId(anyLong(), any(), any())).thenReturn(List.of(order1, order2));
        assertEquals(Optional.of(List.of(order1)), strategy.availableOrders(Optional.of(2L)));
    }

    @Test
    void availableOrdersEmptyList() {
        when(orderRepo.findByVendorIdAndStatusAndCourierId(anyLong(), any(), any())).thenReturn(List.of());
        assertEquals(Optional.of(List.of()), strategy.availableOrders(Optional.of(2L)));
    }

    @Test
    void availableOrdersNoVendorId() {
        when(orderRepo.findByVendorIdAndStatusAndCourierId(anyLong(), any(), any())).thenReturn(List.of());
        assertEquals(Optional.empty(), strategy.availableOrders(Optional.empty()));
    }
}