-- Week 1 proposed additive schema, validated by integration tests.
-- Not applied by application startup until Week 2. Existing users/agents first.
CREATE TABLE IF NOT EXISTS agent_messages (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sender_username TEXT NOT NULL REFERENCES users(username),
    recipient_agent_id INTEGER NOT NULL REFERENCES agents(id),
    recipient_name TEXT NOT NULL COLLATE NOCASE CHECK(length(trim(recipient_name)) > 0),
    body TEXT NOT NULL CHECK(length(trim(body, char(9) || char(10) || char(13) || ' ')) > 0),
    sent_at TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_agent_messages_recipient
    ON agent_messages(recipient_name COLLATE NOCASE, id);
