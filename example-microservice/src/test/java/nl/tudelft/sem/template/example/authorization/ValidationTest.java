package nl.tudelft.sem.template.example.authorization;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;

import com.github.tomakehurst.wiremock.client.WireMock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import nl.tudelft.sem.template.example.controllers.OrderController;
import nl.tudelft.sem.template.example.domain.order.OrderRepository;
import nl.tudelft.sem.template.example.domain.order.OrderService;
import nl.tudelft.sem.template.example.domain.user.CourierService;
import nl.tudelft.sem.template.example.domain.user.VendorRepository;
import nl.tudelft.sem.template.example.externalservices.UserExternalService;
import nl.tudelft.sem.template.example.utils.DbUtils;
import nl.tudelft.sem.template.example.wiremock.WireMockConfig;
import nl.tudelft.sem.template.model.Location;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public class ValidationTest {

    private OrderService orderService;
    private OrderController controller;

    private final UserExternalService userExternalService = new UserExternalService();

    private DbUtils dbUtils;
    private HashMap<String, BiFunction<Long, Long, Boolean>> validationMethods;

    private final HashMap<String, List<Authorization.UserType>> permissions = new HashMap<>(
        Map.of("getFinalDestination", List.of(Authorization.UserType.CUSTOMER))
    );

    @BeforeEach
    void setUp() {
        WireMockConfig.startUserServer();
        WireMockConfig.userMicroservice.stubFor(WireMock.get(urlPathMatching(("/user/11/type")))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody("customer")));
        dbUtils = Mockito.mock(DbUtils.class);
        orderService = Mockito.mock(OrderService.class);
        validationMethods = new HashMap<>(
            Map.of(
                "getFinalDestination", dbUtils::userBelongsToOrder,
                "getPickupDestination", dbUtils::userBelongsToOrder
            )
        );
        AuthorizationService authorizationService =
            new AuthorizationService(dbUtils, userExternalService, permissions, validationMethods);
        CourierService courierService = Mockito.mock(CourierService.class);
        OrderRepository orderRepository = Mockito.mock(OrderRepository.class);
        VendorRepository vendorRepository = Mockito.mock(VendorRepository.class);
        controller =
            new OrderController(orderService, courierService, authorizationService, orderRepository, vendorRepository);
    }

    @Test
    void getFinalDestinationWorks() {
        Mockito.when(dbUtils.userBelongsToOrder(11L, 1L)).thenReturn(true);
        Optional<Location> proper = Optional.of(new Location().latitude(1F).longitude(2F));
        Mockito.when(orderService.getFinalDestinationOfOrder(anyLong())).thenReturn(proper);
        var res = controller.getFinalDestination(11L, 1L);
        assertEquals(new ResponseEntity<>(proper.get(), HttpStatus.OK), res);
    }

    @Test
    void getFinalDestinationNoPermission() {
        Mockito.when(dbUtils.userBelongsToOrder(11L, 1L)).thenReturn(false);
        Optional<Location> proper = Optional.of(new Location().latitude(1F).longitude(2F));
        Mockito.when(orderService.getFinalDestinationOfOrder(anyLong())).thenReturn(proper);

        var res = controller.getFinalDestination(11L, 1L);
        assertEquals(ResponseEntity.status(403).body("User with id " + 11 + " does not have access rights"), res);
    }

    @Test
    void getFinalDestinationNoMapping() {
        validationMethods.remove("getFinalDestination");
        var res = controller.getFinalDestination(11L, 1L);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, res.getStatusCode());
    }


    @AfterEach()
    void tearDown() {
        WireMockConfig.stopUserServer();
    }
}

