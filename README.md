# Jingle Log Matcher

Provides a system to read the current instance's latest.log file and call events based on a matched RegEx pattern. This plugin can be used by other Jingle plugins and Jingle Lua scripts. The log file is checked for changes every 25 milliseconds and the plugin starts with no matching patterns or events. 

## Docs

The argument `String caller` should always refer to the name or ID of the script or plugin making the call.

- `addLogMatchEvent(String caller, String matchEventName, Pattern pattern) -> void`

Adds a new Pattern to match and the associated event name to call when matched. When there are no existing Patterns to match the plugin will not make any checks to the log file.

- `removeLogMatchEvent(String caller, String matchEventName) -> void`

Removes the Patterns for the given event name from being matched and calling their events. This does not unregister any Runnables or their associations to event names.

- `clearLogMatchEventListeners(String caller) -> void`

Removes all the existing runnable events, across all matching events, registered under the ownership of `caller`. Important for Jingle Lua script development as reloading a script does not unregister it's Runnables from their associated event names.

- `registerLogMatchEventListener(String caller, String matchEventName, Runnable runnable) -> void`

Registers the given Runnable to the given event name. All Runnables registered to an event name will be called when a Pattern associated with the same event name is matched.

## Developing

### With Jingle
This plugin can be used by other Jingle plugins by adding `implementation com.github.luvvlyjude:Jingle-Log-Matcher:main-SNAPSHOT` to your build.gradle dependencies and using the public methods in manager:LogManager. That plugin then depends on Jingle-Log-Matcher being loaded by Jingle.

This plugin can be used by Jingle Lua scripts by calling the functions generated in the logmatcher Lua docs file.

### Jingle Plugin GUIs (Unused)
Jingle GUIs are made with the IntelliJ IDEA form designer, if you intend on changing GUI portions of the code, IntelliJ IDEA must be configured in a certain way to ensure the GUI form works properly:
- `Settings` -> `Build, Execution, Deployment` -> `Build Tools` -> `Gradle` -> `Build and run using: IntelliJ Idea`
- `Settings` -> `Editor` -> `GUI Designer` -> `Generate GUI into: Java source code`