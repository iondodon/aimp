package com.aimp.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AimpConfigLoaderTest {
    private final AimpConfigLoader loader = new AimpConfigLoader();

    @Test
    void parsesAllowlistedAnnotations() throws Exception {
        AimpConfig config = loader.parse("""
            aimp:
              propagation:
                annotations:
                  - org.springframework.stereotype.Service
                  - jakarta.validation.Valid
            """);

        assertEquals(
            List.of("org.springframework.stereotype.Service", "jakarta.validation.Valid"),
            config.propagation().annotations()
        );
    }

    @Test
    void missingConfigReturnsEmpty(@TempDir Path tempDir) throws Exception {
        AimpConfig config = loader.load(tempDir, getClass().getClassLoader());

        assertEquals(List.of(), config.propagation().annotations());
    }

    @Test
    void prefersProjectRootConfig(@TempDir Path tempDir) throws Exception {
        Files.writeString(
            tempDir.resolve("aimp.yml"),
            """
            aimp:
              propagation:
                annotations:
                  - com.example.Foo
            """
        );

        AimpConfig config = loader.load(tempDir, getClass().getClassLoader());

        assertEquals(List.of("com.example.Foo"), config.propagation().annotations());
    }

    @Test
    void invalidClassNameFails() {
        AimpConfigException exception = assertThrows(
            AimpConfigException.class,
            () -> loader.parse("""
                aimp:
                  propagation:
                    annotations:
                      - invalid class name
                """)
        );

        assertEquals(true, exception.getMessage().contains("Invalid annotation class name"));
    }

    @Test
    void misplacedListFails() {
        assertThrows(
            AimpConfigException.class,
            () -> loader.parse("""
                aimp:
                  propagation:
                  - com.example.Foo
                """)
        );
    }
}
