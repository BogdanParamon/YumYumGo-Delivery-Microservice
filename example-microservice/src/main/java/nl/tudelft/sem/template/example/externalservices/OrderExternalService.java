package nl.tudelft.sem.template.example.externalservices;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class OrderExternalService {

    private final RestTemplate restTemplate;

    private final String orderServerBaseUrl = "http://localhost:8082";

    public OrderExternalService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Retrieves the order from the order microservice based on the provided order ID.
     *
     * @param userId  The ID of the user.
     * @param orderId The ID of the order.
     * @return The response entity obtained from the order service as a String.
     */
    public ResponseEntity<String> getOrder(Long userId, Long orderId) {
        String getOrderServiceEndpoint = orderServerBaseUrl + "/order/" + orderId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("userId", String.valueOf(userId));

        return restTemplate.exchange(
            getOrderServiceEndpoint,
            org.springframework.http.HttpMethod.GET,
            new org.springframework.http.HttpEntity<>(headers),
            String.class
        );
    }


}
