/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.SmartList
import org.jetbrains.kotlin.KtFakeSourceElementKind.*
import org.jetbrains.kotlin.analysis.api.KtAnalysisNonPublicApi
import org.jetbrains.kotlin.analysis.api.components.KtDataFlowExitPointSnapshot
import org.jetbrains.kotlin.analysis.api.components.KtDataFlowExitPointSnapshot.VariableReassignment
import org.jetbrains.kotlin.analysis.api.components.KtDataFlowInfoProvider
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.utils.unwrap
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirOfType
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.collectUseSiteContainers
import org.jetbrains.kotlin.analysis.utils.errors.withKtModuleEntry
import org.jetbrains.kotlin.analysis.utils.printer.parentsOfType
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.isLocalMember
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirUnitExpression
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.AnonymousObjectExpressionExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNodeWithSubgraphs
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.DelegateExpressionExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ElvisExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ElvisLhsExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ExitNodeMarker
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ExitSafeCallNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ExitValueParameterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.LocalClassExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.PostponedLambdaExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.SmartCastExpressionExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.StubNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.WhenBranchResultExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.WhenSubjectExpressionExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry
import java.util.ArrayList
import kotlin.math.sign

@OptIn(KtAnalysisNonPublicApi::class)
internal class KtFirDataFlowInfoProvider(override val analysisSession: KtFirAnalysisSession) : KtDataFlowInfoProvider() {
    private val firResolveSession: LLFirResolveSession
        get() = analysisSession.firResolveSession

    override fun getExitPointSnapshot(statements: List<KtExpression>): KtDataFlowExitPointSnapshot {
        val firParent = getCommonParent(statements)

        val unwrappedStatements = statements.map { it.unwrap() }

        val statementSearcher = FirStatementSearcher(unwrappedStatements)
        firParent.accept(statementSearcher)

        val firStatementPaths = unwrappedStatements
            .map { statementSearcher.getPath(it) ?: listOf(firParent, it.getOrBuildFirOfType<FirElement>(firResolveSession)) }

        val firStatements = firStatementPaths.map { it.last() }

        val collector = FirElementCollector()
        firStatements.forEach { it.accept(collector) }

        val firValuedReturnExpressions = collector.firReturnExpressions.filter { !it.result.resolvedType.isUnit }

        val firDefaultStatementCandidate = firStatements.last()
        val defaultExpressionInfo = computeDefaultExpression(statements, firDefaultStatementCandidate, firValuedReturnExpressions)

        val graphIndex = ControlFlowGraphIndex {
            getControlFlowGraph(anchor = statements.first(), firAnchorPath = firStatementPaths.first())
        }

        val jumpExpressions = buildList {
            fun add(expressions: List<FirElement>) {
                expressions.mapNotNullTo(this) { it.psi as? KtExpression }
            }

            add(collector.firReturnExpressions)
            add(collector.firBreakExpressions)
            add(collector.firContinueExpressions)
        }

        return KtDataFlowExitPointSnapshot(
            defaultExpressionInfo = defaultExpressionInfo,
            valuedReturnExpressions = firValuedReturnExpressions.mapNotNull { it.psi as? KtExpression },
            returnValueType = computeReturnType(firValuedReturnExpressions),
            jumpExpressions = jumpExpressions,
            hasJumps = collector.hasJumps,
            hasEscapingJumps = graphIndex.computeHasEscapingJumps(firDefaultStatementCandidate, collector),
            hasMultipleJumpKinds = collector.hasMultipleJumpKinds,
            hasMultipleJumpTargets = graphIndex.computeHasMultipleJumpTargets(collector),
            variableReassignments = collector.variableReassignments
        )
    }

    private fun getCommonParent(statements: List<KtElement>): FirElement {
        require(statements.isNotEmpty())

        val parent = statements[0].parent as KtElement

        for (i in 1..<statements.size) {
            require(statements[i].parent == parent)
        }

        val firContainer = collectUseSiteContainers(parent, firResolveSession)?.lastOrNull()
        if (firContainer != null) {
            firContainer.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
            return firContainer
        }

        return parent.parentsWithSelf
            .filterIsInstance<KtElement>()
            .firstNotNullOf { it.getOrBuildFir(firResolveSession) }
    }

