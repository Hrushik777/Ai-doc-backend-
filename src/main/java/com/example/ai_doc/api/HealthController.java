package com.example.ai_doc.api;

import com.example.ai_doc.api.dto.ServiceStatus;
import com.example.ai_doc.auth.FreeAttemptAllowance;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Says the service is up, and how many runs the caller has left before signing in.
 *
 * <p>It exists mostly to be called early. A free instance sleeps after a quarter of an hour and
 * takes the better part of a minute to come back, and until now the first request to arrive was
 * whatever the user had just asked for - a sign-in, or a document they had waited to upload.
 * Either way the wait landed on them at the worst moment. The page now calls this on load, so the
 * instance is waking while they are still reading and choosing files.
 *
 * <p>Deliberately cheap: it touches no model, no disk and no network, so waking the instance costs
 * nothing beyond the start-up it was going to pay anyway.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    private final FreeAttemptAllowance freeAttemptAllowance;

    public HealthController(FreeAttemptAllowance freeAttemptAllowance) {
        this.freeAttemptAllowance = freeAttemptAllowance;
    }

    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceStatus> health(HttpServletRequest request) {
        return ResponseEntity.ok(new ServiceStatus(
                "ok",
                freeAttemptAllowance.freeAttempts(),
                freeAttemptAllowance.remainingFor(request)));
    }
}
