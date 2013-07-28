package org.airpnp.plist

class Array(private val objects: PropertyListObject[_]*) extends PropertyListObject[Seq[PropertyListObject[_]]] {
  def getValue() = objects
  def accept(visitor: PropertyListObjectVisitor) = visitor.visit(this)
}

class Date(private val date: java.util.Date) extends PropertyListObject[java.util.Date] {
  def getValue() = date.clone().asInstanceOf[java.util.Date]
  def accept(visitor: PropertyListObjectVisitor) = visitor.visit(this)
}

class Data(private val data: Seq[Byte]) extends PropertyListObject[Seq[Byte]] {
  def getValue() = data
  def accept(visitor: PropertyListObjectVisitor) = visitor.visit(this)
}

class String(private val value: java.lang.String) extends PropertyListObject[java.lang.String] {
  def getValue() = value
  def accept(visitor: PropertyListObjectVisitor) = visitor.visit(this)
}

class KeyValue private[plist] (private val k: PropertyListObject[_], private val v: PropertyListObject[_]) {
  def this(k: java.lang.String, v: PropertyListObject[_]) = this(new String(k), v)
  private[plist] def getKeyObject() = k
  def getKey() = k.getValue.toString
  def getValue() = v
}

class Dict(val entries: Seq[KeyValue]) extends PropertyListObject[Map[java.lang.String, Any]] {
  private var map: Map[java.lang.String, Any] = null
  private def createMap() = entries.map(e => (e.getKey, e.getValue.getValue)).toMap
  def accept(visitor: PropertyListObjectVisitor) = visitor.visit(this)
  def getValue() = {
    if (map == null) {
      map = createMap
    }
    map
  }
}

object False {
  val INSTANCE = new False()
}

class False private extends PropertyListObject[Boolean] {
  def accept(visitor: PropertyListObjectVisitor) = visitor.visit(this)
  def getValue() = false
}

object True {
  val INSTANCE = new True()
}

class True private extends PropertyListObject[Boolean] {
  def accept(visitor: PropertyListObjectVisitor) = visitor.visit(this)
  def getValue() = true
}

class Integer(private val value: Long) extends PropertyListObject[Long] {
  def accept(visitor: PropertyListObjectVisitor) = visitor.visit(this)
  def getValue() = value
}

class Real(private val value: Double) extends PropertyListObject[Double] {
  def accept(visitor: PropertyListObjectVisitor) = visitor.visit(this)
  def getValue() = value
}
