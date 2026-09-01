# TikTokSignatrueForJava

A **Java port** of the local **Go** implementation of the TikTok signing
functionality, which itself was ported from the Python
[SignerPy](https://github.com/is-L7N/SignerPy) project.

```
SignerPy (Python)
        ↓
Go port (this repository's parent)
        ↓
TikTokSignatrueForJava (Java port — this project)
```

This is an independent, behavior-compatible port. It is **not** the original
SignerPy project; the original Python project and its authors retain their
applicable rights and licenses. The Java code re-implements every algorithm from
scratch in Java — it does **not** call the Go binary or the Python SignerPy at
runtime. The Go implementation is the reference/oracle only.

## Author / Maintainer

S1

GitHub:
github.com/JokerPython3

## Attribution

This Java project is a port of the local Go implementation
(`SignerPyInGoLang/tiktok-signature`), which is itself based on the original
SignerPy Python project authored by **L7N**
(`https://github.com/is-L7N/SignerPy`). The Go port and this Java port do not
claim ownership of the original SignerPy project. Upstream attribution and
licensing are preserved.

## What This Project Does

The port re-implements the signing pipeline so that a Java program can produce
the same signed header values as the Go reference. It exposes a single public
entry point, `Signer.sign(...)`, which returns the six signed header values:

| Header | Role |
|---|---|
| `X-Argus` | Application-level request signature (SM3, Simon, ProtoBuf, AES-128-CBC). |
| `X-Ladon` | Device/request signature (AES-128-CBC with an MD5-derived key). |
| `X-Gorgon` | Anti-abuse signature; variants `8404` (V1), `8402` (V2), `4404`/`0` (V3). |
| `X-Khronos` | Unix timestamp (seconds) linked to the Gorgon signature. |
| `X-TT-Request-Ticket` | Unix timestamp (milliseconds) linked to the Gorgon signature. |
| `X-SS-Stub` | Uppercase MD5 of the URL-encoded request body. |

Each value is produced by porting the corresponding functionality from the Go
reference implementation and verified byte-for-byte against deterministic test
vectors (the same `testdata/vector_*.json` fixtures used by the Go project).

## Requirements

- **Java 21+** (verified with `javac`/`java` 21.0.10). The code uses only the
  Java standard library — **no external runtime dependencies**.
- **Maven 3.6+** *(optional)* — `./build.sh` builds with plain `javac`; a Maven
  build (`pom.xml`) is also provided for IDE integration and standard packaging.
- **Linux** — the example targets a Linux Android-emulation environment, but the
  library itself is platform independent.

### Build

Zero-dependency build with plain `javac` (no Maven, no downloaded jars):

```bash
cd TikTokSignatrueForJava
./build.sh
```

`build.sh` compiles the library, the example, and the tests into `out/` and runs
the test suite. Maven/Gradle builds (also supported via `pom.xml`) use the real
JUnit 4 test dependency declared there; all other sources are dependency-free.

Alternatively, compile the test suite manually:

```bash
javac -d out/stub  src/test/stub/org/junit/Test.java
javac -d out/main   src/main/java/tiktoksignature/*.java src/main/java/Example.java
javac -cp "out/main:out/stub" -d out/test src/test/java/tiktoksignature/*.java
java  -cp "out/main:out/test" tiktoksignature.TestRunner
```

With Maven installed, an equivalent standard build is available:

```bash
mvn test        # build + run the suite (JUnit 4 via Surefire)
mvn package     # build target/tiktoksignature-java-<version>.jar
```

### Test

```bash
./build.sh          # builds + runs the full test suite (no deps required)
mvn test            # same suite via Maven + JUnit 4
```

The test suite covers the individual primitives (SM3, Simon, ProtoBuf varint,
URL encoding/decoding, parsing, Gorgon helpers, Ladon padding) and all nine
golden vectors (V1/V2/V3 gorgon variants, empty bodies, UTF-8, cookies, and
multi-block AES/SM3 inputs), comparing every one of the six signatures
byte-for-byte against the Go reference output.

### Run Example.java

```bash
./build.sh                      # compiles Example into out/main
java -cp out/main Example
```

Or after a Maven build:

```bash
mvn -q -DskipTests compile
java -cp target/classes Example
```

It reads an email address from stdin, builds the request, prints the six signing
headers, and (like `Example.go`) sends the request and prints the HTTP response.

## VS Code

Open this folder and install the **Extension Pack for Java** (Microsoft). The
`.vscode/` folder ships ready-made build and launch configurations:

- `Run Example (Java)` — run `Example` (mainClass `Example`, project
  `tiktoksignature-java`). Uses `integratedTerminal` so the program can read the
  email from stdin; a `compile` pre-task runs `mvn -q -DskipTests compile`.
- `Debug Example (Java)` — same, but sets `DEBUG=1` so the full (sanitized)
  request/response is printed.
- `Run Tests (Java, custom runner)` — runs `tiktoksignature.TestRunner`
  (prints the plain-text PASS/FAIL summary; failures show in `integratedTerminal`)
  — convenient when you want the zero-dependency runner inside the editor.

Alternatively use the **Java Test Runner** via `mvn test`: right-click any of
`UnitTest` / `GoldenVectorTest` / `RequestCompareTest` in the test explorer once
Maven has imported `pom.xml`. The test methods are annotated with JUnit 4 `@Test`.

> Note: the **Go** sibling project (`tiktok-signature/`) is only a behavioral
> reference for this Java port; do not run it as part of this Maven project.

## API Usage

```java
import tiktoksignature.*;

List<Param> params = List.of(
    new Param("device_id", "7442000000000000001"),
    new Param("version_name", "41.9.3"),
    new Param("aid", "1233")
);
List<Param> data = List.of(
    new Param("type", "3736")
);

Signatures sig = Signer.sign(
    SignRequest.builder().params(params).data(data).build(),
    SignOptions.builder().aid(1233).version(8404).build()
);

// send headers:
// X-Argus:             sig.xArgus
// X-Ladon:             sig.xLadon
// X-Gorgon:            sig.xGorgon
// X-Khronos:           sig.xKhronos
// X-TT-Request-Ticket: sig.xTTRequestTicket
// X-SS-Stub:           sig.xSSStub
```

Note: do **not** use Java's `URLEncoder` for signing-sensitive bodies — encode
query/form bodies with `UrlEncode.encodeParams(...)`, which reproduces the
urllib-compatible encoding in insertion order and is required for `X-SS-Stub`
parity.

### Input format

- `SignRequest.Params` — ordered query parameters.
- `SignRequest.Data` — ordered form-body parameters (used to compute
  `X-SS-Stub`).
- `SignRequest.Payload` — alias of `Data` (used when `Data` is empty).
- `SignRequest.Cookie` — optional ordered cookie pairs (fed into X-Gorgon).
- `SignRequest.RawBody` — optional pre-encoded body string used verbatim.
- `SignRequest.URL` — used only when `Params` is null (split at `?`).

### Deterministic testing

`SignOptions` exposes randomness overrides so tests are reproducible:

- `timestamp` — Unix epoch **seconds** (drives X-Khronos in seconds and
  X-TT-Request-Ticket in milliseconds).
- `gorgonByte3` / `gorgonByte7` — pin the two random bytes of X-Gorgon.
- `argusRand` — pins Argus field 3 (`randint(0, 0x7FFFFFFF)`).
- `ladonRandom` — pins the 4 random bytes of X-Ladon.

When any override is null/absent the value is generated with `SecureRandom`.

## Signatures

### X-Argus

Application-level request signature built by the Argus pipeline. It builds a
ProtoBuf message from the request query, body stub, timestamp and
device/version metadata, hashes relevant parts with SM3, encrypts the structure
with the Simon cipher and AES-128-CBC, and base64-encodes the result.

### X-Ladon

Device/request signature. Derives an AES-128-CBC key from
`MD5(random_bytes || aid)`, encrypts a `khronos-license_id-aid` payload with a
custom Feistel-like round function, and prepends the random bytes before
base64-encoding.

### X-Gorgon

Anti-abuse signature. The port implements the `8404` (V1), `8402` (V2) and
`4404`/`0` (V3) variants, building the signature from MD5 digests of the query,
body and cookies plus the timestamp.

### X-Khronos

Unix timestamp (seconds) used to seed the Gorgon signature.

### X-TT-Request-Ticket

The same signer timestamp expressed in milliseconds, tied to the Gorgon
signature.

### X-SS-Stub

Uppercase MD5 of the URL-encoded request body.

## Project Structure

```
TikTokSignatrueForJava/
├── pom.xml                      # optional Maven build (JUnit 4, test-scope only)
├── build.sh                     # zero-dependency build + test script (plain javac)
├── README.md                    # this document
├── .gitignore
├── .vscode/
│   ├── launch.json              # Run / Debug / Test configurations
│   └── tasks.json               # compile + test tasks (Maven)
├── src/main/java/
│   ├── Example.java             # runnable Java example (default package)
│   └── tiktoksignature/
│       ├── Signer.java          # public Signer.sign() entry point
│       ├── SignRequest.java     # request input model
│       ├── SignOptions.java     # options + deterministic randomness overrides
│       ├── Signatures.java      # six signed header values
│       ├── Param.java           # ordered key/value pair
│       ├── Argus.java           # X-Argus (SM3 + Simon + ProtoBuf + AES-128-CBC)
│       ├── Ladon.java           # X-Ladon (custom round function + AES key derivation)
│       ├── Gorgon.java          # X-Gorgon variants 8404 / 8402 / 4404
│       ├── Simon.java           # Simon 64/256 cipher
│       ├── SM3.java             # SM3 hash
│       ├── ProtoBuf.java        # ProtoBuf writer (Python-compatible varint)
│       ├── CryptoHelpers.java   # md5 / hex / crypto-random helpers
│       ├── UrlEncode.java       # quote_plus / parse_qs ports + encodeParams
│       └── MiniJson.java        # minimal JSON parser (test fixture reading)
├── src/test/java/tiktoksignature/
│   ├── TestRunner.java          # no-dependency test runner (picks up @Test methods)
│   ├── UnitTest.java            # SM3 / Simon / varint / url / gorgon unit tests
│   ├── GoldenVectorTest.java    # compares all six signatures vs. Go vectors
│   ├── RequestCompareTest.java  # signs a request reconstructed from the Python JSON
│   └── TestUtil.java            # assertion helpers
├── src/test/stub/org/junit/     # in-repo @Test stub for the dependency-free build
└── testdata/                    # golden vectors (mirror of the Go project)
    ├── vector_001.json … vector_009.json
    └── python_request.json
```

## Algorithm Notes

- **SM3** — standard SM3 hash (IV `0x7380166F …`, round constants `0x79CC4519` /
  `0x7A879D8A`), 64-byte blocks, big-endian message length suffix.
- **Simon 64/256** — 64-bit block, 256-bit key, 72 rounds, with the fixed
  round-constant `Z = 0x3DC94C3A046D678B`.
- **ProtoBuf varint** — ports the Python reference's 32-bit mask and its
  `> 0x80` loop-condition quirk (e.g. `0x80` encodes as `0x00`).
- **URL encoding** — `quote_plus` semantics (spaces as `+`, `%XX` for other
  bytes, UTF-8 multi-byte percent-encoding, insertion order preserved).

## Privacy / Security

All fixtures and examples are sanitized and must not contain real credentials,
cookies, tokens, or private account information. Values are synthetic. Never
commit passwords, session cookies, access tokens, API keys, or personal account
credentials. Treat `X-Argus`/`X-Ladon`/`X-Gorgon` and any cookies as sensitive
session material. This repository contains no real secrets.

## License

This project is a Java port based on the Go implementation, which is itself
based on the Python SignerPy project.

- **Original SignerPy** — authored by **L7N**
  (`https://github.com/is-L7N/SignerPy`), distributed under the MIT license. Its
  copyright/license notice reads `copyright 2025 aythor : L7N`. The original
  project's rights and license are preserved and not modified by this port.
- **Go port** — written by **S1** (`https://github.com/JokerPython3`).
- **This Java port** — written by **S1** (`https://github.com/JokerPython3`) as
  an independent Java implementation re-implementing the algorithms from scratch.

The upstream SignerPy license terms are preserved. See the upstream SignerPy
repository for the authoritative license text.
