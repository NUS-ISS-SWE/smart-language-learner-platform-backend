package com.nus.iss.agent;

import com.nus.iss.exception.EvidenceGenerationException;
import com.nus.iss.exception.UpstreamServiceException;

/**
 * Central place for "what happens when something fails". Kept as policy, not scattered
 * try/catch behaviour, so the failure story is one thing to point to (and change) rather than
 * re-derived per call site.
 *
 * <ul>
 *   <li>composition/admin/user-service unreachable -&gt; {@link UpstreamServiceException}, 502.
 *       There's nothing to fall back to — evidence collection cannot proceed without them.</li>
 *   <li>the model call itself fails, or returns no usable result -&gt; {@link EvidenceGenerationException}, 500.</li>
 *   <li>reflection's retry is exhausted -&gt; NOT a failure. The result is still returned, with the
 *       affected items flagged {@code verifiedOnFirstPass = false} rather than dropped.</li>
 * </ul>
 */
public final class FallbackPolicy {

    /** First attempt + at most one corrective retry. Kept small on purpose — see the dossier. */
    public static final int MAX_REFLECTION_ATTEMPTS = 2;

    private FallbackPolicy() {
    }

    public static UpstreamServiceException upstreamUnavailable(String serviceName, Throwable cause) {
        return new UpstreamServiceException(
                "Could not reach " + serviceName + ". Evidence collection cannot proceed without it.", cause);
    }

    public static EvidenceGenerationException agentFailed(String reason, Throwable cause) {
        return new EvidenceGenerationException("Evidence agent failed: " + reason, cause);
    }
}
