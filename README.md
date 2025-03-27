# Log Matcher Plugin

Provides a system to constantly read the current instance's latest.log file and call custom events based on a matched 
RegEx pattern. This plugin can be used by other Jingle plugins and Jingle Lua scripts. The log file is checked for 
changes every 25 milliseconds and the plugin starts with no matching patterns or events. Documentation for this 
plugin's public functions is written in the source code and is also generated in the Jingle Lua docs file.

## Developing

### With Jingle
This plugin can be used by other Jingle plugins by adding 
`implementation com.github.luvvlyjude:Jingle-Log-Matcher:main-SNAPSHOT` to your build.gradle dependencies and including
it that dependency in your plugins jar build. You should then run `LogMatcherPlugin::initialize` near the start of
your plugin's initialization followed by using the public methods in `manager.LogManager` to add patterns and callbacks.
If you want to depend on the plugin separately you can provide a Lua library in your plugin and make a script to link
the functionality between the 2 plugins. You may consider using the [gradle shadow plugin](https://gradleup.com/shadow/)
to package this plugin with yours.

This plugin can be used by Jingle Lua scripts by calling the functions generated in the logmatcher Lua docs file.

**Note:** This plugin has no GUI in the Jingle plugins menu.
