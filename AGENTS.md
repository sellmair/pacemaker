# Agent instructions

Always use the Android Studio MCP (`mcp__studio__*`) for all code operations supported by its tools, including reading, searching, creating, editing, and refactoring code, inspecting problems, building, and running code.

Always use the Android Studio MCP tools (`mcp__studio__*`) for project exploration, file reads and edits, symbol lookup, usage and call analysis, refactoring, inspections, builds, and run configurations whenever a suitable tool is available. Never use `mcp__idea__*` tools in this repository, including as a fallback. This project's `.codex/config.toml` configures the `studio` server at `http://127.0.0.1:64342/stream` and disables the standalone IntelliJ IDEA `idea` server. Pass `projectPath` explicitly using the repository's absolute path. Fall back to shell commands or other non-IDE tools only when the Android Studio MCP connection is unavailable or its tools do not support the operation.
