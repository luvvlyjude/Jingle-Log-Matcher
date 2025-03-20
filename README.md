# Log Matcher Plugin

Provides a system to constantly read the current instance's latest.log file and call custom events based on a matched 
RegEx pattern. This plugin can be used by other Jingle plugins and Jingle Lua scripts. The log file is checked for 
changes every 25 milliseconds and the plugin starts with no matching patterns or events. Documentation for this 
plugin's public functions is written in the source code and is also generated in the Jingle Lua docs file.

## Developing

### With Jingle
This plugin can be used by other Jingle plugins by adding 
`implementation com.github.luvvlyjude:Jingle-Log-Matcher:main-SNAPSHOT` to your build.gradle dependencies and using the
public methods in manager:LogManager. That plugin then depends on Jingle-Log-Matcher being loaded by Jingle.

This plugin can be used by Jingle Lua scripts by calling the functions generated in the logmatcher Lua docs file.

**Note:** This plugin has no GUI in the Jingle plugins menu.
