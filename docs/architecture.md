# Architecture

## Architecture Overview

DTTBench is organized into components responsible for the user interface, script execution,
command processing, port communication, logging, data visualization, and utility functions.

The component diagram shows the main application components and their dependencies.

![](images/Components.png)

## Main Components

### UI and Execution Control

The UI layer contains the main application window, mode-specific controllers,
command list management, and device-specific tabs.

[`ViewModeManager`](../src/main/java/com/terminal/controller/ViewModeManager.java) manages
the transition between modes.
Modes use [`ScriptEngine`](../src/main/java/com/terminal/script/ScriptEngine.java) to execute
test scripts, while their controllers manage port access and handle session events.

### Script Execution

[`ScriptEngine`](../src/main/java/com/terminal/script/ScriptEngine.java) coordinates script
execution. It obtains command handlers from
[`HandlerRegistry`](../src/main/java/com/terminal/handlers/HandlerRegistry.java)
and sends commands through [`PortSession`](../src/main/java/com/terminal/port/PortSession.java). 
It also forwards plot data to [`PlotterManager`](../src/main/java/com/terminal/plotter/PlotterManager.java).


### Command Handling

The handler layer processes device-specific commands and responses through the 
[`CommandHandler`](../src/main/java/com/terminal/handlers/CommandHandler.java) interface.
[`HandlerRegistry`](../src/main/java/com/terminal/handlers/HandlerRegistry.java) selects an appropriate handler,
with [`DefaultCommandHandler`](../src/main/java/com/terminal/handlers/DefaultCommandHandler.java)
serving as a fallback. Specialized handlers, such as
[`SC18IMHandler`](../src/main/java/com/terminal/handlers/devices/SC18IMHandler.java)
and [`BH1745NUCHandler`](../src/main/java/com/terminal/handlers/devices/BH1745NUCHandler.java), 
provide device-specific processing.  
[`EnumCommandMatcher`](../src/main/java/com/terminal/handlers/EnumCommandMatcher.java) is a helper class that encapsulates command matching logic for enums implementing
[`CommandDefinition`](../src/main/java/com/terminal/handlers/CommandDefinition.java). 
It allows device-specific enums to reuse the same matching logic.

This design allows support for additional devices to be added by implementing the
[`CommandHandler`](../src/main/java/com/terminal/handlers/CommandHandler.java) interface and registering the new handler in
[`HandlerRegistry`](../src/main/java/com/terminal/handlers/HandlerRegistry.java).

### Port Management

[`PortManager`](../src/main/java/com/terminal/port/PortManager.java)
maintains a registry of [`PortSession`](../src/main/java/com/terminal/port/PortSession.java) instances and manages their lifecycle.  
[`PortSession`](../src/main/java/com/terminal/port/PortSession.java) encapsulates communication with a single serial port using **jSerialComm**.  
[`SessionListenerManager`](../src/main/java/com/terminal/port/SessionListenerManager.java)
manages the delivery of session events to registered listeners through the
[`SessionListener`](../src/main/java/com/terminal/port/SessionListener.java) interface.

[`PortID`](../src/main/java/com/terminal/port/PortID.java) provides identifiers for the supported ports.

**Existing bug in Coordinated Mode**: An entry in **Additional Ports** can be removed while its port is still open.
The associated [`PortSession`](../src/main/java/com/terminal/port/PortSession.java)
remains bound to the original [`PortID`](../src/main/java/com/terminal/port/PortID.java).
To close the port, restore the removed entry at its original position (row number),
select the corresponding port, and click **Open**, followed by **Close**.
Alternatively, use the `-close` keyword in a script.

### Plotting and Utilities

[`PlotterManager`](../src/main/java/com/terminal/plotter/PlotterManager.java) manages plotter instances,
while [`Plotter`](../src/main/java/com/terminal/plotter/Plotter.java) handles data visualization.  
Each port has its own [`Plotter`](../src/main/java/com/terminal/plotter/Plotter.java), 
containing multiple measurement series named according to command comments added by `-c`.  

**Current limitation:** In Coordinated Mode, plotting is currently available only for the port
displayed in the left panel (`PortID.COORDINATED_1`).

The utility layer provides reusable functionality shared across the application. 
- [`ByteUtils`](../src/main/java/com/terminal/utils/ByteUtils.java) handles byte-level data conversion and formatting.
- [`FileUtils`](../src/main/java/com/terminal/utils/FileUtils.java) provides file-related operations.
- [`UtilsRunner`](../src/main/java/com/terminal/utils/UtilsRunner.java) launches external utilities.  
- [`Config`](../src/main/java/com/terminal/config/Config.java) handles application configuration.
- [`BuildInfo`](../src/main/java/com/terminal/config/BuildInfo.java) provides build information.



## Command Execution Flow

[`ScriptEngine`](../src/main/java/com/terminal/script/ScriptEngine.java) coordinates execution,
while [`PortManager`](../src/main/java/com/terminal/port/PortManager.java) provides access to port sessions.  
The response is processed by the selected command handler, which parses the data and produces a
[`CommandResult`](../src/main/java/com/terminal/handlers/CommandResult.java). 
The resulting data is logged, and plot data is forwarded to 
[`PlotterManager`](../src/main/java/com/terminal/plotter/PlotterManager.java).

The sequence diagram illustrates the interaction between the main components during command execution.  
For more detailed information see the full [sequence-diagram](images/sequence.png).

![](images/Sequence_short.png)
