# Agents Sync — Global AGENTS.md Synchronization for IntelliJ

![Plugin Icon](src/main/resources/META-INF/pluginIcon.svg)

[![Get from Marketplace](docs/assets/marketplace-badge.svg)](https://plugins.jetbrains.com/plugin/36037-agents-sync)

**Agents Sync** is an IntelliJ Platform plugin designed to create, edit, manage, and synchronize a system-wide `AGENTS.md` guidelines file across all projects, IDE instances, and workstations using your **JetBrains Account Backup & Sync (Settings Sync)**.

It provides a rich Markdown editor tab in IntelliJ, automatic version history snapshots with rollback capabilities, local file mirroring (e.g. `~/.junie/AGENTS.md`), and an embedded **Model Context Protocol (MCP)** server so **JetBrains AI Assistant**, **Junie**, **AIR**, and **Claude** can read and update your global agent rules directly.

---

## Key Features

- 🔄 **Cross-Machine Settings Sync:** Automatically synchronizes `AGENTS.md` and configuration across machines via the built-in JetBrains Account Settings Sync (`RoamingType.DEFAULT`).
- 📝 **Full-Blown Markdown Editor Tab:** Open `AGENTS.md` in a full IntelliJ editor tab with live split preview, outline view, and Markdown syntax highlighting (`Shift + Alt + A`).
- 🔗 **Automatic Agent Symlink Management:** Maintains symlinks pointing to your designated shared `AGENTS.md` in default agent locations (`~/.claude/CLAUDE.md`, `~/.cursorrules`, `~/.windsurfrules`, `~/.aider.conventions.md`, `~/.config/github-copilot/instructions.md`, `~/.cline/rules/AGENTS.md`, `~/.roo/rules/AGENTS.md`, etc.).
- 🕒 **Snapshot Revision History & Rollback:** Every modification automatically creates a timestamped snapshot with author metadata and commit message. Full history is synced across devices and can be rolled back with one click.
- 🤖 **AI Assistant & MCP Integration:** Runs an embedded MCP server (`localhost:8765`) enabling AI agents to read, append, update, and inspect guidelines via standardized tools and resources.
- 💾 **Designated Local Storage:** Persisted to a user-configured designated path (`~/.junie/AGENTS.md` by default) with automatic bidirectional sync between disk and IDE state.
- ⚡ **Context Actions:** Right-click any code snippet or instruction in the IDE editor and choose **Add Selection to Global AGENTS.md** to immediately promote it to a global rule.

---

## Why Synced Snapshots vs. Git Repository?

When synchronizing system instructions across machines via JetBrains Account:
- **Synced Snapshots (Implemented):** Integrated natively into IntelliJ's `PersistentStateComponent` with zero external dependencies. Changes replicate instantaneously and deterministically without merge conflict locks, credential management, or external repo plumbing.
- **Git Repositories (Disadvantages):** Require external Git binaries or heavy JGit dependencies, explicit branch merge resolution during background sync, network authentication setup on every new machine, and file-locking edge cases during concurrent IDE sessions.

---

## Installation & Setup

### 1. Build from Source
```bash
# Clone repository
git clone https://github.com/tpluscode/intellij-agents-sync.git
cd intellij-agents-sync

# Build plugin zip artifact
./gradlew buildPlugin
```
The packaged plugin will be generated at `build/distributions/intellij-agents-sync-1.0.0.zip`.

### 2. Install into IDE
1. Open IntelliJ IDEA (2024.2+).
2. Go to **Settings / Preferences** (`Cmd + ,` or `Ctrl + Alt + S`) -> **Plugins**.
3. Click the gear icon ⚙️ -> **Install Plugin from Disk...**.
4. Select `build/distributions/intellij-agents-sync-1.0.0.zip`.
5. Restart IDE.

---

## Usage Guide

### Opening the Editor Tab
- **Keyboard Shortcut:** `Shift + Alt + A`
- **Menu:** **Tools** -> **Agents Sync** -> **Open Global AGENTS.md Editor**
- **Settings Page:** **Settings** -> **Tools** -> **Agents Sync (AGENTS.md)**

Saving changes (`Cmd + S` or auto-save) writes to the local file and queues the document for JetBrains Settings Sync.

### Adding Rules from Code
1. Select any code block or comment in any file.
2. Right-click and choose **Add Selection to Global AGENTS.md**.
3. Enter an optional summary message and confirm.

### History & Rollback
1. Open **Settings** -> **Tools** -> **Agents Sync (AGENTS.md)**.
2. Switch to the **History & Snapshots** tab.
3. Select any historical snapshot and click **Restore Selected Snapshot** or **Preview Snapshot Content**.

### Managing Agent Symlinks
Agents Sync automatically creates and maintains symlinks pointing from native agent configurations to the designated shared `AGENTS.md`:

| Agent / Tool | Default Location | Description |
|---|---|---|
| **Junie / JetBrains** | `~/.junie/AGENTS.md` | Primary guidelines for JetBrains AI agents |
| **OpenAI Codex** | `~/.codex/AGENTS.md` | Global instructions for OpenAI Codex |
| **Claude Code** | `~/.claude/CLAUDE.md` | Global prompt instructions for Claude CLI |
| **Cursor** | `~/.cursorrules` | Global rule set for Cursor editor |
| **Windsurf / Codeium** | `~/.windsurfrules` | Global system rules for Windsurf IDE |
| **Aider** | `~/.aider.conventions.md` | Conventions file for Aider assistant |
| **GitHub Copilot** | `~/.config/github-copilot/instructions.md` | Prompt instructions for GitHub Copilot |
| **Cline** | `~/.cline/rules/AGENTS.md` | Global rules directory for Cline |
| **Roo Code** | `~/.roo/rules/AGENTS.md` | Global rules directory for Roo Code |
| **User Home** | `~/.AGENTS.md` | Standard root location for CLI tools |
| **XDG Config** | `~/.config/AGENTS.md` | XDG standard configuration directory |

You can add custom symlink paths, toggle individual agents, or trigger an immediate refresh in **Settings** -> **Tools** -> **Agents Sync (AGENTS.md)** -> **Agent Symlinks**.

For existing installations, use **Reset Defaults** in **Agent Symlinks** to include newly supported agents, or add the Codex path manually to preserve your custom list. Codex uses `~/.codex/AGENTS.md` by default; if you set a custom `CODEX_HOME`, adjust the symlink path to match. See the [official Codex instructions documentation](https://learn.chatgpt.com/docs/agent-configuration/agents-md).

---

## AI Assistant / AIR / MCP Integration

Agents Sync embeds a Model Context Protocol (MCP) server on port `8765` (configurable in settings).

### Available MCP Tools & Resources
- `read_agents_md`: Returns the current system-wide `AGENTS.md` content and metadata.
- `update_agents_md(content, message)`: Updates the full markdown content with author attribution and message.
- `append_agents_rule(rule, message)`: Appends a specific guideline or section.
- `get_agents_history()`: Lists recent snapshot IDs, timestamps, and commit messages.
- `rollback_agents_md(snapshot_id)`: Reverts `AGENTS.md` to a previous revision.
- Resource `agent://system/AGENTS.md`: Direct markdown stream.

### Connecting MCP Clients (Claude Desktop, Junie, AIR)
Add the following to your MCP client config (e.g. `claude_desktop_config.json`):

```json
{
  "mcpServers": {
    "agents-sync": {
      "url": "http://127.0.0.1:8765"
    }
  }
}
```

Or connect via REST/HTTP directly:
- `GET http://127.0.0.1:8765/health`
- `GET http://127.0.0.1:8765/content`

---

## GitHub Actions & Marketplace Publishing

The repository includes pre-configured GitHub Actions workflows in `.github/workflows/`:

1. **`build.yml` (CI):**
   - Automatically runs on pull requests and pushes to `main`.
   - Executes tests, runs IntelliJ Plugin Verifier, and validates distribution packaging.

2. **`publish.yml` (CD):**
   - Reusable workflow that verifies, signs, and publishes the plugin to JetBrains Marketplace.
   - Can be triggered manually via `workflow_dispatch` (e.g. for EAP releases) or automatically called by `release-please.yml`.
   - Requires repository secrets configured in **GitHub -> Settings -> Secrets and variables -> Actions**:
     - `PUBLISH_TOKEN`: JetBrains Marketplace upload token.
     - `CERTIFICATE_CHAIN`: (Optional) Plugin signing certificate chain.
     - `PRIVATE_KEY`: (Optional) Plugin signing private key.
     - `PRIVATE_KEY_PASSWORD`: (Optional) Private key password.

3. **`release-please.yml` (Release Orchestration):**
   - Maintains automated Release PRs based on Conventional Commits.
   - On Release PR merge, automatically creates the GitHub Release tag and invokes `publish.yml` to deploy the artifact to JetBrains Marketplace.

## Automated Releases & Release Please

This repository uses **Google Release Please** (`release-please-action`) alongside the **JetBrains Changelog Plugin** (`org.jetbrains.changelog`) for automated Semantic Versioning and Marketplace releases:

1. **Conventional Commits:** Contributors write commits using conventional prefixes:
   - `fix: ...` -> triggers automated **patch** bump (e.g. `1.0.1`)
   - `feat: ...` -> triggers automated **minor** bump (e.g. `1.1.0`)
   - `feat!: ...` or `BREAKING CHANGE:` -> triggers automated **major** bump (e.g. `2.0.0`)
2. **Automated Release PR:** On pushes to `main`, Release Please automatically maintains an open Release PR that bumps `pluginVersion` in `gradle.properties` and updates `CHANGELOG.md`.
3. **Automated Deployment:** Merging the Release PR automatically tags the repository, creates a GitHub Release, and publishes the verified plugin directly to the JetBrains Marketplace.

---

## License
MIT License. Created by Tomasz Pluskiewicz.
