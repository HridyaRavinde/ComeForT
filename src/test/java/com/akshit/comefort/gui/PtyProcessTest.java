package com.akshit.comefort.gui;

import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import com.pty4j.WinSize;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pty4j Pseudo-Terminal Integration Tests")
class PtyProcessTest {

    @Test
    @DisplayName("Successfully starts native PTY process and communicates via streams")
    void testPtyProcessLaunch() throws Exception {
        boolean isWin = System.getProperty("os.name", "").toLowerCase().contains("win");
        String[] cmd = isWin ? new String[]{"cmd.exe"} : new String[]{"/bin/sh"};

        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("TERM", "xterm-256color");

        PtyProcess process = new PtyProcessBuilder()
                .setCommand(cmd)
                .setEnvironment(env)
                .setInitialColumns(80)
                .setInitialRows(24)
                .start();

        assertNotNull(process, "PtyProcess must start successfully");
        assertTrue(process.isAlive(), "PtyProcess must be alive");

        OutputStream os = process.getOutputStream();
        InputStream is = process.getInputStream();

        // Write a simple echo command
        os.write("echo PTY_TEST_OK\r\n".getBytes(StandardCharsets.UTF_8));
        os.flush();

        // Read output
        byte[] buffer = new byte[1024];
        StringBuilder sb = new StringBuilder();
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 3000) {
            if (is.available() > 0) {
                int read = is.read(buffer);
                if (read > 0) {
                    sb.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
                    if (sb.toString().contains("PTY_TEST_OK")) {
                        break;
                    }
                }
            } else {
                Thread.sleep(50);
            }
        }

        System.out.println("PTY Output received: " + sb);
        assertTrue(sb.toString().contains("PTY_TEST_OK"), "PTY must execute command and return output");

        // Resize test
        process.setWinSize(new WinSize(120, 40));

        process.destroyForcibly();
    }
}
