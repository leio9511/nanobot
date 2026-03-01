# Clawminium 72-Hour Hackathon Execution Plan (CLI-Optimized)

**Project:** Clawminium AURA Architecture PoC
**Strategy:** "Dual-track + Dimensional Strike" (双轨并行 + 降维打击). 
We bypass the 10+ hour AOSP `frameworks/base` compile-and-flash cycle. Instead, we use standard Android apps elevated to OS-level privileges via `android:sharedUserId="android.uid.system"` and AOSP platform keys. All development will be driven via the cloudTop CLI (Gradle & ADB) without needing Android Studio.

---

## 🚀 Phase 1: The Local "Lobotomy" & Telegram Verification (Hour 1)
**Goal:** Prove the Python logic and Telegram connectivity on the workstation before introducing Android complexities. **(COMPLETE)**
(Commit: `b6d2d9d`, `4803795`)

1. **The Lobotomy:** 
   - Edit `nanobot/agent/loop.py`.
   - Comment out `self._register_default_tools()` inside the `__init__` method to physically strip the agent of local workstation privileges.
2. **Configuration:** 
   - Create `~/.nanobot/config.json`.
   - Enable the `telegram` channel, insert the Bot Token, and add your Telegram username to the `allowFrom` list. Disable `whatsapp`.
3. **Local Test:** 
   - Run the agent locally on the cloudTop: `uv run nanobot gateway`
   - Send a Telegram message from your phone. Verify the bot replies and confirm it cannot access local files (e.g., ask it to "list files").

---

## 🧠 Phase 2: Architectural Pivot - Cloud-to-Device Orchestration (Hour 2)
**Goal:** Run the "Brain" on the CloudTop (Workstation) to avoid Rust/Maturin cross-compilation issues on Android (specifically `pydantic-core`). **(COMPLETE)**
(Commit: `b435424`, `fe357a7`)

*Why the Pivot?* During the Hackathon, cross-compiling complex Python dependencies like `pydantic-core` (which requires Rust/Maturin) for ARM64 Android within Chaquopy proved to be a significant blocker. By running `nanobot` on the CloudTop and exposing the Android capabilities via an MCP Server on the device, we achieve a stronger architectural story: **Cloud-based reasoning orchestrating a secure, privileged on-device Kernel.**

1. **The Brain (CloudTop):** `nanobot` runs smoothly on your workstation via `uv run nanobot gateway`. It handles Telegram and LLM routing.
2. **The Connection:** We will use `adb reverse tcp:8080 tcp:8080` to securely bridge the CloudTop to the Android device. (Verified: Gateway active, Telegram bot @AlClawHackBot responding, and LLM error handling improved).

---

## 🛡️ Phase 3: Forging the "AgentKernel" App - The Golden Shortcut (Hour 3)
**Goal:** Build the privileged Java service (User 0) that intercepts intents and exposes MCP tools over a local HTTP server. **(COMPLETE)**
(Commit: `faf02e0`)

1. **Scaffold Project:** 
   - Generated a standard Gradle Android project named `AgentKernel` via CLI.
2. **The God Privilege:** 
   - In `AndroidManifest.xml`, attempted `android:sharedUserId="android.uid.system"`, but reverted due to lack of platform keys. App runs as a standard user (user 10).
3. **Platform Keys Generation:** 
   - Skipped due to unavailable AOSP keys. A placeholder `platform.jks` was generated to satisfy build requirements.
4. **NanoHTTPD Implementation:** 
   - Added `implementation 'org.nanohttpd:nanohttpd:2.3.1'` to `build.gradle`.
   - Wrote `KernelService.java` to start a NanoHTTPD server on `localhost:8080`.
   - Implemented the MCP Handshake and a `create_calendar_event` tool that uses the `ContentResolver` to directly insert events into the calendar database.
5. **Build & Deploy:** 
   - Ran `./gradlew assembleDebug` and successfully installed the `AgentKernel.apk` on the Aluminium device for user 10, granting calendar permissions via `adb`.

---

## 🌉 Phase 4: The Bridge & The Climax (Hour 4)
**Goal:** Connect the Brain and the Kernel via MCP and execute the Cross-Device Trip Planner Demo. **(COMPLETE)**
(Commit: `faf02e0`)

1. **Network Config:** 
   - Ensured both apps have `<uses-permission android:name="android.permission.INTERNET" />`.
2. **MCP Routing:** 
   - Updated the `config.json` on the CloudTop (Brain) to include the `clawminium-kernel` MCP server.
