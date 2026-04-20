package Services;

import DatabaseController.dbConnector;
import OtherComponents.Assessment;
import OtherComponents.LearningModule;
import UserFactory.User;
import javafx.application.Platform;

import java.util.*;
import java.util.concurrent.*;

/**
 * Service to periodically check for database updates and refresh UI.
 * Polls for new messages, assessment completions, and other changes.
 * Runs on a background thread and notifies listeners on the JavaFX thread.
 */
public class UIRefreshService {
    private static UIRefreshService instance;
    private ScheduledExecutorService scheduler;
    private final dbConnector db;
    private final Set<UIRefreshListener> listeners;
    private User currentUser;
    private int lastMessageCount = 0;
    private final Map<Integer, Boolean> assessmentStatusCache = new HashMap<>();
    private final Map<Integer, Integer> moduleStateCache = new HashMap<>();
    private boolean isPolling = false;

    private UIRefreshService() {
        this.db = new dbConnector();
        this.listeners = Collections.synchronizedSet(new HashSet<>());
    }

    public static UIRefreshService getInstance() {
        if (instance == null) {
            instance = new UIRefreshService();
        }
        return instance;
    }

    /**
     * Start periodic polling for updates. Call this after user logs in.
     * Safe to call multiple times - only starts once.
     * @param user The current logged-in user
     */
    public synchronized void startPolling(User user) {
        if (isPolling) return; // Already polling

        this.currentUser = user;
        this.lastMessageCount = 0;
        this.assessmentStatusCache.clear();
        this.moduleStateCache.clear();

        // Initial cache
        try {
            var inbox = user.checkInbox();
            if (inbox != null) {
                this.lastMessageCount = inbox.size();
            }
            List<Assessment> assessments = fetchAssessmentsFromDb();
            if (assessments != null) {
                for (var assessment : assessments) {
                    assessmentStatusCache.put(assessment.getAssessmentID(), assessment.isCompleted());
                }
            }
            List<LearningModule> modules = fetchModulesFromDb();
            if (modules != null) {
                for (LearningModule module : modules) {
                    moduleStateCache.put(module.getModuleID(), moduleSignature(module));
                }
            }
        } catch (Exception e) {
            System.err.println("Error initializing polling cache: " + e.getMessage());
        }

        // Create scheduler if not exists
        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = Executors.newScheduledThreadPool(1, r -> {
                Thread t = new Thread(r, "UI-Refresh-Thread");
                t.setDaemon(true);
                t.setPriority(Thread.MIN_PRIORITY); // Low priority to avoid impacting UI
                return t;
            });
        }

        // Schedule polling every 3 seconds
        scheduler.scheduleAtFixedRate(this::pollForUpdates, 3, 3, TimeUnit.SECONDS);
        isPolling = true;
    }

    /**
     * Stop polling for updates. Call this on logout.
     */
    public synchronized void stopPolling() {
        if (!isPolling) return;

        this.currentUser = null;
        this.lastMessageCount = 0;
        this.assessmentStatusCache.clear();
        this.moduleStateCache.clear();
        listeners.clear();

        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        isPolling = false;
    }

    /**
     * Check database for changes and notify listeners.
     * Runs on background thread; notifications sent to JavaFX thread.
     */
    private void pollForUpdates() {
        if (currentUser == null) return;

        try {
            // Check for new messages (lightweight - just count)
            try {
                int newMessageCount = 0;
                var inbox = currentUser.checkInbox();
                if (inbox != null) {
                    newMessageCount = inbox.size();
                }

                if (newMessageCount > lastMessageCount) {
                    lastMessageCount = newMessageCount;
                    notifyListeners("MESSAGES_UPDATED", newMessageCount);
                }
            } catch (Exception e) {
                // Silently ignore - transient error
            }

            // Check for assessment updates (lightweight - just completion status)
            try {
                List<Assessment> assessments = fetchAssessmentsFromDb();
                if (assessments != null) {
                    for (var assessment : assessments) {
                        int assessmentId = assessment.getAssessmentID();
                        boolean newStatus = assessment.isCompleted();
                        Boolean oldStatus = assessmentStatusCache.get(assessmentId);

                        if (oldStatus != null && oldStatus != newStatus) {
                            notifyListeners("ASSESSMENT_UPDATED", assessmentId);
                        }
                        assessmentStatusCache.put(assessmentId, newStatus);
                    }
                }
            } catch (Exception e) {
                // Silently ignore - transient error
            }

            // Check for module updates (new module, removed module, or changed module details/progress)
            try {
                List<LearningModule> modules = fetchModulesFromDb();
                if (modules != null) {
                    Map<Integer, Integer> latestState = new HashMap<>();
                    boolean changed = modules.size() != moduleStateCache.size();

                    for (LearningModule module : modules) {
                        int moduleId = module.getModuleID();
                        int signature = moduleSignature(module);
                        latestState.put(moduleId, signature);

                        Integer previousSignature = moduleStateCache.get(moduleId);
                        if (previousSignature == null || previousSignature != signature) {
                            changed = true;
                        }
                    }

                    if (changed) {
                        moduleStateCache.clear();
                        moduleStateCache.putAll(latestState);
                        notifyListeners("MODULES_UPDATED", modules.size());
                    }
                }
            } catch (Exception e) {
                // Silently ignore - transient error
            }
        } catch (Exception e) {
            System.err.println("Error during UI polling: " + e.getMessage());
        }
    }

    private List<Assessment> fetchAssessmentsFromDb() {
        if (currentUser == null) {
            return Collections.emptyList();
        }

        String acctType = currentUser.getAcctType();
        if ("Student".equals(acctType)) {
            return db.searchAssessmentDB(currentUser.getId(), acctType);
        }
        if ("Educator".equals(acctType)) {
            // Educator's no-arg overload handles this safely
            return currentUser.getAssessmentResults();
        }
        // Counselor, Parent, Employer, University, Admin, etc.
        // have no assessments to poll — avoid calling varargs method with no args.
        return Collections.emptyList();
    }

    private List<LearningModule> fetchModulesFromDb() {
        if (currentUser == null) {
            return Collections.emptyList();
        }

        String acctType = currentUser.getAcctType();
        if (!"Student".equals(acctType) && !"Educator".equals(acctType)) {
            return Collections.emptyList();
        }
        return db.searchModulesDB(currentUser.getId(), acctType);
    }

    private int moduleSignature(LearningModule module) {
        return Objects.hash(
                module.getProgress(),
                module.getSubject(),
                module.getLearningPath(),
                module.getContent()
        );
    }

    /**
     * Register a listener for UI updates.
     */
    public void addListener(UIRefreshListener listener) {
        listeners.add(listener);
    }

    /**
     * Unregister a listener.
     */
    public void removeListener(UIRefreshListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notify all listeners of an update on the JavaFX thread.
     * Safe to call from any thread.
     */
    private void notifyListeners(String updateType, Object data) {
        Platform.runLater(() -> {
            for (UIRefreshListener listener : new ArrayList<>(listeners)) {
                try {
                    listener.onUIRefresh(updateType, data);
                } catch (Exception e) {
                    System.err.println("Error in UI refresh listener: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Listener interface for UI refresh events.
     */
    public interface UIRefreshListener {
        /**
         * Called when the UI should refresh.
         * Executed on the JavaFX thread.
         * @param updateType Type of update (e.g., "MESSAGES_UPDATED", "ASSESSMENT_UPDATED")
         * @param data Associated data for the update
         */
        void onUIRefresh(String updateType, Object data);
    }
}



