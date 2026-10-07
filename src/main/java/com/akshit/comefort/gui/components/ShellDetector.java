package com.akshit.comefort.gui.components;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Dynamically discovers real terminal shells actually installed on the user's host machine.
 * Zero hardcoded lists — queries the real operating system, PATH, and filesystem locations.
 */
public class ShellDetector {

    public record ShellProfile(
            String id,
            String displayName,
            String executablePath,
            List<String> launchArgs,
            String icon,
            String subtitle
    ) {
        @Override
        public String toString() {
            return icon + " " + displayName;
        }
    }

    private static final boolean IS_WINDOWS = System.getProperty("os.name", "").toLowerCase().contains("win");
    private static final boolean IS_MAC = System.getProperty("os.name", "").toLowerCase().contains("mac");

    /**
     * Probes the operating system and returns all verified, runnable shells.
     */
    public static List<ShellProfile> detectInstalledShells() {
        List<ShellProfile> shells = new ArrayList<>();

        if (IS_WINDOWS) {
            detectWindowsShells(shells);
        } else {
            detectUnixShells(shells);
        }

        // Safety fallback if no standard shell was found
        if (shells.isEmpty()) {
            if (IS_WINDOWS) {
                shells.add(new ShellProfile("cmd", "Command Prompt", "cmd.exe",
                        List.of("cmd.exe", "/Q", "/K"), "⌨", "Windows Command Processor"));
            } else {
                shells.add(new ShellProfile("sh", "Sh", "/bin/sh",
                        List.of("/bin/sh", "-i"), "🐚", "Standard Shell"));
            }
        }

        return Collections.unmodifiableList(shells);
    }

    /**
     * Determines the most capable default shell for this system.
     */
    public static ShellProfile getDefaultShell(List<ShellProfile> availableShells) {
        if (availableShells == null || availableShells.isEmpty()) {
            return null;
        }

        // Priority 1: PowerShell 7 (modern cross-platform)
        for (ShellProfile s : availableShells) {
            if ("pwsh".equalsIgnoreCase(s.id())) return s;
        }

        // Priority 2: Windows PowerShell on Windows, Zsh on macOS
        for (ShellProfile s : availableShells) {
            if (IS_WINDOWS && "powershell".equalsIgnoreCase(s.id())) return s;
            if (IS_MAC && "zsh".equalsIgnoreCase(s.id())) return s;
        }

        // Priority 3: Bash
        for (ShellProfile s : availableShells) {
            if ("bash".equalsIgnoreCase(s.id()) || "git-bash".equalsIgnoreCase(s.id())) return s;
        }

        // Default: First discovered shell
        return availableShells.get(0);
    }

    // =========================================================================
    // WINDOWS DISCOVERY
    // =========================================================================

    private static void detectWindowsShells(List<ShellProfile> list) {
        Set<String> addedPaths = new HashSet<>();

        // 1. PowerShell 7 (pwsh.exe) — Modern PowerShell
        String pwshPath = findWindowsExecutable("pwsh.exe");
        if (pwshPath == null) {
            pwshPath = checkExistingFile(
                    "C:\\Program Files\\PowerShell\\7\\pwsh.exe",
                    "C:\\Program Files\\PowerShell\\7-preview\\pwsh.exe",
                    System.getenv("LOCALAPPDATA") + "\\Microsoft\\PowerShell\\pwsh.exe"
            );
        }
        if (pwshPath == null) {
            pwshPath = findInWindowsApps("pwsh.exe");
        }
        if (pwshPath != null && addedPaths.add(pwshPath.toLowerCase())) {
            list.add(new ShellProfile(
                    "pwsh",
                    "PowerShell 7",
                    pwshPath,
                    List.of(pwshPath, "-NoLogo", "-NoExit", "-NoProfile"),
                    "⚡",
                    "PowerShell 7 (Core)"
            ));
        }

        // 2. Windows PowerShell (powershell.exe) — Built-in 5.1
        String winPsPath = checkExistingFile(
                System.getenv("SystemRoot") + "\\System32\\WindowsPowerShell\\v1.0\\powershell.exe",
                "C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe"
        );
        if (winPsPath == null) {
            winPsPath = findWindowsExecutable("powershell.exe");
        }
        if (winPsPath != null && addedPaths.add(winPsPath.toLowerCase())) {
            list.add(new ShellProfile(
                    "powershell",
                    "Windows PowerShell",
                    winPsPath,
                    List.of(winPsPath, "-NoLogo", "-NoExit", "-NoProfile"),
                    "",
                    "Windows PowerShell 5.1"
            ));
        }

        // 3. Command Prompt (cmd.exe)
        String cmdPath = checkExistingFile(
                System.getenv("SystemRoot") + "\\System32\\cmd.exe",
                "C:\\Windows\\System32\\cmd.exe"
        );
        if (cmdPath == null) {
            cmdPath = findWindowsExecutable("cmd.exe");
        }
        if (cmdPath != null && addedPaths.add(cmdPath.toLowerCase())) {
            list.add(new ShellProfile(
                    "cmd",
                    "Command Prompt",
                    cmdPath,
                    List.of(cmdPath, "/Q", "/K"),
                    "⌨",
                    "Windows Command Processor"
            ));
        }

        // 4. Git Bash (bash.exe)
        String gitBashPath = checkExistingFile(
                "C:\\Program Files\\Git\\bin\\bash.exe",
                "C:\\Program Files (x86)\\Git\\bin\\bash.exe",
                System.getenv("LOCALAPPDATA") + "\\Programs\\Git\\bin\\bash.exe",
                System.getenv("ProgramW6432") + "\\Git\\bin\\bash.exe"
        );
        if (gitBashPath != null && addedPaths.add(gitBashPath.toLowerCase())) {
            list.add(new ShellProfile(
                    "git-bash",
                    "Git Bash",
                    gitBashPath,
                    List.of(gitBashPath, "--login", "-i"),
                    "",
                    "Git Bash (MINGW64)"
            ));
        }

        // 5. WSL (Windows Subsystem for Linux)
        detectWslDistributions(list, addedPaths);

        // 6. Nushell (if installed on device)
        String nuPath = findWindowsExecutable("nu.exe");
        if (nuPath != null && addedPaths.add(nuPath.toLowerCase())) {
            list.add(new ShellProfile(
                    "nu",
                    "Nushell",
                    nuPath,
                    List.of(nuPath),
                    "🐚",
                    "Nu Shell"
            ));
        }
    }

