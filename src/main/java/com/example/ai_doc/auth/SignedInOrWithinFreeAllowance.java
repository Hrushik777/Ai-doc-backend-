package com.example.ai_doc.auth;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Admits a signed-in caller, or an anonymous one who still has free attempts left.
 *
 * <p>Expressed as an authorization rule rather than a check inside the controller so that the
 * whole policy for these endpoints stays in the filter chain, where it can be read in one place -
 * and so a caller with nothing left is turned away before their upload is transferred rather than
 * after.
 */
@Component
public class SignedInOrWithinFreeAllowance
        implements AuthorizationManager<RequestAuthorizationContext> {

    private final FreeAttemptAllowance freeAttemptAllowance;

    public SignedInOrWithinFreeAllowance(FreeAttemptAllowance freeAttemptAllowance) {
        this.freeAttemptAllowance = freeAttemptAllowance;
    }

    @Override
    public AuthorizationResult authorize(Supplier<? extends Authentication> authentication,
                                         RequestAuthorizationContext context) {
        Authentication current = authentication.get();
        if (isSignedIn(current)) {
            // Costs no free attempt: the point of signing in is to stop counting.
            return new AuthorizationDecision(true);
        }
        return new AuthorizationDecision(freeAttemptAllowance.tryConsume(context.getRequest()));
    }

    /**
     * {@code isAuthenticated()} alone is not enough - Spring represents an anonymous caller with
     * an authentication object that answers true to it, so relying on that would make every
     * request free.
     */
    private boolean isSignedIn(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof org.springframework.security.authentication
                        .AnonymousAuthenticationToken);
    }
}
