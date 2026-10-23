# Top Secret

Top Secret is a Java command-line application for managing and searching classified mission briefs. The application imports mission data from a tab-separated values (TSV) file into SQLite, provides an interactive terminal menu for viewing and searching briefs, and includes reusable file, cipher, and credential-management components.

## Features

- List all available mission briefs.
- Browse and read briefs, agents, and facilities through the interactive menu.
- Allow administrators to add briefs, agents, and facilities.
- Restrict record creation and audit-log review to administrator accounts.
- Record read and create operations in a persistent audit log.
- Store user account metadata in SQLite and store each user's encrypted password separately.
- Import tabular mission data into SQLite when the database is initialized.
- Read and decipher encrypted data files using character mappings from `data/key.txt`.
- Create users, authenticate users, and change passwords through the account-management layer.
- Run unit tests, integration tests, and JaCoCo coverage reports with Gradle.

## Technology

- Java
- Gradle 9.6
- SQLite
- SQLite JDBC driver
- JUnit 6
- Mockito

## Project Structure

```text
.
├── data/                         Runtime data files
│   ├── mission_briefs.tsv        Source mission data
│   ├── mission_briefs.db         SQLite database
│   ├── key.txt                   Cipher character mappings
│   ├── *.txt                     Plaintext sample data
│   └── *.cip                     Encrypted credential/data files
├── docs/                         Design notes, schemas, plans, and UML artifacts
├── src/main/java/org/example/    Application and persistence services
├── src/main/java/org/model/      Brief, agent, facility, and mission models
├── src/test/java/org/example/    Unit and integration tests
├── build.gradle.kts              Gradle build configuration
├── gradlew                       Gradle wrapper for Unix-like systems
└── gradlew.bat                   Gradle wrapper for Windows
```

## Architecture

The application starts in `TopSecret`, initializes persistence and authentication services, and launches `UserInterface` for the authenticated user.

| Component | Responsibility |
| --- | --- |
| `TopSecret` | Application entry point and startup orchestration |
| `ProgramControl` | Initializes the database, account services, and application flow |
| `UserInterface` | Authenticated interactive menu for briefs, agents, facilities, and audit logs |
| `AgencyDataService` | Data-access contract for retrieving and creating agency records |
| `DatabaseManager` | SQLite persistence for briefs, agents, and facilities |
| `UserAccountManager` | Validates users, authenticates accounts, and manages encrypted passwords |
| `UserAccountStore` / `SqliteUserAccountStore` | Persist and retrieve account metadata |
| `AuditLogManager` | Validates and records content operations |
| `AuditLogStore` / `SqliteAuditLogStore` | Persist and retrieve audit entries |
| `MissionSearch` | Provides mission-text search support |
| `SearchResult` | Carries search matches and status messages |
| `FileHandler` | Reads, writes, and lists files under the data directory |
| `CipherKey` | Validates and stores substitution-cipher mappings |
| `Cipher` | Encrypts and decrypts text using a `CipherKey` |
| `CredentialManager` | Defines the credential-management contract |
| `CredentialManagerLibrary` | Stores and verifies encrypted credentials |
| `DebugHelper` | Provides lightweight diagnostic output |

## Data Model

The SQLite database now supports agency records, user accounts, and audit events:

```sql
facilities (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    abbreviation TEXT NOT NULL
)

agents (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    date_of_birth TEXT NOT NULL,
    date_of_death TEXT,
    notes TEXT,
    facility_id INTEGER NOT NULL REFERENCES facilities(id)
)

briefs (
    id INTEGER PRIMARY KEY,
    title TEXT NOT NULL,
    date TEXT NOT NULL,
    text TEXT NOT NULL,
    mission_id INTEGER UNIQUE
)
```

The `Agent`, `Brief`, `Facility`, and `Mission` classes in `src/main/java/org/model/` represent database records in the application layer. User passwords are not stored in SQLite; each account uses a separate ciphered credential file.

Audit entries record the username, action, content type, content identifier, and timestamp for operations such as reading or creating records.

## User Roles and Menu

After authentication, users can:

- List and read briefs, agents, and facilities.
- Add records when they are administrators.
- Review the audit log when they are administrators.
- Exit the application.

The UI uses record IDs when reading or creating records. Agents reference an existing facility, and model validation occurs before records are inserted into SQLite.

## Requirements

- Java 17 or newer
- A shell capable of running the Gradle wrapper

The Gradle wrapper downloads the configured Gradle distribution and project dependencies as needed.

## Build and Run

From the project root, make the Gradle wrapper executable if necessary:

```bash
chmod +x gradlew
```

Run the application with:

```bash
./gradlew run
```

The current interactive menu provides:

```text
1. Briefs
2. Agents
3. Facilities
4. Review audit log       (administrators only)
0. Exit
```

To build the application:

```bash
./gradlew build
```

The packaged application can be generated with:

```bash
./gradlew jar
```

## Testing

Run the full test suite with:

```bash
./gradlew test
```

Run isolated unit tests and persistence/cross-component integration tests separately:

```bash
./gradlew unitTest
./gradlew integrationTest
```

Generate JaCoCo coverage reports with:

```bash
./gradlew jacocoTestReport
./gradlew jacocoUnitTestReport
```

Tests cover cipher and key validation, file handling, account management, audit logging, database operations, model validation, program startup, and UI behavior.

## Cipher and Credential Files

`data/key.txt` contains two character-mapping lines:

1. The original character set.
2. The corresponding encrypted character set.

Both lines must have the same length, and characters must be unique within each line. `CredentialManagerLibrary` uses the cipher for the legacy credential flow, while `UserAccountManager` writes per-user files named from the username's encoded bytes.

These files contain application data for local development and testing. Do not use real passwords or sensitive credentials in the repository.

## Documentation and UML

The `docs/` directory contains:

- Class design notes and UML text files.
- Database and audit-log schemas.
- User-account, data-model, and UML artifacts.
- Homework work plans and test plans.

The project-wide visual UML diagram is available at [`docs/UML Diagram.png`](docs/UML%20Diagram.png). Text UML and ERD descriptions are also available in `docs/`.

The Java source and the documentation files should be kept in sync when class responsibilities or data flows change.

## Team

The project documentation identifies the following responsibilities:

- Zaid Member A
- Dillon Member B
- Mike Ly Member C
- Vrisag Desinani Member D
- Credential-management ownership is documented as pending assignment in `docs/team.txt`
