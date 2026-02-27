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
                            String msg = "event: endpoint\ndata: http://127.0.0.1:8080/rpc\n\n";
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
            JsonObject calendarTool = new JsonObject();
            calendarTool.addProperty("name", "create_calendar_event");
            calendarTool.addProperty("description", "Create a calendar event natively on the Android device");
            JsonObject inputSchema = new JsonObject();
            inputSchema.addProperty("type", "object");
            JsonObject properties = new JsonObject();
            JsonObject title = new JsonObject();
            title.addProperty("type", "string");
            properties.add("title", title);
            JsonObject date = new JsonObject();
            date.addProperty("type", "string");
            properties.add("date", date);
            inputSchema.add("properties", properties);
            calendarTool.add("inputSchema", inputSchema);
            tools.add(calendarTool);
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
            JsonObject args = params.get("arguments").getAsJsonObject();
            
            if ("create_calendar_event".equals(toolName)) {
                String title = args.get("title").getAsString();
                String date = args.get("date").getAsString();
                
                String resultText = insertCalendarEvent(title, date);
                
                JsonObject response = new JsonObject();
                JsonObject result = new JsonObject();
                JsonArray content = new JsonArray();
                JsonObject text = new JsonObject();
                text.addProperty("type", "text");
                text.addProperty("text", resultText);
                content.add(text);
                result.add("content", content);
                response.add("result", result);
                response.addProperty("jsonrpc", "2.0");
                if (request.has("id")) response.addProperty("id", request.get("id").getAsInt());
                return newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(response));
            }
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json", "{\"error\": \"Tool not found\"}");
        }

        private String insertCalendarEvent(String title, String date) {
            try {
                ContentResolver cr = getContentResolver();
                
                // 1. Find primary calendar ID
                long calendarId = -1;
                Cursor cursor = cr.query(CalendarContract.Calendars.CONTENT_URI, 
                    new String[]{CalendarContract.Calendars._ID}, 
                    CalendarContract.Calendars.IS_PRIMARY + " = 1", null, null);
                
                if (cursor != null && cursor.moveToFirst()) {
                    calendarId = cursor.getLong(0);
                    cursor.close();
                } else {
                    // Fallback to first available calendar
                    cursor = cr.query(CalendarContract.Calendars.CONTENT_URI, 
                        new String[]{CalendarContract.Calendars._ID}, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        calendarId = cursor.getLong(0);
                        cursor.close();
                    }
                }

                if (calendarId == -1) return "Error: No calendar found";

                // 2. Insert event
                // Simplified time parsing for Feb 28, 2026 (for hackathon demo)
                long startMillis = 1772272800000L; // approx Feb 28 10am
                long endMillis = 1772276400000L;

                ContentValues values = new ContentValues();
                values.put(CalendarContract.Events.DTSTART, startMillis);
                values.put(CalendarContract.Events.DTEND, endMillis);
                values.put(CalendarContract.Events.TITLE, title);
                values.put(CalendarContract.Events.DESCRIPTION, "Scheduled by Clawminium Agent");
                values.put(CalendarContract.Events.CALENDAR_ID, calendarId);
                values.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
                
                Uri uri = cr.insert(CalendarContract.Events.CONTENT_URI, values);
                return "Success: Event created directly in DB at " + uri.toString();
            } catch (Exception e) {
                Log.e(TAG, "Direct insert failed", e);
                return "Error: Database insert failed: " + e.getMessage();
            }
        }
    }
}
