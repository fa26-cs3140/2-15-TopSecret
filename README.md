# Top Secret

Top Secret is a Java command-line application for managing and searching classified mission briefs. The application imports mission data from a tab-separated values (TSV) file into SQLite, provides an interactive terminal menu for viewing and searching briefs, and includes reusable file, cipher, and credential-management components.

## Features

- List all available mission briefs.
- Read an individual mission brief by its displayed number.
- Search mission brief text using a case-insensitive SQLite `LIKE` query.
- Import `data/mission_briefs.tsv` into `data/mission_briefs.db` on first launch.
- Read and decipher encrypted data files using character mappings from `data/key.txt`.
- Create, verify, and change encrypted credentials through the credential-management library.
- Run unit and integration tests with Gradle.

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
│   ├── mission_briefs.db         Generated SQLite database
│   ├── key.txt                   Cipher character mappings
│   ├── *.txt                     Plaintext sample data
│   └── *.cip                     Encrypted sample data
├── docs/                         Design notes, schemas, and UML text
├── src/main/java/org/example/    Application source code
├── src/test/java/org/example/    Unit and integration tests
├── build.gradle.kts              Gradle build configuration
├── gradlew                       Gradle wrapper for Unix-like systems
└── gradlew.bat                   Gradle wrapper for Windows
```

## Architecture

The application starts in `TopSecret`, which creates the main collaborators and delegates startup to `ProgramControl`.

| Component | Responsibility |
| --- | --- |
| `TopSecret` | Application entry point and top-level error handling |
| `ProgramControl` | Initializes required files/database state and starts the UI |
| `UserInterface` | Interactive terminal menu and user input/output |
| `MissionSearch` | Validates search terms and formats search outcomes |
| `SearchResult` | Carries matching records and user-facing status messages |
| `DatabaseManager` | Imports TSV data and queries the SQLite mission database |
| `FileHandler` | Reads, writes, and lists files under the data directory |
| `CipherKey` | Validates and stores substitution-cipher mappings |
| `Cipher` | Encrypts and decrypts text using a `CipherKey` |
| `CredentialManager` | Defines the credential-management contract |
| `CredentialManagerLibrary` | Stores and verifies encrypted credentials |
| `DebugHelper` | Provides lightweight diagnostic output |

## Data Model

Mission briefs are stored in the `mission_briefs` SQLite table:

```sql
CREATE TABLE mission_briefs (
    Title TEXT,
    Date DATE,
    Text TEXT
);
```

The source TSV file is expected to contain a header followed by tab-separated rows in this order:

```text
Title    Date    Text
```

On startup, `ProgramControl` creates `data/mission_briefs.db` from `data/mission_briefs.tsv` when the database table does not already exist.

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

The interactive menu provides these options:

```text
1. List available mission briefs
2. Read a mission brief
3. Search mission briefs
4. Exit
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

Tests cover cipher and key validation, file handling, credential management, database operations, mission searching, program startup, and UI behavior.

## Cipher and Credential Files

`data/key.txt` contains two character-mapping lines:

1. The original character set.
2. The corresponding encrypted character set.

Both lines must have the same length, and characters must be unique within each line. `CredentialManagerLibrary` uses the cipher to store credentials in `data/credentials.cip`.

These files contain application data for local development and testing. Do not use real passwords or sensitive credentials in the repository.

## Documentation and UML

The `docs/` directory contains:

- Class design notes and UML text files.
- Database schema documentation.
- Planning and team documentation.

The Java source and the documentation files should be kept in sync when class responsibilities or data flows change.

## Team

The project documentation identifies the following responsibilities:

- Zaid Al Falah — Database / SQLite
- Mike Ly — Mission Search
- Vrisag Desinani — User Interface
- Credential-management ownership is documented as pending assignment in `docs/team.txt`
