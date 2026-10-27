package com.gxmm.motoai;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
    private TextView status;
    private Button toggleButton;
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean listening = false;
    private boolean readyToListen = false;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        tts = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("hi", "IN"));
                readyToListen = true;
                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    greetAndListen();
                } else {
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MIC_PERMISSION);
                }
            } else {
                status.setText("Hindi voice engine unavailable. Check Google Speech Services.");
            }
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
            } else {
                startListening();
            }
        });
        layout.addView(toggleButton);

        TextView help = new TextView(this);
        help.setText("Try: hello, time, stop listening.\nKeep this app open while using voice commands.");
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
            @Override public void onError(int error) {
                if (listening) handler.postDelayed(() -> startListening(), 700);
            }
            @Override public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) handleCommand(matches.get(0));
                else if (listening) handler.postDelayed(() -> startListening(), 500);
            }
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void startListening() {
        if (!readyToListen || !listening && tts == null) return;
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
        String command = raw.toLowerCase(Locale.ROOT);
        status.setText("You said: " + raw);
        if (command.contains("समय") || command.contains("टाइम") || command.contains("time")) {
            String time = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
            say("Abhi " + time + " hai.");
        } else if (command.contains("नमस्ते") || command.contains("हेलो") || command.contains("hello") || command.contains("namaste")) {
            say("Namaste! Main aapki kya madad kar sakti hoon?");
        } else if (command.contains("बंद") || command.contains("रुको") || command.contains("stop listening")) {
            stopListening();
            say("Theek hai, listening band kar di.");
            return;
        } else {
            say("Maine suna: " + raw + ". Abhi main kuch basic commands hi kar sakti hoon.");
        }
        if (listening) handler.postDelayed(this::startListening, 1500);
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
