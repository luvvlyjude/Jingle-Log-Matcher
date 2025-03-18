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
import java.util.regex.Pattern;
import java.util.*;

import static me.luvvlyjude.logmatcher.LogMatcherPlugin.log;

public class LogManager {
    private static final Map<String, Map<String, RunnableEventType>> logMatchEvents = new HashMap<>(); // <event_name, <runnableevent_owner, runnable_event>>
    private static final Map<Pattern, String> logMatchPatterns = new HashMap<>(); // <matching_pattern, event_name>

    private static long logProgress = -1;
    private static FileTime lastLogModify = null;

    private LogManager() {
    }

    public static void checkLog() {
        if (!Jingle.getMainInstance().isPresent()) {
            return;
        }

        if (logMatchPatterns.isEmpty()) {
            return;
        }

        String newLogContents = getNewLogContents();
        checkLogContents(newLogContents);
    }

    // COPIED FROM JULTI v0.18.0
    private static String getNewLogContents() {
        Path logPath = getLogPath();

        // If log progress has not been jumped, jump and return
        if (logProgress == -1) {
            tryJumpLogProgress();
            return "";
        }

        // If modification date has not changed, return
        if (!checkForLogChanges(logPath)) {
            return "";
        }

        // If file size is significantly less than log progress, reset log progress
        if (checkForLogReset(logPath)) {
            tryJumpLogProgress();
            log(Level.INFO, "Log reading restarted! (" + logPath + ")");
            return "";
        }

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
        } catch (Exception ignored) {
            return "";
        }
    }

    private static Path getLogPath() {
        assert Jingle.getMainInstance().isPresent();
        return Jingle.getMainInstance().get().instancePath.resolve("logs").resolve("latest.log");
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
    private static void tryJumpLogProgress() {
        try {
            Path logPath = getLogPath();
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
        for (Map.Entry<Pattern, String> entry : logMatchPatterns.entrySet()) {
            if (entry.getKey().matcher(line).matches()) {
                logMatchEvents.get(entry.getValue()).values().forEach(RunnableEventType::runAll);
            }
        }
    }

    public static void addLogMatchEvent(String caller, String matchEventName, Pattern pattern) {
        if (logMatchPatterns.containsValue(matchEventName)) {
            Pattern existingEventPattern = logMatchPatterns.entrySet().stream()
                    .filter(e -> Objects.equals(matchEventName, e.getValue()))
                    .map(Map.Entry::getKey)
                    .findFirst().orElseThrow(() -> new IllegalStateException("Expected a matching pattern but found none"));

            log(Level.WARN, caller + " tried to create log file matching event with an " +
                    "existing name (" + matchEventName + "). The pattern for the existing event is " +
                    existingEventPattern.toString() + ". Event not added!");
            return;
        }

        if (logMatchPatterns.containsKey(pattern)) {
            log(Level.WARN, caller + " tried to create log file matching event with an " +
                    "existing pattern (" + pattern.toString() + "). The name for the existing event is " +
                    logMatchPatterns.get(pattern) + ". Event not added!");
            return;
        }
        logMatchPatterns.put(pattern, matchEventName);
    }

    public static void removeLogMatchEvent(String caller, String matchEventName) {
        if (!logMatchPatterns.containsValue(matchEventName)) {
            log(Level.WARN, caller + " tried to remove a log file matching event that " +
                    "doesn't exist (" + matchEventName + "). No events removed!");
            return;
        }
        logMatchPatterns.entrySet().removeIf(e -> Objects.equals(matchEventName, e.getValue()));
    }

    public static void clearLogMatchEventListeners(String caller) {
        logMatchEvents.values().forEach(m -> m.remove(caller));
    }

    public static void registerLogMatchEventListener(String caller, String matchEventName, Runnable runnable) {
        logMatchEvents.computeIfAbsent(matchEventName, n -> new HashMap<>())
                .computeIfAbsent(caller, s -> new RunnableEventType())
                .register(runnable);
    }
}
