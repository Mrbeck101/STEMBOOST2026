package Services;

import UI.UserContext;
import UserFactory.User;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Keyboard-driven TTS service.
 * Toggle: F1
 * Pause/Resume: F2
 * Next sentence: F4
 * Previous sentence: F3
 */
public class KeyboardTtsService {

    public enum AccessMode {
        PUBLIC_TOGGLE,
        STUDENT_ONLY
    }

    public enum NavigationMode {
        SENTENCE,
        NONE
    }

    public static final class ReadingContent {
        private final String text;
        private final Integer moduleId;
        private final Integer moduleProgress;

        public ReadingContent(String text) {
            this(text, null, null);
        }

        public ReadingContent(String text, Integer moduleId, Integer moduleProgress) {
            this.text = text == null ? "" : text;
            this.moduleId = moduleId;
            this.moduleProgress = moduleProgress;
        }

        public String text() {
            return text;
        }

        public Integer moduleId() {
            return moduleId;
        }

        public Integer moduleProgress() {
            return moduleProgress;
        }
    }

    private static KeyboardTtsService instance;

    private final WebEngine engine;

    private boolean enabled = true;
    private boolean paused = false;

    private List<String> sentences = List.of();
    private int sentenceIndex = 0;
    private boolean speaking = false;

    private Integer activeModuleId;
    private Integer activeModuleProgress;
    private boolean moduleProgressCommitted;

    private Supplier<ReadingContent> contentSupplier;
    private AccessMode accessMode = AccessMode.PUBLIC_TOGGLE;
    private NavigationMode navigationMode = NavigationMode.SENTENCE;
    private Runnable sceneReadingCompleteHandler;
    private Runnable readingCompleteHandler;

    private boolean speechCapabilityChecked;
    private boolean webSpeechSupported;
    private Process windowsSpeechProcess;
    private long playToken;

    private KeyboardTtsService() {
        WebView webView = new WebView();
        this.engine = webView.getEngine();
        this.engine.loadContent("""
                <html><body><script>
                window.__tts_done = true;
                window.__tts_cancelled = false;
                window.__tts_voice = null;
                window.__tts_supported = (typeof speechSynthesis !== 'undefined');
                function __pickVoice() {
                    if (!window.__tts_supported) return null;
                    const voices = speechSynthesis.getVoices();
                    if (!voices || voices.length === 0) return null;
                    let preferred = voices.find(v => (v.lang || '').toLowerCase().startsWith('en'));
                    return preferred || voices[0];
                }
                function __tts_cancel() {
                    window.__tts_cancelled = true;
                    if (window.__tts_supported) {
                        speechSynthesis.cancel();
                    }
                    window.__tts_done = true;
                }
                function __tts_speak(text) {
                    if (!window.__tts_supported) {
                        window.__tts_done = true;
                        return;
                    }
                    window.__tts_done = false;
                    window.__tts_cancelled = false;
                    const u = new SpeechSynthesisUtterance(text);
                    const v = window.__tts_voice || __pickVoice();
                    if (v) { u.voice = v; window.__tts_voice = v; }
                    u.rate = 1.0;
                    u.onend = function() { window.__tts_done = true; };
                    u.onerror = function() { window.__tts_done = true; };
                    speechSynthesis.speak(u);
                }
                </script></body></html>
                """);
    }

    public static KeyboardTtsService getInstance() {
        if (instance == null) {
            instance = new KeyboardTtsService();
        }
        return instance;
    }

    public void bindScene(Scene scene, AccessMode mode, Supplier<ReadingContent> supplier) {
        bindScene(scene, mode, NavigationMode.SENTENCE, supplier, null);
    }

    public void bindScene(Scene scene, AccessMode mode, NavigationMode navMode, Supplier<ReadingContent> supplier) {
        bindScene(scene, mode, navMode, supplier, null);
    }

    public void bindScene(Scene scene, AccessMode mode, Supplier<ReadingContent> supplier, Runnable onReadingComplete) {
        bindScene(scene, mode, NavigationMode.SENTENCE, supplier, onReadingComplete);
    }

