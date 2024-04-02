/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousObjectSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.SmartcastStability
import java.util.*

sealed class DataFlowVariable(private val variableIndexForDebug: Int) : Comparable<DataFlowVariable> {
    final override fun toString(): String {
        return "d$variableIndexForDebug"
    }

    override fun compareTo(other: DataFlowVariable): Int = variableIndexForDebug.compareTo(other.variableIndexForDebug)
}

class RealVariable(
    val symbol: FirBasedSymbol<*>,
    val isReceiver: Boolean,
    val dispatchReceiver: RealVariable?,
    val extensionReceiver: RealVariable?,
    variableIndexForDebug: Int,
) : DataFlowVariable(variableIndexForDebug) {
    val dependentVariables = mutableSetOf<RealVariable>()

    private var checkModuleData = false
    private var checkReceiver = false

    override fun equals(other: Any?): Boolean =
        other is RealVariable && symbol == other.symbol &&
                dispatchReceiver == other.dispatchReceiver && extensionReceiver == other.extensionReceiver

    override fun hashCode(): Int =
        Objects.hash(symbol, dispatchReceiver, extensionReceiver)

    private val baseStability: SmartcastStability by lazy {
        when (val fir = symbol.fir) {
            !is FirVariable -> SmartcastStability.STABLE_VALUE // named object or containing class for a static field reference
            is FirEnumEntry -> SmartcastStability.STABLE_VALUE
            is FirErrorProperty -> SmartcastStability.STABLE_VALUE
            is FirValueParameter -> SmartcastStability.STABLE_VALUE
            is FirBackingField -> if (fir.isVal) SmartcastStability.STABLE_VALUE else SmartcastStability.MUTABLE_PROPERTY
            is FirField -> if (!fir.isFinal)
                SmartcastStability.MUTABLE_PROPERTY
            else
                SmartcastStability.STABLE_VALUE.also { checkModuleData = true }
            is FirProperty -> when {
                fir.isExpect -> SmartcastStability.EXPECT_PROPERTY
                fir.delegate != null -> SmartcastStability.DELEGATED_PROPERTY
                // Local vars are only *sometimes* unstable (when there are concurrent assignments). `FirDataFlowAnalyzer`
                // will check that at each use site individually and produce `CAPTURED_VARIABLE` instead when necessary.
                fir.isLocal -> SmartcastStability.STABLE_VALUE
                fir.isVar -> SmartcastStability.MUTABLE_PROPERTY
                fir.receiverParameter != null -> SmartcastStability.PROPERTY_WITH_GETTER
                fir.getter !is FirDefaultPropertyAccessor? -> SmartcastStability.PROPERTY_WITH_GETTER
                fir.visibility == Visibilities.Private -> SmartcastStability.STABLE_VALUE
                else -> SmartcastStability.STABLE_VALUE.also {
                    checkModuleData = true
                    checkReceiver = !fir.isFinal
                }
            }
        }
    }

    fun getStability(flow: Flow, session: FirSession): SmartcastStability {
        val base = if (isReceiver) SmartcastStability.STABLE_VALUE else baseStability
        if (checkReceiver) {
            // This is an open val with a default getter; it is stable if the receiver has final type,
            // so it's known that there are no overrides.
            val baseType = dispatchReceiver?.fullyExpandedTypeSymbol(session) ?: return SmartcastStability.PROPERTY_WITH_GETTER
            val smartCastTypes = flow.getTypeStatement(dispatchReceiver)?.exactType
            val intersectedType = if (smartCastTypes.isNullOrEmpty())
                baseType
            else
                ConeTypeIntersector.intersectTypes(session.typeContext, smartCastTypes + baseType)
            if (intersectedType.isFinal(session) != true) return SmartcastStability.PROPERTY_WITH_GETTER
        }
        if (checkModuleData) {
            // This is a public member val with a default getter; it is stable if it's in the same module.
            // Adding a getter to another module is an ABI-compatible change that shouldn't affect dependencies.
            val moduleData = (symbol.fir as FirVariable).originalOrSelf().moduleData
            val currentModuleData = session.moduleData
            val isFriendModule = moduleData == currentModuleData ||
                    moduleData in currentModuleData.friendDependencies ||
                    moduleData in currentModuleData.allDependsOnDependencies
            if (!isFriendModule) return SmartcastStability.ALIEN_PUBLIC_PROPERTY
        }
        return base
            .combine(dispatchReceiver?.getStability(flow, session))
            .combine(extensionReceiver?.getStability(flow, session))
    }
}

class SyntheticVariable(val fir: FirElement, variableIndexForDebug: Int) : DataFlowVariable(variableIndexForDebug) {
    override fun equals(other: Any?): Boolean =
        other is SyntheticVariable && fir == other.fir

    override fun hashCode(): Int =
        fir.hashCode()
}

private fun SmartcastStability.combine(other: SmartcastStability?): SmartcastStability =
    if (other == null) this else maxOf(this, other)

private fun RealVariable.fullyExpandedTypeSymbol(session: FirSession): ConeKotlinType? = when (symbol) {
    is FirClassSymbol<*> -> symbol.defaultType()
    is FirCallableSymbol<*> -> if (isReceiver)
        symbol.resolvedReceiverTypeRef?.type?.fullyExpandedType(session)
    else
        symbol.resolvedReturnType.fullyExpandedType(session)
    else -> null
}

private fun ConeKotlinType.isFinal(session: FirSession): Boolean? =
    ((lowerBoundIfFlexible() as? ConeClassLikeType)?.toSymbol(session) as FirClassSymbol<*>?)?.let {
        it is FirAnonymousObjectSymbol || it.isFinal
    }