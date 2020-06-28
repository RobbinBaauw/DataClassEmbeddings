package nl.robbinbaauw.embedding

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import com.squareup.kotlinpoet.metadata.isVal
import com.squareup.kotlinpoet.metadata.isVar
import kotlinx.metadata.ClassName as KClassName

private data class Field(val ps: PropertySpec, val isFinal: Boolean)

class FieldProcessor : ProxyProcessor {
    private val classFieldMap: MutableMap<String, List<Field>> = HashMap()

    override fun parseType(embeddedClass: ImmutableKmClass, typeSpec: TypeSpec) {
        classFieldMap[embeddedClass.name] = embeddedClass.properties.mapNotNull { property ->
            if (!property.isVal && !property.isVar) return@mapNotNull null

            val propertySpec = typeSpec.propertySpecs.first { it.name == property.name }
            val isFinal = property.isVal
            Field(propertySpec, isFinal)
        }
    }

    override fun addTypes(
        classWithEmbeds: ImmutableKmClass,
        typeSpec: TypeSpec,
        embeddedProperties: Map<String, KClassName>,
        builder: FileSpec.Builder
    ) {
        embeddedProperties.forEach { (propertyWithEmbed, clazz) ->
            val fields = classFieldMap[clazz] ?: return@forEach
            fields.forEach { field ->
                val packageClassName = ClassName.bestGuess(classWithEmbeds.name.replace("/", "."))
                val proxyFieldName = "${propertyWithEmbed}.${field.ps.name}"

                val propertySpec = PropertySpec
                    .builder(field.ps.name, field.ps.type)
                    .receiver(packageClassName)
                    .mutable(!field.isFinal)
                    .getter(
                        FunSpec.getterBuilder()
                            .addStatement("return $proxyFieldName")
                            .build()
                    )

                val builtSpec = if (!field.isFinal) {
                    propertySpec.setter(
                        FunSpec.setterBuilder()
                            .addParameter("value", field.ps.type)
                            .addStatement("$proxyFieldName = value")
                            .build()
                    )
                        .build()
                } else {
                    propertySpec.build()
                }

                builder.addProperty(builtSpec)
            }
        }
    }
}