    private fun computeDefaultExpression(
        statements: List<KtExpression>,
        firDefaultStatement: FirElement,
        firValuedReturnExpressions: List<FirReturnExpression>
    ): KtDataFlowExitPointSnapshot.DefaultExpressionInfo? {
        val defaultExpressionFromPsi = statements.last()

        if (firDefaultStatement in firValuedReturnExpressions) {
            return null
        }

        when (firDefaultStatement) {
            !is FirExpression,
            is FirJump<*>,
            is FirThrowExpression,
            is FirUnitExpression,
            is FirErrorExpression -> {
                return null
            }

            is FirBlock,
            is FirResolvedQualifier -> {
                if (firDefaultStatement.resolvedType.isUnit) {
                    return null
                }
            }
        }

        @Suppress("USELESS_IS_CHECK") // K2 warning suppression, TODO: KT-62472
        require(firDefaultStatement is FirExpression)

        val defaultExpressionFromFir = firDefaultStatement.psi as? KtExpression ?: return null

        if (!PsiTreeUtil.isAncestor(defaultExpressionFromFir, defaultExpressionFromPsi, false)) {
            // In certain cases, expressions might be different in PSI and FIR sources.
            // E.g., in 'foo.<expr>bar()</expr>', there is no FIR expression that corresponds to the 'bar()' KtCallExpression.
            return null
        }

        val defaultConeType = firDefaultStatement.resolvedType
        if (defaultConeType.isNothing) {
            return null
        }

        val defaultType = defaultConeType.toKtType()
        return KtDataFlowExitPointSnapshot.DefaultExpressionInfo(defaultExpressionFromPsi, defaultType)
    }

    private fun computeReturnType(firReturnExpressions: List<FirReturnExpression>): KtType? {
        val coneTypes = ArrayList<ConeKotlinType>(firReturnExpressions.size)

        for (firReturnExpression in firReturnExpressions) {
            val coneType = firReturnExpression.result.resolvedType
            if (coneType.isUnit) {
                return null
            }

            coneTypes.add(coneType)
        }

        return analysisSession.useSiteSession.typeContext.commonSuperTypeOrNull(coneTypes)?.toKtType()
    }

    private fun ControlFlowGraphIndex.computeHasEscapingJumps(firDefaultStatement: FirElement, collector: FirElementCollector): Boolean {
        val firTargets = buildSet<FirElement> {
            add(firDefaultStatement)
            addAll(collector.firReturnExpressions)
            addAll(collector.firBreakExpressions)
            addAll(collector.firContinueExpressions)
        }

        return hasMultipleExitPoints(firTargets)
    }

    private fun ControlFlowGraphIndex.computeHasMultipleJumpTargets(collector: FirElementCollector): Boolean {
        val firTargets = buildSet<FirElement> {
            addAll(collector.firReturnExpressions)
            addAll(collector.firBreakExpressions)
            addAll(collector.firContinueExpressions)
        }

        return hasMultipleExitPoints(firTargets)
    }

    private fun getControlFlowGraph(anchor: KtElement, firAnchorPath: List<FirElement>): ControlFlowGraph {
        return findControlFlowGraph(anchor, firAnchorPath)
            ?: errorWithAttachment("Cannot find a control flow graph for element") {
                withKtModuleEntry("module", analysisSession.useSiteModule)
                withPsiEntry("anchor", anchor)
                withFirEntry("firAnchor", firAnchorPath.last())
            }
    }

    private fun findControlFlowGraph(anchor: KtElement, firAnchorPath: List<FirElement>): ControlFlowGraph? {
        /**
         * Not all expressions appear in the [ControlFlowGraph].
         * Still, if we find at least some of them, it's very unlikely that we will ever find a better graph.
         */
        val firCandidates = buildSet {
            fun addCandidate(firCandidate: FirElement) {
                add(firCandidate)

                if (firCandidate is FirBlock) {
                    firCandidate.statements.forEach(::addCandidate)
                }
            }

            // Drop the first path chunk as it's the statement parent
            for (firChunk in firAnchorPath.subList(1, firAnchorPath.size)) {
                addCandidate(firChunk)
            }
        }

        val parentDeclarations = anchor.parentsOfType<KtDeclaration>(withSelf = true)
        for (parentDeclaration in parentDeclarations) {
            val parentFirDeclaration = parentDeclaration.getOrBuildFir(firResolveSession)
            if (parentFirDeclaration is FirControlFlowGraphOwner) {
                val graph = parentFirDeclaration.controlFlowGraphReference?.controlFlowGraph
                if (graph != null && graph.contains(firCandidates)) {
                    return graph
                }
            }
        }

        return null
    }

