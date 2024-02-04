package nl.tudelft.sem.template.example.wiremock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.http.MediaType;

/**
 * Utility class for managing WireMock server instances.
 */
public class WireMockConfig {

    private static final int USER_SERVER_PORT = 4269;
    private static final int ORDER_SERVER_PORT = 8082;
    public static WireMockServer userMicroservice;
    public static WireMockServer orderMicroservice;

    /**
     * Starts the WireMock user server.
     * If the server is already running, this method has no effect.
     */
    public static void startUserServer() {
        if (userMicroservice == null || !userMicroservice.isRunning()) {
            userMicroservice = new WireMockServer(WireMockConfiguration.options().port(USER_SERVER_PORT));
            userMicroservice.start();
        }
    }

    /**
     * Starts the WireMock order server.
     * If the server is already running, this method has no effect.
     */
    public static void startOrderServer() {
        if (orderMicroservice == null || !orderMicroservice.isRunning()) {
            orderMicroservice = new WireMockServer(WireMockConfiguration.options().port(ORDER_SERVER_PORT));
            orderMicroservice.start();
        }
    }

    /**
     * Stops the WireMock user server if it is running.
     * If the server is not running, this method has no effect.
     */
    public static void stopUserServer() {
        if (userMicroservice != null && userMicroservice.isRunning()) {
            userMicroservice.stop();
        }
    }

    /**
     * Stops the WireMock order server if it is running.
     * If the server is not running, this method has no effect.
     */
    public static void stopOrderServer() {
        if (orderMicroservice != null && orderMicroservice.isRunning()) {
            orderMicroservice.stop();
        }
    }

    /**
     * Sets up a WireMock stub for simulating a successful authorization.
     * Use this in test classes to ignore the authorization.
     */
    public static void ignoreAuthorization() {
        userMicroservice.stubFor(WireMock.get(urlPathMatching(("/user/[0-9]+/type")))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody("admin")));
    }

}
