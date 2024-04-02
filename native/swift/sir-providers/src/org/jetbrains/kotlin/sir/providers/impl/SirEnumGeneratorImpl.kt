/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.SirEnum
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.SirOrigin
import org.jetbrains.kotlin.sir.builder.buildEnum
import org.jetbrains.kotlin.sir.providers.SirEnumGenerator
import org.jetbrains.kotlin.sir.util.setParent
import org.jetbrains.kotlin.utils.addToStdlib.getOrPut

// TODO: Handle different modules
// Package Inflator should be rewritten to use this - during 3.1 of KT-66639
public class SirEnumGeneratorImpl : SirEnumGenerator {

    private val createdEnums: MutableMap<String, SirEnum> = mutableMapOf()

    override fun FqName.sirPackageEnum(module: SirModule): SirEnum {
        require(!this.isRoot)
        if (this.parent().isRoot) {
            return createEnum(this@sirPackageEnum).also {
                it.setParent(module)
            }
        } else {
            val parent = this.parent().sirPackageEnum(module)
            return createEnum(this@sirPackageEnum).also {
                it.setParent(parent)
            }
        }
    }

    private fun createEnum(fqName: FqName): SirEnum = createdEnums.getOrPut(fqName.toString()) {
        buildEnum {
            origin = SirOrigin.Namespace(fqName.pathSegments().map { it.asString() })
            name = fqName.pathSegments().last().asString()
        }
    }
}
