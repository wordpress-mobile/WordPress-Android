# AGENTS.md

Guidance for AI agents working in this repository. Read alongside `CLAUDE.md`
(build/test commands, architecture, style rules). This file lists Kotlin/Android
**antipatterns to avoid** when generating or modifying code.

## Concurrency & Coroutines

- **Do not use `GlobalScope`.** Use an injected `CoroutineScope` tied to a
  lifecycle (`viewModelScope`, `lifecycleScope`) or a scope owned by the class.
  For fire-and-forget work that must outlive the lifecycle owner, inject
  `@Named(APPLICATION_SCOPE) CoroutineScope` — never reach for `GlobalScope`.
- **Do not use `runBlocking` on the main thread or in production code.** It is
  acceptable only in tests.
- **Do not hardcode `Dispatchers.IO`/`Dispatchers.Main` inside classes.** Inject
  `CoroutineDispatcher` via constructor with `@Named(IO_THREAD)` so it can be
  swapped in tests. See `ReaderPostRepository.kt` for the canonical pattern.
- **Do not launch coroutines from constructors or `init` blocks.** Start them
  from a lifecycle-aware entry point.
- **Do not use `Thread.sleep`** to wait for async work. Use coroutines, `delay`,
  or proper synchronization. Never in tests either — use `runTest` and virtual
  time.
- **Do not use `AsyncTask`, `Handler(Looper.getMainLooper())` for new code, or
  raw `Thread`.** Prefer coroutines.

## Nullability & Kotlin Idioms

- **Avoid `!!` (non-null assertion).** If a value is guaranteed non-null,
  restructure to express that in the type. If not, handle the null branch.
  Exception: the Fragment view-binding backing-field idiom
  (`private val binding get() = _binding!!`) is accepted in this codebase.
- **Avoid `lateinit var` for values that have a sensible default or can be
  constructor-injected.** Reserve it for DI-injected fields. For view
  bindings, use the nullable backing-field idiom described below in
  *Android Lifecycle & Memory Leaks*.
- **Do not use `!!` on `intent.extras`, `arguments`, or `savedInstanceState`.**
  Use safe calls with sensible fallbacks.
- **Do not write `if (x != null) x.foo()`.** Use `x?.foo()` or `x?.let { ... }`.
- **Prefer `val` over `var`.** Only use `var` when reassignment is required.
- **Do not use `Any` as a type when a sealed class / interface fits.**

## Android Lifecycle & Memory Leaks

- **Do not store `Activity`, `Fragment`, `View`, or `Context` references in
  `companion object`, singletons, or `object` declarations.** Use
  `applicationContext` if a Context is truly required at app scope.
- **Do not capture `Activity`/`Fragment` in long-lived callbacks, listeners, or
  coroutine scopes** without lifecycle awareness.
- **Do not access `binding` after `onDestroyView` in Fragments.** Use the
  nullable backing-field pattern already used across the codebase
  (`private var _binding: FooBinding? = null` exposed via
  `private val binding get() = _binding!!`, nulled in `onDestroyView`). See
  `ReaderAuthorProfileBottomSheetFragment.kt` for an example. For
  ViewHolders, use the
  `ViewGroup.viewBinding(...)` extension in
  `util/extensions/ViewGroupExtensions.kt`.
- **Do not start work in `onCreate` that must outlive the screen.** Use
  `ViewModel`, `WorkManager`, or a foreground service as appropriate.
- **Do not register `BroadcastReceiver`/observers without unregistering** in
  the matching lifecycle callback.

## Error Handling

- **Do not catch broad exceptions (`Exception`, `Throwable`) without logging
  via `AppLog` or rethrowing.** Prefer catching the specific exception when
  practical. Broad catches are acceptable at network/IO boundaries as long
  as the error flows through the existing `AppLog`/Sentry pipeline. If you
  intentionally swallow an exception, add a one-line comment explaining
  why.
- **Do not throw raw `RuntimeException`/`IllegalStateException` for control
  flow or expected error paths.** Use sealed result types or nullable
  returns. `check`/`require`/`error` for genuine precondition violations is
  fine.
