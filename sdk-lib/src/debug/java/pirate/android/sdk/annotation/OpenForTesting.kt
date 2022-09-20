package pirate.android.sdk.annotation

@Target(AnnotationTarget.CLASS)
annotation class PirateOpenClass

/**
 * Used in conjunction with the kotlin-allopen plugin to make any class with this annotation open for extension.
 * Typically, we apply this to classes that we want to mock in androidTests because unit tests don't have this problem,
 * it's only an issue with JUnit4 Instrumentation tests.
 *
 * Note: the counterpart to this annotation in the release buildType does not apply the PirateOpenClass annotation
 */
@PirateOpenClass
@Target(AnnotationTarget.CLASS)
annotation class PirateOpenForTesting
