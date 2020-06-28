package nl.robbinbaauw.embedding

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.metadata.hasAnnotations
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import kotlinx.metadata.KmClassifier
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("nl.robbinbaauw.embedding.Embed", "nl.robbinbaauw.embedding.Embeddable")
class Processor : AbstractProcessor() {
    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        val processors = listOf(FieldProcessor(), MethodProcessor())

        roundEnv
            .getElementsAnnotatedWith(Embeddable::class.java)
            .forEach { element ->
                val metadataAnnotation = element.getAnnotation(Metadata::class.java)
                val immutableKmClass = metadataAnnotation.toImmutableKmClass()
                val typeSpec = immutableKmClass.toTypeSpec(null)
                processors.forEach { it.parseType(immutableKmClass, typeSpec, element) }
            }

        roundEnv
            .getElementsAnnotatedWith(HasEmbeds::class.java)
            .forEach { element ->
                val packageName = processingEnv.elementUtils.getPackageOf(element).qualifiedName.toString()
                val fileName = "${element.simpleName}Embed.kt"

                val metadataAnnotation = element.getAnnotation(Metadata::class.java)
                val immutableKmClass = metadataAnnotation.toImmutableKmClass()
                val typeSpec = immutableKmClass.toTypeSpec(null)

                val embeddedProperties = immutableKmClass.properties.map { property ->
                    val classifier = property.returnType.classifier
                    if (classifier is KmClassifier.Class) {
                        property.name to classifier.name
                    } else {
                        throw IllegalStateException()
                    }
                }.toMap()

                val fileSpecBuilder = FileSpec.builder(packageName, fileName)
                processors.forEach { it.addTypes(immutableKmClass, typeSpec, embeddedProperties, fileSpecBuilder) }
                fileSpecBuilder.build().writeTo(processingEnv.filer)
            }
        return true
    }
}
