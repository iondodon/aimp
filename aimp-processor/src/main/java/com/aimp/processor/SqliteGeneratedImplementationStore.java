package com.aimp.processor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

final class SqliteGeneratedImplementationStore {
    private static final String CREATE_TABLE_SQL = """
        CREATE TABLE IF NOT EXISTS generated_implementations (
            contract_fqn TEXT NOT NULL,
            contract_version TEXT NOT NULL,
            generated_qualified_name TEXT NOT NULL,
            generated_source TEXT NOT NULL,
            PRIMARY KEY (contract_fqn, contract_version)
        )
        """;

    private static final String SELECT_SQL = """
        SELECT contract_fqn, contract_version, generated_qualified_name, generated_source
        FROM generated_implementations
        WHERE contract_fqn = ? AND contract_version = ?
        """;

    private static final String UPSERT_SQL = """
        INSERT INTO generated_implementations (
            contract_fqn,
            contract_version,
            generated_qualified_name,
            generated_source
        ) VALUES (?, ?, ?, ?)
        ON CONFLICT(contract_fqn, contract_version) DO UPDATE SET
            generated_qualified_name = excluded.generated_qualified_name,
            generated_source = excluded.generated_source
        """;

    private final Path databasePath;

    SqliteGeneratedImplementationStore(Path databasePath) {
        this.databasePath = databasePath.toAbsolutePath().normalize();
        try {
            Class.forName("org.sqlite.JDBC");
            Path parent = this.databasePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            ensureSchema();
        } catch (ClassNotFoundException | IOException | SQLException exception) {
            throw new GeneratedImplementationStoreException(
                "Failed to initialize SQLite store at " + this.databasePath + ": " + exception.getMessage(),
                exception
            );
        }
    }

    Optional<StoredGeneratedImplementation> find(String contractQualifiedName, String contractVersion) {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_SQL)) {
            statement.setString(1, contractQualifiedName);
            statement.setString(2, contractVersion);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new StoredGeneratedImplementation(
                    resultSet.getString("contract_fqn"),
                    resultSet.getString("contract_version"),
                    resultSet.getString("generated_qualified_name"),
                    resultSet.getString("generated_source")
                ));
            }
        } catch (SQLException exception) {
            throw new GeneratedImplementationStoreException(
                "Failed to read SQLite store at " + databasePath + ": " + exception.getMessage(),
                exception
            );
        }
    }

    void save(StoredGeneratedImplementation implementation) {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(UPSERT_SQL)) {
            statement.setString(1, implementation.contractQualifiedName());
            statement.setString(2, implementation.contractVersion());
            statement.setString(3, implementation.generatedQualifiedName());
            statement.setString(4, implementation.generatedSource());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new GeneratedImplementationStoreException(
                "Failed to write SQLite store at " + databasePath + ": " + exception.getMessage(),
                exception
            );
        }
    }

    private void ensureSchema() throws SQLException {
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(CREATE_TABLE_SQL);
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + databasePath);
    }
}
