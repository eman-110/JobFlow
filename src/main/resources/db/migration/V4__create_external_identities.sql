CREATE TABLE external_identities (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    issuer VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_external_identities PRIMARY KEY (id),
    CONSTRAINT uq_external_identity_issuer_subject UNIQUE (issuer, subject),
    CONSTRAINT fk_external_identities_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE oauth_login_exchanges (
    id BIGINT NOT NULL AUTO_INCREMENT,
    credential_hash CHAR(64) NOT NULL,
    user_id INT NOT NULL,
    client_id VARCHAR(255) NOT NULL,
    verifier_challenge CHAR(43) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    consumed_at TIMESTAMP NULL,
    CONSTRAINT pk_oauth_login_exchanges PRIMARY KEY (id),
    CONSTRAINT uq_oauth_login_exchange_hash UNIQUE (credential_hash),
    CONSTRAINT fk_oauth_login_exchanges_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    INDEX ix_oauth_login_exchanges_expiry (expires_at)
) ENGINE = InnoDB;

CREATE TABLE oauth_link_intents (
    id BIGINT NOT NULL AUTO_INCREMENT,
    intent_hash CHAR(64) NOT NULL,
    user_id INT NOT NULL,
    client_id VARCHAR(255) NOT NULL,
    verifier_challenge CHAR(43) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    consumed_at TIMESTAMP NULL,
    CONSTRAINT pk_oauth_link_intents PRIMARY KEY (id),
    CONSTRAINT uq_oauth_link_intent_hash UNIQUE (intent_hash),
    CONSTRAINT fk_oauth_link_intents_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB;
