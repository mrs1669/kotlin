/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.builder

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.UnresolvedExpressionTypeAccess
import org.jetbrains.kotlin.fir.expressions.impl.FirLiteralExpressionImpl
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.ConstantValueKind

fun <T> buildLiteralExpression(
    source: KtSourceElement?,
    kind: ConstantValueKind<T>,
    value: T,
    annotations: MutableList<FirAnnotation>? = null,
    setType: Boolean,
): FirLiteralExpression<T> {
    return FirLiteralExpressionImpl(source, null, annotations.toMutableOrEmpty(), kind, value).also {
        if (setType) {
            it.setType()
        }
    }
}

@OptIn(UnresolvedExpressionTypeAccess::class)
fun <T> FirLiteralExpression<T>.setType() {
    if (this !is FirLiteralExpressionImpl<T>) return
    when (kind) {
        ConstantValueKind.Boolean -> coneTypeOrNull = StandardClassIds.Boolean.constructClassLikeType()
        ConstantValueKind.Byte -> coneTypeOrNull = StandardClassIds.Byte.constructClassLikeType()
        ConstantValueKind.Char -> coneTypeOrNull = StandardClassIds.Char.constructClassLikeType()
        ConstantValueKind.Double -> coneTypeOrNull = StandardClassIds.Double.constructClassLikeType()
        ConstantValueKind.Float -> coneTypeOrNull = StandardClassIds.Float.constructClassLikeType()
        ConstantValueKind.Int -> coneTypeOrNull = StandardClassIds.Int.constructClassLikeType()
        ConstantValueKind.Long -> coneTypeOrNull = StandardClassIds.Long.constructClassLikeType()
        ConstantValueKind.Null -> coneTypeOrNull = StandardClassIds.Any.constructClassLikeType(isNullable = true)
        ConstantValueKind.Short -> coneTypeOrNull = StandardClassIds.Short.constructClassLikeType()
        ConstantValueKind.String -> coneTypeOrNull = StandardClassIds.String.constructClassLikeType()
        ConstantValueKind.UnsignedByte -> coneTypeOrNull = StandardClassIds.UByte.constructClassLikeType()
        ConstantValueKind.UnsignedInt -> coneTypeOrNull = StandardClassIds.UInt.constructClassLikeType()
        ConstantValueKind.UnsignedLong -> coneTypeOrNull = StandardClassIds.ULong.constructClassLikeType()
        ConstantValueKind.UnsignedShort -> coneTypeOrNull = StandardClassIds.UShort.constructClassLikeType()
        ConstantValueKind.IntegerLiteral,
        ConstantValueKind.UnsignedIntegerLiteral,
        ConstantValueKind.Error,
        -> {
        }
    }
}