    private fun ControlFlowGraphIndex.hasMultipleExitPoints(firTargets: Set<FirElement>): Boolean {
        if (firTargets.size < 2) {
            return false
        }

        val exitPoints = firTargets
            .mapNotNull { findLast(it) }
            .flatMap { node ->
                node.followingNodes
                    .filter { it !is StubNode }
                    .map { it.unwrap() }
                    .distinct()
                    .sortedBy { it.id }
            }.distinct()

        return exitPoints.size > 1
    }

    private fun CFGNode<*>.unwrap(): CFGNode<*> {
        var current = this

        while (current.isExitNode()) {
            val following = current.followingNodes
            if (following.size == 1) {
                current = following.first()
            } else {
                break
            }
        }

        return current
    }

    private fun CFGNode<*>.isExitNode(): Boolean {
        return when (this) {
            is ExitNodeMarker, is ExitValueParameterNode, is WhenSubjectExpressionExitNode, is AnonymousObjectExpressionExitNode,
            is SmartCastExpressionExitNode, is PostponedLambdaExitNode, is DelegateExpressionExitNode, is WhenBranchResultExitNode,
            is ElvisExitNode, is ExitSafeCallNode, is LocalClassExitNode, is ElvisLhsExitNode -> {
                true
            }
            else -> {
                false
            }
        }
    }

    /**
     * Returns `true` if the control graph contains at least one of the [firCandidates].
     */
    private fun ControlFlowGraph.contains(firCandidates: Set<FirElement>): Boolean {
        for (node in nodes) {
            if (node.fir in firCandidates) {
                return true
            }
            if (node is CFGNodeWithSubgraphs<*> && node.subGraphs.any { it.contains(firCandidates) }) {
                return true
            }
        }

        return false
    }

    /**
     * This class, unlike 'getOrBuildFirOfType()', tries to find topmost FIR elements, together with the path to them.
     * This is useful when PSI expressions get wrapped in the FIR tree, such as implicit return expressions from lambdas.
     */
    private inner class FirStatementSearcher(statements: List<KtExpression>) : FirDefaultVisitorVoid() {
        private val statements = statements.toHashSet()

        private val mapping = LinkedHashMap<KtExpression, List<FirElement>>()
        private val stack = ArrayDeque<FirElement>()
        private var unmappedCount = statements.size

        fun getPath(statement: KtExpression): List<FirElement>? {
            return mapping[statement]
        }

        override fun visitElement(element: FirElement) {
            withElement(element) {
                val psi = element.psi

                if (psi is KtExpression && psi in statements) {
                    mapping.computeIfAbsent(psi) { _ ->
                        unmappedCount -= 1
                        buildList {
                            addAll(stack)
                            addAll(computePathLeaves(element))
                        }
                    }
                }

                if (unmappedCount > 0) {
                    element.acceptChildren(this)
                }
            }
        }

        private fun withElement(element: FirElement, block: () -> Unit) {
            stack.addLast(element)
            try {
                block()
            } finally {
                stack.removeLast()
            }
        }

        private fun computePathLeaves(element: FirElement): List<FirElement> {
            if (element is FirBlock && element.statements.size == 1) {
                val statement = element.statements[0]
                if (statement is FirExpression && statement.resolvedType == element.resolvedType) {
                    // Trivial blocks might not appear in the CFG, so here we include their content
                    return listOf(statement)
                }
            }

            return emptyList()
        }
    }

