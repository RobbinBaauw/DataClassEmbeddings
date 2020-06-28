package nl.robbinbaauw.embedding

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import kotlinx.metadata.ClassName as KClassName

private data class Method(val fs: FunSpec)

class MethodProcessor : ProxyProcessor {
    private val classMethodMap: MutableMap<String, List<Method>> = HashMap()

    override fun parseType(embeddedClass: ImmutableKmClass, typeSpec: TypeSpec) {
        classMethodMap[embeddedClass.name] = typeSpec.funSpecs.map {
            Method(it)
        }
    }

    override fun addTypes(
        classWithEmbeds: ImmutableKmClass,
        typeSpec: TypeSpec,
        embeddedProperties: Map<String, KClassName>,
        builder: FileSpec.Builder
    ) {
        embeddedProperties.forEach { (propertyWithEmbed, clazz) ->
            val methods = classMethodMap[clazz] ?: return@forEach
            methods.forEach { method ->
                val packageClassName = ClassName.bestGuess(classWithEmbeds.name.replace("/", "."))

                val proxyFunctionName = "${propertyWithEmbed}.${method.fs.name}"
                val args = method.fs.parameters.joinToString(", ") {
                    it.name
                }

                val funSpec = method.fs
                    .toBuilder()
                    .receiver(packageClassName)
                    .clearBody()
                    .addStatement("return $proxyFunctionName($args)")
                    .build()

                builder.addFunction(funSpec)
            }
        }
    }
}
