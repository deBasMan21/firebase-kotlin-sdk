/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase

import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlin.collections.set

actual data class EncodedObject internal constructor(actual val raw: Map<String, Any?>) : Map<Any?, Any?> by raw.mapKeys({ (key, _) -> key as? Any })

@PublishedApi
internal actual fun List<Pair<String, Any?>>.asEncodedObject() = EncodedObject(toMap())

@PublishedApi
internal actual fun Any.asNativeMap(): Map<*, *>? = this as? Map<*, *>

actual fun FirebaseEncoder.structureEncoder(descriptor: SerialDescriptor): FirebaseCompositeEncoder = when(descriptor.kind) {
    StructureKind.LIST -> encodeAsList()
    StructureKind.MAP -> mutableListOf<Any?>()
        .let { FirebaseCompositeEncoder(settings, { value = it.chunked(2).associate { (k, v) -> k to v } }) { _, _, value -> it.add(value) } }
    StructureKind.CLASS,  StructureKind.OBJECT-> encodeAsMap(descriptor)
    is PolymorphicKind -> encodeAsMap(descriptor)
    else -> TODO("The firebase-kotlin-sdk does not support $descriptor for serialization yet")
}

private fun FirebaseEncoder.encodeAsList(): FirebaseCompositeEncoder = mutableListOf<Any?>()
    .also { value = it }
    .let { FirebaseCompositeEncoder(settings) { _, index, value -> it.add(index, value) } }
private fun FirebaseEncoder.encodeAsMap(descriptor: SerialDescriptor): FirebaseCompositeEncoder = mutableMapOf<Any?, Any?>()
    .also { value = it }
    .let {
        FirebaseCompositeEncoder(
            settings,
            setPolymorphicType = { discriminator, type ->
                it[discriminator] = type
            },
            set = { _, index, value -> it[descriptor.getElementName(index)] = value }
        )
    }
