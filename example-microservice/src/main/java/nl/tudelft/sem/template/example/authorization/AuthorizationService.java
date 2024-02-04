package nl.tudelft.sem.template.example.authorization;

import static nl.tudelft.sem.template.example.authorization.Authorization.UserType.COURIER;
import static nl.tudelft.sem.template.example.authorization.Authorization.UserType.CUSTOMER;
import static nl.tudelft.sem.template.example.authorization.Authorization.UserType.VENDOR;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import javax.annotation.PostConstruct;
import lombok.Getter;
import nl.tudelft.sem.template.example.externalservices.UserExternalService;
import nl.tudelft.sem.template.example.utils.DbUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {

    private final UserExternalService userExternalService;
    private final DbUtils dbUtils;
    @Getter
    private HashMap<String, List<Authorization.UserType>> permissions;

    @Getter
    private HashMap<String, BiFunction<Long, Long, Boolean>> validationMethods;

    /**
     * Constructor for the AuthorizationService.
     *
     * @param dbUtils             the dbUtils
     * @param userExternalService the userExternalService
     * @param permissions         the permissions
     * @param validationMethods   the validationMethods
     */
    public AuthorizationService(DbUtils dbUtils, UserExternalService userExternalService,
                                HashMap<String, List<Authorization.UserType>> permissions,
                                HashMap<String, BiFunction<Long, Long, Boolean>> validationMethods) {
        this.userExternalService = userExternalService;
        this.permissions = permissions;
        this.dbUtils = dbUtils;
        this.validationMethods = validationMethods;
    }

    /**
     * Wrapper for authorize.
     *
     * @param response response
     * @return boolean for authorization
     */
    public static boolean doesNotHaveAuthority(Optional<ResponseEntity> response) {
        return response.isPresent();
    }

    /**
     * Checks if the user is authorized to call the method.
     *
     * @param userId     the id of the user
     * @param methodName the name of the method
     * @param other      id of order or vendor
     * @return an empty optional if the user is authorized, otherwise a response entity
     */
    public Optional<ResponseEntity> checkIfUserIsAuthorized(Long userId, String methodName, Long other) {
        Handler handler = Handler.link(new Authorization(userExternalService, permissions),
            new Validation(dbUtils, validationMethods));
        return handler.check(userId, methodName, other);
    }

    /**
     * Checks if the user is an admin.
     *
     * @param userId the id of the user
     * @return an empty optional if the user is authorized, otherwise a response entity
     */
    public Optional<ResponseEntity> authorizeAdminOnly(Long userId) {
        Authorization authorization = new Authorization(userExternalService, permissions);
        return authorization.authorizeAdminOnly(userId);
    }

    /**
     * Initializes the permissions map with default values if it is null.
     * You do not need to add permissions for admin only methods.
     * Initializes the validationMethods map with default values if it is null.
     */
    @PostConstruct
    public void init() throws NoSuchMethodException {
        permissions = new HashMap<>();
        validationMethods = new HashMap<>();

        // OrderController
        permissions.put("getNextOrderForVendor", List.of(COURIER));
        validationMethods.put("getNextOrderForVendor", dbUtils::courierBelongsToVendor);

        permissions.put("getIndependentOrders", List.of(COURIER));
        validationMethods.put("getIndependentOrders", dbUtils::courierBelongsToVendor);

        permissions.put("getFinalDestination", List.of(CUSTOMER, VENDOR, COURIER));
        validationMethods.put("getFinalDestination", dbUtils::userBelongsToOrder);

        permissions.put("getOrder", List.of(VENDOR));
        validationMethods.put("getOrder", dbUtils::userBelongsToOrder);

        permissions.put("getPickupDestination", List.of(COURIER));
        validationMethods.put("getPickupDestination", dbUtils::userBelongsToOrder);

        permissions.put("updateOrder", List.of(CUSTOMER, VENDOR, COURIER));
        validationMethods.put("updateOrder", dbUtils::userBelongsToOrder);

        permissions.put("getOrderRating", List.of(CUSTOMER, VENDOR));
        validationMethods.put("getOrderRating", dbUtils::userBelongsToOrder);

        permissions.put("putOrderRating", List.of(CUSTOMER));
        validationMethods.put("putOrderRating", dbUtils::userBelongsToOrder);

        permissions.put("setDeliverTime", List.of(CUSTOMER, VENDOR, COURIER));
        validationMethods.put("setDeliverTime", dbUtils::userBelongsToOrder);

        permissions.put("getETA", List.of(CUSTOMER, VENDOR, COURIER));
        validationMethods.put("getETA", dbUtils::userBelongsToOrder);

        permissions.put("getOrderDistance", List.of(CUSTOMER, VENDOR, COURIER));
        validationMethods.put("getOrderDistance", dbUtils::userBelongsToOrder);

        // StatusController
        permissions.put("updateToAccepted", List.of(VENDOR));
        validationMethods.put("updateToAccepted", dbUtils::userBelongsToOrder);

        permissions.put("updateToRejected", List.of(VENDOR));
        validationMethods.put("updateToRejected", dbUtils::userBelongsToOrder);

        permissions.put("updateToGivenToCourier", List.of(VENDOR));
        validationMethods.put("updateToGivenToCourier", dbUtils::userBelongsToOrder);

        permissions.put("updateToInTransit", List.of(COURIER));
        validationMethods.put("updateToInTransit", dbUtils::userBelongsToOrder);

        permissions.put("updateToPreparing", List.of(VENDOR));
        validationMethods.put("updateToPreparing", dbUtils::userBelongsToOrder);

        permissions.put("updateToDelivered", List.of(COURIER));
        validationMethods.put("updateToDelivered", dbUtils::userBelongsToOrder);

        permissions.put("getStatus", List.of(CUSTOMER, VENDOR, COURIER));
        validationMethods.put("getStatus", dbUtils::userBelongsToOrder);

        // UserController
        permissions.put("updateBossOfCourier", List.of(VENDOR));
        validationMethods.put("updateBossOfCourier", dbUtils::courierBelongsToVendor);

    }


}
