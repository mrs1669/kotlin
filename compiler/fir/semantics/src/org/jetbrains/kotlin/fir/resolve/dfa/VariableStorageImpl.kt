/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.types.SmartcastStability
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

class VariableStorageImpl(private val session: FirSession) : VariableStorage() {
    // These are basically hash sets, since they map each key to itself. The only point of having them as maps
    // is to deduplicate equal instances with lookups. The impact of that is questionable, but whatever.
    private val realVariables: MutableMap<RealVariable, RealVariable> = HashMap()
    private val syntheticVariables: MutableMap<FirElement, SyntheticVariable> = HashMap()

    private val nextVariableIndex: Int
        get() = realVariables.size + syntheticVariables.size + 1

    fun clear(): VariableStorageImpl = VariableStorageImpl(session)

    override fun getLocalVariable(symbol: FirBasedSymbol<*>, isReceiver: Boolean): RealVariable? =
        RealVariable(symbol, isReceiver, null, null, SmartcastStability.STABLE_VALUE, nextVariableIndex).takeIfKnown()

    fun getOrCreateLocalVariable(symbol: FirBasedSymbol<*>, isReceiver: Boolean): RealVariable =
        RealVariable(symbol, isReceiver, null, null, SmartcastStability.STABLE_VALUE, nextVariableIndex).remember()

    fun getAllLocalVariables(): Iterable<RealVariable> =
        realVariables.values.filter {
            it.symbol is FirPropertySymbol && it.symbol.isLocal && it.dispatchReceiver == null && it.extensionReceiver == null
        }

    // General pattern when using these functions:
    //
    //   val argumentVariable = variableStorage.{get,getOrCreateIfReal}(flow, fir.argument) ?: return
    //   val expressionVariable = variableStorage.createSynthetic(fir)
    //   flow.addImplication(somethingAbout(expressionVariable) implies somethingElseAbout(argumentVariable))
    //
    // If "something else" is a type/nullability statement, use `getOrCreateIfReal`; if it's `... == true/false`, use `get`.
    // The point is to only create variables and statements if they lead to useful conclusions; if a variable
    // does not exist, then no statements about it have been made, and if it's synthetic, none will be created later.
    override fun get(flow: Flow, fir: FirElement, unwrapAlias: Boolean): DataFlowVariable? =
        getImpl(flow, fir, createReal = false, unwrapAlias)?.takeSyntheticIfKnown()

    fun getOrCreateIfReal(flow: Flow, fir: FirElement, unwrapAlias: Boolean): DataFlowVariable? =
        getImpl(flow, fir, createReal = true, unwrapAlias)?.takeSyntheticIfKnown()

    fun getOrCreate(flow: Flow, fir: FirElement, unwrapAlias: Boolean): DataFlowVariable =
        getImpl(flow, fir, createReal = true, unwrapAlias)!!.rememberSynthetic()

    fun getRealVariableWithoutUnwrappingAlias(flow: Flow, fir: FirElement): RealVariable? =
        getImpl(flow, fir, createReal = false, unwrapAlias = false) as? RealVariable

    fun getOrCreateRealVariableWithoutUnwrappingAlias(flow: Flow, fir: FirElement): RealVariable? =
        getImpl(flow, fir, createReal = true, unwrapAlias = false) as? RealVariable

    // NOTE: this does not do any checks; don't pass any elements that could map to real variables.
    fun createSynthetic(fir: FirElement): SyntheticVariable =
        fir.unwrapElement().let { syntheticVariables.getOrPut(it) { SyntheticVariable(it, nextVariableIndex) } }

