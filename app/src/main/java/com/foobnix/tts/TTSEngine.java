package com.foobnix.tts;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Build;
import android.preference.PreferenceManager;
import android.speech.tts.SynthesisCallback;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.EngineInfo;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.speech.tts.UtteranceProgressListener;
import android.support.v4.media.session.MediaSessionCompat;
import android.widget.Toast;

import com.foobnix.android.utils.IO;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.MyMath;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.android.utils.Vibro;
import com.foobnix.mobi.parser.IOUtils;
import com.foobnix.mobi.parser.MobiParserIS;
import com.foobnix.model.AppBookmark;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.BookmarksData;
import com.reecedunn.espeak.SpeechSynthesis;
import com.reecedunn.espeak.Voice;
import com.reecedunn.espeak.VoiceSettings;
import com.reecedunn.espeak.VoiceVariant;
import com.txkj.readingapp.R;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.sys.TempHolder;
import com.github.axet.lamejni.Lame;

import com.foobnix.LibreraApp;

import org.greenrobot.eventbus.EventBus;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class TTSEngine {
    private final static boolean USE_EMBEDDED_TTS = false;
    TextToSpeech.OnUtteranceCompletedListener mListenerComplete = null;
    public void setOnUtteranceCompletedListener(TextToSpeech.OnUtteranceCompletedListener listener) {
        getTTS();
        if (USE_EMBEDDED_TTS) {
            mListenerComplete = listener;
        } else {
            mEngine.setOnUtteranceCompletedListener(listener);
        }
    }

    UtteranceProgressListener mListenerProgress = null;
    public void setOnUtteranceProgressListener(UtteranceProgressListener listener) {
        getTTS();
        if (USE_EMBEDDED_TTS) {
            mListenerProgress = listener;
        } else {
            mEngine.setOnUtteranceProgressListener(listener);
        }
    }

    public static final String FINISHED_SIGNAL = "Finished";
    public static final String STOP_SIGNAL = "Stoped";
    public static final String UTTERANCE_ID_DONE = "LirbiReader";
    public static final String WAV = ".wav";
    public static final String MP3 = ".mp3";
    private static final String TAG = "TTSEngine";
    private static TTSEngine INSTANCE = new TTSEngine();
    volatile SpeechSynthesis mSpeak;
    volatile TextToSpeech mEngine;
    volatile MediaPlayer mp;
    Timer mTimer;
    Object helpObject = new Object();
    HashMap<String, String> map = new HashMap<String, String>();
    HashMap<String, String> mapTemp = new HashMap<String, String>();


    OnInitListener listener = new OnInitListener() {

        @Override
        public void onInit(int status) {
            LOG.d(TAG, "onInit", "SUCCESS", status == TextToSpeech.SUCCESS);
            if (status == TextToSpeech.ERROR) {
                Toast.makeText(LibreraApp.context, R.string.msg_unexpected_error, Toast.LENGTH_LONG).show();
            }

        }
    };
    private String text = "";

    {
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UTTERANCE_ID_DONE);
    }

    {
        mapTemp.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "Temp");
    }

    public static TTSEngine get() {
        return INSTANCE;
    }

    public static AppBookmark fastTTSBookmakr(DocumentController dc) {
        return fastTTSBookmakr(dc.getActivity(), dc.getCurrentBook().getPath(), dc.getCurentPageFirst1(), dc.getPageCount());

    }

    public static AppBookmark fastTTSBookmakr(Context c, String bookPath, int page, int pages) {
        LOG.d("fastTTSBookmakr", page, pages);

        if (pages == 0) {
            LOG.d("fastTTSBookmakr skip");
            return null;
        }
        boolean hasBookmark = BookmarksData.get().hasBookmark(bookPath, page, pages);

        if (!hasBookmark) {
            final AppBookmark bookmark = new AppBookmark(bookPath, c.getString(R.string.fast_bookmark), MyMath.percent(page, pages));
            BookmarksData.get().add(bookmark);

            String TEXT = c.getString(R.string.fast_bookmark) + " " + TxtUtils.LONG_DASH1 + " " + c.getString(R.string.page) + " " + page + "";
            Toast.makeText(c, TEXT, Toast.LENGTH_SHORT).show();
            return bookmark;
        }
        Vibro.vibrate();
        return null;


    }

    public static String engineToString(EngineInfo info) {
        return info.label;
    }

    public void shutdown() {
        LOG.d(TAG, "shutdown");

        synchronized (helpObject) {
            if (USE_EMBEDDED_TTS) {
                if (mSpeak != null) {
                    mSpeak.stop();
                }
                mSpeak = null;
            } else {
                if (mEngine != null) {
                    mEngine.shutdown();
                }
                mEngine = null;
            }
        }

    }

    public void /* SpeechSynthesis TextToSpeech*/ getTTS() {
        getTTS(null);
    }

    public void /*SpeechSynthesis TextToSpeech*/ getTTS(OnInitListener onLisnter) {
        if (LibreraApp.context == null) {
            return;// null;
        }

        synchronized (helpObject) {

            if (TTSEngine.get().isMp3() && mp == null) {
                TTSEngine.get().loadMP3(BookCSS.get().mp3BookPathGet());
            }

            if (USE_EMBEDDED_TTS) {
                if (mSpeak != null) {
                    return; //return mSpeak
                }
            } else {
                if (mEngine != null) {
                    return;//return mEngine;
                }
            }
            if (onLisnter == null) {
                onLisnter = listener;
            }
            if (!USE_EMBEDDED_TTS) {
                mEngine = new TextToSpeech(LibreraApp.context, onLisnter);
            } else {
                final OnInitListener onLisnter_ = onLisnter;
				Context storageContext = LibreraApp.getStorageContext();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    storageContext.moveSharedPreferencesFrom(LibreraApp.context, LibreraApp.context.getPackageName() + "_preferences");

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
                        if (onLisnter_ != null) {
                            onLisnter_.onInit(TextToSpeech.SUCCESS);
                        }
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
                mSpeak = new SpeechSynthesis(storageContext, new SpeechSynthesis.SynthReadyCallback() {
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
                        if (mListenerComplete != null) {
                            mListenerComplete.onUtteranceCompleted(TTSEngine.UTTERANCE_ID_DONE);
                        }
                        if (mListenerProgress != null) {
                            mListenerProgress.onDone(TTSEngine.UTTERANCE_ID_DONE);
                        }
                    }
                });
                mAvailableVoices.clear();
                List<Voice> voices = mSpeak.getAvailableVoices();
                for (Voice voice : voices) {
                    if (mMatchingVoice == null && voice.name.equals("en-us")) {
                        mMatchingVoice = voice;
                    }
                    mAvailableVoices.put(voice.name, voice);
                }

                mCallback.start(mSpeak.getSampleRate(), mSpeak.getAudioFormat(), mSpeak.getChannelCount());

                if (true) {
                    mSpeak.setVoice(mMatchingVoice, VoiceVariant.parseVoiceVariant(VoiceVariant.MALE));//settings.getVoiceVariant());
                    mSpeak.Rate.setValue(mSpeak.Rate.getDefaultValue(), 100);//request.getSpeechRate()); //175
                    mSpeak.Pitch.setValue(50, 50);//request.getPitch());
                    mSpeak.PitchRange.setValue(50);
                    mSpeak.Volume.setValue(100);//settings.getVolume());
                    mSpeak.Punctuation.setValue(0);//settings.getPunctuationLevel());
                    mSpeak.setPunctuationCharacters(null);//settings.getPunctuationCharacters());
                } else {
                    final VoiceSettings settings = new VoiceSettings(
                            PreferenceManager.getDefaultSharedPreferences(storageContext),
                            mSpeak);
                    mSpeak.setVoice(mMatchingVoice, settings.getVoiceVariant());
                    mSpeak.Rate.setValue(settings.getRate(), 100);
                    mSpeak.Pitch.setValue(settings.getPitch(), 50); //not 100
                    mSpeak.PitchRange.setValue(settings.getPitchRange());
                    mSpeak.Volume.setValue(settings.getVolume());
                    mSpeak.Punctuation.setValue(settings.getPunctuationLevel());
                    mSpeak.setPunctuationCharacters(settings.getPunctuationCharacters());
                }
            }
        }

        return; //return ttsEngine;

    }

    //-------------------
    private AudioTrack audioTrack;
    private int bufferSize;
    private final Map<String, Voice> mAvailableVoices = new HashMap<String, Voice>();
    protected Voice mMatchingVoice = null;
    //-------------------

    public synchronized boolean isShutdown() {
        if (USE_EMBEDDED_TTS) {
            return mSpeak == null;
        } else {
            return mEngine == null;
        }
    }

    public void stop() {
        stop(null);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    public void stop(MediaSessionCompat mediaSessionCompat) {
        if (mediaSessionCompat != null) {
            mediaSessionCompat.setActive(false);
        }
        if (!AppState.get().allowOtherMusic) {
            try {

                AudioManager mAudioManager = (AudioManager) LibreraApp.context.getSystemService(Context.AUDIO_SERVICE);
                mAudioManager.abandonAudioFocus(null);
            } catch (Exception e) {
                LOG.e(e);
            }
        }

        LOG.d(TAG, "stop");
        synchronized (helpObject) {

            if (USE_EMBEDDED_TTS) {
                if (mSpeak != null) {
                    if (Build.VERSION.SDK_INT >= 15) {
                        setOnUtteranceProgressListener(null);
                    } else {
                        setOnUtteranceCompletedListener(null);
                    }
                    mSpeak.stop();
                    EventBus.getDefault().post(new TtsStatus());
                }
            } else {
                if (mEngine != null) {
                    if (Build.VERSION.SDK_INT >= 15) {
                        mEngine.setOnUtteranceProgressListener(null);
                    } else {
                        mEngine.setOnUtteranceCompletedListener(null);
                    }
                    mEngine.stop();
                    EventBus.getDefault().post(new TtsStatus());
                }
            }
        }
    }

    public void stopDestroy() {
        LOG.d(TAG, "stop");
        TxtUtils.dictHash = "";
        synchronized (helpObject) {
            if (USE_EMBEDDED_TTS) {
                if (mSpeak != null) {
                    mSpeak.stop();
                }
                mSpeak = null;
            } else {
                if (mEngine != null) {
                    mEngine.shutdown();
                }
                mEngine = null;
            }
        }
        AppSP.get().lastBookParagraph = 0;
    }

    public void /*SpeechSynthesis*/ setTTSWithEngine(String engine) {
        shutdown();
        synchronized (helpObject) {
            if (USE_EMBEDDED_TTS) {
                //FIXME:
                mSpeak = new SpeechSynthesis(LibreraApp.context, null);
            } else {
                mEngine = new TextToSpeech(LibreraApp.context, listener, engine);
            }
        }
        return; //return ttsEngine;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public synchronized void speek(final String text) {
        this.text = text;

        if (AppSP.get().tempBookPage != AppSP.get().lastBookPage) {
            AppSP.get().tempBookPage = AppSP.get().lastBookPage;
            AppSP.get().lastBookParagraph = 0;
        }

        LOG.d(TAG, "speek", AppSP.get().lastBookPage, "par", AppSP.get().lastBookParagraph);

        if (TxtUtils.isEmpty(text)) {
            return;
        }
        if (USE_EMBEDDED_TTS) {
            if (mSpeak == null) {
                LOG.d("getTTS-status was null");
            } else {
                LOG.d("getTTS-status not null");
            }
        } else {
            if (mEngine == null) {
                LOG.d("getTTS-status was null");
            } else {
                LOG.d("getTTS-status not null");
            }
        }

        /*ttsEngine = */getTTS(new OnInitListener() {

            @Override
            public void onInit(int status) {
                LOG.d("getTTS-status", status);
                if (status == TextToSpeech.SUCCESS) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                    speek(text);
                }
            }
        });

        if (USE_EMBEDDED_TTS) {
            mSpeak.Pitch.setValue((int) AppState.get().ttsPitch);
        } else {
            mEngine.setPitch(AppState.get().ttsPitch);
        }
        if (AppState.get().ttsSpeed == 0.0f) {
            AppState.get().ttsSpeed = 0.01f;
        }
        if (USE_EMBEDDED_TTS) {
            mSpeak.Rate.setValue((int) (AppState.get().ttsSpeed));
        } else {
            mEngine.setSpeechRate(AppState.get().ttsSpeed);
        }
        LOG.d(TAG, "Speek s", AppState.get().ttsSpeed);
        LOG.d(TAG, "Speek AppSP.get().lastBookParagraph", AppSP.get().lastBookParagraph);

        if (AppState.get().ttsPauseDuration > 0 && text.contains(TxtUtils.TTS_PAUSE)) {
            String[] parts = text.split(TxtUtils.TTS_PAUSE);
            if (USE_EMBEDDED_TTS) {
                //mSpeak.playSilence(0l, TextToSpeech.QUEUE_FLUSH, mapTemp);
            } else {
                mEngine.playSilence(0l, TextToSpeech.QUEUE_FLUSH, mapTemp);
            }
            for (int i = AppSP.get().lastBookParagraph; i < parts.length; i++) {

                String big = parts[i];
                big = big.trim();

                if (TxtUtils.isNotEmpty(big)) {
                    if (big.length() == 1 && !Character.isLetterOrDigit(big.charAt(0))) {
                        LOG.d("Skip: " + big);
                        continue;

                    }
                    if (big.contains(TxtUtils.TTS_SKIP)) {
                        continue;
                    }

                    if (big.contains(TxtUtils.TTS_STOP)) {
                        HashMap<String, String> mapStop = new HashMap<String, String>();
                        mapStop.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, STOP_SIGNAL);
                        if (USE_EMBEDDED_TTS) {
                            //mSpeak.playSilence(AppState.get().ttsPauseDuration, TextToSpeech.QUEUE_ADD, mapStop);
                        } else {
			                mEngine.playSilence(AppState.get().ttsPauseDuration, TextToSpeech.QUEUE_ADD, mapStop);
                        }
                        LOG.d("Add stop signal");
                    }
                    if (big.contains(TxtUtils.TTS_NEXT)) {
                        if (USE_EMBEDDED_TTS) {
                            //mSpeak.playSilence(0L, TextToSpeech.QUEUE_ADD, map);
                        } else {
                            mEngine.playSilence(0L, TextToSpeech.QUEUE_ADD, map);
                        }
						LOG.d("next-page signal");
                        break;
                    }

                    HashMap<String, String> mapTemp1 = new HashMap<String, String>();
                    mapTemp1.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, FINISHED_SIGNAL + i);

                    if (USE_EMBEDDED_TTS) {
                        mSpeak.synthesize(big, false);
                        //mSpeak.playSilence(AppState.get().ttsPauseDuration, TextToSpeech.QUEUE_ADD, mapTemp);
                    } else {
                        mEngine.speak(big, 0, null, null);
                        mEngine.playSilence(AppState.get().ttsPauseDuration, TextToSpeech.QUEUE_ADD, mapTemp);
                    }
                    LOG.d("pageHTML-parts", i, big);
                }
            }
			if (USE_EMBEDDED_TTS) {
                //mSpeak.playSilence(0L, TextToSpeech.QUEUE_ADD, map);
			} else {
			    mEngine.playSilence(0L, TextToSpeech.QUEUE_ADD, map);
			}
        } else {
            String textToPlay = text.replace(TxtUtils.TTS_PAUSE, "");
            LOG.d("pageHTML-parts-single", text);
            if (USE_EMBEDDED_TTS) {
                mSpeak.synthesize(textToPlay, false);// TextToSpeech.QUEUE_FLUSH, map);
            } else {
                mEngine.speak(textToPlay, TextToSpeech.QUEUE_FLUSH, map);
            }
        }

    }

    public void speakToFile(final DocumentController controller, final ResultResponse<String> info, int from, int to) {
        File dirFolder = new File(BookCSS.get().ttsSpeakPath, "TTS_" + controller.getCurrentBook().getName());
        if (!dirFolder.exists()) {
            dirFolder.mkdirs();
        }
        if (!dirFolder.exists()) {
            info.onResultRecive(controller.getActivity().getString(R.string.file_not_found) + " " + dirFolder.getPath());
            return;
        }


        String path = dirFolder.getPath();
        speakToFile(controller, from - 1, path, info, from - 1, to);
    }

    public void speakToFile(final DocumentController controller, final int page, final String folder, final ResultResponse<String> info, int from, int to) {
        LOG.d("speakToFile", page, controller.getPageCount());
        if (USE_EMBEDDED_TTS) {
            if (mSpeak == null) {
                LOG.d("TTS is null");
                if (controller != null && controller.getActivity() != null) {
                    Toast.makeText(controller.getActivity(), R.string.msg_unexpected_error, Toast.LENGTH_SHORT).show();
                }
                return;
            }
            mSpeak.Pitch.setValue((int)AppState.get().ttsPitch);
            mSpeak.Rate.setValue((int)(AppState.get().ttsSpeed));
        } else {
            if (mEngine == null) {
                LOG.d("TTS is null");
                if (controller != null && controller.getActivity() != null) {
                    Toast.makeText(controller.getActivity(), R.string.msg_unexpected_error, Toast.LENGTH_SHORT).show();
                }
                return;
            }
            mEngine.setPitch(AppState.get().ttsPitch);
            mEngine.setSpeechRate(AppState.get().ttsSpeed);
        }

        if (page >= to || !TempHolder.isRecordTTS) {
            LOG.d("speakToFile finish", page, controller.getPageCount());
            info.onResultRecive((controller.getActivity().getString(R.string.success)));
            TempHolder.isRecordTTS = false;
            return;
        }

        info.onResultRecive((page + 1) + " / " + to);

        DecimalFormat df = new DecimalFormat("0000");
        String pageName = "page-" + df.format(page + 1);
        final String wav = new File(folder, pageName + WAV).getPath();
        String fileText = controller.getTextForPage(page);
        controller.recyclePage(page);


        LOG.d("synthesizeToFile", fileText);
        if (TxtUtils.isEmpty(fileText)) {
            speakToFile(controller, page + 1, folder, info, from, to);
        } else {

            if (fileText.length() > 3950) {
                fileText = TxtUtils.substringSmart(fileText, 3950) + " " + controller.getString(R.string.text_is_too_long);
                LOG.d("Text-too-long", page);
            }

            if (USE_EMBEDDED_TTS) {
                //mSpeak.synthesize(fileText, false);
            } else {
                mEngine.synthesizeToFile(fileText, map, wav);
            }

            TTSEngine.get().getTTS();
            TTSEngine.get().setOnUtteranceCompletedListener(new OnUtteranceCompletedListener() {

                @Override
                public void onUtteranceCompleted(String utteranceId) {
                    LOG.d("speakToFile onUtteranceCompleted", page, controller.getPageCount());


                    if (AppState.get().isConvertToMp3) {
                        try {
                            File file = new File(wav);
                            Lame lame = new Lame();


                            InputStream input = new BufferedInputStream(new FileInputStream(file));
                            input.mark(44);
                            int bitrate = MobiParserIS.asInt_LITTLE_ENDIAN(input, 24, 4);
                            LOG.d("bitrate", bitrate);
                            input.close();
                            input = new FileInputStream(file);


                            byte[] bytes = IOUtils.toByteArray(input);

                            short[] shorts = new short[bytes.length / 2];
                            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);

                            lame.open(1, bitrate, 128, 4);
                            byte[] res = lame.encode(shorts, 44, shorts.length);
                            lame.close();
                            File toFile = new File(wav.replace(".wav", ".mp3"));
                            toFile.delete();
                            IO.copyFile(new ByteArrayInputStream(res), toFile);
                            input.close();
                            file.delete();

                        } catch (Exception e) {
                            LOG.e(e);
                        }
                    }
                    //lame.encode();


                    speakToFile(controller, page + 1, folder, info, from, to);
                }

            });
        }

    }

    public boolean isTempPausing() {
        if (AppState.get().isEnableAccessibility) {
            return true;
        }
        if (USE_EMBEDDED_TTS) {
            return mp != null || mSpeak != null;
        } else {
            return mp != null || mEngine != null;
        }
    }

    public boolean isPlaying() {
        if (TempHolder.isRecordTTS) {
            return false;
        }
        if (isMp3()) {
            return mp != null && mp.isPlaying();
        }

        synchronized (helpObject) {
            if (USE_EMBEDDED_TTS) {
                if (mSpeak == null) {
                    return false;
                }
                return mSpeak != null && false;//ttsEngine.isSpeaking(); //FIXME:
            } else {
                if (mEngine == null) {
                    return false;
                }
                return mEngine != null && mEngine.isSpeaking();
            }
        }
    }

    public boolean hasNoEngines() {
        try {
            if (USE_EMBEDDED_TTS) {
                return mSpeak != null; //FIXME：
            } else {
                return mEngine != null && (mEngine.getEngines() == null || mEngine.getEngines().size() == 0);
            }
        } catch (Exception e) {
            return true;
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public String getCurrentLang() {
        try {
		     if (USE_EMBEDDED_TTS) {
                 return "en_US"; // FIXME:
			 } else { 
                 if (Build.VERSION.SDK_INT >= 21 && mEngine != null && mEngine.getDefaultVoice() != null && mEngine.getDefaultVoice().getLocale() != null) {
                    return mEngine.getDefaultVoice().getLocale().getDisplayLanguage();
                 }
			}            
        } catch (Exception e) {
            LOG.e(e);
        }
        return "---";
    }

    public int getEngineCount() {
        try {
            if (USE_EMBEDDED_TTS) {
                if (mSpeak == null/* || ttsEngine.getEngines() == null*/) {
                    return -1;
                }
                return 1;
            } else {
                if (mEngine == null || mEngine.getEngines() == null) {
                    return -1;
                }
                return mEngine.getEngines().size();
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        return 0;
    }

    public String getCurrentEngineName() {
        try {
            if (USE_EMBEDDED_TTS) {
                if (mSpeak != null) {
                    return LibreraApp.context.getPackageName();
                }
            } else {
                if (mEngine != null) {
                    String enginePackage = mEngine.getDefaultEngine();
                    List<EngineInfo> engines = mEngine.getEngines();
                    for (final EngineInfo eInfo : engines) {
                        if (eInfo.name.equals(enginePackage)) {
                            return engineToString(eInfo);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        return "---";
    }

    public void loadMP3(String ttsPlayMp3Path) {
        loadMP3(ttsPlayMp3Path, false);
    }

    public void loadMP3(String ttsPlayMp3Path, final boolean play) {
        LOG.d("loadMP3-", ttsPlayMp3Path);
        if (TxtUtils.isEmpty(ttsPlayMp3Path) || !new File(ttsPlayMp3Path).isFile()) {
            LOG.d("loadMP3-skip mp3");
            return;
        }
        try {
            mp3Destroy();
            mp = new MediaPlayer();
            mp.setDataSource(ttsPlayMp3Path);
            mp.prepare();
            mp.setOnCompletionListener(new OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.pause();
                }
            });
            if (play) {
                mp.start();
            }

            mTimer = new Timer();

            mTimer.schedule(new TimerTask() {

                @Override
                public void run() {
                    AppState.get().mp3seek = mp.getCurrentPosition();
                    //LOG.d("Run timer-task");
                    EventBus.getDefault().post(new TtsStatus());
                }

                ;
            }, 1000, 1000);

        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public MediaPlayer getMP() {
        return mp;
    }

    public void mp3Destroy() {
        if (mp != null) {
            mp.stop();
            mp.reset();
            mp = null;
            if (mTimer != null) {
                mTimer.purge();
                mTimer.cancel();
                mTimer = null;
            }
        }
        LOG.d("mp3Desproy");
    }

    public void mp3Next() {
        int seek = mp.getCurrentPosition();
        mp.seekTo(seek + 5 * 1000);
    }

    public void mp3Prev() {
        int seek = mp.getCurrentPosition();
        mp.seekTo(seek - 5 * 1000);
    }

    public boolean isMp3PlayPause() {
        if (isMp3()) {
            if (mp == null) {
                loadMP3(BookCSS.get().mp3BookPathGet());
            }
            if (mp == null) {
                return false;
            }
            if (mp.isPlaying()) {
                mp.pause();
            } else {
                mp.start();
            }
            TTSNotification.showLast();
            return true;
        }
        return false;
    }

    public void playMp3() {
        if (mp != null) {
            mp.start();
        }
    }

    public void pauseMp3() {
        if (mp != null) {
            mp.pause();
        }
    }

    public boolean isMp3() {
        return TxtUtils.isNotEmpty(BookCSS.get().mp3BookPathGet());
    }

    public void seekTo(int i) {
        if (mp != null) {
            mp.seekTo(i);
        }

    }

}