    private static void detectWslDistributions(List<ShellProfile> list, Set<String> addedPaths) {
        String wslPath = checkExistingFile(
                System.getenv("SystemRoot") + "\\System32\\wsl.exe",
                "C:\\Windows\\System32\\wsl.exe"
        );
        if (wslPath == null) return;

        try {
            Process process = new ProcessBuilder(wslPath, "-l", "-q")
                    .redirectErrorStream(true)
                    .start();

            boolean finished = process.waitFor(1200, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                return;
            }

            if (process.exitValue() == 0) {
                // WSL outputs UTF-16LE on Windows
                byte[] bytes = process.getInputStream().readAllBytes();
                String output = new String(bytes, StandardCharsets.UTF_16LE).trim();
                if (output.isEmpty() || output.toLowerCase().contains("not installed")) {
                    return;
                }

                String[] distros = output.split("[\\r\\n]+");
                for (String distro : distros) {
                    String clean = distro.trim().replace("\u0000", "");
                    if (!clean.isEmpty() && !clean.contains(" ") && addedPaths.add("wsl:" + clean)) {
                        list.add(new ShellProfile(
                                "wsl-" + clean.toLowerCase(),
                                "WSL: " + clean,
                                wslPath,
                                List.of(wslPath, "-d", clean),
                                "🐧",
                                "WSL Linux Distribution"
                        ));
                    }
                }
            }
        } catch (Exception ignored) {
            // WSL probe failed or not enabled
        }
    }

    private static String findWindowsExecutable(String name) {
        try {
            Process p = new ProcessBuilder("where.exe", name)
                    .redirectErrorStream(true)
                    .start();
            boolean done = p.waitFor(800, TimeUnit.MILLISECONDS);
            if (done && p.exitValue() == 0) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
                String line = reader.readLine();
                if (line != null && !line.trim().isEmpty() && new File(line.trim()).exists()) {
                    return line.trim();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String findInWindowsApps(String exeName) {
        String programFiles = System.getenv("ProgramFiles");
        if (programFiles == null) return null;

        File windowsApps = new File(programFiles, "WindowsApps");
        if (!windowsApps.exists() || !windowsApps.isDirectory()) return null;

        try {
            File[] matching = windowsApps.listFiles((dir, name) ->
                    name.toLowerCase().contains("powershell") && !name.contains(".old")
            );
            if (matching != null) {
                for (File f : matching) {
                    File candidate = new File(f, exeName);
                    if (candidate.exists() && candidate.canExecute()) {
                        return candidate.getAbsolutePath();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String checkExistingFile(String... paths) {
        for (String p : paths) {
            if (p != null) {
                File f = new File(p);
                if (f.exists() && f.canExecute()) {
                    return f.getAbsolutePath();
                }
            }
        }
        return null;
    }

    // =========================================================================
    // UNIX (macOS / Linux) DISCOVERY
    // =========================================================================

    private static void detectUnixShells(List<ShellProfile> list) {
        Set<String> addedPaths = new HashSet<>();

        // 1. Read /etc/shells (system registry of installed login shells)
        File etcShells = new File("/etc/shells");
        if (etcShells.exists()) {
            try (BufferedReader reader = Files.newBufferedReader(etcShells.toPath())) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("/") && !line.contains("#")) {
                        File f = new File(line);
                        if (f.exists() && f.canExecute() && addedPaths.add(f.getAbsolutePath())) {
                            String name = f.getName().toLowerCase();
                            String displayName = capitalize(name);
                            String icon = switch (name) {
                                case "zsh" -> "⚡";
                                case "bash" -> "";
                                case "fish" -> "🐟";
                                default -> "🐚";
                            };
                            list.add(new ShellProfile(name, displayName, f.getAbsolutePath(),
                                    List.of(f.getAbsolutePath(), "-i"), icon, "Login Shell (" + line + ")"));
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // 2. Check $SHELL if not in /etc/shells
        String currentShell = System.getenv("SHELL");
        if (currentShell != null && addedPaths.add(currentShell)) {
            File f = new File(currentShell);
            if (f.exists() && f.canExecute()) {
                list.add(0, new ShellProfile(f.getName().toLowerCase(), capitalize(f.getName()) + " (Default)",
                        f.getAbsolutePath(), List.of(f.getAbsolutePath(), "-i"), "⚡", "Current User Shell"));
            }
        }
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