    // Looking up real variables has two "failure modes": when the FIR statement cannot have a real variable in the first place,
    // and when it could if not for `createReal = false`. Having one `null` value does not help us here, so to tell those two
    // situations apart this function has somewhat inconsistent return values:
    //   1. if `fir` maps to a real variable, and `createReal` is true, it returns `RealVariable`
    //      that IS in the `realVariables` map (possibly just added there);
    //   2. if `fir` maps to a real variable, but it's not in the map and `createReal` is false, it returns `null`;
    //   3. if `fir` does not map to a real variable, it returns a `SyntheticVariable`
    //      that IS NOT in the `syntheticVariables` map, so either `takeIfKnown` or `remember` should be called.
    // This way synthetic variables can always be recognized 100% precisely, but using this function requires a bit of care.
    private fun getImpl(flow: Flow, fir: FirElement, createReal: Boolean, unwrapAlias: Boolean): DataFlowVariable? {
        val unwrapped = fir.unwrapElement()
        val synthetic = SyntheticVariable(unwrapped, nextVariableIndex)
        val symbol = unwrapped.symbol ?: return synthetic
        val qualifiedAccess = unwrapped as? FirQualifiedAccessExpression
        val stability = symbol.getStability(qualifiedAccess) ?: return synthetic
        val dispatchReceiverVar = qualifiedAccess?.dispatchReceiver?.let {
            (getImpl(flow, it, createReal, unwrapAlias = true) ?: return null) as? RealVariable ?: return synthetic
        }
        val extensionReceiverVar = qualifiedAccess?.extensionReceiver?.let {
            (getImpl(flow, it, createReal, unwrapAlias = true) ?: return null) as? RealVariable ?: return synthetic
        }
        val isReceiver = qualifiedAccess?.calleeReference is FirThisReference
        val combinedStability = stability
            .combineWithReceiverStability(dispatchReceiverVar?.stability)
            .combineWithReceiverStability(extensionReceiverVar?.stability)
        val prototype = RealVariable(symbol, isReceiver, dispatchReceiverVar, extensionReceiverVar, combinedStability, nextVariableIndex)
        val real = if (createReal) prototype.remember() else prototype.takeIfKnown() ?: return null
        return if (unwrapAlias) flow.unwrapVariable(real) else real
    }

    private fun SmartcastStability.combineWithReceiverStability(receiverStability: SmartcastStability?): SmartcastStability {
        if (receiverStability == null) return this
        return maxOf(this, receiverStability)
    }

    private fun DataFlowVariable.takeSyntheticIfKnown(): DataFlowVariable? =
        if (this is SyntheticVariable) syntheticVariables[fir] else this

    private fun DataFlowVariable.rememberSynthetic(): DataFlowVariable =
        if (this is SyntheticVariable) syntheticVariables.getOrPut(fir) { this } else this

    private fun RealVariable.takeIfKnown(): RealVariable? =
        realVariables[this]

    private fun RealVariable.remember(): RealVariable =
        realVariables.getOrPut(this) {
            dispatchReceiver?.dependentVariables?.add(this)
            extensionReceiver?.dependentVariables?.add(this)
            this
        }

    fun copyRealVariableWithRemapping(variable: RealVariable, from: RealVariable, to: RealVariable): RealVariable {
        // Precondition: `variable in from.dependentVariables`, so at least one of the receivers is `from`.
        return with(variable) {
            val newDispatchReceiver = if (dispatchReceiver == from) to else dispatchReceiver
            val newExtensionReceiver = if (extensionReceiver == from) to else extensionReceiver
            RealVariable(symbol, isReceiver, newDispatchReceiver, newExtensionReceiver, stability, nextVariableIndex).remember()
        }
    }

    fun getOrPut(variable: RealVariable): RealVariable {
        return with(variable) {
            val newDispatchReceiver = dispatchReceiver?.let(::getOrPut)
            val newExtensionReceiver = extensionReceiver?.let(::getOrPut)
            RealVariable(symbol, isReceiver, newDispatchReceiver, newExtensionReceiver, stability, nextVariableIndex).remember()
        }
    }

