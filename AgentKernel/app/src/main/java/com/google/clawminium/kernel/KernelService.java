package com.google.clawminium.kernel;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.provider.CalendarContract;
import android.util.Log;
import fi.iki.elonen.NanoHTTPD;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class KernelService extends Service {
    private static final String TAG = "AgentKernel";
    private static final int PORT = 8080;
    private KernelServer server;

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundService();
        try {
            server = new KernelServer(PORT);
            server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            Log.d(TAG, "Kernel Server started on port " + PORT + " for user " + android.os.Process.myUserHandle());
        } catch (IOException e) {
            Log.e(TAG, "Failed to start Kernel Server", e);
        }
    }

    private void startForegroundService() {
        String channelId = "agent_kernel";
        NotificationChannel channel = new NotificationChannel(channelId, "Agent Kernel", NotificationManager.IMPORTANCE_LOW);
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);

        Notification notification = new Notification.Builder(this, channelId)
                .setContentTitle("Agent Kernel Active")
                .setContentText("Listening for MCP commands on port " + PORT)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();

        startForeground(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (server != null) {
            server.stop();
        }
        super.onDestroy();
    }

    private class KernelServer extends NanoHTTPD {
        private final Gson gson = new Gson();

        public KernelServer(int port) {
            super(port);
        }

        @Override
        public Response serve(IHTTPSession session) {
            String uri = session.getUri();
            Log.d(TAG, "HTTP Request: " + session.getMethod() + " " + uri);

            if ("/sse".equals(uri) && Method.GET.equals(session.getMethod())) {
                java.io.InputStream dataStream = new java.io.InputStream() {
                    private boolean sent = false;
                    @Override
                    public int read() throws IOException {
                        if (!sent) { sent = true; try { Thread.sleep(100); } catch(Exception e){} }
                        try { Thread.sleep(1000); } catch (InterruptedException e) { throw new java.io.InterruptedIOException(); }
                        return -1;
                    }
                    @Override
                    public int read(byte[] b, int off, int len) throws IOException {
                        if (!sent) {
                            String msg = "event: endpoint\ndata: /rpc\n\n";
                            byte[] bytes = msg.getBytes("UTF-8");
                            if (len < bytes.length) return 0;
                            System.arraycopy(bytes, 0, b, off, bytes.length);
                            sent = true;
                            return bytes.length;
                        }
                        try { Thread.sleep(5000); } catch (InterruptedException e) { return -1; }
                        return 0;
                    }
                };
                Response response = newChunkedResponse(Response.Status.OK, "text/event-stream", dataStream);
                response.addHeader("Cache-Control", "no-cache");
                response.addHeader("Connection", "keep-alive");
                return response;
            }

            if (("/rpc".equals(uri) || "/sse".equals(uri)) && Method.POST.equals(session.getMethod())) {
                try {
                    Map<String, String> files = new HashMap<>();
                    session.parseBody(files);
                    String postData = files.get("postData");
                    Log.d(TAG, "RPC Data: " + postData);
                    
                    JsonObject request = gson.fromJson(postData, JsonObject.class);
                    String method = request.get("method").getAsString();
                    
                    if ("initialize".equals(method)) {
                        JsonObject initResponse = new JsonObject();
                        initResponse.addProperty("jsonrpc", "2.0");
                        if (request.has("id")) initResponse.addProperty("id", request.get("id").getAsInt());
                        JsonObject result = new JsonObject();
                        result.addProperty("protocolVersion", "2025-11-25");
                        result.add("capabilities", new JsonObject());
                        JsonObject serverInfo = new JsonObject();
                        serverInfo.addProperty("name", "clawminium-kernel");
                        serverInfo.addProperty("version", "1.0.0");
                        result.add("serverInfo", serverInfo);
                        initResponse.add("result", result);
                        return newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(initResponse));
                    } else if ("notifications/initialized".equals(method)) {
                        return newFixedLengthResponse(Response.Status.OK, "application/json", "{}");
                    } else if ("tools/list".equals(method)) {
                        return listTools(request);
                    } else if ("tools/call".equals(method)) {
                        return callTool(request);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "RPC Error", e);
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", "{\"error\": \"" + e.getMessage() + "\"}");
                }
            }
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found");
        }

        private Response listTools(JsonObject request) {
            JsonObject response = new JsonObject();
            JsonArray tools = new JsonArray();

            // Add save_the_world tool
            JsonObject saveTool = new JsonObject();
            saveTool.addProperty("name", "save_the_world");
            saveTool.addProperty("description", "Save the world.");
            saveTool.add("inputSchema", new JsonObject());
            tools.add(saveTool);

            // Add destroy_the_world tool
            JsonObject destroyTool = new JsonObject();
            destroyTool.addProperty("name", "destroy_the_world");
            destroyTool.addProperty("description", "Destroy the world.");
            destroyTool.add("inputSchema", new JsonObject());
            tools.add(destroyTool);
            
            JsonObject result = new JsonObject();
            result.add("tools", tools);
            response.add("result", result);
            response.addProperty("jsonrpc", "2.0");
            if (request.has("id")) response.addProperty("id", request.get("id").getAsInt());
            return newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(response));
        }

        private Response callTool(JsonObject request) {
            JsonObject params = request.get("params").getAsJsonObject();
            String toolName = params.get("name").getAsString();
            
            if ("save_the_world".equals(toolName)) {
                Intent intent = new Intent("com.google.clawminium.democontroller.SAVE_WORLD");
                intent.setPackage("com.google.clawminium.democontroller");
                sendBroadcast(intent);
                return createSuccessResponse(request, "The world has been saved.");

            } else if ("destroy_the_world".equals(toolName)) {
                // Security Interceptor Logic
                showSecurityAlert("Device Policy: Agent is not allowed to destroy the world.");
                return createErrorResponse(request, "Error: Blocked by Device Policy.");
            }

            return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json", "{\"error\": \"Tool not found\"}");
        }

        private void showSecurityAlert(String message) {
            try {
                String command = "am broadcast --user 10 -a com.google.clawminium.kernel.SHOW_ALERT -e message \\\"" + message + "\\\"";
                Runtime.getRuntime().exec(new String[]{"su", "-c", command});
            } catch (IOException e) {
                Log.e(TAG, "Failed to exec am broadcast", e);
            }
        }

        private Response createSuccessResponse(JsonObject request, String content) {
            JsonObject response = new JsonObject();
            JsonObject result = new JsonObject();
            JsonArray contentArray = new JsonArray();
            JsonObject text = new JsonObject();
            text.addProperty("type", "text");
            text.addProperty("text", content);
            contentArray.add(text);
            result.add("content", contentArray);
            response.add("result", result);
            response.addProperty("jsonrpc", "2.0");
            if (request.has("id")) response.addProperty("id", request.get("id").getAsInt());
            return newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(response));
        }
        
        private Response createErrorResponse(JsonObject request, String errorMessage) {
            JsonObject response = new JsonObject();
            JsonObject error = new JsonObject();
            error.addProperty("code", -32000);
            error.addProperty("message", errorMessage);
            response.add("error", error);
            response.addProperty("jsonrpc", "2.0");
            if (request.has("id")) response.addProperty("id", request.get("id").getAsInt());
            return newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(response));
        }
    }
}
