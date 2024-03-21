/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkTypeRefIsFullyExpanded
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.transformers.FirSuperTypeAliasTransformer
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase

internal data object LLFirSuperTypeAliasLazyResolver : LLFirLazyResolver(FirResolvePhase.SUPER_TYPE_ALIASES) {
    override fun createTargetResolver(target: LLFirResolveTarget): LLFirTargetResolver = LLFirSuperTypeAliasTargetResolver(target)

    override fun phaseSpecificCheckIsResolved(target: FirElementWithResolveState) {
        when (target) {
            is FirClass -> {
                for (superTypeRef in target.superTypeRefs) {
                    checkTypeRefIsFullyExpanded(superTypeRef, "class super type", target.llFirSession, target)
                }
            }

            is FirTypeAlias -> {
                checkTypeRefIsFullyExpanded(target.expandedTypeRef, typeRefName = "type alias expanded type", target.llFirSession, target)
            }
        }
    }
}

/**
 * This resolver is responsible for [FirResolvePhase.SUPER_TYPE_ALIASES] phase.
 *
 * This resolver:
 * - Replaces all typealias types in supertypes with their full expansions.
 *
 * @see FirResolvePhase.SUPER_TYPE_ALIASES
 */
private class LLFirSuperTypeAliasTargetResolver(
    target: LLFirResolveTarget,
    private val visitedElements: MutableSet<FirElementWithResolveState> = hashSetOf(),
) : LLFirTargetResolver(target, FirResolvePhase.SUPER_TYPE_ALIASES) {
    override fun doResolveWithoutLock(target: FirElementWithResolveState): Boolean {
        val isVisited = !visitedElements.add(target)
        if (isVisited) return true

        when (target) {
            is FirRegularClass -> performResolve(
                declaration = target,
                superTypeRefsForTransformation = {
                    // We should create a copy of the original collection
                    // to avoid [ConcurrentModificationException] during another thread publication
                    ArrayList(target.superTypeRefs)
                },
                resolver = { types -> types.map { FirSuperTypeAliasTransformer.expandTypealiasInPlace(it, resolveTargetSession) } },
                superTypeUpdater = target::replaceSuperTypeRefs,
            )
            is FirTypeAlias -> performResolve(
                declaration = target,
                superTypeRefsForTransformation = { target.expandedTypeRef },
                resolver = { FirSuperTypeAliasTransformer.expandTypealiasInPlace(it, resolveTargetSession) },
                superTypeUpdater = target::replaceExpandedTypeRef,
            )
            else -> {
                performCustomResolveUnderLock(target) {
                    // just update the phase
                }
            }
        }

        return true
    }

    /**
     * [superTypeRefsForTransformation] will be executed under [declaration] lock
     */
    private inline fun <T : FirClassLikeDeclaration, S, K> performResolve(
        declaration: T,
        superTypeRefsForTransformation: () -> S,
        resolver: (S) -> K,
        crossinline superTypeUpdater: (K) -> Unit,
    ) {
        // To avoid redundant work, because a publication won't be executed
        if (declaration.resolvePhase >= resolverPhase) return

        declaration.lazyResolveToPhase(resolverPhase.previous)

        var superTypeRefs: S? = null
        withReadLock(declaration) {
            superTypeRefs = superTypeRefsForTransformation()
        }

        // "null" means that some other thread is already resolved [declaration] to [resolverPhase]
        val expandedSuperTypeRefs = superTypeRefs?.let(resolver) ?: return

        performCustomResolveUnderLock(declaration) {
            superTypeUpdater(expandedSuperTypeRefs)
        }
    }

    override fun doLazyResolveUnderLock(target: FirElementWithResolveState) {
        error("Should be resolved without lock in ${::doResolveWithoutLock.name}")
    }
}
