/*
 * Copyright (C) 2012-2013 Reece H. Dunn
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.reecedunn.espeak;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.speech.tts.SynthesisCallback;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.foobnix.LibreraApp;
import com.txkj.readingapp.R;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class eSpeakActivity2 extends Activity {
    private static final String ACTION_TTS_SETTINGS = "com.android.settings.TTS_SETTINGS";

    /** Handler code for TTS initialization hand-off. */
    private static final int TTS_INITIALIZED = 1;

//    private static final int REQUEST_CHECK = 1;
//    private static final int REQUEST_DEFAULT = 3;

	private static final String TAG = "eSpeakActivity2";

    private enum State {
        LOADING,
        DOWNLOAD_FAILED,
        ERROR,
        SUCCESS
    }

    private State mState;
    private SpeechSynthesis/*TextToSpeech*/ mTts;
    private List<Pair<String,String>> mInformation;
    private InformationListAdapter mInformationView;
    private EditText mText;

    private final BroadcastReceiver mOnEspeakInitialized = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            populateInformationView();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        mInformation = new ArrayList<Pair<String,String>>();
        mInformationView = new InformationListAdapter(this, mInformation);
        ((ListView)findViewById(R.id.properties)).setAdapter(mInformationView);
        mText = (EditText)findViewById(R.id.editText1);

        setState(State.LOADING);
