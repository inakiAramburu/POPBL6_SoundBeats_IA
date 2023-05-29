package edu.mondragon.soundbeats.IA;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringWriter;

import org.python.util.PythonInterpreter;
import org.python.util.jython;
import org.python.core.*;

public class main {

    String path = "../main.py";

    public void givenPythonScript_whenPythonProcessInvoked_thenSuccess() throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder("python", path);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        process.waitFor();

        BufferedReader bfr = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = "";
        while ((line = bfr.readLine()) != null) {
            System.out.println(line);
        }
    }

    // @Test
    public void givenPythonInterpreter_whenPrintExecuted_thenOutputDisplayed() {
        try (PythonInterpreter pyInterp = new PythonInterpreter()) {
            StringWriter output = new StringWriter();
            pyInterp.setOut(output);

            pyInterp.exec("print('Hello Baeldung Readers!!')");
            // assertEquals("Should contain script output: ", "Hello Baeldung Readers!!",
            // output.toString().trim());
        }
    }
}
