package org.nypl.audiobook.android.api

/**
 * A result type - either success or failure.
 */

sealed class PlayerResult<S : Any, F : Any> {

  /**
   * A success value.
   */

  data class Success<S : Any, F : Any>(
    val result: S)
    : PlayerResult<S, F>()

  /**
   * A failure value.
   */

  data class Failure<S : Any, F : Any>(
    val failure: F)
    : PlayerResult<S, F>()

  /**
   * Monadic bind.
   * If this == Success(x), return f(this)
   * If this == Failure(y), return Failure(y)
   */

  fun <T : Any> flatMap(f: (S) -> PlayerResult<T, F>): PlayerResult<T, F> =
    flatMap(this, f)

  /**
   * Functor map.
   * If this == Success(x), return Success(f(x))
   * If this == Failure(y), return Failure(y)
   */

  fun <T : Any> map(f: (S) -> T): PlayerResult<T, F> =
    map(this, f)

  companion object {

    /**
     * Monadic bind.
     * If r == Success(x), return f(r)
     * If r == Failure(y), return Failure(y)
     */

    fun <A : Any, B : Any, F : Any> flatMap(
      r: PlayerResult<A, F>,
      f: (A) -> PlayerResult<B, F>): PlayerResult<B, F> {
      return when (r) {
        is PlayerResult.Success -> f(r.result)
        is PlayerResult.Failure -> PlayerResult.Failure(r.failure)
      }
    }

    /**
     * Functor map.
     * If r == Success(x), return Success(f(x))
     * If r == Failure(y), return Failure(y)
     */

    fun <A : Any, B : Any, F : Any> map(
      r: PlayerResult<A, F>,
      f: (A) -> B): PlayerResult<B, F> {
      return when (r) {
        is PlayerResult.Success -> PlayerResult.Success(f(r.result))
        is PlayerResult.Failure -> PlayerResult.Failure(r.failure)
      }
    }

    /**
     * Monadic "unit" or "return".
     */

    fun <A : Any, F : Any> unit(x: A): PlayerResult<A, F> =
      PlayerResult.Success(x)

  }
}

