/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirImplicitBuiltinTypeRef
import org.jetbrains.kotlin.fir.withFileAnalysisExceptionWrapping

class FirSuperTypeAliasProcessor(session: FirSession, scopeSession: ScopeSession) : FirTransformerBasedResolveProcessor(
    session, scopeSession, FirResolvePhase.SUPER_TYPE_ALIASES
) {
    override val transformer = FirSuperTypeAliasTransformer(session)
}

class FirSuperTypeAliasTransformer(
    override val session: FirSession,
) : FirAbstractPhaseTransformer<Any?>(FirResolvePhase.SUPER_TYPE_ALIASES) {
    override fun <E : FirElement> transformElement(element: E, data: Any?): E {
        return element
    }

    private fun <D : FirDeclaration> transformDeclarationContent(declaration: D, data: Any?): D {
        declaration.transformChildren(this, data)
        return declaration
    }

    override fun transformFile(file: FirFile, data: Any?): FirFile {
        checkSessionConsistency(file)
        return withFileAnalysisExceptionWrapping(file) {
            transformDeclarationContent(file, data)
        }
    }

    override fun transformClass(klass: FirClass, data: Any?): FirStatement {
        val fullyExpandedSupertypes = klass.superTypeRefs.map { expandTypealiasInPlace(it, session) }
        klass.replaceSuperTypeRefs(fullyExpandedSupertypes)
        return transformDeclarationContent(klass, data)
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: Any?): FirStatement {
        return transformClass(regularClass, data)
    }

    override fun transformAnonymousObject(anonymousObject: FirAnonymousObject, data: Any?): FirStatement {
        return transformClass(anonymousObject, data)
    }

    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: Any?): FirStatement {
        val fullyExpandedType = expandTypealiasInPlace(typeAlias.expandedTypeRef, session)
        typeAlias.replaceExpandedTypeRef(fullyExpandedType)
        return typeAlias
    }

    companion object {
        fun expandTypealiasInPlace(typeRef: FirTypeRef, session: FirSession): FirTypeRef {
            return when (typeRef) {
                is FirImplicitBuiltinTypeRef, is FirErrorTypeRef -> typeRef
                else -> when (val expanded = typeRef.coneType.fullyExpandedType(session)) {
                    typeRef.coneType -> typeRef
                    else -> expanded.assign(AbbreviatedTypeAttribute(typeRef.coneType)).let(typeRef::withReplacedConeType)
                }
            }
        }
    }
}