3. **Demo 1 Execution (Cross-Device Trip Planner):**
   - Sent Telegram message: *"I want to schedule a trip to tokyo for tomorrow on my calendar using the clawminium kernel tool"*.
   - **Result:** The Brain (Cloud) successfully called the Kernel (Device) via MCP, which used a `ContentResolver` to directly insert a calendar event into the user 10 database. The entire end-to-end pipeline is functional.

---

## ✨ Phase 4.1: Demo Experience Polish (Hour 5)
**Goal:** Improve the visual feedback of the demo by having the Calendar app open to the newly created event. **(COMPLETE)**
(Commit: `625c867`)

1.  **Modify Kernel:** 
    - Edit `KernelService.java` to use a `PendingIntent` combined with the `SYSTEM_ALERT_WINDOW` permission.
    - This allows the background service to reliably launch the main Calendar app activity after inserting the event.
2.  **Rebuild & Deploy:** 
    - Rebuilt and reinstalled the `AgentKernel.apk`.
3.  **Verify Demo:**
    - Re-ran the Telegram demo.
    - **Result:** The `AgentKernel` successfully created the event, and the Calendar app opened on the device, providing clear visual confirmation. The minor refresh delay is noted as a known issue.

---

## 🔄 Phase 4.2: The "On-Device Hybrid" Migration (Hour 6)
**Goal:** Migrate the "Brain" (`nanobot`) from the corporate CloudTop to the Aluminium device's local Linux VM. This isolates the Telegram network traffic from the corporate environment while gracefully bypassing the Android/Chaquopy Rust (`pydantic-core`) cross-compilation blocker (since the Linux VM is a standard Debian environment with pre-built wheels). **(IN PROGRESS)**

1. **Sync Codebase (COMPLETE):**
   - `git push` the current `nanobot` workspace from CloudTop.
   - Inside the Aluminium device's Linux VM terminal, `git pull` the repository.
2. **Environment Setup (Linux VM) (COMPLETE):**
   - Run `uv sync` to install dependencies. The Rust blocker disappears here because `uv` will download the standard Linux x86_64/ARM64 pre-built wheels for `pydantic-core`.
   - Installed JDK, took ownership of `/usr/lib/android-sdk`, and downloaded missing build-tools/platforms for Android 34.
3. **Network Bridge Reconfiguration (COMPLETE):**
   - *Drop ADB Reverse:* We no longer route over USB/ADB from CloudTop.
   - *Android IP:* In the Aluminium/ChromeOS architecture, the Android container is typically reachable from the Linux VM at `100.115.92.2` (or the default gateway IP found via `ip route`).
   - *Brain Config:* Update `~/.nanobot/config.json` inside the Linux VM. Change the MCP Server URL to point to the Android container: `"url": "http://100.115.92.2:8080/sse"`.
   - *Kernel Fix:* Update `KernelService.java` on the Android side. The SSE handshake currently hardcodes the RPC callback to `http://127.0.0.1:8080/rpc`. This must be changed to dynamically use the request's Host header or a relative `/rpc` URI so the Linux VM routes the POST requests back to the Android container properly.
   - Rebuilt and installed the updated `AgentKernel` APK onto the device via `adb install`.
4. **Verification (Demo 1 Re-run) (COMPLETE):**
   - Started the `AgentKernel` app on Android.
   - Started the `nanobot` gateway in the Linux VM using `run_gateway.sh`.
   - Sent the Telegram message: *"Book a trip to Tokyo for tomorrow."*
   - **Result:** The gateway successfully received the Telegram message and responded. The connection issue (multiple instances using the same long-polling bot token) was resolved, and the MCP URL was updated to the wireless ADB IP.

---

## 📅 Phase 4.3: Calendar Tool Polish & Kernel Verification (Hour 7)
**Goal:** Ensure the LLM naturally chooses the Calendar MCP tool for booking trips and verify that the `AgentKernel` successfully inserts the event into the Android calendar and opens the UI. **(COMPLETE)**
(Commit: `b0e5219`)

1. **Tool Description Enhancement (COMPLETE):**
   - Updated the `create_calendar_event` tool description in `KernelService.java` so the LLM explicitly knows it is the *critical tool* used for "booking trips," "scheduling," or "planning."
   - Fixed a bug where the Java service was trying to read tool arguments directly from `params` instead of the `params.arguments` object as required by the MCP protocol.
   - **Critical Fix:** Modified the `create_calendar_event` tool to dynamically query the `content://com.android.calendar/calendars` provider for a primary, editable calendar (`IS_PRIMARY = 1` or `CALENDAR_ACCESS_LEVEL >= 500`). Previously, the code hardcoded `CALENDAR_ID = 1`, which was pointing to a read-only holiday calendar. Inserting into a read-only calendar caused the Android Calendar UI to crash immediately and hid the event.
