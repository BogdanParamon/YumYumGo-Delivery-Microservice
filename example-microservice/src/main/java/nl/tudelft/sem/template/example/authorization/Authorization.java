package nl.tudelft.sem.template.example.authorization;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import nl.tudelft.sem.template.example.externalservices.UserExternalService;
import org.springframework.http.ResponseEntity;

public class Authorization extends Handler {

    private final UserExternalService userExternalService;
    // Maps method names to the user types that are allowed to call them
    protected HashMap<String, List<Authorization.UserType>> permissions;

    public Authorization(UserExternalService userExternalService,
                         HashMap<String, List<Authorization.UserType>> permissions) {
        this.userExternalService = userExternalService;
        this.permissions = permissions;
    }

    /**
     * Authorizes a user based on the provided user ID and required user type.
     *
     * @param userId     The ID of the user to be authorized.
     * @param methodName Name of the method that was called.
     * @return An optional containing a ResponseEntity with an error message if authorization fails, or empty if authorized.
     */
    @Override
    public Optional<ResponseEntity> check(Long userId, String methodName, Long other) {

        Authorization.UserType actualUserType = getUserType(userId);
        if (actualUserType == Authorization.UserType.NAN) {
            return Optional.of(ResponseEntity.status(500).body("Error while retrieving user type"));
        }
        if (actualUserType == Authorization.UserType.ADMIN) {
            return Optional.empty();
        }
        if (!permissions.get(methodName).contains(actualUserType)) {
            return Optional.of(ResponseEntity.status(403).body("User with id " + userId + " does not have access rights"));
        }
        return checkNext(userId, methodName, other);
    }

    /**
     * Authorizes a user based on the provided user ID.
     *
     * @param userId The ID of the user to be authorized.
     * @return An optional containing a ResponseEntity with an error message if authorization fails, or empty if authorized.
     */
    public Optional<ResponseEntity> authorizeAdminOnly(Long userId) {

        Authorization.UserType actualUserType = getUserType(userId);
        if (actualUserType == Authorization.UserType.NAN) {
            return Optional.of(ResponseEntity.status(500).body("Error while retrieving user type"));
        }
        if (actualUserType != Authorization.UserType.ADMIN) {
            return Optional.of(ResponseEntity.status(403).body("User with id " + userId + " does not have access rights"));
        }
        return Optional.empty();
    }

    /**
     * Retrieves the user type from the user microservice based on the provided user ID.
     *
     * @param userId The ID of the user.
     * @return The user type obtained from the user service, or UserType.NAN if an error occurs.
     */
    private Authorization.UserType getUserType(Long userId) {
        try {
            return parseUserType(userExternalService.getUserTypeFromService(userId));
        } catch (Exception e) {
            return Authorization.UserType.NAN;
        }
    }

    /**
     * Parses the string representation of a user type into the corresponding UserType enum value.
     *
     * @param userType The string representation of the user type.
     * @return The corresponding UserType enum value.
     * @throws IllegalArgumentException If the provided user type is invalid.
     */
    public Authorization.UserType parseUserType(String userType) {
        return switch (userType) {
            case "vendor" -> Authorization.UserType.VENDOR;
            case "courier" -> Authorization.UserType.COURIER;
            case "admin" -> Authorization.UserType.ADMIN;
            case "customer" -> Authorization.UserType.CUSTOMER;
            default -> throw new IllegalArgumentException("Invalid user type: " + userType);
        };
    }

    public enum UserType {
        VENDOR,
        COURIER,
        ADMIN,
        CUSTOMER,

        NAN
    }

}
