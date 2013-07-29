package org.airpnp.airplay.protocol

import org.fest.assertions.Assertions.assertThat

import java.util.List

import org.airpnp.plist.Array
import org.airpnp.plist.Dict
import org.airpnp.plist.PropertyList
import org.airpnp.plist.PropertyListObject
import org.airpnp.plist.KeyValue
import org.testng.annotations.Test

trait PropertyListTester {

  protected def plist: PropertyList

  @Test(dataProvider = "dictOrder")
  def shouldHaveCorrectDictKeysInOrder(path: String, expectedKeys: scala.Array[String]): Unit = {
    val d = findObject(plist, path).asInstanceOf[Dict]
    val pairs = d.entries
    val keys = pairs.map(_.getKey).toArray
    assertThat(keys).isEqualTo(expectedKeys)
  }

  private def findObject(plist: PropertyList, path: String): PropertyListObject[_] = {
    var o = plist.root
    //String[] parts = path.split("/")
    // Skip first empty part
    path.split("/").drop(1).foreach(part => {
      try {
        val idx = java.lang.Integer.parseInt(part)
        o = o.asInstanceOf[Array].getValue()(idx)
      } catch {
        case _: NumberFormatException => {
          // Dict key
          val pairs = o.asInstanceOf[Dict].entries
          pairs.find(_.getKey == part).map(_.getValue) match {
            case Some(x) => o = x
            case None => throw new IllegalArgumentException("Key not found in Dict: " + part)
          }
        }
      }
    })
    o
  }

  @Test(dataProvider = "expectedData")
  def shouldHaveCorrectDictValuesInOrder(path: String, expected: Object): Unit = {
    val value = findObject(plist, path).getValue
    assertThat(value).isEqualTo(expected)
  }

}
