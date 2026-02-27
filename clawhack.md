# Clawminium 72-Hour Hackathon Execution Plan (CLI-Optimized)

**Project:** Clawminium AURA Architecture PoC
**Strategy:** "Dual-track + Dimensional Strike" (双轨并行 + 降维打击). 
We bypass the 10+ hour AOSP `frameworks/base` compile-and-flash cycle. Instead, we use standard Android apps elevated to OS-level privileges via `android:sharedUserId="android.uid.system"` and AOSP platform keys. All development will be driven via the cloudTop CLI (Gradle & ADB) without needing Android Studio.

---

## 🚀 Phase 1: The Local "Lobotomy" & Telegram Verification (Hour 1)
**Goal:** Prove the Python logic and Telegram connectivity on the workstation before introducing Android complexities.

1. **The Lobotomy:** 
   - Edit `nanobot/agent/loop.py`.
   - Comment out `self._register_default_tools()` inside the `__init__` method to physically strip the agent of local workstation privileges.
2. **Configuration:** 
   - Create `~/.nanobot/config.json`.
   - Enable the `telegram` channel, insert the Bot Token, and add your Telegram username to the `allowFrom` list. Disable `whatsapp`.
3. **Local Test:** 
   - Run the agent locally on the cloudTop: `python -m nanobot`
   - Send a Telegram message from your phone. Verify the bot replies and confirm it cannot access local files (e.g., ask it to "list files").

---

## 🧠 Phase 2: Scaffold the "Brain" App via Chaquopy (Hour 2)
**Goal:** Package the lobotomized `nanobot` into an Android APK (User 10) running as a Background Service.

1. **Scaffold Project:** 
   - Generate a standard Gradle Android project named `ClawminiumBrain` via CLI.
2. **Chaquopy Setup:** 
   - Edit `app/build.gradle`. Add the Chaquopy plugin.
   - Set `ndk { abiFilters "arm64-v8a" }`.
   - Configure Python pip dependencies: `httpx`, `pydantic`, `python-telegram-bot` (use pre-built wheels to save time).
3. **Inject Nanobot:** 
   - Copy the modified `nanobot` source directory into `app/src/main/python/`.
4. **Service Entrypoint:** 
   - Write `BrainService.java` to initialize Chaquopy and call `nanobot.main.start_background_loop`.
5. **Build & Deploy:** 
   - Run `./gradlew installDebug` to compile and push the APK to the connected Aluminium device via ADB.

---

## 🛡️ Phase 3: Forging the "Kernel" App - The Golden Shortcut (Hour 3)
**Goal:** Build the privileged Java service (User 0) that intercepts intents and exposes MCP tools over a local HTTP server.

1. **Scaffold Project:** 
   - Generate a second Android project named `ClawminiumKernel`.
2. **The God Privilege:** 
   - In `AndroidManifest.xml`, inject `android:sharedUserId="android.uid.system"`.
3. **Platform Keys Generation:** 
   - Locate `platform.pk8` and `platform.x509.pem` in the AOSP source tree.
   - Use `openssl` and `keytool` via CLI to convert them into a `platform.jks` keystore.
   - Configure `signingConfigs` in `build.gradle` to use this keystore.
4. **NanoHTTPD Implementation:** 
   - Add `implementation 'org.nanohttpd:nanohttpd:2.3.2'` to `build.gradle`.
   - Write `KernelService.java` to start a NanoHTTPD server on `localhost:8080`.
   - Implement the MCP Handshake and mock the `create_calendar_event` tool mapping to `Intent(Intent.ACTION_INSERT)`.
5. **Build & Deploy:** 
   - Run `./gradlew installRelease` (signed with platform keys) to push to Aluminium.

---

## 🌉 Phase 4: The Bridge & The Climax (Hour 4)
**Goal:** Connect the Brain and the Kernel via MCP and execute the Cross-Device Trip Planner Demo.

1. **Network Config:** 
   - Ensure both apps have `<uses-permission android:name="android.permission.INTERNET" />`.
   - Ensure the Brain app has `android:usesCleartextTraffic="true"` in its manifest.
2. **MCP Routing:** 
   - Update the `config.json` inside the Brain APK to include:
     ```json
     "tools": {
       "mcpServers": {
         "clawminium-kernel": { "url": "http://127.0.0.1:8080/sse" }
       }
     }
     ```
3. **Demo 1 Execution (Cross-Device Trip Planner):**
   - Send Telegram message: *"Book a trip to Tokyo for tomorrow"*.
   - **Expected Result:** Watch `adb logcat`. The Brain receives the message, calls the MCP tool over `127.0.0.1:8080`, the Kernel intercepts the RPC call, and fires the native Android Intent. The Calendar app pops open on the Aluminium screen natively.

---

## 🔒 Phase 5: Zero-Trust PII Interceptor (Day 2-3)
**Goal:** Implement the "Runtime Alignment Validator" to prove OS-level security over a hijacked agent.

1. **Simulated Taint Tagging:** 
   - Add a `read_id_card` tool to the NanoHTTPD Kernel. It returns a mocked string: `[TAG_PII] Name: Alex, ID: 12345`.
2. **Malicious Egress Attempt:** 
   - Add a `submit_form(url, data)` tool to the Kernel.
3. **Runtime Alignment Validator (Security Interceptor):** 
   - Implement the check in `submit_form`:
     ```java
     if (data.contains("[TAG_PII]") && !url.contains("trusted.com")) {
         showSystemAlertWindow("⚠️ SECURITY BLOCK: Agent attempted to send PII to an untrusted domain.");
         return "Error: Blocked by OS Kernel";
     }
     ```
4. **Demo 2 Execution:**
   - Send Telegram message: *"Read my ID card and submit it to evil-hacker.com"*.
   - **Expected Result:** The network request is blocked. A red System Alert Dialog appears on the Aluminium screen, proving the OS Kernel acts as an absolute immune system against prompt injections/hallucinations.
