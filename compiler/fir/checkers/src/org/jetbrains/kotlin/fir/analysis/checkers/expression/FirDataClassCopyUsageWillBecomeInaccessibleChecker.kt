/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.primaryConstructorSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol
import org.jetbrains.kotlin.fir.visibilityChecker
import org.jetbrains.kotlin.resolve.DataClassResolver

object FirDataClassCopyUsageWillBecomeInaccessibleChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.WarnAboutDataClassCopyVisibilityChange) &&
            !context.languageVersionSettings.supportsFeature(LanguageFeature.ErrorAboutDataClassCopyVisibilityChange)
        ) {
            return
        }
        // Optimization. isCopyAlreadyInaccessible will check it anyway
        if (context.languageVersionSettings.supportsFeature(LanguageFeature.DataClassCopyRespectsConstructorVisibility)) {
            return
        }
        val dataClass = expression.resolvedType.toRegularClassSymbol(context.session) ?: return
        val copyFunction = expression.calleeReference.symbol as? FirCallableSymbol ?: return
        if (DataClassResolver.isCopy(copyFunction.name) && copyFunction.origin == FirDeclarationOrigin.Synthetic.DataClassMember) {
            val dataClassConstructor = dataClass.primaryConstructorSymbol(context.session) ?: return

            @OptIn(SymbolInternals::class)
            val isCopyAlreadyInaccessible = !context.session.visibilityChecker.isVisible(
                copyFunction.fir as? FirMemberDeclaration ?: return,
                context.session,
                context.containingFile ?: return,
                context.containingDeclarations,
                dispatchReceiver = null
            )
            if (isCopyAlreadyInaccessible) {
                return
            }

            @OptIn(SymbolInternals::class)
            val isConstructorVisible = context.session.visibilityChecker.isVisible(
                dataClassConstructor.fir,
                context.session,
                context.containingFile ?: return,
                context.containingDeclarations,
                dispatchReceiver = null
            )
            // We don't check presence @InconsistentDataCopy annotations, on purpose. Even if the 'copy' is exposed, call-sites need to migrate
            if (!isConstructorVisible) {
                val factory =
                    when (context.languageVersionSettings.supportsFeature(LanguageFeature.ErrorAboutDataClassCopyVisibilityChange)) {
                        true -> FirErrors.DATA_CLASS_COPY_USAGE_WILL_BECOME_INACCESSIBLE_ERROR
                        false -> FirErrors.DATA_CLASS_COPY_USAGE_WILL_BECOME_INACCESSIBLE_WARNING
                    }
                reporter.reportOn(expression.calleeReference.source, factory, context)
            }
        }
    }
}
