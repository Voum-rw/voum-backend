-- V13: Chat messages between passenger and motari
CREATE TABLE IF NOT EXISTS chat_messages (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    receiver_id   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    -- context_id is either a ride_request_id or a trip_id
    context_id    UUID NOT NULL,
    context_type  VARCHAR(20) NOT NULL DEFAULT 'REQUEST',  -- REQUEST | TRIP
    content       TEXT NOT NULL,
    sent_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_read       BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_chat_messages_context  ON chat_messages(context_id, sent_at DESC);
CREATE INDEX IF NOT EXISTS idx_chat_messages_sender   ON chat_messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_chat_messages_receiver ON chat_messages(receiver_id);
