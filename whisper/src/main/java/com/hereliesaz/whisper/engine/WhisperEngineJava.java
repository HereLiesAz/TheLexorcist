package com.hereliesaz.whisper.engine;

import android.content.Context;
import android.util.Log;

import com.google.ai.edge.litert.LiteRuntime;
import com.google.ai.edge.litert.InterpreterOptions;
import com.google.ai.edge.litert.Interpreter;
// It's possible delegate classes are also in com.google.ai.edge.lite, e.g., com.google.ai.edge.lite.delegates.GpuDelegate
// For now, assuming InterpreterOptions handles delegate configuration internally if GPU dependency is added.

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

    private final Context mContext;
    private volatile boolean mIsInitialized = false;
    private LiteRuntime mLiteRuntime = null;
    private Interpreter mInterpreter = null;

    public WhisperEngineJava(Context context) {
        mContext = context;
    }

    @Override
    public boolean isInitialized() {
        return mIsInitialized && mInterpreter != null;
    }

    @Override
    public boolean initialize(String modelPath, String vocabPath, boolean multilingual) {
        mIsInitialized = false;
        if (mLiteRuntime != null) {
            Log.d(TAG, "LiteRuntime already initialized, re-initializing interpreter.");
            // Proceed to load model and vocab with existing mLiteRuntime
            try {
                loadModelAndVocab(modelPath, vocabPath, multilingual);
            } catch (IOException e) {
                Log.e(TAG, "Failed to load model or vocab even with existing LiteRuntime.", e);
                mIsInitialized = false;
            }
            return false; // Actual readiness depends on async loadModelAndVocab if it were async
        }

        LiteRuntime.initialize(mContext)
            .addOnSuccessListener(runtime -> {
                Log.d(TAG, "LiteRuntime initialized successfully.");
                mLiteRuntime = runtime;
                try {
                    loadModelAndVocab(modelPath, vocabPath, multilingual);
                } catch (IOException e) {
                    Log.e(TAG, "Failed to load model or vocab after LiteRuntime initialization.", e);
                    mIsInitialized = false;
                    // Consider de-initializing mLiteRuntime or cleaning up if partial failure
                    if (mInterpreter != null) {
                        mInterpreter.close();
                        mInterpreter = null;
                    }
                     if (mLiteRuntime != null) {
                        // mLiteRuntime itself doesn't have a close/deinitialize method in the typical examples
                        // It's managed by the library or Play Services
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "LiteRuntime initialization failed.", e);
                mLiteRuntime = null;
                mIsInitialized = false;
            });

        return false; // Initialization is asynchronous
    }

    private void loadModelAndVocab(String modelPath, String vocabPath, boolean multilingual) throws IOException {
        if (mLiteRuntime == null) {
            throw new IOException("LiteRuntime is not initialized.");
        }
        loadModel(modelPath); // This will use mLiteRuntime to create the interpreter
        Log.d(TAG, "Model is loaded: " + modelPath);

        boolean vocabLoaded = mWhisperUtil.loadFiltersAndVocab(multilingual, vocabPath);
        if (vocabLoaded && mInterpreter != null) {
            mIsInitialized = true; // Fully initialized
            Log.d(TAG, "Filters and Vocab are loaded: " + vocabPath);
        } else {
            mIsInitialized = false;
            Log.e(TAG, "Failed to load Filters and Vocab, or interpreter is null.");
            if (mInterpreter != null) {
                mInterpreter.close();
                mInterpreter = null;
            }
        }
    }

    private void loadModel(String modelPath) throws IOException {
        if (mLiteRuntime == null) {
            throw new IOException("LiteRuntime is not initialized, cannot load model.");
        }
        FileInputStream fileInputStream = null;
        FileChannel fileChannel = null;
        try {
            fileInputStream = new FileInputStream(modelPath);
            fileChannel = fileInputStream.getChannel();
            long startOffset = 0;
            long declaredLength = fileChannel.size();
            ByteBuffer tfliteModel = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);

            InterpreterOptions options = new InterpreterOptions();
            options.setNumThreads(Runtime.getRuntime().availableProcessors());
            // For GPU delegate with com.google.ai.edge.litert:litert-gpu:
            // if (com.google.ai.edge.lite.delegates.GpuDelegateHelper.isGpuDelegateAvailable()) {
            //     options.addDelegate(new com.google.ai.edge.lite.delegates.GpuDelegate());
            // }
            // The exact delegate mechanism might differ; check specific `com.google.ai.edge.lite` docs if GPU is needed.

            mInterpreter = mLiteRuntime.createInterpreter(tfliteModel, options);
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
        // mLiteRuntime itself typically doesn't need explicit deinitialization from app code
        // It's managed by the library / Play Services.
        // mLiteRuntime = null; // Optionally nullify if you want to force re-init via initialize()
        mIsInitialized = false;
        Log.d(TAG, "WhisperEngine de-initialized.");
    }

    @Override
    public String transcribeFile(String wavePath) {
        if (!isInitialized()) {
            Log.e(TAG, "Engine not initialized. Cannot transcribe.");
            return "Error: Engine not initialized.";
        }

        Log.d(TAG, "Calculating Mel spectrogram for file: " + wavePath);
        float[] melSpectrogram = getMelSpectrogram(wavePath);
        if (melSpectrogram == null) {
            Log.e(TAG, "Mel spectrogram calculation failed.");
            return "Error: Mel spectrogram calculation failed.";
        }
        Log.d(TAG, "Mel spectrogram calculated. Length: " + melSpectrogram.length);

        return runInference(melSpectrogram);
    }

    @Override
    public String transcribeBuffer(float[] samples) {
        if (!isInitialized()) {
            Log.e(TAG, "Engine not initialized. Cannot transcribe buffer.");
            return "Error: Engine not initialized.";
        }
        Log.d(TAG, "Calculating Mel spectrogram for buffer...");
        float[] melSpectrogram = mWhisperUtil.getMelSpectrogram(samples, samples.length, Runtime.getRuntime().availableProcessors());
        if (melSpectrogram == null) {
            Log.e(TAG, "Mel spectrogram calculation from buffer failed.");
            return "Error: Mel spectrogram calculation failed (from buffer).";
        }
        Log.d(TAG, "Mel spectrogram from buffer calculated. Length: " + melSpectrogram.length);
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

        int inputSize = melSpectrogramInput.length * Float.BYTES;
        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(inputSize);
        inputBuffer.order(ByteOrder.nativeOrder());
        inputBuffer.rewind();
        for (float val : melSpectrogramInput) {
            inputBuffer.putFloat(val);
        }
        inputBuffer.rewind();

        Object[] inputsArray = new Object[1];
        inputsArray[0] = inputBuffer;

        final int MAX_OUTPUT_TOKENS = WhisperUtil.MAX_DECODER_TOKENS;
        ByteBuffer outputByteBuffer = ByteBuffer.allocateDirect(1 * MAX_OUTPUT_TOKENS * Integer.BYTES);
        outputByteBuffer.order(ByteOrder.nativeOrder());
        outputByteBuffer.rewind();

        Map<Integer, Object> outputs = new HashMap<>();
        outputs.put(0, outputByteBuffer);

        try {
            Log.d(TAG, "Running inference...");
            mInterpreter.run(inputsArray, outputs);
            Log.d(TAG, "Inference completed.");
        } catch (Exception e) {
            Log.e(TAG, "Error running inference", e);
            return "Error: Inference execution failed";
        }

        outputByteBuffer.rewind();
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < MAX_OUTPUT_TOKENS; i++) {
            if (outputByteBuffer.remaining() < Integer.BYTES) {
                Log.w(TAG, "Output buffer ended before MAX_OUTPUT_TOKENS at token " + i);
                break;
            }
            int token = outputByteBuffer.getInt();

            if (token == mWhisperUtil.getTokenEOT()) {
                Log.d(TAG, "EOT token encountered.");
                break;
            }
            if (token < mWhisperUtil.getTokenSOT() || token > mWhisperUtil.getTokenNoSpeech()) { 
                if (token < mWhisperUtil.getVocabSize()) { 
                    String word = mWhisperUtil.getWordFromToken(token);
                    result.append(word);
                } else {
                     Log.w(TAG, "Token out of expected vocab range (excluding EOT): " + token);
                }
            } else {
                String word = mWhisperUtil.getWordFromToken(token); 
                if (token == mWhisperUtil.getTokenTranscribe()) {
                    Log.d(TAG, "Special token: Transcribe (" + word + ")");
                } else if (token == mWhisperUtil.getTokenTranslate()) {
                    Log.d(TAG, "Special token: Translate (" + word + ")");
                } else {
                    Log.d(TAG, "Skipping special token: " + token + " (" + word + ")");
                }
            }
        }
        Log.d(TAG, "Transcription result: " + result.toString());
        return result.toString();
    }
}
