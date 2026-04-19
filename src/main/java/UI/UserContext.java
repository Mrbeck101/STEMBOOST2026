package UI;

import UserFactory.*;

/**
 * Singleton class to maintain persistent user instance across all views.
 * This ensures the logged-in user remains available throughout the application.
 */
public class UserContext {

    private static UserContext instance;
    private User currentUser;

    private UserContext() {
    }

    public static UserContext getInstance() {
        if (instance == null) {
            instance = new UserContext();
        }
        return instance;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return this.currentUser;
    }

    public boolean isUserLoggedIn() {
        return currentUser != null;
    }

    public void logout() {
        this.currentUser = null;
    }

    public String getUserType() {
        if (currentUser != null) {
            return currentUser.getAcctType();
        }
        return null;
    }
}

