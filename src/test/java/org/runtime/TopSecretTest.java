package org.runtime;

import org.controller.ProgramControl;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TopSecretTest {

    @Test
    void entryPointPassesArgumentsToProgramControl() {
        try (MockedConstruction<ProgramControl> controls = mockConstruction(ProgramControl.class)) {
            assertNotNull(new TopSecret());
            String[] args = {"-web"};
            TopSecret.main(args);
            assertEquals(1, controls.constructed().size());
            verify(controls.constructed().getFirst()).run(args);
        }
    }

    @Test
    void entryPointReportsRuntimeFailure() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream previous = System.err;
        try (MockedConstruction<ProgramControl> controls = mockConstruction(ProgramControl.class,
                (control, context) -> doThrow(new IllegalStateException("startup failure"))
                        .when(control).run(any(String[].class)))) {
            System.setErr(new PrintStream(output));
            TopSecret.main(new String[0]);
            assertEquals(1, controls.constructed().size());
            assertTrue(output.toString().contains("startup failure"));
        } finally {
            System.setErr(previous);
        }
    }
}
