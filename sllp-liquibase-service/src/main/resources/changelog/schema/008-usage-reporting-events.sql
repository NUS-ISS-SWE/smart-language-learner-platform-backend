--liquibase formatted sql

--changeset yasin:008-review-and-document-metric-timestamps
ALTER TABLE evaluation ADD COLUMN IF NOT EXISTS request_id UUID;
ALTER TABLE evaluation ADD COLUMN IF NOT EXISTS requested_at TIMESTAMP;
-- Historical evaluations predate request timestamps. Their completion time is the best available fallback.
UPDATE evaluation SET requested_at = evaluated_at WHERE requested_at IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_evaluation_request_id ON evaluation (request_id) WHERE request_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_evaluation_evaluated_at ON evaluation (evaluated_at);
CREATE INDEX IF NOT EXISTS idx_evaluation_success_evaluated_at ON evaluation (evaluated_at) WHERE success = TRUE;

ALTER TABLE documents ADD COLUMN IF NOT EXISTS processed_at TIMESTAMP;
-- Existing terminal documents were processed before this event timestamp existed.
UPDATE documents SET processed_at = updated_at
WHERE processed_at IS NULL AND status IN ('READY', 'FAILED');
CREATE INDEX IF NOT EXISTS idx_documents_processed_at ON documents (processed_at);

--changeset yasin:008-ai-usage-event-ledger
CREATE TABLE IF NOT EXISTS ai_usage_event (
    id                                      BIGSERIAL PRIMARY KEY,
    request_id                              UUID NOT NULL UNIQUE,
    user_id                                 BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    composition_id                          BIGINT REFERENCES composition(id) ON DELETE SET NULL,
    operation                               VARCHAR(50) NOT NULL,
    provider                                VARCHAR(50) NOT NULL,
    model                                   VARCHAR(100) NOT NULL,
    prompt_tokens                           BIGINT NOT NULL,
    completion_tokens                       BIGINT NOT NULL,
    cached_input_tokens                     BIGINT NOT NULL DEFAULT 0,
    total_tokens                            BIGINT NOT NULL,
    estimated_cost_micros                   BIGINT NOT NULL,
    currency                                VARCHAR(3) NOT NULL DEFAULT 'USD',
    pricing_version                         VARCHAR(100) NOT NULL,
    input_price_micros_per_million          BIGINT NOT NULL,
    cached_input_price_micros_per_million   BIGINT NOT NULL,
    output_price_micros_per_million         BIGINT NOT NULL,
    created_at                              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ai_usage_event_created_at ON ai_usage_event (created_at);
CREATE INDEX IF NOT EXISTS idx_ai_usage_event_user_created_at ON ai_usage_event (user_id, created_at);
