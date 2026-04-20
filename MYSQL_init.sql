-- Usuário dedicado ao Debezium, precisa de permissões de replicação
-- não é um usuário de aplicação comum
CREATE USER IF NOT EXISTS 'debezium'@'%' IDENTIFIED BY 'dbz_secret';
GRANT SELECT, RELOAD, SHOW DATABASES, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'debezium'@'%';
GRANT SELECT ON music_db.* TO 'debezium'@'%';
FLUSH PRIVILEGES;

USE music_db;

CREATE TABLE IF NOT EXISTS musics (
    id           VARCHAR(36)    NOT NULL PRIMARY KEY,
    music_name   VARCHAR(100)   NOT NULL,
    artist_name  VARCHAR(100)   NOT NULL,
    genre        VARCHAR(100)   NOT NULL,
    created_at   DATETIME(6)    NOT NULL,
    INDEX idx_musica_name (music_name),
    INDEX idx_artist_name (artist_name),
    INDEX idx_genre (genre),
    UNIQUE KEY uq_music_artist (music_name, artist_name)
    );

CREATE TABLE IF NOT EXISTS outbox_events (
    id             VARCHAR(36)   NOT NULL PRIMARY KEY,
    aggregate_type VARCHAR(100)  NOT NULL,   -- ex: "Music"
    aggregate_id   VARCHAR(36)   NOT NULL,   -- ex: "music.id" — vira a key do Kafka
    event_type     VARCHAR(100)  NOT NULL,   -- ex: "MusicRegistered"
    payload        JSON          NOT NULL,   -- evento completo serializado
    created_at     DATETIME(6)   NOT NULL,

    INDEX idx_aggregate (aggregate_type, aggregate_id),
    INDEX idx_created   (created_at)
    );

-- Limpeza automática após 7 dias
-- O Debezium já leu via binlog antes disso
CREATE EVENT IF NOT EXISTS cleanup_outbox
    ON SCHEDULE EVERY 1 DAY
    DO DELETE FROM outbox_events
       WHERE created_at < DATE_SUB(NOW(), INTERVAL 7 DAY);