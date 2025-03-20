package me.luvvlyjude.logmatcher.lua;

import me.luvvlyjude.logmatcher.manager.LogManager;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaFunction;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.script.ScriptFile;
import xyz.duncanruns.jingle.script.lua.LuaLibrary;

import javax.annotation.Nullable;
import java.util.function.Supplier;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class LogMatcherLuaLibrary extends LuaLibrary {
    public LogMatcherLuaLibrary(ScriptFile script, Globals globals) {
        super("logmatcher", script, globals);
    }

    @NotALuaFunction
    private static Runnable wrapFunction(LuaFunction function, Supplier<String> argSupplier) {
        return () -> {
            synchronized (Jingle.class) {
                function.call(argSupplier.get());
            }
        };
    }

    @LuaDocumentation(description = "Add a specified RegEx pattern responsible for calling the specified match event when matched in latest.log.\n" +
            "Returns true if the match event and pattern were successfully added, otherwise false.")
    public boolean addPattern(String matchEvent, String pattern) {
        assert this.script != null;
        return LogManager.addPattern(this.script.getName(), matchEvent, Pattern.compile(pattern));
    }

    @LuaDocumentation(description = "Remove the pattern responsible for calling the specified match event.\n" +
            "Returns the previous pattern responsible for the specified match event, or null if no pattern was removed.")
    public String removePattern(String matchEvent) {
        assert this.script != null;
        return LogManager.removePattern(this.script.getName(), matchEvent);
    }

    @LuaDocumentation(description = "Across all match events, clear functions registered under this script or plugin's name.\n" +
            "This should be called once in Jingle scripts before any calls to logmatcher.registerListener() to ensure there are no registered functions for previous versions of the script.")
    public void clearListeners() {
        assert this.script != null;
        LogManager.clearListeners(this.script.getName());
    }

    @LuaDocumentation(description = "Registers a specified function to the specified match event.\n" +
            "When a match event is called the contents of the match are passed as an argument to all of its registered function calls.\n" +
            "A pattern responsible for calling any specified match event must exist for its registered functions to ever be called.\n" +
            "Jingle scripts' registered functions persist through reloading Jingle scripts, so scripts should call logmatcher.clearListeners() once, near the beginning of the script, before any calls to this function.",
            paramTypes = {"string","function<string>"})
    public void registerListener(String matchEvent, LuaFunction matchFunction) {
        assert this.script != null;
        Runnable runnable = wrapFunction(matchFunction, () -> LogManager.getLastMatch(matchEvent));
        LogManager.registerListener(this.script.getName(), matchEvent, runnable);
    }

    @LuaDocumentation(description = "Get the contents of the last matched pattern responsible for calling the specified match event.\n" +
            "Returns the contents as a string, or null if the specified match event has not been matched yet.",
            returnTypes = "string|nil")
    @Nullable
    public String getLastMatch(String matchEvent) {
        return LogManager.getLastMatch(matchEvent);
    }
}