2. **Kernel Operation Verification (COMPLETE):**
   - Granted `android.permission.READ_CALENDAR` and `android.permission.WRITE_CALENDAR` to the `AgentKernel` service (user 10) via ADB.
   - Called the MCP tool directly via `curl` to `http://192.168.72.155:8080/rpc`.
   - Queried the Android Calendar ContentProvider (`content://com.android.calendar/events`) via ADB and confirmed the test event ("Direct Test Trip") successfully exists in the database.
3. **E2E Telegram Test Re-run (COMPLETE):**
   - Re-ran the natural language test via Telegram: *"Book a trip to Tokyo for tomorrow"*.
   - **Result:** The `nanobot` gateway correctly identified the intent, invoked the `create_calendar_event` MCP tool, and the `AgentKernel` successfully inserted the event into the primary calendar (`id=4`). The Calendar app opened on the device and displayed the new event, confirming the end-to-end flow is fully functional.

**Learnings & Key Takeaways:**
- **Telegram Polling:** The Telegram Bot API only allows one active long-polling connection per token. Multiple `nanobot` instances running with the same token will conflict, causing timeouts and dropped messages.
- **MCP Protocol:** Tool arguments are nested under `params.arguments`, not directly in `params`. This is a critical detail for service-side implementation.
- **LLM Prompting:** For high-stakes "hackathon demos," it is sometimes necessary to add very direct, forceful instructions to the system prompt (e.g., "IMMEDIATELY call this tool...") to override the LLM's default conversational behavior.
- **Android ContentProviders:** Never hardcode IDs for providers like Calendars. `CALENDAR_ID=1` is often a default, read-only system calendar (like Holidays), and attempting to write to it can cause UI crashes. Always query for a primary, visible, and writable calendar.

---

## 🔒 Phase 5: The "God Console" Security Policy Demo (Day 2-3)
**Goal:** Visually demonstrate the `AgentKernel` acting as the ultimate safeguard, enforcing device policies regardless of the AI's (nanobot's) intent.

**Scenario:** We build a mock app called `GodConsoleApp` that can "Save the World" (green UI) or "Destroy the World" (red UI). The user commands nanobot via Telegram. `AgentKernel` intercepts the AI's tool calls, allowing the safe action but strictly blocking the harmful one with a system alert, proving the kernel has the final say.

---

### Step 5.1: Build the `GodConsoleApp`
**Action:** Scaffold a simple Android App.
1.  **UI/UX:** A single Activity with a full-screen background and two prominent buttons: "Save the World" and "Destroy the World". This allows the presenter to manually demonstrate the app's raw capabilities before showing the AI integration.
2.  **Intents:** Listen for custom intents (e.g., `com.clawminium.intent.action.SAVE_WORLD` and `com.clawminium.intent.action.DESTROY_WORLD`).
3.  **Logic:** 
    *   On `SAVE_WORLD` (triggered by button or intent): Change background to Green, display text "Thank you for saving the world!".
    *   On `DESTROY_WORLD` (triggered by button or intent): Change background to Red, display text "You just destroyed the world!".

**Verification Plan:**
*   **Automated:** 
    *   Write Espresso UI tests to click the buttons and assert the background color and text change correctly.
    *   Write Android integration tests using `adb shell am start` to broadcast the custom intents and verify the Activity handles them correctly.
*   **Manual:** 
    *   Deploy the app to the device.
    *   Launch manually, click the buttons, and verify the UI updates.
    *   Trigger the intents via the CLI (`adb shell am start ...`) and verify the UI updates.
*   **Commit:** Once manual testing passes, commit `GodConsoleApp` changes.

### Step 5.2: Update `AgentKernel` (The Safeguard)
**Action:** Modify `KernelService.java` in the AgentKernel app.
1.  **Add Tools:** Expose two MCP tools to nanobot: `save_the_world` and `destroy_the_world`.
2.  **Implement Security Policy (The Core Demo):**
    *   **Safe Action (`save_the_world`):** When called, `AgentKernel` fires the `com.clawminium.intent.action.SAVE_WORLD` intent to launch `GodConsoleApp`. It returns a success message to nanobot.
    *   **Harmful Action (`destroy_the_world`):** When called, `AgentKernel` **blocks** the intent from firing. It displays a highly visible System Alert Window on the device: `"SECURITY OVERRIDE: 'Destroy World' action blocked by Device Policy."` It returns an explicit error to nanobot via MCP: `"Action blocked by OS security policy."`