    public void bindScene(Scene scene, AccessMode mode, NavigationMode navMode, Supplier<ReadingContent> supplier, Runnable onReadingComplete) {
        this.accessMode = mode;
        this.navigationMode = navMode == null ? NavigationMode.SENTENCE : navMode;
        this.contentSupplier = supplier;
        this.sceneReadingCompleteHandler = onReadingComplete;
        this.readingCompleteHandler = onReadingComplete;

        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (!isToggleAllowed()) {
                return;
            }

            if (isToggleKey(e)) {
                toggle();
                e.consume();
                return;
            }

            if (!enabled || !isReadControlsAllowed()) {
                return;
            }

            if (e.getCode() == KeyCode.F2) {
                togglePause();
                e.consume();
                return;
            }

            if (navigationMode == NavigationMode.SENTENCE && isForwardKey(e)) {
                skipForward();
                e.consume();
                return;
            }

            if (navigationMode == NavigationMode.SENTENCE && isBackKey(e)) {
                skipBackward();
                e.consume();
            }
        });

        if (enabled && isReadControlsAllowed()) {
            loadFromSupplierAndStart(false);
        }
    }

    public void onSceneExit() {
        commitModuleProgressIfNeeded(false);
        cancelSpeech();
        contentSupplier = null;
        sentences = List.of();
        sentenceIndex = 0;
        paused = false;
        speaking = false;
        activeModuleId = null;
        activeModuleProgress = null;
        moduleProgressCommitted = false;
        sceneReadingCompleteHandler = null;
        readingCompleteHandler = null;
    }

    public boolean isEnabled() {
        return enabled && isReadControlsAllowed();
    }

    public void refreshCurrentSceneReading() {
        if (enabled && isReadControlsAllowed()) {
            loadFromSupplierAndStart(true);
        }
    }

    public void speakNow(String text) {
        speakNow(new ReadingContent(text), null);
    }

    public void speakNow(String text, Runnable onReadingComplete) {
        speakNow(new ReadingContent(text), onReadingComplete);
    }

    public void speakNow(ReadingContent content) {
        speakNow(content, null);
    }

    public void speakNow(ReadingContent content, Runnable onReadingComplete) {
        if (!enabled || !isReadControlsAllowed() || content == null) {
            if (onReadingComplete != null) {
                Platform.runLater(onReadingComplete);
            }
            return;
        }

        cancelSpeech();
        List<String> parsed = splitSentences(content.text());
        if (parsed.isEmpty()) {
            parsed = List.of("No readable content is currently available.");
        }

        this.sentences = parsed;
        this.sentenceIndex = 0;
        this.activeModuleId = content.moduleId();
        this.activeModuleProgress = content.moduleProgress();
        this.moduleProgressCommitted = false;
        this.paused = false;
        this.readingCompleteHandler = onReadingComplete != null ? onReadingComplete : sceneReadingCompleteHandler;
        startCurrentSentence();
    }

    private boolean isToggleAllowed() {
        if (accessMode == AccessMode.PUBLIC_TOGGLE) {
            return true;
        }
        return isStudentUser();
    }

    private boolean isReadControlsAllowed() {
        if (accessMode == AccessMode.PUBLIC_TOGGLE) {
            return true;
        }
        return isStudentUser();
    }

    private boolean isStudentUser() {
        User user = UserContext.getInstance().getCurrentUser();
        return user != null && "Student".equals(user.getAcctType());
    }

    private boolean isToggleKey(KeyEvent e) {
        return e.getCode() == KeyCode.F1;
    }

    private boolean isForwardKey(KeyEvent e) {
        return e.getCode() == KeyCode.F4;
    }

    private boolean isBackKey(KeyEvent e) {
        return e.getCode() == KeyCode.F3;
    }

    private void toggle() {
        enabled = !enabled;
        if (!enabled) {
            commitModuleProgressIfNeeded(false);
            cancelSpeech();
            return;
        }
        loadFromSupplierAndStart(true);
    }

    private void togglePause() {
        paused = !paused;
        if (!paused && enabled && isReadControlsAllowed() && !speaking) {
            startCurrentSentence();
        } else if (paused) {
            cancelSpeech();
        }
    }

    private void skipForward() {
        if (sentences.isEmpty()) {
            return;
        }
        cancelSpeech();
        sentenceIndex = Math.min(sentences.size() - 1, sentenceIndex + 1);
        paused = false;
        startCurrentSentence();
    }

    private void skipBackward() {
        if (sentences.isEmpty()) {
            return;
        }
        cancelSpeech();
        sentenceIndex = Math.max(0, sentenceIndex - 1);
        paused = false;
        startCurrentSentence();
    }

    private void loadFromSupplierAndStart(boolean restartFromBeginning) {
        if (contentSupplier == null) {
            return;
        }

        ReadingContent content = contentSupplier.get();
        if (content == null) {
            return;
        }

        List<String> parsed = splitSentences(content.text());
        if (parsed.isEmpty()) {
            parsed = List.of("No readable content is currently available.");
        }

        boolean changedText = !parsed.equals(this.sentences);
        this.sentences = parsed;

        this.activeModuleId = content.moduleId();
        this.activeModuleProgress = content.moduleProgress();
        this.moduleProgressCommitted = false;
        this.readingCompleteHandler = sceneReadingCompleteHandler;

        if (restartFromBeginning || changedText || sentenceIndex >= sentences.size()) {
            this.sentenceIndex = 0;
        }

        this.paused = false;
        startCurrentSentence();
    }

    private void startCurrentSentence() {
        if (!enabled || paused || sentenceIndex >= sentences.size()) {
            return;
        }
        String sentence = sentences.get(sentenceIndex);
        if (sentence == null || sentence.isBlank()) {
            sentenceIndex++;
            startCurrentSentence();
            return;
        }

        speaking = true;
        long token = ++playToken;

        if (isWebSpeechAvailable()) {
            Platform.runLater(() -> {
                try {
                    engine.executeScript("__tts_cancel();");
                    String escaped = sentence
                            .replace("\\", "\\\\")
                            .replace("\"", "\\\"")
                            .replace("\n", " ");
                    engine.executeScript("__tts_speak(\"" + escaped + "\");");
                    monitorSentenceEnd(token);
                } catch (Exception ex) {
                    speakWithWindowsFallback(sentence, token);
                }
            });
            return;
        }

        speakWithWindowsFallback(sentence, token);
    }

    private void monitorSentenceEnd(long token) {
        Thread watcher = new Thread(() -> {
            while (enabled && !paused) {
                try {
                    if (token != playToken) {
                        speaking = false;
                        return;
                    }
                    Object doneObj = Platform.isFxApplicationThread()
                            ? engine.executeScript("window.__tts_done")
                            : runOnFxAndGet(() -> engine.executeScript("window.__tts_done"));
                    boolean done = Boolean.TRUE.equals(doneObj);
                    if (done) {
                        onSentenceCompleted(token);
                        return;
                    }
                    Thread.sleep(120);
                } catch (Exception e) {
                    speaking = false;
                    return;
                }
            }
            speaking = false;
        }, "tts-monitor");
        watcher.setDaemon(true);
        watcher.start();
    }

    private void speakWithWindowsFallback(String sentence, long token) {
        if (!isWindowsHost()) {
            // No compatible TTS backend; stop speaking quietly to avoid crashing UI.
            speaking = false;
            return;
        }

        Thread speaker = new Thread(() -> {
            try {
                String escaped = sentence.replace("'", "''");
                String command = "Add-Type -AssemblyName System.Speech; "
                        + "$s = New-Object System.Speech.Synthesis.SpeechSynthesizer; "
                        + "$s.Rate = 0; "
                        + "$s.Speak('" + escaped + "');";

                Process process = new ProcessBuilder(
                        "powershell.exe",
                        "-NoProfile",
                        "-NonInteractive",
                        "-Command",
                        command
                ).start();

                synchronized (this) {
                    windowsSpeechProcess = process;
                }

                process.waitFor();
            } catch (IOException | InterruptedException ignored) {
            } finally {
                synchronized (this) {
                    if (windowsSpeechProcess != null && !windowsSpeechProcess.isAlive()) {
                        windowsSpeechProcess = null;
                    }
                }
                onSentenceCompleted(token);
            }
        }, "tts-windows-fallback");
        speaker.setDaemon(true);
        speaker.start();
    }

    private void onSentenceCompleted(long token) {
        if (token != playToken) {
            speaking = false;
            return;
        }
        speaking = false;
        sentenceIndex++;
        if (sentenceIndex >= sentences.size()) {
            commitModuleProgressIfNeeded(true);
            Runnable onComplete = readingCompleteHandler;
            if (onComplete != null) {
                Platform.runLater(onComplete);
            }
            if (readingCompleteHandler != sceneReadingCompleteHandler) {
                readingCompleteHandler = sceneReadingCompleteHandler;
            }
            return;
        }
        if (!paused && enabled) {
            startCurrentSentence();
        }
    }

    private void cancelSpeech() {
        playToken++;
        speaking = false;
        paused = false;

        synchronized (this) {
            if (windowsSpeechProcess != null && windowsSpeechProcess.isAlive()) {
                windowsSpeechProcess.destroyForcibly();
                try {
                    windowsSpeechProcess.waitFor(1, java.util.concurrent.TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
            windowsSpeechProcess = null;
        }

        try {
            if (Platform.isFxApplicationThread()) {
                engine.executeScript("__tts_cancel();");
            } else {
                runOnFxAndGet(() -> {
                    engine.executeScript("__tts_cancel();");
                    return null;
                });
            }
        } catch (Exception ignored) {
        }
    }

    private boolean isWebSpeechAvailable() {
        if (speechCapabilityChecked) {
            return webSpeechSupported;
        }

        speechCapabilityChecked = true;
        try {
            Object supported = Platform.isFxApplicationThread()
                    ? engine.executeScript("typeof speechSynthesis !== 'undefined'")
                    : runOnFxAndGet(() -> engine.executeScript("typeof speechSynthesis !== 'undefined'"));
            webSpeechSupported = Boolean.TRUE.equals(supported);
        } catch (Exception ignored) {
            webSpeechSupported = false;
        }
        return webSpeechSupported;
    }

    private boolean isWindowsHost() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win");
    }

    private void commitModuleProgressIfNeeded(boolean completedReading) {
        if (moduleProgressCommitted || activeModuleId == null || activeModuleProgress == null || sentences.isEmpty() || !isStudentUser()) {
            return;
        }

        int current = Math.max(0, activeModuleProgress);
        if (current >= 100) {
            moduleProgressCommitted = true;
            return;
        }

        int target;
        if (completedReading) {
            target = 99;
        } else {
            int heardSentences = Math.max(0, Math.min(sentenceIndex, sentences.size()));
            double ratio = (heardSentences * 1.0) / sentences.size();
            target = (int) Math.floor(ratio * 99.0);
        }

        target = Math.max(current, Math.min(target, 99));
        if (target <= current) {
            moduleProgressCommitted = true;
            return;
        }

        try {
            User user = UserContext.getInstance().getCurrentUser();
            if (user != null) {
                user.getDbConnector().updateModuleProgress(user.getId(), activeModuleId, target);
            }
        } catch (Exception ignored) {
        }

        moduleProgressCommitted = true;
    }

    private static List<String> splitSentences(String text) {
        String normalized = text == null ? "" : text.replace('\r', ' ').replace('\n', ' ').trim();
        if (normalized.isEmpty()) {
            return List.of();
        }

        String[] raw = normalized.split("(?<=[.!?])\\s+");
        List<String> out = new ArrayList<>();
        for (String s : raw) {
            String clean = s == null ? "" : s.trim();
            if (!clean.isEmpty()) {
                out.add(clean);
            }
        }
        if (out.isEmpty()) {
            out.add(normalized);
        }
        return out;
    }

    private Object runOnFxAndGet(FxSupplier supplier) throws Exception {
        final Object[] box = new Object[1];
        final Exception[] error = new Exception[1];
        final Object lock = new Object();

        Platform.runLater(() -> {
            synchronized (lock) {
                try {
                    box[0] = supplier.get();
                } catch (Exception e) {
                    error[0] = e;
                }
                lock.notifyAll();
            }
        });

        synchronized (lock) {
            lock.wait(4000);
        }

        if (error[0] != null) {
            throw error[0];
        }
        return box[0];
    }

    @FunctionalInterface
    private interface FxSupplier {
        Object get() throws Exception;
    }
}


