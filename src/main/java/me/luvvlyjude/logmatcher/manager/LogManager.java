package me.luvvlyjude.logmatcher.manager;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.event.RunnableEventType;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.*;
import java.util.stream.Stream;

import static me.luvvlyjude.logmatcher.LogMatcherPlugin.log;

public class LogManager {
    private static final Map<String, Map<String, RunnableEventType>> MATCH_EVENTS = new HashMap<>(); // <event_name, <runnableevent_owner, runnable_event>>
    private static final Map<String, Pattern> MATCH_PATTERNS = new HashMap<>(); // <event_name, matching_pattern>
    private static final Map<String, String> LAST_MATCHES = new HashMap<>(); // <event_name, last_match_contents>

    private static long logProgress = -1;
    private static FileTime lastLogModify = null;

    private LogManager() {
    }

    public static void checkLog() {
        if (!Jingle.getMainInstance().isPresent() || MATCH_PATTERNS.isEmpty() || MATCH_EVENTS.isEmpty()) {
            return;
        }

        Path logPath = Jingle.getMainInstance().get().instancePath.resolve("logs").resolve("latest.log");

        // Check if the log file hasn't been read yet, hasn't been modified, or has been reset by midnight or overflow
        if (logProgress == -1 || checkForLogReset(logPath) || !checkForLogChanges(logPath)) {
            tryJumpLogProgress(logPath);
            return;
        }

        String newLogContents = getNewLogContents(logPath);
        checkLogContents(newLogContents);
    }

