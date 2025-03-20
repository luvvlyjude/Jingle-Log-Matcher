package me.luvvlyjude.logmatcher;

import com.google.common.io.Resources;
import me.luvvlyjude.logmatcher.manager.LogManager;
import me.luvvlyjude.logmatcher.lua.LogMatcherLuaLibrary;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.JingleAppLaunch;
import xyz.duncanruns.jingle.plugin.PluginEvents;
import xyz.duncanruns.jingle.plugin.PluginManager;
import xyz.duncanruns.jingle.script.lua.LuaLibraries;
import xyz.duncanruns.jingle.util.ExceptionUtil;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class LogMatcherPlugin {
    public static final String PLUGIN_NAME = Optional.ofNullable(LogMatcherPlugin.class.getPackage().getImplementationTitle()).orElse("DEV-PLUGIN");
    public static final String VERSION = Optional.ofNullable(LogMatcherPlugin.class.getPackage().getImplementationVersion()).orElse("DEV-VERSION");

    public static void main(String[] args) throws IOException {
        // This is only used to test the plugin in the dev environment
        // LogMatcherPlugin.main itself is never used when users run Jingle

        JingleAppLaunch.launchWithDevPlugin(args, PluginManager.JinglePluginData.fromString(
                Resources.toString(Resources.getResource(LogMatcherPlugin.class, "/jingle.plugin.json"), Charset.defaultCharset())
        ), LogMatcherPlugin::initialize);
    }

    public static void initialize() {
        // This gets run once when Jingle launches
        log(Level.INFO, "Running " + PLUGIN_NAME + " Plugin v" + VERSION + "!");

        LuaLibraries.registerLuaLibrary(LogMatcherLuaLibrary::new);

        AtomicLong timeTracker = new AtomicLong(System.currentTimeMillis());

        PluginEvents.END_TICK.register(() -> {
            // This gets run every tick (1 ms)
            long currentTime = System.currentTimeMillis();
            if (currentTime - timeTracker.get() > 25) {
                // Check every 25ms (twice an MC game tick)
                try {
                    LogManager.checkLog();
                } catch (Exception e) {
                    logError("Error while checking latest.log:", e);
                }
                timeTracker.set(currentTime);
            }
        });

    }

    public static void log(Level level, String message) {
        Jingle.log(level, "(LogMatcher) " + message);
    }

    public static void logError(String failMessage, Throwable t) {
        log(Level.ERROR, failMessage + " " + ExceptionUtil.toDetailedString(t));
    }
}
