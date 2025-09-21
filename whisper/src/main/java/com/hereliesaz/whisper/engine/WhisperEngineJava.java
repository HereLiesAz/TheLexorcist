package com.hereliesaz.whisper.engine;

import android.content.Context;
import android.util.Log;

// Standard TensorFlow Lite classes for Interpreter and its Options
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Interpreter.Options;

import com.hereliesaz.whisper.utils.WaveUtil;
import com.hereliesaz.whisper.utils.WhisperUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

public class WhisperEngineJava implements WhisperEngine {
    private final String TAG = "WhisperEngineJava";
    private final WhisperUtil mWhisperUtil = new WhisperUtil();

    private final Context mContext; // Keep context if needed for other things, like vocab loading path
    private volatile boolean mIsFullyInitialized = false;   // Tracks if interpreter and vocab are ready
    private Interpreter mInterpreter = null; // From org.tensorflow.lite.Interpreter

    public WhisperEngineJava(Context context) {
        mContext = context;
    }

    @Override
    public boolean isInitialized() {
        return mIsFullyInitialized && mInterpreter != null;
    }

    @Override
    public boolean initialize(String modelPath, String vocabPath, boolean multilingual) {
        mIsFullyInitialized = false;
        Log.d(TAG, "Initializing WhisperEngine. Attempting to load model and vocab.");

        try {
            loadModel(modelPath); // This will create the org.tensorflow.lite.Interpreter
            Log.d(TAG, "Model is loaded: " + modelPath);

            boolean vocabLoaded = mWhisperUtil.loadFiltersAndVocab(multilingual, vocabPath);
            if (vocabLoaded && mInterpreter != null) {
                mIsFullyInitialized = true;
                Log.d(TAG, "Filters and Vocab are loaded: " + vocabPath);
            } else {
                mIsFullyInitialized = false;
                Log.e(TAG, "Failed to load Filters and Vocab, or interpreter became null.");
                if (mInterpreter != null) {
                    mInterpreter.close();
                    mInterpreter = null;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to load model or vocab.", e);
            mIsFullyInitialized = false;
            if (mInterpreter != null) {
                mInterpreter.close();
                mInterpreter = null;
            }
        }
        return mIsFullyInitialized;
    }

    private void loadModel(String modelPath) throws IOException {
        FileInputStream fileInputStream = null;
        FileChannel fileChannel = null;
        try {
            fileInputStream = new FileInputStream(modelPath);
            fileChannel = fileInputStream.getChannel();
            long startOffset = 0;
            long declaredLength = fileChannel.size();
            ByteBuffer tfliteModel = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);

            Options options = new Options(); // org.tensorflow.lite.Interpreter.Options
            options.setNumThreads(Runtime.getRuntime().availableProcessors());
            mInterpreter = new Interpreter(tfliteModel, options); // org.tensorflow.lite.Interpreter

        } finally {
            if (fileChannel != null) {
                try { fileChannel.close(); } catch (IOException e) { Log.e(TAG, "Failed to close FileChannel", e); }
            }
            if (fileInputStream != null) {
                try { fileInputStream.close(); } catch (IOException e) { Log.e(TAG, "Failed to close FileInputStream", e); }
            }
        }
    }

    @Override
    public void deinitialize() {
        if (mInterpreter != null) {
            mInterpreter.close();
            mInterpreter = null;
        }
        mIsFullyInitialized = false;
        Log.d(TAG, "WhisperEngine de-initialized.");
    }

    @Override
    public String transcribeFile(String wavePath) {
        if (!isInitialized()) {
            Log.e(TAG, "Engine not initialized. Cannot transcribe.");
            return "Error: Engine not initialized.";
        }
        float[] melSpectrogram = getMelSpectrogram(wavePath);
        if (melSpectrogram == null) {
            Log.e(TAG, "Mel spectrogram calculation failed.");
            return "Error: Mel spectrogram calculation failed.";
        }
        return runInference(melSpectrogram);
    }

    @Override
    public String transcribeBuffer(float[] samples) {
        if (!isInitialized()) {
            Log.e(TAG, "Engine not initialized. Cannot transcribe buffer.");
            return "Error: Engine not initialized.";
        }
        float[] melSpectrogram = mWhisperUtil.getMelSpectrogram(samples, samples.length, Runtime.getRuntime().availableProcessors());
        if (melSpectrogram == null) {
            Log.e(TAG, "Mel spectrogram calculation from buffer failed.");
            return "Error: Mel spectrogram calculation failed (from buffer).";
        }
        return runInference(melSpectrogram);
    }

    private float[] getMelSpectrogram(String wavePath) {
        float[] samples = WaveUtil.getSamples(wavePath);
        if (samples == null) {
            Log.e(TAG, "Failed to get samples from wave file: " + wavePath);
            return null;
        }
        int fixedInputSize = WhisperUtil.WHISPER_SAMPLE_RATE * WhisperUtil.WHISPER_CHUNK_SIZE;
        float[] inputSamples = new float[fixedInputSize];
        int copyLength = Math.min(samples.length, fixedInputSize);
        System.arraycopy(samples, 0, inputSamples, 0, copyLength);
        int cores = Runtime.getRuntime().availableProcessors();
        return mWhisperUtil.getMelSpectrogram(inputSamples, inputSamples.length, cores);
    }

    private String runInference(float[] melSpectrogramInput) {
        if (mInterpreter == null) {
            Log.e(TAG, "Interpreter is not initialized for inference.");
            return "Error: Interpreter not initialized";
        }

        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(melSpectrogramInput.length * Float.BYTES);
        inputBuffer.order(ByteOrder.nativeOrder());
        inputBuffer.asFloatBuffer().put(melSpectrogramInput);

        Object[] inputsArray = {inputBuffer};

        ByteBuffer outputByteBuffer = ByteBuffer.allocateDirect(WhisperUtil.MAX_DECODER_TOKENS * Integer.BYTES);
        outputByteBuffer.order(ByteOrder.nativeOrder());

        Map<Integer, Object> outputs = new HashMap<>();
        outputs.put(0, outputByteBuffer);

        try {
            Log.d(TAG, "Running inference...");
            mInterpreter.runForMultipleInputsOutputs(inputsArray, outputs);
            Log.d(TAG, "Inference completed.");
        } catch (Exception e) {
            Log.e(TAG, "Error running inference", e);
            return "Error: Inference execution failed";
        }

        outputByteBuffer.rewind();
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < WhisperUtil.MAX_DECODER_TOKENS; i++) {
            if (outputByteBuffer.remaining() < Integer.BYTES) {
                Log.w(TAG, "Output buffer ended before MAX_OUTPUT_TOKENS at token " + i);
                break;
            }
            int token = outputByteBuffer.getInt();

            if (token == mWhisperUtil.getTokenEOT()) {
                Log.d(TAG, "EOT token encountered.");
                break;
            }

            // Check if the token is a special token (SOT, PREV, NOT, etc.) or out of vocab range
            if (token < mWhisperUtil.getTokenSOT() || token > mWhisperUtil.getTokenNoSpeech()) {
                // Check if it's a valid vocabulary token before attempting to get the word
                if (token < mWhisperUtil.getVocabSize()) { 
                    String word = mWhisperUtil.getWordFromToken(token);
                    if (word != null) {
                        result.append(word);
                    } else {
                        Log.w(TAG, "Null word for token (within vocab size but not found): " + token);
                    }
                } else {
                    // Token is outside the normal vocabulary range (and not EOT)
                    Log.w(TAG, "Token out of expected vocab range (and not EOT): " + token);
                }
            } else { // This 'else' covers SOT, TRANSCRIBE, TRANSLATE, and other special tokens within that range
                String word = mWhisperUtil.getWordFromToken(token);
                if (token == mWhisperUtil.getTokenTranscribe()) {
                    Log.d(TAG, "Special token: Transcribe (" + (word != null ? word : "null_word") + ")");
                } else if (token == mWhisperUtil.getTokenTranslate()) {
                    Log.d(TAG, "Special token: Translate (" + (word != null ? word : "null_word") + ")");
                } else if (token >= mWhisperUtil.getTokenSOT() && token <= mWhisperUtil.getTokenBEG()){
                     Log.d(TAG, "Skipping other special token: " + token + " (" + (word != null ? word : "null_word") + ")");
                } else {
                    // This case should ideally not be hit if the above conditions are exhaustive for special tokens
                    Log.w(TAG, "Unhandled token case in special token block: " + token + " (" + (word != null ? word : "null_word") + ")");
                }
            }
        }
        Log.d(TAG, "Transcription result: " + result.toString());
        return result.toString();
    }
}
