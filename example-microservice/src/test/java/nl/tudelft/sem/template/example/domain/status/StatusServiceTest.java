package nl.tudelft.sem.template.example.domain.status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import nl.tudelft.sem.template.example.domain.exception.DeliveryExceptionRepository;
import nl.tudelft.sem.template.example.domain.order.OrderRepository;
import nl.tudelft.sem.template.example.domain.order.StatusService;
import nl.tudelft.sem.template.model.DeliveryException;
import nl.tudelft.sem.template.model.Order;
import nl.tudelft.sem.template.model.Time;
import nl.tudelft.sem.template.model.UpdateToDeliveredRequest;
import nl.tudelft.sem.template.model.UpdateToGivenToCourierRequest;
import nl.tudelft.sem.template.model.UpdateToPreparingRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class StatusServiceTest {

    public OrderRepository orderRepo;
    public Order order1;
    public Order order2;
    public Order order3;
    public DeliveryException delException1;
    public Order order4;
    public Order order5;
    public Order order6;


    public StatusService ss;
    private DeliveryExceptionRepository exceptionRepo;

    @BeforeEach
    void setUp() {
        this.orderRepo = mock(OrderRepository.class);
        this.exceptionRepo = mock(DeliveryExceptionRepository.class);
        this.order1 = new Order().id(1L).status(Order.StatusEnum.PENDING);
        this.order2 = new Order().id(1L).status(Order.StatusEnum.GIVEN_TO_COURIER);
        this.order3 = new Order().id(1L).status(Order.StatusEnum.PREPARING);
        this.order4 = new Order().id(1L).status(Order.StatusEnum.IN_TRANSIT).timeValues(new Time().prepTime("00:22::00"));
        this.order5 = new Order().id(1L).status(Order.StatusEnum.ACCEPTED);
        this.order6 = new Order().id(1L).status(Order.StatusEnum.ACCEPTED).timeValues(new Time());
        this.delException1 =
            new DeliveryException().exceptionType(DeliveryException.ExceptionTypeEnum.OTHER).message("Test exception")
                .isResolved(false).id(1L);
        this.ss = new StatusService(orderRepo, exceptionRepo);
    }

    @Test
    void updateStatusToAccepted200() {
        Order order11 = new Order().id(1L).status(Order.StatusEnum.ACCEPTED);

        Mockito.when(orderRepo.findById(anyLong())).thenReturn(Optional.of(order1));
        Mockito.lenient().when(orderRepo.saveAndFlush(order1)).thenReturn(order11);

        assertEquals(order1.getStatus(), Order.StatusEnum.PENDING);

        Optional<Order> res = ss.updateStatusToAccepted(order1.getId());
        assertTrue(res.isPresent());
        assertEquals(res.get().getStatus(), Order.StatusEnum.ACCEPTED);

        assertEquals(order1.getStatus(), Order.StatusEnum.ACCEPTED);
    }

    @Test
    void updateStatusToAccepted404() {
        Mockito.when(orderRepo.getOne(anyLong())).thenThrow(new javax.persistence.EntityNotFoundException());

        Optional<Order> ret = ss.updateStatusToAccepted(order1.getId());
        assertTrue(ret.isEmpty());
    }

    @Test
    void updateStatusToRejected200() {
        Order order11 = new Order().id(1L).status(Order.StatusEnum.REJECTED);

        Mockito.when(orderRepo.findById(anyLong())).thenReturn(Optional.of(order1));
        Mockito.lenient().when(orderRepo.saveAndFlush(order1)).thenReturn(order11);

        assertEquals(order1.getStatus(), Order.StatusEnum.PENDING);

        Optional<Order> res = ss.updateStatusToRejected(order1.getId());
        assertTrue(res.isPresent());
        assertEquals(res.get().getStatus(), Order.StatusEnum.REJECTED);

        assertEquals(order1.getStatus(), Order.StatusEnum.REJECTED);
    }

    @Test
    void updateStatusToRejected404() {
        Mockito.when(orderRepo.getOne(anyLong())).thenThrow(new javax.persistence.EntityNotFoundException());

        Optional<Order> ret = ss.updateStatusToRejected(order1.getId());
        assertTrue(ret.isEmpty());
    }

    @Test
    void updateStatusToGivenToCourier200() {
        Order order33 = new Order().id(1L).status(Order.StatusEnum.GIVEN_TO_COURIER).courierId(3L);
        UpdateToGivenToCourierRequest req = new UpdateToGivenToCourierRequest();
        req.courierId(3L);

        Mockito.when(orderRepo.findById(anyLong())).thenReturn(Optional.of(order3));
        Mockito.lenient().when(orderRepo.saveAndFlush(order3)).thenReturn(order33);

        Optional<Order> ret = ss.updateStatusToGivenToCourier(order3.getId(), req);
        assertTrue(ret.isPresent());
        assertEquals(ret.get().getStatus(), Order.StatusEnum.GIVEN_TO_COURIER);
        assertEquals(ret.get().getCourierId(), 3L);

        assertEquals(order3.getStatus(), Order.StatusEnum.GIVEN_TO_COURIER);
        assertEquals(order3.getCourierId(), 3L);
    }

    @Test
    void updateStatusToGivenToCourier404() {
        UpdateToGivenToCourierRequest req = new UpdateToGivenToCourierRequest();
        req.courierId(3L);

        Mockito.when(orderRepo.getOne(anyLong())).thenThrow(new javax.persistence.EntityNotFoundException());

        Optional<Order> ret = ss.updateStatusToGivenToCourier(order3.getId(), req);
        assertTrue(ret.isEmpty());
    }

    @Test
    void updateStatusToInTransit200() {
        Order order22 = new Order().id(1L).status(Order.StatusEnum.IN_TRANSIT);

        Mockito.when(orderRepo.findById(anyLong())).thenReturn(Optional.of(order2));
        Mockito.lenient().when(orderRepo.saveAndFlush(order2)).thenReturn(order22);

        assertEquals(order2.getStatus(), Order.StatusEnum.GIVEN_TO_COURIER);

        Optional<Order> ret = ss.updateStatusToInTransit(order2.getId());
        assertTrue(ret.isPresent());
        assertEquals(ret.get().getStatus(), Order.StatusEnum.IN_TRANSIT);

        assertEquals(order2.getStatus(), Order.StatusEnum.IN_TRANSIT);
    }

    @Test
    void updateStatusToInTransit404() {
        Mockito.when(orderRepo.getOne(anyLong())).thenThrow(new javax.persistence.EntityNotFoundException());

        Optional<Order> ret = ss.updateStatusToInTransit(order2.getId());
        assertTrue(ret.isEmpty());
    }

    @Test
    void getOrderStatus200() {
        Mockito.when(orderRepo.findById(anyLong())).thenReturn(Optional.of(order1));

        Optional<Order.StatusEnum> status = ss.getOrderStatus(1L);

        assertTrue(status.isPresent());
        assertEquals(status.get(), Order.StatusEnum.PENDING);
    }

    @Test
    void getOrderStatus404() {
        Mockito.when(orderRepo.getOne(anyLong())).thenThrow(new javax.persistence.EntityNotFoundException());

        Optional<Order.StatusEnum> status = ss.getOrderStatus(1L);

        assertTrue(status.isEmpty());
    }

    @Test
    void addDeliveryExceptionEmpty() {
        Optional<DeliveryException> res = Optional.empty();
        assertEquals(res, ss.addDeliveryException(null));
    }

    @Test
    void addDeliveryExceptionSuccess() {
        Mockito.when(exceptionRepo.saveAndFlush(delException1)).thenReturn(delException1);

        Optional<DeliveryException> res = Optional.of(delException1);
        assertEquals(res, ss.addDeliveryException(delException1));
    }

    @Test
    void updateStatusToDelivered200() {
        OffsetDateTime deliveryTime = OffsetDateTime.of(2023, 12, 17, 12, 30, 0, 0, ZoneOffset.UTC);
        UpdateToDeliveredRequest req = new UpdateToDeliveredRequest().actualDeliveryTime(deliveryTime);
        Order order44 = new Order().id(order4.getId()).status(Order.StatusEnum.DELIVERED)
            .timeValues(new Time().actualDeliveryTime(deliveryTime).prepTime(order4.getTimeValues().getPrepTime()));
        Mockito.when(orderRepo.findById(anyLong())).thenReturn(Optional.of(order4));
        Mockito.when(orderRepo.saveAndFlush(order4)).thenReturn(order44);

        Optional<Order> ret = ss.updateStatusToDelivered(order4.getId(), req);

        assertTrue(ret.isPresent());
        assertEquals(Order.StatusEnum.DELIVERED, ret.get().getStatus());
        assertNotNull(ret.get().getTimeValues().getActualDeliveryTime());
        assertEquals(deliveryTime, ret.get().getTimeValues().getActualDeliveryTime());
        assertEquals(order4.getTimeValues(), ret.get().getTimeValues());

        ArgumentCaptor<Order> argumentCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepo).saveAndFlush(argumentCaptor.capture());
        Order res = argumentCaptor.getValue();

        assertEquals(res.getStatus(), Order.StatusEnum.DELIVERED);
    }

    @Test
    void updateStatusToDeliveredNullTimeValues() {
        order4.setTimeValues(null);
        Mockito.when(orderRepo.findById(anyLong())).thenReturn(Optional.of(order4));
        Mockito.when(orderRepo.saveAndFlush(order4)).thenReturn(order4);

        OffsetDateTime deliveryTime = OffsetDateTime.of(2023, 12, 17, 12, 30, 0, 0, ZoneOffset.UTC);
        UpdateToDeliveredRequest req = new UpdateToDeliveredRequest().actualDeliveryTime(deliveryTime);
        Optional<Order> ret = ss.updateStatusToDelivered(order4.getId(), req);

        assertTrue(ret.isEmpty());
    }

    @Test
    void updateStatusToDeliveredNullDeliveryTime() {
        Mockito.when(orderRepo.findById(anyLong())).thenReturn(Optional.of(order4));
        Mockito.when(orderRepo.saveAndFlush(order4)).thenReturn(order4);

        UpdateToDeliveredRequest req = new UpdateToDeliveredRequest();
        Optional<Order> ret = ss.updateStatusToDelivered(order4.getId(), req);

        assertTrue(ret.isEmpty());
    }

    @Test
    void updateStatusToDeliveredDeliveryTimeAlreadySet() {
        OffsetDateTime deliveryTime = OffsetDateTime.of(2023, 12, 17, 12, 30, 0, 0, ZoneOffset.UTC);
        order4.setTimeValues(new Time().actualDeliveryTime(deliveryTime));
        Mockito.when(orderRepo.findById(anyLong())).thenReturn(Optional.of(order4));
        Mockito.when(orderRepo.saveAndFlush(order4)).thenReturn(order4);

        UpdateToDeliveredRequest req = new UpdateToDeliveredRequest().actualDeliveryTime(deliveryTime);
        Optional<Order> ret = ss.updateStatusToDelivered(order4.getId(), req);

        assertTrue(ret.isEmpty());
    }

    @Test
    void updateStatusToPreparing200timeValuesNull() {
        Mockito.when(orderRepo.findById(anyLong())).thenReturn(Optional.of(order5));
        UpdateToPreparingRequest updateToPreparingRequest = new UpdateToPreparingRequest().prepTime("00:22::00");
        OffsetDateTime currentDateTime = OffsetDateTime.now();
        updateToPreparingRequest.setExpectedDeliveryTime(currentDateTime);
        Time timeValues = new Time().expectedDeliveryTime(updateToPreparingRequest.getExpectedDeliveryTime())
            .prepTime(updateToPreparingRequest.getPrepTime());
        Order order55 = new Order().id(order5.getId()).status(Order.StatusEnum.PREPARING).timeValues(timeValues);
        Mockito.when(orderRepo.saveAndFlush(order5)).thenReturn(order55);

        Optional<Order> ret = ss.updateStatusToPreparing(order5.getId(), updateToPreparingRequest);
        assertTrue(ret.isPresent());
        assertEquals(ret.get().getStatus(), Order.StatusEnum.PREPARING);
        assertEquals(ret.get().getTimeValues(), timeValues);

        assertEquals(order5.getStatus(), Order.StatusEnum.PREPARING);
        assertEquals(order5.getTimeValues(), timeValues);

    }

    @Test
    void updateStatusToPreparing200timeValuesNotNull() {
        Mockito.when(orderRepo.findById(anyLong())).thenReturn(Optional.of(order6));
        UpdateToPreparingRequest updateToPreparingRequest = new UpdateToPreparingRequest().prepTime("00:22::00");
        OffsetDateTime currentDateTime = OffsetDateTime.now();
        updateToPreparingRequest.setExpectedDeliveryTime(currentDateTime);
        Time timeValues = new Time().expectedDeliveryTime(updateToPreparingRequest.getExpectedDeliveryTime())
            .prepTime(updateToPreparingRequest.getPrepTime());
        Order order55 = new Order().id(order6.getId()).status(Order.StatusEnum.PREPARING).timeValues(timeValues);
        Mockito.when(orderRepo.saveAndFlush(order6)).thenReturn(order55);

        Optional<Order> ret = ss.updateStatusToPreparing(order6.getId(), updateToPreparingRequest);
        assertTrue(ret.isPresent());
        assertEquals(ret.get().getStatus(), Order.StatusEnum.PREPARING);
        assertEquals(ret.get().getTimeValues(), timeValues);
    }

    @Test
    void updateStatusToPreparing404() {
        Mockito.when(orderRepo.findById(anyLong())).thenReturn(Optional.empty());
        UpdateToPreparingRequest updateToPreparingRequest = new UpdateToPreparingRequest().prepTime("00:22::00");
        OffsetDateTime currentDateTime = OffsetDateTime.now();
        updateToPreparingRequest.setExpectedDeliveryTime(currentDateTime);
        Time timeValues = new Time().expectedDeliveryTime(updateToPreparingRequest.getExpectedDeliveryTime())
            .prepTime(updateToPreparingRequest.getPrepTime());


        Optional<Order> ret = ss.updateStatusToPreparing(order5.getId(), updateToPreparingRequest);
        assertTrue(ret.isEmpty());
    }

    @Test
    void updateStatusToDeliveredEmpty() {
        Mockito.when(orderRepo.findById(0L)).thenReturn(Optional.empty());

        var res = ss.updateStatusToDelivered(0L, null);
        assertTrue(res.isEmpty());
    }

}
