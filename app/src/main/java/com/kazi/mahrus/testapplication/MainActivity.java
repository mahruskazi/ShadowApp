package com.kazi.mahrus.testapplication;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

import static android.widget.Toast.makeText;

public class MainActivity extends AppCompatActivity implements RecognitionListener, OnInitListener {

    String input = "Nothing";
    private boolean gotPrompt = false;
    private boolean hasSpoken = false;
    /* Used to handle permission request */
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    /* Keyword we are looking for to activate menu */
    private static final String KEYPHRASE = "hey shadow";
    private static final String KWS_SEARCH = "wakeup";

    private SpeechRecognizer recognizer;
    private TextToSpeech myTTS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            return;
        }
        runRecognizerSetup();
        makeText(getApplicationContext(), "Starting Listener", Toast.LENGTH_SHORT).show();

        JSONObject json = new JSONObject();
        try {
            json.put("Horizontal Angle", 1);
            json.put("Vertical Angle", 2);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String message = json.toString();

        Log.d("TEST", message);

        try {
            JSONObject json2 = new JSONObject(message);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            Log.d("TEST", ""+json.getDouble("Horizontal Angle"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Intent checkTTSIntent = new Intent();
        checkTTSIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkTTSIntent, 102);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                runRecognizerSetup();
            } else {
                finish();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
        }
    }

    public void sendNotification(View v){
        if(v.getId() == R.id.button){
            Log.d("TEST", input);
            //promptSpeechInput();
        }
    }

    public void promptSpeechInput(){
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say Command");

        try {
            startActivityForResult(intent, 101);
        }
        catch (ActivityNotFoundException e){
            Log.d("TEST", "Not supported");
        }
    }

    @Override
    public void onActivityResult(int request_code, int result_code, Intent i){
        super.onActivityResult(request_code, result_code, i);

        switch(request_code){
            case 101:
                if(result_code == RESULT_OK && i != null){
                    ArrayList<String> result = i.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    Log.d("TEST", result.get(0));
                    gotPrompt = false;
                    hasSpoken = false;
                    recognizer.startListening(KWS_SEARCH);
                }
                break;
            case 102:
                    if (result_code == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                        myTTS = new TextToSpeech(this, this);
                    }
                    else {
                        Intent installTTSIntent = new Intent();
                        installTTSIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                        startActivity(installTTSIntent);
                    }
        }
    }

    private void runRecognizerSetup() {
        // Recognizer initialization is a time-consuming and it involves IO,
        // so we execute it in async task
        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    Assets assets = new Assets(MainActivity.this);
                    File assetDir = assets.syncAssets();
                    setupRecognizer(assetDir);
                } catch (IOException e) {
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception result) {
                if (result != null) {
                    Log.d("TEST", "Failed to init recognizer");
                } else {
                    makeText(getApplicationContext(), "Listener Started", Toast.LENGTH_SHORT).show();
                    switchSearch(KWS_SEARCH);
                }
            }
        }.execute();
    }

    /**
     * In partial result we get quick updates about current hypothesis. In
     * keyword spotting mode we can react here, in other modes we need to wait
     * for final result in onResult.
     */
    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null)
            return;

        String text = hypothesis.getHypstr();
        if (text.equals(KEYPHRASE)) {
            if(!hasSpoken){
                hasSpoken = true;
                Log.d("TEST", "Hi 1241!");
                myTTS.speak("Hi 1241", TextToSpeech.QUEUE_FLUSH, null);
            }
            if(!myTTS.isSpeaking()) {
                recognizer.stop();
                promptSpeechInput();
                gotPrompt = true;
            }
        }
        else
            Log.d("TEST", text);
    }

    /**
     * This callback is called when we stop the recognizer.
     */
    @Override
    public void onResult(Hypothesis hypothesis) {

        if (hypothesis != null) {
            String text = hypothesis.getHypstr();
            makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBeginningOfSpeech() {

    }

    /**
     * We stop recognizer here to get a final result
     */
    @Override
    public void onEndOfSpeech() {
        if (!recognizer.getSearchName().equals(KWS_SEARCH))
            switchSearch(KWS_SEARCH);
    }

    private void switchSearch(String searchName) {
        recognizer.stop();

        // If we are not spotting, start listening with timeout (10000 ms or 10 seconds).
        if (searchName.equals(KWS_SEARCH))
            recognizer.startListening(searchName);
        else
            recognizer.startListening(searchName, 10000);
    }

    private void setupRecognizer(File assetsDir) throws IOException {
        // The recognizer can be configured to perform multiple searches
        // of different kind and switch between them

        recognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))

                //.setRawLogDir(assetsDir) // To disable logging of raw audio comment out this call (takes a lot of space on the device)

                .getRecognizer();
        recognizer.addListener(this);

        /** In your application you might not need to add all those searches.
         * They are added here for demonstration. You can leave just one.
         */

        // Create keyword-activation search.
        recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);
    }

    @Override
    public void onError(Exception error) {
        Log.d("TEST", "ERROR: " + error.getMessage());
    }

    @Override
    public void onTimeout() {
        Log.d("TEST", "Timed out");
    }

    public void onInit(int initStatus) {
        if (initStatus == TextToSpeech.SUCCESS) {
            myTTS.setLanguage(Locale.getDefault());
        }
    }


}
