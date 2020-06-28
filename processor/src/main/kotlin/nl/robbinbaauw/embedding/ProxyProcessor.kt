package nl.robbinbaauw.embedding

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import kotlinx.metadata.ClassName

interface ProxyProcessor {
    fun parseType(embeddedClass: ImmutableKmClass, typeSpec: TypeSpec)
    fun addTypes(
        classWithEmbeds: ImmutableKmClass,
        typeSpec: TypeSpec,
        embeddedProperties: Map<String, ClassName>,
        builder: FileSpec.Builder
    )
}
