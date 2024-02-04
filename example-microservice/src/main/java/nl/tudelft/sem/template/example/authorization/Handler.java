package nl.tudelft.sem.template.example.authorization;

import java.util.Optional;
import org.springframework.http.ResponseEntity;

public abstract class Handler {

    private Handler next;

    /**
     * Links the handlers in the chain together.
     *
     * @param first The first handler in the chain.
     * @param chain The rest of the handlers in the chain.
     * @return The first handler in the chain.
     */
    public static Handler link(Handler first, Handler... chain) {
        Handler head = first;
        for (Handler nextInChain : chain) {
            head.next = nextInChain;
            head = nextInChain;
        }
        return first;
    }

    /**
     * Check method that every handler needs to implement.
     *
     * @param authorization The ID of the user.
     * @param methodName    The name of the method that was called.
     * @return An optional containing a ResponseEntity with an error message if a link fails or empty otherwise.
     */
    public abstract Optional<ResponseEntity> check(Long authorization, String methodName, Long other);

    protected Optional<ResponseEntity> checkNext(Long authorization, String methodName, Long other) {
        if (next == null) {
            return Optional.empty();
        }
        return next.check(authorization, methodName, other);
    }
}
