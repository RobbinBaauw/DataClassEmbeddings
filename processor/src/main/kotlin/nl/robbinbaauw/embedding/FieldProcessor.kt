package nl.robbinbaauw.embedding

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import com.squareup.kotlinpoet.metadata.isVal
import com.squareup.kotlinpoet.metadata.isVar
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Element
import kotlinx.metadata.ClassName as KClassName

private data class Field(val ps: PropertySpec, val isFinal: Boolean)

class FieldProcessor : ProxyProcessor {
    private val classFieldMap: MutableMap<String, List<Field>> = HashMap()

    override fun parseType(embeddedClass: ImmutableKmClass, typeSpec: TypeSpec, classElement: Element) {
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
                val proxiedFieldName = "${propertyWithEmbed}.${field.ps.name}"

                val propertyBuilder = PropertySpec
                    .builder(field.ps.name, field.ps.type)
                    .receiver(packageClassName)
                    .mutable(!field.isFinal)

                val withGetter = propertyBuilder.getter(
                    FunSpec.getterBuilder()
                        .addStatement("return $proxiedFieldName")
                        .build()
                )

                val withSetter = addSetter(withGetter, field, proxiedFieldName)

                builder.addProperty(withSetter.build())
            }
        }
    }

    private fun addSetter(builder: PropertySpec.Builder, field: Field, proxiedFieldName: String): PropertySpec.Builder {
        return if (!field.isFinal) {
            builder.setter(
                FunSpec.setterBuilder()
                    .addParameter("value", field.ps.type)
                    .addStatement("$proxiedFieldName = value")
                    .build()
            )
        } else {
            builder
        }
    }
}

