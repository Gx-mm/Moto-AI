package com.gxmm.motoai;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends Activity {
    private static final int MIC_PERMISSION = 42;
    private static final int CONTACT_PERMISSION = 43;
    private TextView status;
    private Button toggleButton;
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean listening = false;
    private boolean readyToListen = false;
    private String pendingContactAction = "";

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        tts = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("hi", "IN"));
                readyToListen = true;
                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) greetAndListen();
                else requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MIC_PERMISSION);
            } else status.setText("Hindi voice engine unavailable. Check Google Speech Services.");
        });
    }

    private void buildUi() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(32, 32, 32, 32);
        layout.setBackgroundColor(Color.rgb(15, 18, 28));
        TextView title = new TextView(this);
        title.setText("MOTO AI");
        title.setTextSize(32);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);
        status = new TextView(this);
        status.setText("Starting voice assistant...");
        status.setTextSize(17);
        status.setTextColor(Color.LTGRAY);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, 24, 0, 24);
        layout.addView(status);
        toggleButton = new Button(this);
        toggleButton.setText("STOP LISTENING");
        toggleButton.setOnClickListener(v -> {
            if (listening) {
                stopListening();
                say("Theek hai, main ruk rahi hoon.");
            } else startListening();
        });
        layout.addView(toggleButton);
        TextView help = new TextView(this);
        help.setText("Try: Hey Baby, time batao, WhatsApp kholo, call Ravi, message Ravi.\nKeep the app open while using voice commands.");
        help.setTextColor(Color.LTGRAY);
        help.setGravity(Gravity.CENTER);
        help.setPadding(0, 20, 0, 0);
        layout.addView(help);
        setContentView(layout);
    }

    private void greetAndListen() {
        say("Namaste! Main Moto AI hoon. Boliye.");
        handler.postDelayed(this::startListening, 1600);
    }

    private void createRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.setText("Speech recognition is not available on this phone.");
            return;
        }
        if (recognizer != null) recognizer.destroy();
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { status.setText("Sun rahi hoon... Boliye."); }
            @Override public void onBeginningOfSpeech() { status.setText("Awaaz sun rahi hoon..."); }
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() { status.setText("Samajh rahi hoon..."); }
            @Override public void onError(int error) { if (listening) handler.postDelayed(MainActivity.this::startListening, 700); }
            @Override public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) handleCommand(matches.get(0));
                else if (listening) handler.postDelayed(MainActivity.this::startListening, 500);
            }
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void startListening() {
        if (!readyToListen || tts == null) return;
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MIC_PERMISSION);
            return;
        }
        if (recognizer == null) createRecognizer();
        if (recognizer == null) return;
        listening = true;
        toggleButton.setText("STOP LISTENING");
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN");
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        try { recognizer.startListening(intent); }
        catch (Exception e) { status.setText("Could not start listening. Tap Start Listening."); }
    }

    private void handleCommand(String raw) {
        String command = raw.toLowerCase(Locale.ROOT).trim();
        status.setText("You said: " + raw);

        if (command.contains("hey baby") || command.contains("हे बेबी") || command.contains("हाय बेबी")) {
            say("Haan, main sun rahi hoon. Boliye.");
        } else if (command.contains("समय") || command.contains("टाइम") || command.contains("time")) {
            String time = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
            say("Abhi " + time + " hai.");
        } else if (command.contains("whatsapp") && (command.contains("खोल") || command.contains("open") || command.contains("launch"))) {
            openApp("com.whatsapp", "WhatsApp");
        } else if ((command.contains("youtube") || command.contains("यूट्यूब")) && (command.contains("खोल") || command.contains("open") || command.contains("launch"))) {
            openApp("com.google.android.youtube", "YouTube");
        } else if ((command.contains("chrome") || command.contains("क्रोम")) && (command.contains("खोल") || command.contains("open") || command.contains("launch"))) {
            openApp("com.android.chrome", "Chrome");
        } else if ((command.contains("settings") || command.contains("सेटिंग")) && (command.contains("खोल") || command.contains("open"))) {
            try { startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS)); say("Settings khol rahi hoon."); }
            catch (Exception e) { say("Settings nahi khul paayi."); }
        } else if (command.startsWith("call ") || command.startsWith("कॉल ") || command.contains("ko call") || command.contains("को कॉल")) {
            String name = extractName(raw, new String[]{"call ", "कॉल ", "ko call", "को कॉल"});
            if (!name.isEmpty()) findContactThen(name, "call");
            else say("Kiska call lagana hai? Naam boliye.");
        } else if (command.startsWith("message ") || command.startsWith("मैसेज ") || command.contains("ko message") || command.contains("को मैसेज")) {
            String name = extractName(raw, new String[]{"message ", "मैसेज ", "ko message", "को मैसेज"});
            if (!name.isEmpty()) findContactThen(name, "message");
            else say("Kisko message bhejna hai? Naam boliye.");
        } else if (command.contains("नमस्ते") || command.contains("हेलो") || command.contains("hello") || command.contains("namaste")) {
            say("Namaste! Main aapki kya madad kar sakti hoon?");
        } else if (command.contains("बंद") || command.contains("रुको") || command.contains("stop listening")) {
            stopListening();
            say("Theek hai, listening band kar di.");
            return;
        } else {
            say("Maine suna: " + raw + ". Is command ko abhi support nahi karti.");
        }
        if (listening) handler.postDelayed(this::startListening, 1600);
    }

    private String extractName(String raw, String[] markers) {
        String lower = raw.toLowerCase(Locale.ROOT);
        for (String marker : markers) {
            int i = lower.indexOf(marker.toLowerCase(Locale.ROOT));
            if (i >= 0) {
                String name = raw.substring(i + marker.length()).trim();
                name = name.replaceAll("(?i)\\b(call|message|ko call|ko message)\\b", "").trim();
                return name;
            }
        }
        return "";
    }

    private void openApp(String packageName, String label) {
        Intent launch = getPackageManager().getLaunchIntentForPackage(packageName);
        if (launch != null) {
            say(label + " khol rahi hoon.");
            startActivity(launch);
        } else say(label + " install nahi hai.");
    }

    private void findContactThen(String name, String action) {
        pendingContactAction = action + "|" + name;
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, CONTACT_PERMISSION);
            return;
        }
        performContactAction(name, action);
    }

    private void performContactAction(String name, String action) {
        String number = null;
        try (Cursor cursor = getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?",
                new String[]{"%" + name + "%"},
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC")) {
            if (cursor != null && cursor.moveToFirst()) number = cursor.getString(0);
        } catch (Exception ignored) {}
        if (number == null || number.trim().isEmpty()) {
            say(name + " naam ka contact nahi mila.");
            return;
        }
        Intent intent;
        if ("call".equals(action)) {
            intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null));
            say(name + " ka number dialer mein khol rahi hoon. Call karne ke liye tap karein.");
        } else {
            intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", number, null));
            say(name + " ke liye message likhne ki screen khol rahi hoon. Bhejne se pehle check kar lein.");
        }
        try { startActivity(intent); }
        catch (Exception e) { say("Yeh action abhi nahi ho paaya."); }
    }

    private void say(String message) {
        if (tts != null) tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "moto-ai-utterance");
    }

    private void stopListening() {
        listening = false;
        if (recognizer != null) recognizer.cancel();
        if (toggleButton != null) toggleButton.setText("START LISTENING");
        if (status != null) status.setText("Listening paused. Tap Start Listening to resume.");
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MIC_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            greetAndListen();
        } else if (requestCode == MIC_PERMISSION) {
            status.setText("Microphone permission is required for voice commands.");
        } else if (requestCode == CONTACT_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            String[] parts = pendingContactAction.split("\\|", 2);
            if (parts.length == 2) performContactAction(parts[1], parts[0]);
        } else if (requestCode == CONTACT_PERMISSION) {
            say("Contact permission nahi mili.");
        }
    }

    @Override protected void onDestroy() {
        listening = false;
        handler.removeCallbacksAndMessages(null);
        if (recognizer != null) recognizer.destroy();
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}
