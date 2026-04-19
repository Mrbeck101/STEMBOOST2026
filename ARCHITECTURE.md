# STEMBOOST System Architecture

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     JavaFX Application                       │
│                      (MainApp.java)                          │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
        ┌─────────────────────────────┐
        │      SceneRouter            │
        │  (Route management)         │
        └────────────────┬────────────┘
                         │
        ┌────────────────┼────────────────┐
        ▼                ▼                ▼
    ┌────────┐      ┌────────┐      ┌────────┐
    │ Login  │  ──▶ │UserCtx │ ◀── │Dashbrd │
    │ View   │      │(Store) │      │ Views  │
    └────────┘      └────────┘      └────────┘
        │                                 │
        └─────────────────┬───────────────┘
                          ▼
            ┌──────────────────────────┐
            │   Supporting Views       │
            │  (Modules, Assessments)  │
            └──────────────────────────┘
                          │
                          ▼
            ┌──────────────────────────┐
            │   Database Layer         │
            │   (dbConnector)          │
            └──────────────────────────┘
```

## Component Relationships

### User Creation & Storage Flow

```
LoginView
    │
    ├─ Get email & password
    │
    ├─ Call AuthService.login()
    │
    ├─ If successful, create user instance
    │   │
    │   └─ switch(accountType)
    │       ├─ "Student" ──▶ new Student(id)
    │       ├─ "Educator" ──▶ new Educator(id)
    │       ├─ "Counselor" ──▶ new Counselor(id)
    │       ├─ "Parent" ──▶ new Parent(id)
    │       ├─ "Employer" ──▶ new Employer(id)
    │       └─ "University" ──▶ new University(id)
    │
    ├─ Store in UserContext.getInstance().setCurrentUser(user)
    │
    └─ Route to dashboard via SceneRouter.goToDashboard(id, type)
```

### View Access to User

```
Any View (Dashboard, Module, Assessment, etc)
    │
    ├─ On scene creation
    │  │
    │  ├─ Get user: User user = UserContext.getInstance().getCurrentUser()
    │  │
    │  ├─ Check null: if (user == null) return LoginView.create(router)
    │  │
    │  └─ Use user data throughout view
    │
    └─ User remains accessible during entire session
```

## Class Hierarchy

```
User (Abstract)
├── Student
├── Educator
├── Counselor
├── Parent
├── Employer
└── University

Assessment
├── getAssessmentID()
├── getLearningPath()
├── getGrade()
├── getModuleID()
└── getContent()

LearningModule
├── getModuleID()
├── getModProgress()
├── getEducatorID()
├── getLearningPath()
├── getContent()
└── getSubject()

JobProgram
├── getEmployerID()
├── getPreferredLearningPath()
├── getModRequired()
├── getAssessmentRequired()
├── getDescription()
└── getJobType()

Message
├── getSenderID()
├── getReceiverID()
├── getSubject()
├── getContent()
└── getConvoID()
```

## Database Integration Points

```
DatabaseController
│
├─ addUser()
├─ searchUserDB()
├─ searchAccountDB()
├─ searchModulesDB()
├─ searchAssessmentDB()
├─ searchJobProgramsDB()
├─ searchStudentsCounselorDB()
├─ searchGuardedStudentsDB()
├─ searchEnrolledStudentsDB()
├─ searchMessagesDB()
├─ addMessage()
├─ addModuleDB()
├─ addStudentToModule()
├─ addAssessmentDB()
├─ addJobProgram()
├─ updateModuleDB()
├─ updateModuleProgress()
├─ updateAssessmentGrade()
├─ updateJobProgram()
├─ findAvailableCounselor()
└─ assignStudent()
```

## View Hierarchy

```
LoginView
    │
    └─▶ SceneRouter
        │
        ├─▶ StudentDashBoardView
        │   └─▶ (Tabs)
        │       ├─ Dashboard
        │       ├─ Modules ──▶ ModuleView
        │       ├─ Assessments ──▶ AssessmentView
        │       └─ Inbox ──▶ InboxView
        │
        ├─▶ EducatorDashBoardView
        │   └─▶ (Tabs)
        │       ├─ Dashboard
        │       ├─ Modules ──▶ ModuleView
        │       ├─ Create ──▶ CreateModuleView
        │       └─ Inbox ──▶ InboxView
        │
        ├─▶ CounselorDashBoardView
        │   └─▶ (Tabs)
        │       ├─ Dashboard
        │       ├─ Students
        │       └─ Inbox ──▶ InboxView
        │
        ├─▶ ParentDashBoardView
        │   └─▶ (Tabs)
        │       ├─ Dashboard
        │       ├─ Children
        │       └─ Inbox ──▶ InboxView
        │
        ├─▶ EmployerDashBoardView
        │   └─▶ (Tabs)
        │       ├─ Dashboard
        │       ├─ Jobs
        │       └─ Inbox ──▶ InboxView
        │
        ├─▶ UniversityDashboardView
        │   └─▶ (Tabs)
        │       ├─ Dashboard
        │       ├─ Students
        │       └─ Inbox ──▶ InboxView
        │
        └─▶ RegisterView
