package org.airpnp.plist

abstract class PropertyListObjectVisitor {

    def visit(true1: True): Unit

    def visit(real: Real): Unit

    def visit(string: String): Unit

    def visit(array: Array): Unit

    def visit(date: Date): Unit

    def visit(dict: Dict): Unit

    def visit(false1: False): Unit

    def visit(integer: Integer): Unit

    def visit(data: Data): Unit

}
