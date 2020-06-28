package nl.robbinbaauw.embedding

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import kotlinx.metadata.ClassName
import javax.lang.model.element.Element

interface ProxyProcessor {
    fun parseType(embeddedClass: ImmutableKmClass, typeSpec: TypeSpec, classElement: Element)
    fun addTypes(
        classWithEmbeds: ImmutableKmClass,
        typeSpec: TypeSpec,
        embeddedProperties: Map<String, ClassName>,
        builder: FileSpec.Builder
    )
}
