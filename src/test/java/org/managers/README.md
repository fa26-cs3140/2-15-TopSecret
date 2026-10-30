Due to the compilation errors caused by this refactoring, run this command to isolate the managers tests specifically:

```
./gradlew clean test -PmanagersOnly --tests "org.managers.*"
```

Replace `*` with a test class (in `/managers/`) to run tests on a specific manager.