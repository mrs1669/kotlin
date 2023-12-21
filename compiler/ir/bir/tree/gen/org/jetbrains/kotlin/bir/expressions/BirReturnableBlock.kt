/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions

import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.BirElementClass
import org.jetbrains.kotlin.bir.BirElementVisitor
import org.jetbrains.kotlin.bir.declarations.BirReturnTarget
import org.jetbrains.kotlin.bir.declarations.BirSymbolOwner
import org.jetbrains.kotlin.bir.symbols.BirReturnableBlockSymbol

/**
 * A leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.bir.generator.BirTree.returnableBlock]
 */
abstract class BirReturnableBlock : BirBlock(), BirElement, BirSymbolOwner, BirReturnTarget, BirReturnableBlockSymbol {
    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        statements.acceptChildren(visitor, data)
    }

    companion object : BirElementClass(BirReturnableBlock::class.java, 47, true)
}
