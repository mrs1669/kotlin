/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations.impl

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.declarations.BirConstructor
import org.jetbrains.kotlin.bir.declarations.BirTypeParameter
import org.jetbrains.kotlin.bir.declarations.BirValueParameter
import org.jetbrains.kotlin.bir.expressions.BirBody
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name

class BirConstructorImpl(
    sourceSpan: CompressedSourceSpan,
    signature: IdSignature?,
    origin: IrDeclarationOrigin,
    name: Name,
    isExternal: Boolean,
    visibility: DescriptorVisibility,
    isInline: Boolean,
    isExpect: Boolean,
    returnType: BirType,
    dispatchReceiverParameter: BirValueParameter?,
    extensionReceiverParameter: BirValueParameter?,
    contextReceiverParametersCount: Int,
    body: BirBody?,
    isPrimary: Boolean,
) : BirImplElementBase(BirConstructor), BirConstructor {
    override val owner: BirConstructorImpl
        get() = this

    private var _sourceSpan: CompressedSourceSpan = sourceSpan
    /**
     * The span of source code of the syntax node from which this BIR node was generated,
     * in number of characters from the start the source file. If there is no source information for this BIR node,
     * the [SourceSpan.UNDEFINED] is used. In order to get the line number and the column number from this offset,
     * [IrFileEntry.getLineNumber] and [IrFileEntry.getColumnNumber] can be used.
     *
     * @see IrFileEntry.getSourceRangeInfo
     */
    override var sourceSpan: CompressedSourceSpan
        get() {
            recordPropertyRead()
            return _sourceSpan
        }
        set(value) {
            if (_sourceSpan != value) {
                _sourceSpan = value
                invalidate()
            }
        }

    private var _signature: IdSignature? = signature
    override var signature: IdSignature?
        get() {
            recordPropertyRead()
            return _signature
        }
        set(value) {
            if (_signature != value) {
                _signature = value
                invalidate()
            }
        }

    private var _origin: IrDeclarationOrigin = origin
    override var origin: IrDeclarationOrigin
        get() {
            recordPropertyRead()
            return _origin
        }
        set(value) {
            if (_origin != value) {
                _origin = value
                invalidate()
            }
        }

    private var _name: Name = name
    override var name: Name
        get() {
            recordPropertyRead()
            return _name
        }
        set(value) {
            if (_name != value) {
                _name = value
                invalidate()
            }
        }

    private var _isExternal: Boolean = isExternal
    override var isExternal: Boolean
        get() {
            recordPropertyRead()
            return _isExternal
        }
        set(value) {
            if (_isExternal != value) {
                _isExternal = value
                invalidate()
            }
        }

    private var _visibility: DescriptorVisibility = visibility
    override var visibility: DescriptorVisibility
        get() {
            recordPropertyRead()
            return _visibility
        }
        set(value) {
            if (_visibility != value) {
                _visibility = value
                invalidate()
            }
        }

    private var _isInline: Boolean = isInline
    override var isInline: Boolean
        get() {
            recordPropertyRead()
            return _isInline
        }
        set(value) {
            if (_isInline != value) {
                _isInline = value
                invalidate()
            }
        }

    private var _isExpect: Boolean = isExpect
    override var isExpect: Boolean
        get() {
            recordPropertyRead()
            return _isExpect
        }
        set(value) {
            if (_isExpect != value) {
                _isExpect = value
                invalidate()
            }
        }

    private var _returnType: BirType = returnType
    override var returnType: BirType
        get() {
            recordPropertyRead()
            return _returnType
        }
        set(value) {
            if (_returnType != value) {
                _returnType = value
                invalidate()
            }
        }

    private var _dispatchReceiverParameter: BirValueParameter? = dispatchReceiverParameter
    override var dispatchReceiverParameter: BirValueParameter?
        get() {
            recordPropertyRead()
            return _dispatchReceiverParameter
        }
        set(value) {
            if (_dispatchReceiverParameter !== value) {
                childReplaced(_dispatchReceiverParameter, value)
                _dispatchReceiverParameter = value
                invalidate()
            }
        }

    private var _extensionReceiverParameter: BirValueParameter? = extensionReceiverParameter
    override var extensionReceiverParameter: BirValueParameter?
        get() {
            recordPropertyRead()
            return _extensionReceiverParameter
        }
        set(value) {
            if (_extensionReceiverParameter !== value) {
                childReplaced(_extensionReceiverParameter, value)
                _extensionReceiverParameter = value
                invalidate()
            }
        }

    private var _contextReceiverParametersCount: Int = contextReceiverParametersCount
    override var contextReceiverParametersCount: Int
        get() {
            recordPropertyRead()
            return _contextReceiverParametersCount
        }
        set(value) {
            if (_contextReceiverParametersCount != value) {
                _contextReceiverParametersCount = value
                invalidate()
            }
        }

    private var _body: BirBody? = body
    override var body: BirBody?
        get() {
            recordPropertyRead()
            return _body
        }
        set(value) {
            if (_body !== value) {
                childReplaced(_body, value)
                _body = value
                invalidate()
            }
        }

    private var _isPrimary: Boolean = isPrimary
    override var isPrimary: Boolean
        get() {
            recordPropertyRead()
            return _isPrimary
        }
        set(value) {
            if (_isPrimary != value) {
                _isPrimary = value
                invalidate()
            }
        }

    override val annotations: BirImplChildElementList<BirConstructorCall> = BirImplChildElementList(this, 1, false)
    override val typeParameters: BirImplChildElementList<BirTypeParameter> = BirImplChildElementList(this, 2, false)
    override val valueParameters: BirImplChildElementList<BirValueParameter> = BirImplChildElementList(this, 3, false)

    init {
        initChild(_dispatchReceiverParameter)
        initChild(_extensionReceiverParameter)
        initChild(_body)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        annotations.acceptChildrenLite(visitor)
        typeParameters.acceptChildrenLite(visitor)
        _dispatchReceiverParameter?.acceptLite(visitor)
        _extensionReceiverParameter?.acceptLite(visitor)
        valueParameters.acceptChildrenLite(visitor)
        _body?.acceptLite(visitor)
    }

    override fun replaceChildProperty(old: BirElement, new: BirElement?) {
        return when {
            this._dispatchReceiverParameter === old -> {
                this._dispatchReceiverParameter = new as BirValueParameter?
            }
            this._extensionReceiverParameter === old -> {
                this._extensionReceiverParameter = new as BirValueParameter?
            }
            this._body === old -> {
                this._body = new as BirBody?
            }
            else -> throwChildForReplacementNotFound(old)
        }
    }

    override fun getChildrenListById(id: Int): BirChildElementList<*> {
        return when (id) {
            1 -> this.annotations
            2 -> this.typeParameters
            3 -> this.valueParameters
            else -> throwChildrenListWithIdNotFound(id)
        }
    }
}
