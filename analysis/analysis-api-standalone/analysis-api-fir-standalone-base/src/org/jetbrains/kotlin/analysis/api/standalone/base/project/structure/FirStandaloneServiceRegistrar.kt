/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.project.structure

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.ide.plugins.PluginXmlPathResolver
import com.intellij.ide.plugins.RawPluginDescriptor
import com.intellij.ide.plugins.ReadModuleContext
import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.extensions.DefaultPluginDescriptor
import com.intellij.platform.util.plugins.DataLoader
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.impl.PsiElementFinderImpl
import com.intellij.util.NoOpXmlInterner
import com.intellij.util.lang.ZipFilePool
import com.intellij.util.messages.ListenerDescriptor
import com.intellij.util.messages.impl.MessageBusEx
import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals
import org.jetbrains.kotlin.analysis.providers.analysisMessageBus
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.utils.SmartList
import java.io.InputStream

@Suppress("UnstableApiUsage")
@OptIn(LLFirInternals::class, KtAnalysisApiInternals::class)
object FirStandaloneServiceRegistrar : AnalysisApiStandaloneServiceRegistrar {
    private val pluginDescriptor: RawPluginDescriptor by lazy {
        val readContext = object : ReadModuleContext {
            override val interner get() = NoOpXmlInterner
            override val isMissingIncludeIgnored: Boolean get() = false
        }

        val dataLoader = object : DataLoader {
            override val pool: ZipFilePool? get() = null
            override fun load(path: String): InputStream? = ClassLoader.getSystemResource(path)?.openStream()
            override fun toString(): String = "resources data loader"
        }

        val descriptor = RawPluginDescriptor()
        PluginXmlPathResolver.DEFAULT_PATH_RESOLVER.resolvePath(
            readContext = readContext,
            dataLoader = dataLoader,
            relativePath = "analysis-api-fir-standalone-base.xml",
            readInto = descriptor,
        )

        descriptor
    }

    override fun registerApplicationServices(application: MockApplication) {}

    override fun registerProjectExtensionPoints(project: MockProject) {
        for (extensionPointDescriptor in pluginDescriptor.projectContainerDescriptor.extensionPoints.orEmpty()) {
            CoreApplicationEnvironment.registerExtensionPoint(
                project.extensionArea,
                extensionPointDescriptor.name,
                Class.forName(extensionPointDescriptor.className),
            )
        }
    }

    override fun registerProjectServices(project: MockProject) {
        for (serviceDescriptor in pluginDescriptor.projectContainerDescriptor.services) {
            val serviceImplementationClass = Class.forName(serviceDescriptor.serviceImplementation)
            val serviceInterface = serviceDescriptor.serviceInterface
            if (serviceInterface != null) {
                val serviceInterfaceClass = Class.forName(serviceInterface)

                @Suppress("UNCHECKED_CAST")
                project.registerServiceWithInterface(serviceInterfaceClass as Class<Any>, serviceImplementationClass as Class<Any>)
            } else {
                project.registerService(serviceImplementationClass)
            }
        }

        registerProjectListeners(project)
    }

    private fun registerProjectListeners(project: MockProject) {
        val listenerDescriptors = pluginDescriptor.projectContainerDescriptor.listeners.orEmpty().ifEmpty {
            return
        }

        val pluginDescriptor = DefaultPluginDescriptor("analysis-api-fir-standalone")
        val listenersMap = mutableMapOf<String, MutableList<ListenerDescriptor>>()
        for (listenerDescriptor in listenerDescriptors) {
            listenerDescriptor.pluginDescriptor = pluginDescriptor
            listenersMap.computeIfAbsent(listenerDescriptor.topicClassName) { SmartList() }.add(listenerDescriptor)
        }

        (project.analysisMessageBus as MessageBusEx).setLazyListeners(listenersMap)
    }

    @Suppress("TestOnlyProblems")
    override fun registerProjectModelServices(project: MockProject, disposable: Disposable) {
        with(PsiElementFinder.EP.getPoint(project)) {
            registerExtension(JavaElementFinder(project), disposable)
            registerExtension(PsiElementFinderImpl(project), disposable)
        }
    }
}

// workaround for ambiguity resolution
private fun <T> MockProject.registerServiceWithInterface(interfaceClass: Class<T>, implementationClass: Class<T>) {
    registerService(interfaceClass, implementationClass)
}
