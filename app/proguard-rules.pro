# Regras do R8 para o Supaco.
# Retrofit, OkHttp, Room, WorkManager, Glance e kotlinx.serialization já trazem
# regras próprias (consumer rules); aqui ficam só as específicas do app.

# Mensagens de crash legíveis (o mapping.txt de cada release fica no CI).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Retrofit resolve o tipo de retorno pela assinatura genérica dos métodos.
-keepattributes Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations, AnnotationDefault
-keep,allowobfuscation interface io.github.kellyson71.supaco.data.remote.SuapApi

# DTOs da API do SUAP: mantidos inteiros para o conversor kotlinx.serialization
# achar os serializers gerados de tipos genéricos (PaginatedResponse<T>).
-keep class io.github.kellyson71.supaco.data.model.** { *; }

# Tink (usado pelo security-crypto) referencia anotações de compilação que não
# vão para o APK.
-dontwarn com.google.errorprone.annotations.**
