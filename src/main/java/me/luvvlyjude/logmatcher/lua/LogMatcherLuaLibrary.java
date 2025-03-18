package me.luvvlyjude.logmatcher.lua;

import me.luvvlyjude.logmatcher.manager.LogManager;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaFunction;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.script.ScriptFile;
import xyz.duncanruns.jingle.script.lua.LuaLibrary;

import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class LogMatcherLuaLibrary extends LuaLibrary {
    public LogMatcherLuaLibrary(ScriptFile script, Globals globals) {
        super("logmatcher", script, globals);
    }

    @NotALuaFunction
    private static Runnable wrapFunction(LuaFunction function) {
        return () -> {
            synchronized (Jingle.class) {
                function.call();
            }
        };
    }

    @LuaDocumentation(description = "Add a new log match event to be called when the given Java regex pattern is matched in the latest.log file.")
    public void addLogMatchEvent(String matchEventName, String pattern) {
        assert this.script != null;
        LogManager.addLogMatchEvent(this.script.getName(), matchEventName, Pattern.compile(pattern));
    }

    @LuaDocumentation(description = "Remove a previously added log match event by name.")
    public void removeLogMatchEvent(String matchEventName) {
        assert this.script != null;
        LogManager.removeLogMatchEvent(this.script.getName(), matchEventName);
    }

    @LuaDocumentation(description = "Clear all of the runnable event listeners owned by this script. " +
            "This should be run at the start of the script to ensure no listeners for previous versions of this script exist.")
    public void clearMatchEventListeners() {
        assert this.script != null;
        LogManager.clearLogMatchEventListeners(this.script.getName());
    }

    @LuaDocumentation(description = "Registers the given function to a log match event by name. " +
            "A matching pattern to call this event should be defined, by either a plugin or script, for this listener to ever be called. " +
            "Script listeners persist through script reloads so make sure to use logmatcher.clearMatchEventListeners() before calling this.")
    public void listenMatchEvent(String matchEventName, LuaFunction matchFunction) {
        assert this.script != null;
        LogManager.registerLogMatchEventListener(this.script.getName(), matchEventName, wrapFunction(matchFunction));
    }
}
