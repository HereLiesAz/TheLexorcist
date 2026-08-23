package com.hereliesaz.lexorcist.core

/**
 * The result of an operation that can fail.
 *
 * The Android-only predecessor of this type carried a `UserRecoverableError`
 * variant typed directly as `UserRecoverableAuthIOException` -- a Google
 * Sign-In SDK class. Because [LexResult] is the return type of every
 * repository and storage call in the app, that one field meant a purely local
 * file read could not be typed without the Google SDK on the classpath, and
 * every exhaustive `when` in every layer had to carry a branch for a Google
 * OAuth exception. Recoverability is expressed here as [Recoverable], which
 * carries a platform-neutral [RecoverableAction] describing what the user has
 * to do; the Android layer maps `UserRecoverableAuthIOException` onto it.
 */
sealed interface LexResult<out T> {
    data object Loading : LexResult<Nothing>

    data class Success<out T>(val data: T) : LexResult<T>

    data class Failure(
        val error: LexError,
    ) : LexResult<Nothing>

    /**
     * The operation failed but the user can fix it -- typically by granting a
     * permission or re-consenting to an OAuth scope.
     */
    data class Recoverable(
        val error: LexError,
        val action: RecoverableAction,
    ) : LexResult<Nothing>
}

/** What the user must do to make a [LexResult.Recoverable] operation succeed. */
enum class RecoverableAction {
    Reauthenticate,
    GrantPermission,
    ReconnectNetwork,
    FreeUpStorage,
    ResolveConflict,
}

/**
 * A platform-neutral description of a failure.
 *
 * [cause] is deliberately a nullable [Throwable] rather than a JVM `Exception`
 * so that native targets can carry their own error types.
 */
data class LexError(
    val kind: Kind,
    val message: String,
    val cause: Throwable? = null,
) {
    enum class Kind {
        Storage,
        Network,
        Auth,
        Permission,
        Parse,
        Script,
        NotFound,
        Conflict,
        Cancelled,
        Unknown,
    }

    companion object {
        fun storage(message: String, cause: Throwable? = null) = LexError(Kind.Storage, message, cause)
        fun network(message: String, cause: Throwable? = null) = LexError(Kind.Network, message, cause)
        fun auth(message: String, cause: Throwable? = null) = LexError(Kind.Auth, message, cause)
        fun parse(message: String, cause: Throwable? = null) = LexError(Kind.Parse, message, cause)
        fun script(message: String, cause: Throwable? = null) = LexError(Kind.Script, message, cause)
        fun notFound(message: String, cause: Throwable? = null) = LexError(Kind.NotFound, message, cause)
        fun conflict(message: String, cause: Throwable? = null) = LexError(Kind.Conflict, message, cause)
        fun unknown(message: String, cause: Throwable? = null) = LexError(Kind.Unknown, message, cause)
    }
}

inline fun <T, R> LexResult<T>.map(transform: (T) -> R): LexResult<R> = when (this) {
    is LexResult.Success -> LexResult.Success(transform(data))
    is LexResult.Failure -> this
    is LexResult.Recoverable -> this
    LexResult.Loading -> LexResult.Loading
}

inline fun <T, R> LexResult<T>.flatMap(transform: (T) -> LexResult<R>): LexResult<R> = when (this) {
    is LexResult.Success -> transform(data)
    is LexResult.Failure -> this
    is LexResult.Recoverable -> this
    LexResult.Loading -> LexResult.Loading
}

fun <T> LexResult<T>.getOrNull(): T? = (this as? LexResult.Success)?.data

fun <T> LexResult<T>.getOrDefault(fallback: T): T = getOrNull() ?: fallback

fun <T> LexResult<T>.errorOrNull(): LexError? = when (this) {
    is LexResult.Failure -> error
    is LexResult.Recoverable -> error
    else -> null
}

inline fun <T> LexResult<T>.onSuccess(block: (T) -> Unit): LexResult<T> {
    if (this is LexResult.Success) block(data)
    return this
}

inline fun <T> LexResult<T>.onFailure(block: (LexError) -> Unit): LexResult<T> {
    errorOrNull()?.let(block)
    return this
}