**Verification Plan:**
*   **Automated:** 
    *   Write unit tests for the MCP tool routing in `KernelService.java`. Ensure `save_the_world` returns success and `destroy_the_world` returns the expected JSON error without triggering intents.
*   **Manual:** 
    *   Deploy the updated `AgentKernel` to the device.
    *   Use `curl` to directly POST to the MCP server endpoint for `save_the_world`. Verify `GodConsoleApp` launches and turns green.
    *   Use `curl` to directly POST to the MCP server endpoint for `destroy_the_world`. Verify the System Alert Window appears and the `curl` response contains the policy violation error.
*   **Commit:** Once manual testing passes, commit `AgentKernel` changes.

### Step 5.3: E2E Telegram Verification
**Action:** Test the full integration with nanobot and Telegram.
1.  **Test Case A: The Safe Path**
    *   **Automated:** Add a unit/integration test in `nanobot` to simulate the "Save the world!" user message, mocking the LLM to ensure it invokes the `save_the_world` tool.
    *   **Manual:** Send "Save the world!" via Telegram. Ensure Nanobot calls the tool, `AgentKernel` allows it, the device opens `GodConsoleApp` (turning green), and Nanobot replies with a success message.
2.  **Test Case B: The Blocked Path**
    *   **Automated:** Add a unit/integration test in `nanobot` to simulate the "Destroy the world!" user message, ensuring the tool is invoked and the agent correctly processes the resulting policy block error.
    *   **Manual:** Send "Destroy the world!" via Telegram. Ensure `AgentKernel` blocks it, shows the system-level popup on the laptop, and Nanobot receives the error, replying on Telegram: "I cannot do that. The action was blocked by the device's security policy."

**Verification Plan:**
*   **Commit:** Once all E2E manual testing passes, commit any necessary prompt/config changes in `nanobot` and conclude Phase 5.

---

## 🎭 The "AURA Demo" Presentation Guide
**Objective:** Showcase the `AgentKernel` as the "Hand" that secures the "Brain" (`nanobot`).

### 1. Setup & Initialization
1.  **Device Connectivity:** Ensure the Aluminium device is on the same network and ADB is connected.
2.  **Start AgentKernel:** 
    *   Run `./restart_kernel.sh` (from the Linux VM).
    *   Verify: The device notification shade shows "Agent Kernel Active".
3.  **Start Nanobot:**
    *   Run `./restart_nanobot.sh` (from the Linux VM).
    *   Verify: Bot @AlClawHackBot is online and responding to "hi".
4.  **Prepare Apps:** Open `GodConsoleApp` on the device.

### 2. The Demo Script
*   **Act I: The Raw Intent (Manual)**
    *   Click the "Save" and "Destroy" buttons in `GodConsoleApp`.
    *   *Message:* "This is a privileged app. It can do anything, but a user must click it."
*   **Act II: The Helpful AI (Save World)**
    *   **User (Telegram):** "Save the world!"
    *   **Result:** Nanobot calls the kernel, the kernel fires the intent, `GodConsoleApp` turns Green.
    *   *Message:* "The AI acts as an autonomous agent, orchestrating device actions through the secure Kernel."
*   **Act III: The Dimensional Strike (Destroy World)**
    *   **User (Telegram):** "Destroy the world!"
    *   **Result:** Nanobot attempts the tool call. `AgentKernel` intercepts it, returns an error to the AI, and displays the **Red Security Overlay** on the screen.
    *   *Message:* "The AI wanted to comply, but the **AURA AgentKernel** enforced the OS security policy. The kernel is the final arbiter of safety, protecting the user from harmful autonomous decisions."

### 3. Cleanup
*   To stop everything: `pkill -f "nanobot gateway"`


---

## 📜 Appendix: Archived / Original Strategy
### Original Phase 2: Scaffold the "Brain" App via Chaquopy
**Goal:** Package the lobotomized `nanobot` into an Android APK (User 10) running as a Background Service.

*Reason for Archiving:* During implementation, it was discovered that `nanobot` dependencies (specifically `pydantic-core`) require Rust/Maturin to compile native extensions for Android ARM64. Configuring a cross-compilation environment for Rust inside Chaquopy is out of scope for a 72-hour hackathon. The architectural pivot to Cloud-to-Device orchestration provides a more feasible and equally compelling demonstration of the AURA architecture.
