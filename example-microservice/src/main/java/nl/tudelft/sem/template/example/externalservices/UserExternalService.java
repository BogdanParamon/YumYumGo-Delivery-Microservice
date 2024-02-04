package nl.tudelft.sem.template.example.externalservices;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class UserExternalService {

    private final RestTemplate restTemplate;

    private final String userServerBaseUrl = "http://localhost:4269";

    public UserExternalService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Retrieves the user type from the user microservice based on the provided user ID.
     *
     * @param userId The ID of the user.
     * @return The user type obtained from the user service as a String.
     */
    public String getUserTypeFromService(Long userId) {
        String userTypeServiceEndpoint = userServerBaseUrl + "/user/" + userId + "/type";
        return restTemplate.getForObject(userTypeServiceEndpoint, String.class);
    }
}
