/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirEvaluatorResult
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.name.Name

interface FirExpressionEvaluatorComponent : FirSessionComponent {
    fun evaluatePropertyInitializer(property: FirProperty, session: FirSession): FirEvaluatorResult?
    fun evaluateDefault(valueParameter: FirValueParameter, session: FirSession): FirEvaluatorResult?
    fun evaluateAnnotationArguments(annotation: FirAnnotation, session: FirSession): Map<Name, FirEvaluatorResult>?
}

val FirSession.expressionEvaluator: FirExpressionEvaluatorComponent by FirSession.sessionComponentAccessor()