- **Do not log exceptions with `printStackTrace()`.** Use `AppLog` so output
  ends up in the right place in release builds.

## Android APIs

- **Do not use `findViewById`.** Use View Binding (existing XML) or Compose
  (new screens). Already in `CLAUDE.md`; restated because it is the most
  common regression.
- **Do not use deprecated APIs** (`startActivityForResult`, `onActivityResult`,
  `getDrawable(int)`, `getColor(int)` without theme, `Handler()` no-arg ctor,
  etc.). Use Activity Result APIs, `ContextCompat`, `ResourcesCompat`, etc.
- **Do not use reflection.** Already in `CLAUDE.md`; restated for emphasis.
- **Do not use `SimpleDateFormat` as a static/companion field.** It is not
  thread-safe. Construct per use, or use `java.time` / `DateTimeFormatter`.
- **Do not hardcode user-facing strings.** Add them to `strings.xml` and
  reference via `R.string.*` or `UiString`.
- **Do not hardcode dimensions, colors, or styles in code.** Use resources
  and the design system.
- **Do not perform I/O, network, or DB work on the main thread.**

## Dependency Injection (Hilt/Dagger)

- **Do not instantiate dependencies with `new`/constructor calls inside
  classes that should receive them via DI.**
- **Do not use service locators or `object` singletons for stateful
  dependencies.** Use `@Inject` constructor injection.
- **Do not inject `Context` directly when `@ApplicationContext` is what you
  need.** Annotate explicitly to avoid leaking an Activity context.

## Compose

- **Inside a composable, always wrap `mutableStateOf` in `remember`, and do
  not mutate state during composition.** Do mutations in event handlers or
  `LaunchedEffect`. Top-level `mutableStateOf` properties on a class
  (e.g., a `ViewModel`) are fine.
- **Do not create `ViewModel` instances inside composables via constructors.**
  Use `hiltViewModel()` / `viewModel()`.
- **Do not pass `ViewModel` deep into the composable tree.** Hoist state and
  pass plain data + lambdas.
- **Do not launch coroutines from composables without `LaunchedEffect` /
  `rememberCoroutineScope`.**
- **Do not perform expensive work in the composable body.** Wrap in
  `remember` / `derivedStateOf`.

## Testing

- **Do not write tests that depend on real time, network, or device state.**
  Use fakes, `runTest`, virtual time, and injected dispatchers.
- **Do not assert on `toString()` or implementation details.** Assert on
  observable behavior.
- **Do not use `Thread.sleep` in tests.** Use coroutine test utilities.
- **Do not mock data classes or types you own when a real instance / fake
  works.** Reserve mocks for collaborators.

## Code Hygiene

- **Do not introduce TODOs without a tracking issue or owner.** Already covered
  by "no FIXME" in `CLAUDE.md`; same spirit applies to TODOs.
- **Do not leave commented-out code.** Delete it; git history preserves it.
- **Do not add wrapper functions, interfaces, or abstractions "for future
  flexibility"** when there is exactly one caller and one implementation.
- **Do not duplicate strings across `strings.xml` translations.** Only edit
  the base `values/strings.xml`; translations are managed externally.
  Exception: when *removing* a string, delete it from every
  `values-*/strings.xml` too — lint's `ExtraTranslation` rule will fail
  otherwise.
- **Do not add new dependencies in `build.gradle` directly.** Add to
  `gradle/libs.versions.toml` (already in `CLAUDE.md`).

## Security & Privacy

- **Do not log credentials, tokens, cookies, personal data, or full URLs that
  may contain auth.** Even at `DEBUG` level.
- **Do not store secrets in source.** Use `secrets.properties` / Gradle
  properties.
- **Do not disable SSL verification, hostname verification, or accept all
  certificates.** Even in debug builds.
- **Do not use `MODE_WORLD_READABLE`/`MODE_WORLD_WRITEABLE`** for
  SharedPreferences or files.