    // COPIED FROM JULTI v0.18.0
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static String getNewLogContents(Path logPath) {
        // Read new bytes then format and return as a string
        try (InputStream stream = Files.newInputStream(logPath)) {
            stream.skip(logProgress);

            ArrayList<Byte> byteList = new ArrayList<>();

            int next = stream.read();
            while (next != -1) {
                byteList.add((byte) next);
                logProgress++;
                next = stream.read();
            }

            byte[] bytes = new byte[byteList.size()];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = byteList.get(i);
            }

            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            return "";
        }
    }

    private static boolean checkForLogChanges(Path logPath) {
        try {
            FileTime newModifyTime = Files.getLastModifiedTime(logPath);
            if (newModifyTime.equals(lastLogModify)) {
                return false;
            }
            lastLogModify = newModifyTime;
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private static boolean checkForLogReset(Path logPath) {
        try {
            long size = Files.size(logPath);
            return size < (logProgress / 2);
        } catch (IOException ignored) {
            return false;
        }
    }

    /**
     * COPIED FROM JULTI v0.18.0
     * Sets logProgress to the amount of bytes in the latest log of the instance.
     * Failure is ignored, as logProgress will still be -1 afterward, indicating the task would still need to be done.
     */
    private static void tryJumpLogProgress(Path logPath) {
        try {
            if (Files.isRegularFile(logPath)) {
                logProgress = Files.readAllBytes(logPath).length;
                lastLogModify = Files.getLastModifiedTime(logPath);
            }
        } catch (IOException ignored) {
        }
    }

    // COPIED FROM JULTI v0.18.0
    private static void checkLogContents(String newLogContents) {
        if (newLogContents.isEmpty()) {
            return;
        }
        for (String line : newLogContents.split("\n")) {
            checkLineContents(line.trim());
        }
    }

    private static void checkLineContents(String line) {
        MATCH_PATTERNS.forEach((event, pattern) -> {
            Matcher matcher = pattern.matcher(line);
            if (MATCH_EVENTS.containsKey(event) && matcher.matches()) {
                LAST_MATCHES.put(event, matcher.group());
                MATCH_EVENTS.get(event).values().forEach(RunnableEventType::runAll);
            }
        });
    }

    /**
     * Add a specified RegEx pattern responsible for calling the specified match event when matched in latest.log.
     * @param caller The name of the script or plugin making this call
     * @param matchEvent The match event for which the specified pattern is to be responsible
     * @param pattern The pattern to be responsible for the specified match event
     * @return Returns true if the match event and pattern were successfully added, otherwise false.
     */
    public static boolean addPattern(String caller, String matchEvent, Pattern pattern) {
        if (Stream.of(caller, matchEvent, pattern).anyMatch(Objects::isNull)) {
            log(Level.WARN, "addPattern called with invalid input:\n" +
                    String.join(", ", caller, matchEvent, String.valueOf(pattern)) + "\n" +
                    "All parameters must not be null.");
            return false;
        }

        /* succeeds if both args r not contained in MATCH_PATTERNS, succeeds if pattern is already the value of matchEvent,
        * fails if matchEvent is already a key, fails if pattern is already a value */
        if (MATCH_PATTERNS.containsKey(matchEvent) && MATCH_PATTERNS.values().stream().noneMatch(p -> p.pattern().equals(pattern.pattern()))) {
            log(Level.WARN, caller + " tried to add an existing match event: " + matchEvent + "\n" +
                    "Consider using a different name for this match event, or listening to the existing match event, which matches the pattern:\n" +
                    MATCH_PATTERNS.get(matchEvent).pattern());
        } else if (!MATCH_PATTERNS.containsKey(matchEvent) && MATCH_PATTERNS.values().stream().anyMatch(p -> p.pattern().equals(pattern.pattern()))) {
            log(Level.WARN, caller + " tried to add an existing pattern:\n" +
                    MATCH_PATTERNS.values().stream().filter(p -> p.pattern().equals(pattern.pattern())).findFirst().orElse(pattern).pattern() + "\n" +
                    "Consider listening to the existing match event already using that pattern: " + matchEvent);
        } else {
            if (!MATCH_PATTERNS.containsKey(matchEvent)) {
                log(Level.INFO, "Added match event: " + matchEvent + ", called by patterns matching:\n" +
                        pattern.pattern());
                MATCH_PATTERNS.put(matchEvent, pattern);
            }
            return true;
        }
        return false;
    }

    /**
     * Remove the pattern responsible for calling the specified match event.
     * @param caller The name of the script or plugin making this call
     * @param matchEvent The match event whose pattern is to be removed
     * @return Returns the previous pattern responsible for the specified match event, or null if no pattern was removed.
     */
    public static String removePattern(String caller, String matchEvent) {
        if (MATCH_PATTERNS.containsKey(matchEvent)) {
            return MATCH_PATTERNS.remove(matchEvent).pattern();
        }
        log(Level.DEBUG, caller + " tried to remove a match event that " +
                "doesn't exist: \"" + matchEvent + "\". No events removed!");
        return null;
    }

    /**
     * Across all match events, clear functions registered under this script or plugin's name.
     * <p>
     * This should be called once in Jingle scripts before any calls to logmatcher.registerListener() to ensure there are no registered functions for previous versions of the script.
     * @param caller The name of the script or plugin making this call
     */
    public static void clearListeners(String caller) {
        MATCH_EVENTS.values().forEach(m -> m.remove(caller));
    }

    /**
     * Registers a specified function to the specified match event.
     * <p>
     * When a match event is called the contents of the match are passed as an argument to all of its registered function calls.
     * A pattern responsible for calling any specified match event must exist for its registered functions to ever be called.
     * Jingle scripts' registered functions persist through reloading Jingle scripts, so scripts should call logmatcher.clearListeners() once, near the beginning of the script, before any calls to this function.
     * @param caller The name of the script or plugin making this call
     * @param matchEvent The match event to be responsible for the specified function
     * @param runnable The function for which the specified match event is to be responsible
     */
    public static void registerListener(String caller, String matchEvent, Runnable runnable) {
        MATCH_EVENTS.computeIfAbsent(matchEvent, n -> new HashMap<>())
                .computeIfAbsent(caller, s -> new RunnableEventType())
                .register(runnable);
    }

    /**
     * Get the contents of the last matched pattern responsible for calling the specified match event.
     * @param matchEvent The match event whose last matched pattern contents is to be returned
     * @return Returns the contents as a string, or null if the specified match event has not been matched yet.
     */
    public static String getLastMatch(String matchEvent) {
        return LAST_MATCHES.get(matchEvent);
    }
}