    private inner class FirElementCollector : FirDefaultVisitorVoid() {
        val hasJumps: Boolean
            get() = firReturnTargets.isNotEmpty() || firLoopJumpTargets.isNotEmpty()

        val hasMultipleJumpKinds: Boolean
            get() = (firReturnExpressions.size.sign + firBreakExpressions.size.sign + firContinueExpressions.size.sign) > 1

        val variableReassignments = mutableListOf<VariableReassignment>()

        val firReturnExpressions = mutableListOf<FirReturnExpression>()
        val firBreakExpressions = mutableListOf<FirBreakExpression>()
        val firContinueExpressions = mutableListOf<FirContinueExpression>()

        private val firReturnTargets = mutableSetOf<FirFunction>()
        private val firLoopJumpTargets = mutableSetOf<FirLoop>()

        private val firFunctionDeclarations = mutableSetOf<FirFunction>()
        private val firLoopStatements = mutableSetOf<FirLoop>()

        override fun visitElement(element: FirElement) {
            element.acceptChildren(this)
        }

        override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction) = visitFunction(anonymousFunction)
        override fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor) = visitFunction(propertyAccessor)
        override fun visitSimpleFunction(simpleFunction: FirSimpleFunction) = visitFunction(simpleFunction)
        override fun visitErrorFunction(errorFunction: FirErrorFunction) = visitFunction(errorFunction)
        override fun visitConstructor(constructor: FirConstructor) = visitFunction(constructor)
        override fun visitErrorPrimaryConstructor(errorPrimaryConstructor: FirErrorPrimaryConstructor) = visitFunction(errorPrimaryConstructor)

        override fun visitFunction(function: FirFunction) {
            firFunctionDeclarations.add(function)
            super.visitFunction(function)
        }

        override fun visitLoop(loop: FirLoop) {
            firLoopStatements.add(loop)
            super.visitLoop(loop)
        }

        override fun visitReturnExpression(returnExpression: FirReturnExpression) {
            if (returnExpression.target.labeledElement !in firFunctionDeclarations) {
                firReturnExpressions.add(returnExpression)
                firReturnTargets.add(returnExpression.target.labeledElement)
            }
            super.visitReturnExpression(returnExpression)
        }

        override fun visitBreakExpression(breakExpression: FirBreakExpression) {
            if (breakExpression.target.labeledElement !in firLoopStatements) {
                firBreakExpressions.add(breakExpression)
                firLoopJumpTargets.add(breakExpression.target.labeledElement)
            }
            super.visitBreakExpression(breakExpression)
        }

        override fun visitContinueExpression(continueExpression: FirContinueExpression) {
            if (continueExpression.target.labeledElement !in firLoopStatements) {
                firContinueExpressions.add(continueExpression)
                firLoopJumpTargets.add(continueExpression.target.labeledElement)
            }
            super.visitContinueExpression(continueExpression)
        }

        override fun visitVariableAssignment(variableAssignment: FirVariableAssignment) {
            val firVariableSymbol = variableAssignment.lValue.toResolvedCallableSymbol(analysisSession.useSiteSession)
            val expression = variableAssignment.psi as? KtExpression

            if (firVariableSymbol is FirVariableSymbol<*> && firVariableSymbol.fir.isLocalMember && expression != null) {
                val variableSymbol = analysisSession.firSymbolBuilder.variableLikeBuilder.buildVariableLikeSymbol(firVariableSymbol)
                val reassignment = VariableReassignment(expression, variableSymbol, variableAssignment.isAugmented())
                variableReassignments.add(reassignment)
            }

            super.visitVariableAssignment(variableAssignment)
        }

        private fun FirVariableAssignment.isAugmented(): Boolean {
            val targetSource = lValue.source
            if (targetSource != null) {
                when (targetSource.kind) {
                    is DesugaredCompoundAssignment, is DesugaredIncrementOrDecrement -> return true
                    else -> {}
                }
            }

            return false
        }
    }

    private fun ConeKotlinType.toKtType(): KtType {
        return analysisSession.firSymbolBuilder.typeBuilder.buildKtType(this)
    }
}

private class ControlFlowGraphIndex(graphProvider: () -> ControlFlowGraph) {
    private val mapping: Map<FirElement, List<CFGNode<*>>> by lazy {
        val result = HashMap<FirElement, MutableList<CFGNode<*>>>()

        fun addGraph(graph: ControlFlowGraph) {
            for (node in graph.nodes) {
                result.getOrPut(node.fir) { SmartList() }.add(node)

                if (node is CFGNodeWithSubgraphs<*>) {
                    node.subGraphs.forEach(::addGraph)
                }
            }
        }

        addGraph(graphProvider())

        return@lazy result
    }

    /**
     * Find the last node in a graph (or its subgraphs) that point to the given [fir] element.
     */
    fun findLast(fir: FirElement): CFGNode<*>? {
        val directNodes = mapping[fir]
        if (directNodes != null) {
            return directNodes.last()
        }

        if (fir is FirBlock) {
            return fir.statements
                .asReversed()
                .firstNotNullOfOrNull(::findLast)
        }

        return null
    }
}