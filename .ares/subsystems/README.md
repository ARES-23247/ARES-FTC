# Subsystem definitions

Place canonical subsystem documents in this directory with the extension `.aressubsystem`. These
documents describe capability, hardware connections, typed immutable state, control loops, safety,
telemetry, and autonomous resources. Kotlin is a derived artifact; do not express safety policy as
raw Kotlin embedded in a document.

Prefer creating a definition through the ARES Analytics subsystem authoring screen because it
validates references and presents missing safety configuration before save. A hand-authored document
must still pass:

```powershell
.\gradlew.bat :TeamCode:previewSubsystemChanges
.\gradlew.bat :TeamCode:verifyAresProject
```

Use a stable lowercase filename matching `documentId`, for example
`shooter-hood.aressubsystem`. Keep list ordering deterministic and use explicit physical units.
Review the generated artifact destinations and all warnings before applying starters. See
[`docs/SUBSYSTEM_AUTHORING.md`](../../docs/SUBSYSTEM_AUTHORING.md) for the capability templates,
ownership model, regeneration rules, and manual implementation/test worksheet.

The checked-in GoBilda examples intentionally demonstrate both supported ownership models:

- Create a GUI-owned starter when designing a new mechanism from scratch.
- Register mature Kotlin as `HAND_AUTHORED` / `USER_OWNED` when Robot Studio should explain and
  integrate an existing implementation without replacing it.

The lighting walkthrough is in
[`docs/examples/HAND_AUTHORED_LIGHTING.md`](../../docs/examples/HAND_AUTHORED_LIGHTING.md). The
cached-feedback intake and closed-loop flywheel walkthrough is in
[`docs/examples/HAND_AUTHORED_MECHANISMS.md`](../../docs/examples/HAND_AUTHORED_MECHANISMS.md).

Do not put credentials, network endpoints, arbitrary code, or cloud behavior in subsystem documents.
Robot code remains offline-first and all runtime behavior must be derivable from validated local
project assets.