```

## Data Flow Diagram

```
┌──────────────────────────────────────────────────┐
│             User Registration                    │
└──────────────┬───────────────────────────────────┘
               │
               ▼
        ┌──────────────┐
        │ RegisterView │
        └──────┬───────┘
               │
               ▼
        ┌────────────────┐
        │ AuthService    │
        │  .register()   │
        └──────┬─────────┘
               │
               ▼
        ┌─────────────────┐
        │  dbConnector    │
        │  .addUser()     │
        └──────┬──────────┘
               │
               ▼
        ┌──────────────────┐
        │  Database        │
        │ (users, accounts)│
        └──────────────────┘

                    ▼▼▼

┌──────────────────────────────────────────────────┐
│                User Login                        │
└──────────────┬───────────────────────────────────┘
               │
               ▼
        ┌──────────────┐
        │  LoginView   │
        └──────┬───────┘
               │
               ▼
        ┌────────────────┐
        │ AuthService    │
        │  .login()      │
        └──────┬─────────┘
               │
               ▼
        ┌─────────────────┐
        │  dbConnector    │
        │  .searchUserDB()│
        └──────┬──────────┘
               │
               ▼
        ┌──────────────────┐
        │  Database        │
        │ (credentials)    │
        └──────┬───────────┘
               │
               ▼
        ┌────────────────────────┐
        │ Create User Instance   │
        │ (Student/Educator/etc) │
        └──────┬─────────────────┘
               │
               ▼
        ┌──────────────────────────┐
        │ Store in UserContext     │
        │ (Persistent Access)      │
        └──────┬───────────────────┘
               │
               ▼
        ┌──────────────────────────┐
        │ Route to Dashboard       │
        │ (Role-specific)          │
        └──────────────────────────┘
```

## Theme Application

```
Application Start
    │
    ├─ Each View onCreate
    │  │
    │  └─ Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet())
    │
    └─ Additional CSS (future)
       └─ scene.getStylesheets().add(css_file)
```

## Accessibility Layer

```
User Interface
    │
    ├─ Visual Elements
    │  ├─ Colors (#58a6ff, #238636, etc)
    │  ├─ Fonts (18-20px titles, 12-14px body)
    │  ├─ Icons (future)
    │  └─ Layout (high contrast, proper spacing)
    │
    └─ Accessibility Features
       ├─ setAccessibleText() - Screen reader labels
       ├─ setAccessibleRole() - Semantic structure
       ├─ Keyboard Navigation - Tab, Arrow, Enter
       ├─ Color + Text - No color-only indicators
       ├─ Large Fonts - 12-20px minimum
       └─ Focus Indicators - Clear visual focus
```

## Session Management

```
Application Lifecycle
    │
    ├─ Start
    │  └─ UserContext = null
    │
    ├─ Login
    │  └─ UserContext = User instance
    │
    ├─ Navigation
    │  └─ UserContext = Same instance (persistent)
    │
    ├─ Logout
    │  └─ UserContext = null
    │
    └─ End
       └─ UserContext garbage collected
```

## Error Handling Flow

```
View Operation
    │
    ├─ Check user logged in
    │  └─ if (null) → return LoginView
    │
    ├─ Validate input
    │  └─ if (invalid) → showAlert()
    │
    ├─ Database operation
    │  └─ if (error) → showAlert() + log
    │
    └─ Success
       └─ showAlert() + navigate
```

## Deployment Architecture

```
JAR File
├─ main.demo.MainApp (Entry point)
├─ UI package
│  ├─ LoginView
│  ├─ UserContext
│  ├─ SceneRouter
│  ├─ Dashboard views (6)
│  ├─ Supporting views (5)
│  └─ Style resources
├─ Services package
│  └─ AuthService
├─ UserFactory package
│  ├─ User
│  └─ Subclasses (6)
├─ DatabaseController package
│  ├─ dbConnector
│  └─ CryptoUtil
└─ OtherComponents package
   ├─ Assessment
   ├─ LearningModule
   ├─ JobProgram
   ├─ Message
   └─ etc.
```

## Performance Considerations

```
Memory Usage
├─ UserContext - Single instance (minimal)
├─ Current Scene - One scene object
├─ Cached Views - Optional (future optimization)
└─ Database Connections - Managed by dbConnector

CPU Usage
├─ Scene rendering - Standard JavaFX
├─ Theme application - Once per view
├─ Database queries - On demand
└─ Layout calculations - Standard JavaFX
```

---

**Architecture Version:** 1.0
**Last Updated:** April 19, 2026
**Status:** Complete & Documented

