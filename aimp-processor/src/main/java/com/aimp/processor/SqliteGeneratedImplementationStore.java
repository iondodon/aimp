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
    private static final String IMPLEMENTATIONS_TABLE = "generated_implementations_v2";
    private static final String DEPENDENCIES_TABLE = "generated_implementation_dependencies_v2";

    private static final String CREATE_TABLE_SQL = """
        CREATE TABLE IF NOT EXISTS %s (
            contract_fqn TEXT NOT NULL,
            contract_fingerprint_hash TEXT NOT NULL,
            generated_qualified_name TEXT NOT NULL,
            generated_source TEXT NOT NULL,
            created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (contract_fqn, contract_fingerprint_hash)
        )
        """.formatted(IMPLEMENTATIONS_TABLE);

    private static final String CREATE_DEPENDENCIES_TABLE_SQL = """
        CREATE TABLE IF NOT EXISTS %s (
            contract_fqn TEXT NOT NULL,
            contract_fingerprint_hash TEXT NOT NULL,
            dependency_fqn TEXT NOT NULL,
            dependency_fingerprint_hash TEXT NOT NULL,
            created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (contract_fqn, contract_fingerprint_hash, dependency_fqn)
        )
        """.formatted(DEPENDENCIES_TABLE);

    private static final String SELECT_SQL = """
        SELECT contract_fqn, contract_fingerprint_hash, generated_qualified_name, generated_source
        FROM %s
        WHERE contract_fqn = ? AND contract_fingerprint_hash = ?
        """.formatted(IMPLEMENTATIONS_TABLE);

    private static final String SELECT_DEPENDENCIES_SQL = """
        SELECT dependency_fqn, dependency_fingerprint_hash
        FROM %s
        WHERE contract_fqn = ? AND contract_fingerprint_hash = ?
        ORDER BY dependency_fqn
        """.formatted(DEPENDENCIES_TABLE);

    private static final String UPSERT_SQL = """
        INSERT INTO %s (
            contract_fqn,
            contract_fingerprint_hash,
            generated_qualified_name,
            generated_source,
            created_at
        ) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
        ON CONFLICT(contract_fqn, contract_fingerprint_hash) DO UPDATE SET
            generated_qualified_name = excluded.generated_qualified_name,
            generated_source = excluded.generated_source
        """.formatted(IMPLEMENTATIONS_TABLE);

    private static final String DELETE_DEPENDENCIES_SQL = """
        DELETE FROM %s
        WHERE contract_fqn = ? AND contract_fingerprint_hash = ?
        """.formatted(DEPENDENCIES_TABLE);

    private static final String INSERT_DEPENDENCY_SQL = """
        INSERT INTO %s (
            contract_fqn,
            contract_fingerprint_hash,
            dependency_fqn,
            dependency_fingerprint_hash,
            created_at
        ) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
        """.formatted(DEPENDENCIES_TABLE);

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

    Optional<StoredGeneratedImplementation> find(String contractQualifiedName, String contractFingerprintHash) {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_SQL)) {
            statement.setString(1, contractQualifiedName);
            statement.setString(2, contractFingerprintHash);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new StoredGeneratedImplementation(
                    resultSet.getString("contract_fqn"),
                    resultSet.getString("contract_fingerprint_hash"),
                    resultSet.getString("generated_qualified_name"),
                    resultSet.getString("generated_source"),
                    loadDependencies(connection, contractQualifiedName, contractFingerprintHash)
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
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(UPSERT_SQL);
                 PreparedStatement deleteDependencies = connection.prepareStatement(DELETE_DEPENDENCIES_SQL);
                 PreparedStatement insertDependency = connection.prepareStatement(INSERT_DEPENDENCY_SQL)) {
                statement.setString(1, implementation.contractQualifiedName());
                statement.setString(2, implementation.contractFingerprintHash());
                statement.setString(3, implementation.generatedQualifiedName());
                statement.setString(4, implementation.generatedSource());
                statement.executeUpdate();

                deleteDependencies.setString(1, implementation.contractQualifiedName());
                deleteDependencies.setString(2, implementation.contractFingerprintHash());
                deleteDependencies.executeUpdate();

                for (StoredContextDependency dependency : implementation.contextDependencies()) {
                    insertDependency.setString(1, implementation.contractQualifiedName());
                    insertDependency.setString(2, implementation.contractFingerprintHash());
                    insertDependency.setString(3, dependency.qualifiedName());
                    insertDependency.setString(4, dependency.fingerprintHash());
                    insertDependency.executeUpdate();
                }
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
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
            statement.execute(CREATE_DEPENDENCIES_TABLE_SQL);
        }
    }

    private java.util.List<StoredContextDependency> loadDependencies(
        Connection connection,
        String contractQualifiedName,
        String contractFingerprintHash
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_DEPENDENCIES_SQL)) {
            statement.setString(1, contractQualifiedName);
            statement.setString(2, contractFingerprintHash);
            try (ResultSet resultSet = statement.executeQuery()) {
                java.util.ArrayList<StoredContextDependency> dependencies = new java.util.ArrayList<>();
                while (resultSet.next()) {
                    dependencies.add(new StoredContextDependency(
                        resultSet.getString("dependency_fqn"),
                        resultSet.getString("dependency_fingerprint_hash")
                    ));
                }
                return java.util.List.copyOf(dependencies);
            }
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + databasePath);
    }
}
