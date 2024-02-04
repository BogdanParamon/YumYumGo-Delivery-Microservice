package nl.tudelft.sem.template.example.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.github.tomakehurst.wiremock.client.WireMock;
import nl.tudelft.sem.template.example.domain.order.OrderRepository;
import nl.tudelft.sem.template.example.domain.user.CourierRepository;
import nl.tudelft.sem.template.example.domain.user.VendorRepository;
import nl.tudelft.sem.template.example.externalservices.OrderExternalService;
import nl.tudelft.sem.template.example.wiremock.WireMockConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class DbUtilsTest {

    private final OrderExternalService orderExternalService = new OrderExternalService();
    private DbUtils dbUtils;
    private OrderRepository orderRepo;
    private VendorRepository vendorRepo;
    private CourierRepository courierRepo;

    @BeforeEach
    void setUp() {
        WireMockConfig.startOrderServer();
        orderRepo = mock(OrderRepository.class);
        vendorRepo = mock(VendorRepository.class);
        courierRepo = mock(CourierRepository.class);
        dbUtils = new DbUtils(orderRepo, vendorRepo, courierRepo, orderExternalService);
    }

    @Test
    void testCustomerBelongsToOrder() {
        Mockito.when(orderRepo.existsByIdAndCourierId(11L, 1L)).thenReturn(false);
        Mockito.when(orderRepo.existsByIdAndVendorId(11L, 1L)).thenReturn(false);

        WireMockConfig.orderMicroservice.stubFor(WireMock.get(urlPathMatching("/order/11"))
            .withHeader("userId", WireMock.equalTo("1"))
            .willReturn(aResponse()
                .withStatus(200)));

        assertTrue(dbUtils.userBelongsToOrder(1L, 11L));
    }

    @Test
    void testCustomerDoesNotBelongToOrder() {
        Mockito.when(orderRepo.existsByIdAndCourierId(11L, 1L)).thenReturn(false);
        Mockito.when(orderRepo.existsByIdAndVendorId(11L, 1L)).thenReturn(false);

        WireMockConfig.orderMicroservice.stubFor(WireMock.get(urlPathMatching("/order/11"))
            .withHeader("userId", WireMock.equalTo("1"))
            .willReturn(aResponse()
                .withStatus(401)));

        assertFalse(dbUtils.userBelongsToOrder(1L, 11L));
    }

    @Test
    void testCourierBelongsToOrder() {
        Mockito.when(orderRepo.existsByIdAndCourierId(11L, 1L)).thenReturn(true);
        Mockito.when(orderRepo.existsByIdAndVendorId(11L, 1L)).thenReturn(false);
        Mockito.when(courierRepo.existsByIdAndBossId(11L, 1L)).thenReturn(false);
        assertTrue(dbUtils.userBelongsToOrder(1L, 11L));
    }

    @Test
    void testCourierDoesNotBelongToVendor() {
        Mockito.when(courierRepo.existsByIdAndBossId(11L, 1L)).thenReturn(false);
        assertFalse(dbUtils.courierBelongsToVendor(11L, 1L));
    }

    @AfterEach()
    void tearDown() {
        WireMockConfig.stopOrderServer();
    }
}
