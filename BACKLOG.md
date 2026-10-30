# 📋 Project Backlog

This document tracks planned changes and potential future directions for DTTBench.

> **Legend:**
> 🟡 **Development** — planned changes and fixes
> ⚪ **Ideas** — potential future development directions

---

## 🟡 Development

### Refactoring

- **Extract reusable `PortPanel` component:** Move the port panel UI and its logic into a dedicated FXML file and controller for reuse across scenes.
- **Refactor [`PlotterManager`](src/main/java/com/terminal/plotter/PlotterManager.java)
to use instance-based state and methods.**
- **Refactor [`FindReplace`](src/main/java/com/terminal/controller/FindReplace.java)
class:** Separate initialization from action logic.
- **Refactor logging methods:** Merge `appendUtilLog()` logic into the main `appendLog()` method to reduce code duplication.

### Bug Fixes

- **Fix script execution on closed port:** Prevent script continuation if the port is closed.
- **Fix `additionalPorts` lifecycle bug:** Prevent row deletion if the port is open.

### Features

- **Add plotter selection in Coordinated Mode:** Show a port selection dialog when opening a plotter, listing only ports with available measurement data. Open the plotter for the selected port.
- **Optimize [`Plotter`](src/main/java/com/terminal/plotter/Plotter.java)
data handling:** Avoid unnecessary data accumulation and parse the `TextArea` content only when needed.
- **Improve [`Plotter`](src/main/java/com/terminal/plotter/Plotter.java) UI:**
  - Allow showing and hiding individual curves by name.
  - Prevent curves from connecting across different iterations in the log.
- **Normalize keywords parsing:** Prevent exceptions on malformed keys.
- **Prevent duplicate commands in CommandList:** Check if a command already exists before adding. Show a warning dialog if it does.
- **Add custom response timeout:** Allow scripts to specify a response timeout using the `-t` keyword in scripts (currently hardcoded to 30s).
- **Add periodic command sending:** Support keep-alive commands.
- **Add script control flow:** Support `if` / `else` conditions in the DSL.
- **Extend StepMode:** Allow StepMode to execute a script up to a specified line or command and then pause.
- **Add conditional response handling:** Wait for a specific string in the response before proceeding to the next command.
- **Add DSL byte parsing:** Specify exactly which bytes to parse from the response.
- **Add handler routing:** Allow specifying the target 
[`CommandHandler`](src/main/java/com/terminal/handlers/CommandHandler.java)
by device name in the DSL using `-h`.
- **Add device chaining in handlers:** Support command-merging rules for interacting devices in CommandHandlers.

---

## ⚪ Ideas

- **Additional transports:** Add support for TCP and BLE communication.
- **DLMS/COSEM integration:** Add support for communicating with smart meters using DLMS/COSEM.
- **Real-time port listening:** Optional mode for continuous port monitoring.
- **Auto testing (JUnit):** Write unit tests for core logic.
