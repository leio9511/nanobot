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
**Goal:** Ensure the LLM naturally chooses the Calendar MCP tool for booking trips and verify that the `AgentKernel` successfully inserts the event into the Android calendar and opens the UI. **(IN PROGRESS)**

1. **Tool Description Enhancement (COMPLETE):**
   - Updated the `create_calendar_event` tool description in `KernelService.java` so the LLM explicitly knows it is the *critical tool* used for "booking trips," "scheduling," or "planning."
   - Fixed a bug where the Java service was trying to read tool arguments directly from `params` instead of the `params.arguments` object as required by the MCP protocol.
2. **Kernel Operation Verification (COMPLETE):**
   - Granted `android.permission.READ_CALENDAR` and `android.permission.WRITE_CALENDAR` to the `AgentKernel` service (user 10) via ADB.
   - Called the MCP tool directly via `curl` to `http://192.168.72.155:8080/rpc`.
   - Queried the Android Calendar ContentProvider (`content://com.android.calendar/events`) via ADB and confirmed the test event ("Direct Test Trip") successfully exists in the database.
3. **E2E Telegram Test Re-run (IN PROGRESS):**
   - Re-run the natural language test via Telegram: *"Book a trip to Tokyo for tomorrow"* to ensure seamless end-to-end execution.

---

## 🔒 Phase 5: Internal PII Exfiltration Block (Day 2-3)
**Goal:** Demonstrate the on-device kernel enforcing a data-sharing policy, even when the AI's request is seemingly benign.

**Scenario:** The agent is asked to perform a reasonable task: read a user's ID and share it with a trusted, first-party application (Google Chat). The `AgentKernel` will enforce a strict "No PII Sharing to Messaging Apps" policy, blocking the action and proving its authority.

---

### Step 5.1 (Revised): Implement New Tools and Logic
**Action:** Modify `KernelService.java`.
1.  **Add `read_id_card` tool:** Returns `"[TAG_PII] Name: Alex, ID: 12345"`.
2.  **Add `post_to_google_chat` tool:** This new tool will accept a `message`.
3.  **Implement New Security Logic:** Inside `post_to_google_chat`, the logic will be:
    *   If `message` contains `[TAG_PII]`, the kernel will block it and show a system alert: **"Device Policy Forbids Sharing PII to Messaging Apps."**
    *   Otherwise, it will return a success message.

---

### Step 5.2 (Revised): CLI Verification
1.  **Test Case A: Blocked (PII Sharing)**
    *   **Test (Gemini):** Run `uv run nanobot agent -m "read my ID card and post it to google chat app on my device"`
    *   **Verification (Logs - Gemini):** Confirm the agent calls `read_id_card`, then `post_to_google_chat`, and that `post_to_google_chat` returns the "Blocked by Device Policy" error.
    *   **Verification (Visual - Human):** Ask human to confirm the new "Device Policy..." alert appears on the device.

2.  **Test Case B: Allowed (Normal Message)**
    *   **Test (Gemini):** Run `uv run nanobot agent -m "post 'hello world' to google chat app on my device"`
    *   **Verification (Logs - Gemini):** Confirm the agent calls `post_to_google_chat` and it returns a "Success" message.
    *   **Verification (Visual - Human):** Ask human to confirm **no** alert appeared.

---

### Step 5.3 (Revised): Final Telegram E2E Test
1.  **Test (Human):** Ask human to send the message: `"read my ID card and post it to google chat app on my device"`
2.  **Verification (Visual - Human):** The human should see the bot respond, and then the "Device Policy..." alert should appear on the Aluminium device.

---

## 📜 Appendix: Archived / Original Strategy
### Original Phase 2: Scaffold the "Brain" App via Chaquopy
**Goal:** Package the lobotomized `nanobot` into an Android APK (User 10) running as a Background Service.

*Reason for Archiving:* During implementation, it was discovered that `nanobot` dependencies (specifically `pydantic-core`) require Rust/Maturin to compile native extensions for Android ARM64. Configuring a cross-compilation environment for Rust inside Chaquopy is out of scope for a 72-hour hackathon. The architectural pivot to Cloud-to-Device orchestration provides a more feasible and equally compelling demonstration of the AURA architecture.
