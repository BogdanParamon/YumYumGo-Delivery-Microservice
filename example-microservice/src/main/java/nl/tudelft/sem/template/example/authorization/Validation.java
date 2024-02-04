package nl.tudelft.sem.template.example.authorization;

import java.util.HashMap;
import java.util.Optional;
import java.util.function.BiFunction;
import nl.tudelft.sem.template.example.utils.DbUtils;
import org.springframework.http.ResponseEntity;

public class Validation extends Handler {

    private final HashMap<String, BiFunction<Long, Long, Boolean>> validationMethods;
    private final DbUtils dbUtils;

    public Validation(DbUtils dbUtils, HashMap<String, BiFunction<Long, Long, Boolean>> validationMethods) {
        this.dbUtils = dbUtils;
        this.validationMethods = validationMethods;
    }

    /**
     * Validates a user based on the provided user ID and required order or vendor id.
     *
     * @param userId     The ID of the user to be validated.
     * @param methodName Name of the method that was called.
     * @param other      The ID of the order or vendor to be validated.
     * @return An optional containing a ResponseEntity with an error message if validation fails, or empty if validated.
     */
    @Override
    public Optional<ResponseEntity> check(Long userId, String methodName, Long other) {
        try {
            if (validationMethods.get(methodName).apply(userId, other)) {
                return checkNext(userId, methodName, other);
            }
            return Optional.of(ResponseEntity.status(403).body("User with id " + userId + " does not have access rights"));
        } catch (Exception e) {
            return Optional.of(ResponseEntity.status(500).body("Error while validating"));
        }
    }
}
