package nl.robbinbaauw.embedding

import javax.lang.model.element.Element
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

fun parseType(element: Element, type: TypeMirror): String {
    val nullableAnnotation = element.getAnnotation(org.jetbrains.annotations.Nullable::class.java)
    val isNullable = nullableAnnotation != null

    val typeName = when (type.kind) {
        TypeKind.BOOLEAN -> "Boolean"
        TypeKind.BYTE -> "Byte"
        TypeKind.SHORT -> "Short"
        TypeKind.INT -> "Int"
        TypeKind.LONG -> "Long"
        TypeKind.CHAR -> "Char"
        TypeKind.FLOAT -> "Float"
        TypeKind.DOUBLE -> "Double"
        else -> type.toString()
    }

    return typeName + if (isNullable) "?" else ""
}