//        checkVoiceData();
        install();

        findViewById(R.id.speak).setOnClickListener(new View.OnClickListener() {
            @Override
            @SuppressWarnings("deprecation")
            public void onClick(View v) {
                if (mTts != null) {
                    mTts.synthesize(mText.getText().toString(), mText.getText().toString().startsWith("<speak"));
                }
            }
        });

        findViewById(R.id.ssml).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ssml =
                    "<?xml version=\"1.0\"?>\n" +
                    "<speak xmlns=\"http://www.w3.org/2001/10/synthesis\" version=\"1.0\">\n" +
                    "\n" +
                    "</speak>";
                mText.setText(ssml);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        final IntentFilter filter = new IntentFilter(TtsService.ESPEAK_INITIALIZED);
        registerReceiver(mOnEspeakInitialized, filter);
    }

    @Override
    public void onStop() {
        super.onStop();

        unregisterReceiver(mOnEspeakInitialized);

        if (mTts != null) {
            mTts.stop(); //.shutdown();
            mTts = null;
        }
        if (audioTrack != null) {
            audioTrack.pause();
            audioTrack.flush();
            audioTrack = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options, menu);
        if (Build.VERSION.SDK_INT < 14) {
            // Hide the eSpeak setting menu item on pre-ICS.
            menu.findItem(R.id.espeakSettings).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == R.id.espeakSettings) {
            startActivity(new Intent(eSpeakActivity2.this, TtsSettingsActivity.class));
            //REQUEST_DEFAULT
            return true;
        } else if (item.getItemId() == R.id.ttsSettings) {
            launchGeneralTtsSettings();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Sets the UI state.
     *
     * @param state The current state.
     */
    private void setState(State state) {
        mState = state;
        switch (mState)
        {
        case LOADING:
            findViewById(R.id.loading).setVisibility(View.VISIBLE);
            findViewById(R.id.success).setVisibility(View.GONE);
            break;
        default:
            findViewById(R.id.loading).setVisibility(View.GONE);
            findViewById(R.id.success).setVisibility(View.VISIBLE);
            break;
        }
    }

    /**
     * Launcher the voice data verifier.
     */
//    private void checkVoiceData() {
//        final Intent checkIntent = new Intent(this, CheckVoiceData.class);
//
//        startActivityForResult(checkIntent, REQUEST_CHECK);
//    }

    private final Map<String, Voice> mAvailableVoices = new HashMap<String, Voice>();
    protected Voice mMatchingVoice = null;
    /**
     * Initializes the TTS engine.
     */
    @SuppressLint("WrongConstant")
    private void initializeEngine() {
        //mTts = new TextToSpeech(this, mInitListener);
        Context storageContext = LibreraApp.getStorageContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            storageContext.moveSharedPreferencesFrom(this, this.getPackageName() + "_preferences");


        final SynthesisCallback mCallback = new SynthesisCallback() {
            @Override
            public int getMaxBufferSize() {
                return bufferSize;
            }

            @Override
            public int start(int sampleRateInHz, int audioFormat, int channelCount) {
                //https://github.com/palfrey/RetroArch/blob/fe88693c9015fbec949f5ad953bcedaae230f4cf/android/src/com/retroarch/audio_android.java#L126
                bufferSize = AudioTrack.getMinBufferSize(sampleRateInHz,
                        channelCount == 1 ? AudioFormat.CHANNEL_CONFIGURATION_MONO : AudioFormat.CHANNEL_CONFIGURATION_STEREO,
                        audioFormat);
                audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                        sampleRateInHz,
                        channelCount == 1 ? AudioFormat.CHANNEL_CONFIGURATION_MONO : AudioFormat.CHANNEL_CONFIGURATION_STEREO,
                        audioFormat,
                        bufferSize, AudioTrack.MODE_STREAM);
                audioTrack.play();
                return 0;
            }

            @Override
            public int audioAvailable(byte[] buffer, int offset, int length) {
                if (audioTrack != null) {
                    return audioTrack.write(buffer, offset, length);
                }
                return 0;
            }

            @Override
            public int done() {
                return 0;
            }

            @Override
            public void error() {

            }

            @Override
            public void error(int errorCode) {

            }

            @Override
            public boolean hasStarted() {
                return false;
            }

            @Override
            public boolean hasFinished() {
                return false;
            }
        };
        mTts = new SpeechSynthesis(storageContext, new SpeechSynthesis.SynthReadyCallback() {
            @Override
            public void onSynthDataReady(byte[] audioData) {
                if ((audioData == null) || (audioData.length == 0)) {
                    onSynthDataComplete();
                    return;
                }

                final int maxBytesToCopy = mCallback.getMaxBufferSize();

                int offset = 0;

                while (offset < audioData.length) {
                    final int bytesToWrite = Math.min(maxBytesToCopy, (audioData.length - offset));
                    mCallback.audioAvailable(audioData, offset, bytesToWrite);
                    offset += bytesToWrite;
                }
            }

            @Override
            public void onSynthDataComplete() {
                mCallback.done();
            }
        });
        mAvailableVoices.clear();
        List<Voice> voices = mTts.getAvailableVoices();
        for (Voice voice : voices) {
            if (mMatchingVoice == null && voice.name.equals("en-us")) {
                mMatchingVoice = voice;
            }
            mAvailableVoices.put(voice.name, voice);
        }

        mCallback.start(mTts.getSampleRate(), mTts.getAudioFormat(), mTts.getChannelCount());

        if (true) {
            mTts.setVoice(mMatchingVoice, VoiceVariant.parseVoiceVariant(VoiceVariant.MALE));//settings.getVoiceVariant());
            mTts.Rate.setValue(mTts.Rate.getDefaultValue(), 100);//request.getSpeechRate()); //175
            mTts.Pitch.setValue(50, 50);//request.getPitch());
            mTts.PitchRange.setValue(50);
            mTts.Volume.setValue(100);//settings.getVolume());
            mTts.Punctuation.setValue(0);//settings.getPunctuationLevel());
            mTts.setPunctuationCharacters(null);//settings.getPunctuationCharacters());
        } else {
            final VoiceSettings settings = new VoiceSettings(
                    PreferenceManager.getDefaultSharedPreferences(storageContext),
                    mTts);
            mTts.setVoice(mMatchingVoice, settings.getVoiceVariant());
            mTts.Rate.setValue(settings.getRate(), 100);
            mTts.Pitch.setValue(settings.getPitch(), 50); //not 100
            mTts.PitchRange.setValue(settings.getPitchRange());
            mTts.Volume.setValue(settings.getVolume());
            mTts.Punctuation.setValue(settings.getPunctuationLevel());
            mTts.setPunctuationCharacters(settings.getPunctuationCharacters());
        }

        mInitListener.onInit(TextToSpeech.SUCCESS);
    }

    private AudioTrack audioTrack;
    private int bufferSize;

    @SuppressWarnings("deprecation")
    private Locale getTtsLanguage() {
        if (mTts != null) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                android.speech.tts.Voice voice = mTts.getVoice();
//                if (voice != null) {
//                    return voice.getLocale();
//                }
//            } else {
//                return mTts.getLanguage();
//            }
            for (Voice voice : mTts.getAvailableVoices()) {
                if (voice != null) {
                    return voice.locale;
                }
            }
        }
        return null;
    }

    private void populateInformationView() {
        mInformation.clear();

        Locale language = getTtsLanguage();
        if (language != null) {
            final String currentLocale = getString(R.string.current_tts_locale);
            mInformation.add(new Pair<String, String>(currentLocale, language.getDisplayName()));
        }

        final String availableVoices = getString(R.string.available_voices);
        mInformation.add(new Pair<String,String>(availableVoices, Integer.toString(SpeechSynthesis.getVoiceCount())));

        final String version = getString(R.string.tts_version);
        mInformation.add(new Pair<String,String>(version, SpeechSynthesis.getVersion()));

        final String statusText;
        switch (mState) {
        case ERROR:
            statusText = getString(R.string.error_message);
            break;
        case DOWNLOAD_FAILED:
            statusText = getString(R.string.voice_data_failed_message);
            break;
        default:
//            if (!getPackageName().equals(mTts.getDefaultEngine())) {
//                statusText = getString(R.string.set_default_message);
//            } else {
                statusText = null;
//            }
            break;
        }
        if (statusText != null) {
            final String statusLabel = getString(R.string.status);
            mInformation.add(new Pair<String,String>(statusLabel, statusText));
        }

        mInformationView.notifyDataSetChanged();
    }

    /**
     * Handles the result of voice data verification. If verification fails
     * following a successful installation, displays an error dialog. Otherwise,
     * either launches the installer or attempts to initialize the TTS engine.
     *
     * @param resultCode The result of voice data verification.
     * @param data The intent containing available voices.
     */