    private fun FirBasedSymbol<*>.getStability(qualifiedAccess: FirQualifiedAccessExpression?): SmartcastStability? {
        if (qualifiedAccess is FirThisReceiverExpression) return SmartcastStability.STABLE_VALUE
        when (this) {
            is FirAnonymousObjectSymbol -> return null
            is FirFunctionSymbol<*>, is FirClassSymbol<*> -> return SmartcastStability.STABLE_VALUE
            is FirBackingFieldSymbol -> return if (isVal) SmartcastStability.STABLE_VALUE else SmartcastStability.MUTABLE_PROPERTY
        }
        if (this is FirCallableSymbol && this.isExpect) return SmartcastStability.EXPECT_PROPERTY
        if (this !is FirVariableSymbol<*>) return null
        if (this is FirFieldSymbol && !this.isFinal) return SmartcastStability.MUTABLE_PROPERTY

        val property = when (val variable = this.fir) { // intentionally exhaustive 'when'
            is FirEnumEntry, is FirErrorProperty, is FirValueParameter -> return SmartcastStability.STABLE_VALUE

            // NB: FirJavaField is expected here. FirFieldImpl should've been handled by FirBackingFieldSymbol check above
            is FirField -> {
                if (variable.isJava)
                    return variable.determineStabilityByModule()
                else
                    errorWithAttachment("Expected to handle non-Java FirFields via symbol-based checks") {
                        withFirEntry("fir", variable)
                    }
            }

            is FirBackingField -> errorWithAttachment("Expected to handle Backing Field entirely via symbol-based checks") {
                withFirEntry("fir", variable)
            }

            is FirProperty -> variable
        }

        return when {
            property.delegate != null -> SmartcastStability.DELEGATED_PROPERTY
            property.isLocal -> SmartcastStability.STABLE_VALUE
            property.isVar -> SmartcastStability.MUTABLE_PROPERTY
            property.receiverParameter != null -> SmartcastStability.PROPERTY_WITH_GETTER
            property.getter.let { it != null && it !is FirDefaultPropertyAccessor } -> SmartcastStability.PROPERTY_WITH_GETTER
            property.visibility == Visibilities.Private -> SmartcastStability.STABLE_VALUE
            property.modality != Modality.FINAL && qualifiedAccess?.hasFinalDispatchReceiver() != true -> SmartcastStability.PROPERTY_WITH_GETTER
            else -> property.determineStabilityByModule()
        }
    }

    private fun FirElement.hasFinalDispatchReceiver(): Boolean {
        val dispatchReceiver = (this as? FirQualifiedAccessExpression)?.dispatchReceiver ?: return false
        val receiverType = dispatchReceiver.resolvedType.lowerBoundIfFlexible().fullyExpandedType(session)
        val receiverFir = (receiverType as? ConeClassLikeType)?.lookupTag?.toSymbol(session)?.fir ?: return false
        return receiverFir is FirAnonymousObject || receiverFir.modality == Modality.FINAL
    }

    private fun FirVariable.determineStabilityByModule(): SmartcastStability {
        val propertyModuleData = originalOrSelf().moduleData
        val currentModuleData = session.moduleData
        return when (propertyModuleData) {
            currentModuleData,
            in currentModuleData.friendDependencies,
            in currentModuleData.allDependsOnDependencies,
            -> SmartcastStability.STABLE_VALUE
            else -> SmartcastStability.ALIEN_PUBLIC_PROPERTY
        }
    }
}

private tailrec fun FirElement.unwrapElement(): FirElement {
    return when (this) {
        is FirWhenSubjectExpression -> whenRef.value.let { it.subjectVariable ?: it.subject ?: return this }.unwrapElement()
        is FirSmartCastExpression -> originalExpression.unwrapElement()
        is FirSafeCallExpression -> selector.unwrapElement()
        is FirCheckedSafeCallSubject -> originalReceiverRef.value.unwrapElement()
        is FirCheckNotNullCall -> argument.unwrapElement()
        is FirDesugaredAssignmentValueReferenceExpression -> expressionRef.value.unwrapElement()
        is FirVariableAssignment -> lValue.unwrapElement()
        else -> this
    }
}

private val FirElement.symbol: FirBasedSymbol<*>?
    get() = when (this) {
        is FirDeclaration -> symbol.unwrapFakeOverridesIfNecessary()
        is FirResolvable -> calleeReference.symbol.unwrapFakeOverridesIfNecessary()
        is FirResolvedQualifier -> symbol
        // Only called on the result of `unwrapElement, so handling all those cases here is redundant.
        else -> null
    }?.takeIf {
        this is FirThisReceiverExpression || (it !is FirFunctionSymbol<*> && it !is FirSyntheticPropertySymbol)
    }

private fun FirBasedSymbol<*>?.unwrapFakeOverridesIfNecessary(): FirBasedSymbol<*>? {
    if (this !is FirCallableSymbol) return this
    // This is necessary only for sake of optimizations because this is a really hot place.
    // Not having `dispatchReceiverType` means that this is a local/top-level property that can't be a fake override.
    // And at the same time, checking a field is much faster than a couple of attributes (0.3 secs at MT Full Kotlin)
    if (this.dispatchReceiverType == null) return this
    return this.unwrapFakeOverrides()
}
