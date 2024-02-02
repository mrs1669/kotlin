/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions.impl

import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.BirElementVisitorLite
import org.jetbrains.kotlin.bir.CompressedSourceSpan
import org.jetbrains.kotlin.bir.acceptLite
import org.jetbrains.kotlin.bir.declarations.BirAttributeContainer
import org.jetbrains.kotlin.bir.expressions.BirExpression
import org.jetbrains.kotlin.bir.expressions.BirSuspendableExpression
import org.jetbrains.kotlin.bir.types.BirType

class BirSuspendableExpressionImpl(
    sourceSpan: CompressedSourceSpan,
    type: BirType,
    suspensionPointId: BirExpression?,
    result: BirExpression?,
) : BirSuspendableExpression(BirSuspendableExpression) {
    /**
     * The span of source code of the syntax node from which this BIR node was generated,
     * in number of characters from the start the source file. If there is no source information for this BIR node,
     * the [SourceSpan.UNDEFINED] is used. In order to get the line number and the column number from this offset,
     * [IrFileEntry.getLineNumber] and [IrFileEntry.getColumnNumber] can be used.
     *
     * @see IrFileEntry.getSourceRangeInfo
     */
    override var sourceSpan: CompressedSourceSpan = sourceSpan

    override var attributeOwnerId: BirAttributeContainer = this

    override var type: BirType = type

    private var _suspensionPointId: BirExpression? = suspensionPointId
    override var suspensionPointId: BirExpression?
        get() {
            return _suspensionPointId
        }
        set(value) {
            if (_suspensionPointId !== value) {
                childReplaced(_suspensionPointId, value)
                _suspensionPointId = value
            }
        }

    private var _result: BirExpression? = result
    override var result: BirExpression?
        get() {
            return _result
        }
        set(value) {
            if (_result !== value) {
                childReplaced(_result, value)
                _result = value
            }
        }


    init {
        initChild(_suspensionPointId)
        initChild(_result)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        _suspensionPointId?.acceptLite(visitor)
        _result?.acceptLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        return when {
            this._suspensionPointId === old -> {
                this._suspensionPointId = new as BirExpression?
            }
            this._result === old -> {
                this._result = new as BirExpression?
            }
            else -> throwChildForReplacementNotFound(old)
        }
    }
}
