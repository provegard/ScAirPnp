package org.airpnp.plist

abstract class PropertyListObject[T>:Nothing<:Any] {
    def accept(visitor: PropertyListObjectVisitor): Unit

    def getValue(): T
}
