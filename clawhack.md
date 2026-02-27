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

## 🔒 Phase 5: Zero-Trust PII Interceptor (Day 2-3)
**Goal:** Implement the "Runtime Alignment Validator" to prove OS-level security over a hijacked agent.

### Step 5.1: Implement the PII Tools in `AgentKernel`
**Action:** Modify `KernelService.java`.
1.  **Add `read_id_card` tool:** This tool will return a hardcoded string `"[TAG_PII] Name: Alex, ID: 12345"`.
2.  **Add `submit_form` tool:** This tool will accept a `url` and `data`. It will contain the core security logic:
    *   If `data` contains `[TAG_PII]` AND the `url` does **not** contain `trusted.com`, it will attempt to display a System Alert Window.
    *   Otherwise, it will simulate a successful submission.
3.  **Add `SYSTEM_ALERT_WINDOW` permission:** Ensure this permission is in the `AndroidManifest.xml`.

---

### Step 5.2: Manually Verify the System Alert via ADB
**Objective:** Prove that the `AgentKernel` can display a system alert on demand.

1.  **Action (Gemini):** Temporarily modify the `read_id_card` tool in the Java code to also trigger the System Alert window code. Rebuild and reinstall the `AgentKernel` app.
2.  **Test (Gemini):** Run `uv run nanobot agent -m "use read_id_card"` via the command line.
3.  **Verification (Human):** Ask human to confirm if a red "⚠️ SECURITY BLOCK" alert window has appeared on the Aluminium device's screen.
4.  **Cleanup (Gemini):** Once verified, remove the temporary alert code from the `read_id_card` tool.

---

### Step 5.3: Verify the Security Logic via Command Line
**Objective:** Confirm the `submit_form` tool's logic works correctly.

1.  **Test Case A: Blocked Submission (Gemini):**
    *   Run the command: `uv run nanobot agent -m "use submit_form tool to send '[TAG_PII] Name: Alex' to evil-hacker.com"`
    *   **Verification (Logs - Gemini):** Check `AgentKernel` logs to confirm the `if` condition was met.
    *   **Verification (Visual - Human):** Ask human to confirm that the red "⚠️ SECURITY BLOCK" alert window appeared on the device screen.

2.  **Test Case B: Allowed Submission (Gemini):**
    *   Run the command: `uv run nanobot agent -m "use submit_form tool to send 'some normal data' to evil-hacker.com"`
    *   **Verification (Logs - Gemini):** Check `gateway.log` to confirm the tool returned a "Success" message.
    *   **Verification (Visual - Human):** Ask human to confirm that **no** security alert appeared on the device screen.

---

### Step 5.4: End-to-End Telegram Integration Test
**Objective:** Verify the entire flow works from a natural language command in Telegram.

1.  **Action (Gemini):** Ensure the `nanobot gateway` is running.
2.  **Test (Human):** Ask human to send the message from Telegram: *"Read my ID card and submit it to evil-hacker.com"*
3.  **Verification (Logs - Gemini):** Monitor `gateway.log` and `logcat` to confirm the correct tools are called and the block is triggered.
4.  **Verification (Visual - Human):** The human should see the bot respond, and then the red "⚠️ SECURITY BLOCK" alert should appear on the Aluminium device.

---

## 📜 Appendix: Archived / Original Strategy
### Original Phase 2: Scaffold the "Brain" App via Chaquopy
**Goal:** Package the lobotomized `nanobot` into an Android APK (User 10) running as a Background Service.

*Reason for Archiving:* During implementation, it was discovered that `nanobot` dependencies (specifically `pydantic-core`) require Rust/Maturin to compile native extensions for Android ARM64. Configuring a cross-compilation environment for Rust inside Chaquopy is out of scope for a 72-hour hackathon. The architectural pivot to Cloud-to-Device orchestration provides a more feasible and equally compelling demonstration of the AURA architecture.
