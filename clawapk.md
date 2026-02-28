# Clawminium Native APK Strategy (ClawAPK)

**Objective:** Run the `nanobot` "Brain" natively on the Aluminium device as a background service via Chaquopy, eliminating the need for a secondary Linux VM and achieving a true "On-Device Hybrid" architecture.

---

## 🛑 The Core Blockers (The Rust Wall)

Standard `nanobot` development relies on several libraries that use **Rust-based native extensions**, which lack pre-built wheels for Android ARM64 and are extremely complex to cross-compile within the Android build process:

1.  **Pydantic v2 (`pydantic-core`):** The core validation logic is written in Rust.
2.  **LiteLLM (`tiktoken`):** Token counting for context window management requires Rust.
3.  **MCP SDK (`mcp`):** The official Anthropic SDK requires Pydantic v2, inheriting the Rust blocker.

---

## 🛠️ The "nanobot-droid" Solution

Research into the `nanobot-droid` fork reveals a "surgical downgrade" strategy to bypass these blockers:

### 1. Pydantic Compatibility Layer
*   **Downgrade:** Force `pydantic==1.10.20` in the Android `build.gradle`. Pydantic v1 is pure-Python/C and has stable Android support.
*   **Bridge:** Use `nanobot/utils/pydantic_compat.py` to provide a unified interface for both v1 and v2.
    *   *v2:* `obj.model_dump()`
    *   *v1:* `obj.dict()`

### 2. Lightweight LLM Provider (`HttpxOpenAIProvider`)
*   **Bypass:** Implement a direct OpenAI-compatible client using `httpx` and `json-repair`.
*   **Fallback:** Update `_make_provider` in `commands.py` to catch `ImportError` (when `litellm` or `tiktoken` fails to load) and automatically switch to the lightweight provider.
*   **Trade-off:** Loss of non-OpenAI-compatible providers (Anthropic, native Gemini) in the APK build.

### 3. Foreground Service (`BotService.kt`)
*   Wrap the Python `asyncio` loop in a standard Android Foreground Service to prevent the OS from killing the process while it waits for Telegram/Message Bus events.

---

## ⚠️ The Critical Gap: Broken MCP

The most significant problem with the `nanobot-droid` approach is that **MCP is disabled**. The official `mcp` SDK cannot be installed alongside Pydantic v1. 

**Impact:** The Brain (APK) cannot communicate with the `AgentKernel` (APK) using the standardized Model Context Protocol, breaking the "Privileged Kernel" architecture of Phase 4.

---

## 🌉 The Mitigation: Lite MCP Client

To achieve native APK support without breaking the Phase 4 demo, we will implement a **Lite MCP Client** that bypasses the official SDK:

### 1. Protocol Implementation
Since `AgentKernel` is an HTTP-based MCP server (port 8080), we can implement the JSON-RPC 2.0 protocol manually using `httpx`:
*   **`tools/list`**: Fetch tool definitions from `http://localhost:8080/rpc`.
*   **`tools/call`**: Execute tools by posting JSON-RPC payloads.

### 2. Compatibility Patch
Modify `nanobot/agent/tools/mcp.py` to:
1.  Check if the `mcp` library is available.
2.  If missing, check `config.json` for HTTP/SSE MCP servers.
3.  Initialize the **LiteMCPClient** for those servers.
4.  Wrap the JSON responses into standard `nanobot` Tool results.

---

## 📊 Comparison: Linux VM vs. Native APK

| Feature | Phase 4.2 (Linux VM) | ClawAPK (Native) |
| :--- | :--- | :--- |
| **Effort** | Low (Zero code changes) | High (Requires protocol rewrite) |
| **LLM Support** | Full (Claude, Gemini, etc.) | Restricted (OpenAI-compatible only) |
| **Token Accuracy** | High (via `tiktoken`) | Heuristic (char count/4) |
| **MCP Strategy** | Official SDK | Lite JSON-RPC Implementation |
| **Isolation** | Best (VM Container) | Good (Android Sandbox) |
| **Experience** | "Hybrid Cloud/Edge" | "Pure Mobile AI" |

---

## 🚀 Recommendation

For the **72-Hour Hackathon**, the **Linux VM (Phase 4.2)** remains the primary recommendation due to its stability and full feature set. However, the **ClawAPK** path is the "Gold Standard" for production deployment. If the "Lite MCP" patch is implemented, the APK becomes the superior demo for showcasing aluminium's integrated power.
