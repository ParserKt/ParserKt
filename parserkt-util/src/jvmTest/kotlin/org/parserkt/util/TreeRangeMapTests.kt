package org.parserkt.util

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TreeRangeMapTest {
  private val map = TreeRangeMap<Int, String>()
  private val expectedMap = mapOf(
    0 stop 5 to "doge",
    5 stop 10 to  "cate",
    11 stop 12 to  "1",
    20 stop 21 to  "9",
    -9 stop 0 to  "xd"
  )
  @BeforeTest fun setupMap() {
    for ((k, v) in expectedMap) map[k] = v
  }

  @Test fun get() {
    for ((k, v) in expectedMap) {
      assertMapItem(v, k.start until k.stop)
    }
    assertMapItem(null, 12 until 20)
  }

  @Test fun limitations() {
    map[1 stop 2] = "柴犬"
    assertMapItem("doge", 0..0)
    assertMapItem("柴犬", 1..1)
    assertMapItem(null, 2 until 5)

    map[-9 stop 0] = "笑死"
    assertMapItem("笑死", (-9) until 0)
    assertMapItem("doge", 0..0)
  }

  private val cartoons = rangeMapOf(
    1980 stop 1983 to "舒克和贝塔",
    1989 stop 2001 to "编不出了！",
    2010 stop 2014 to "喜羊羊",
    2015 stop 2019 to "柯南",
    2020 stop 2030 to "熊出没"
  )
  @Test fun quickConstructor() {
    assertEquals("舒克和贝塔", cartoons[1980])
    assertEquals(null, cartoons[1987])
    assertEquals("编不出了！", cartoons[2000])
    assertEquals("熊出没", cartoons[2029])
  }

  private fun assertMapItem(expected: String?, indices: IntRange)
    = indices.forEach { assertEquals(expected, map[it], "@$it") }
}
