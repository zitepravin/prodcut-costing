# Backend build

Requires **JDK 21** (full JDK with `javac`, not a JRE-only install).

```bash
export JAVA_HOME=/path/to/jdk-21
export PATH=$JAVA_HOME/bin:$PATH
javac -version   # must print 21.x
mvn clean test
```

If `mvn` fails with:

```text
Fatal error compiling: error: release version 21 not supported
```

Maven is bound to an older `javac` (often system Java 17). Fix by putting JDK 21 first on `PATH` or:

```bash
./scripts/build.sh clean test
```