//    private void onDataChecked(int resultCode, Intent data) {
//        if (resultCode != TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
//            Log.e(TAG, "Voice data check failed (error code: " + resultCode + ").");
//            setState(State.ERROR);
//        }
//
//        initializeEngine();
//    }

    /**
     * Handles the result of TTS engine initialization. Either displays an error
     * dialog or populates the activity's UI.
     *
     * @param status The TTS engine initialization status.
     */
    private void onInitialized(int status) {
        if (status != TextToSpeech.SUCCESS) {
        	Log.e(TAG, "Initialization failed (status: " + status + ").");
            setState(State.ERROR);
        } else {
            setState(State.SUCCESS);
        }

        populateInformationView();
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        switch (requestCode) {
//            case REQUEST_CHECK:
//                onDataChecked(resultCode, data);
//                break;
//            case REQUEST_DEFAULT:
//                initializeEngine();
//                break;
//        }
//        super.onActivityResult(requestCode, resultCode, data);
//    }

    private final TextToSpeech.OnInitListener mInitListener = new TextToSpeech.OnInitListener() {
        @Override
        public void onInit(int status) {
            mHandler.obtainMessage(TTS_INITIALIZED, status, 0).sendToTarget();
        }
    };

    private static class EspeakHandler extends Handler {
    	private WeakReference<eSpeakActivity2> mActivity;

    	public EspeakHandler(eSpeakActivity2 activity)
    	{
    		mActivity = new WeakReference<eSpeakActivity2>(activity);
    	}

    	@Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case TTS_INITIALIZED:
                    mActivity.get().onInitialized(msg.arg1);
                    break;
            }
        }
    }
    private final Handler mHandler = new EspeakHandler(this);

    private void launchGeneralTtsSettings()
    {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        {
            // The Text-to-Speech settings is a Fragment on 3.x:
            intent = new Intent(android.provider.Settings.ACTION_SETTINGS);
            intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, "com.android.settings.TextToSpeechSettings");
            intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS, intent.getExtras());
        }
        else
        {
            // The Text-to-Speech settings is an Activity on 2.x and 4.x:
            intent = new Intent(ACTION_TTS_SETTINGS);
        }
        //startActivityForResult(intent, REQUEST_DEFAULT);
        startActivity(intent);
    }


    private static final int PROGRESS_STARTING = 0;
    private static final int PROGRESS_EXTRACTING = 1;

    private static class ExtractProgress {
        int total;
        int progress = 0;
        int state = PROGRESS_STARTING;
        File file;

        public ExtractProgress(int total) {
            this.total = total;
        }
    }

    private static class AsyncExtract extends AsyncTask<Void, ExtractProgress, Integer> {
        private final Context mContext;
        private final int mRawResId;
        private final File mOutput;
        private final ProgressBar mProgress;

        public AsyncExtract(Context context, int rawResId, File output, ProgressBar progress) {
            mContext = context;
            mRawResId = rawResId;
            mOutput = output;
            mProgress = progress;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            FileUtils.rmdir(CheckVoiceData.getDataPath(mContext));

            final InputStream stream = mContext.getResources().openRawResource(mRawResId);
            final ZipInputStream zipStream = new ZipInputStream(new BufferedInputStream(stream));

            try {
                ExtractProgress progress = new ExtractProgress(stream.available());
                publishProgress(progress);
                progress.state = PROGRESS_EXTRACTING;

                final byte[] buffer = new byte[10240];

                int bytesRead;
                ZipEntry entry;

                while (!isCancelled() && ((entry = zipStream.getNextEntry()) != null)) {
                    progress.file = new File(mOutput, entry.getName());
                    publishProgress(progress);

                    if (entry.isDirectory()) {
                        progress.file.mkdirs();
                        continue;
                    }

                    // Ensure the target path exists.
                    progress.file.getParentFile().mkdirs();

                    final FileOutputStream outputStream = new FileOutputStream(progress.file);
                    try {
                        while (!isCancelled() && ((bytesRead = zipStream.read(buffer)) != -1)) {
                            outputStream.write(buffer, 0, bytesRead);
                            progress.total += bytesRead;
                        }
                    } finally {
                        outputStream.close();
                    }
                    zipStream.closeEntry();
                }

                final String version = FileUtils.read(mContext.getResources().openRawResource(R.raw.espeakdata_version));
                final File outputFile = new File(mOutput, "espeak-ng-data/version");

                FileUtils.write(outputFile, version);
                return RESULT_OK;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    zipStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return RESULT_CANCELED;
        }

        @Override
        protected void onProgressUpdate(ExtractProgress... progress) {
            if (mProgress != null) {
                if (progress[0].state == PROGRESS_STARTING) {
                    mProgress.setMax(progress[0].total);
                } else {
                    mProgress.setProgress(progress[0].progress);
                }
            }
        }
    }

    AsyncExtract mAsyncExtract;
    @SuppressLint("StaticFieldLeak")
    private void install() {
        Context storageContext = LibreraApp.getStorageContext();

        final File dataPath = CheckVoiceData.getDataPath(storageContext).getParentFile();

        mAsyncExtract = new AsyncExtract(storageContext, R.raw.espeakdata, dataPath, null) {
            @Override
            protected void onPostExecute(Integer result) {
                switch (result) {
                    case RESULT_OK:
//                        final Intent intent = new Intent(BROADCAST_LANGUAGES_UPDATED);
//                        sendBroadcast(intent);
                        Toast.makeText(eSpeakActivity2.this, "Install OK", Toast.LENGTH_SHORT).show();
                        initializeEngine();
                        break;
                    case RESULT_CANCELED:
                        // Do nothing?
                        Toast.makeText(eSpeakActivity2.this, "Install Canceled", Toast.LENGTH_SHORT).show();
                        break;
                }

                //setResult(result);
                //finish();
            }
        };

        mAsyncExtract.execute();
    }
}
