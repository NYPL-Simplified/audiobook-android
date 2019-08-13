package org.nypl.audiobook.android.tests

import org.junit.Assert
import org.junit.Test
import org.librarysimplified.audiobook.api.PlayerResult
import org.librarysimplified.audiobook.api.PlayerResult.Companion.unit

/**
 * Tests for the {@link org.librarysimplified.audiobook.api.PlayerResult} type.
 */

open class PlayerResultContract {

  /**
   * return a >>= f ≡ f a
   */

  @Test
  fun testFlatMapLeftIdentity() {
    val a = 23
    val m = PlayerResult.unit<Int, Unit>(a)
    val f = { y: Int -> PlayerResult.Success<Int, Unit>(y * 2) }
    Assert.assertEquals(m.flatMap(f), f(a))
  }

  /**
   * m >>= return ≡ m
   */

  @Test
  fun testFlatMapRightIdentity() {
    val m = PlayerResult.unit<Int, Unit>(23)
    val r = m.flatMap { y -> unit<Int, Unit>(y) }
    Assert.assertEquals(m, r)
  }

  /**
   * (m >>= f) >>= g ≡ m >>= (\x -> f x >>= g)
   */

  @Test
  fun testFlatMapAssociative() {
    val m = PlayerResult.unit<Int, Unit>(23)
    val f = { y: Int -> unit<Int, Unit>(y * 2) }
    val g = { y: Int -> unit<Int, Unit>(y * 3) }

    Assert.assertEquals(
      m.flatMap(f).flatMap(g),
      m.flatMap({ x -> f(x).flatMap(g) }))
  }

}
