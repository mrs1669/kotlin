/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers

//import com.intellij.core.CoreApplicationEnvironment
//import com.intellij.ide.plugins.PluginXmlPathResolver
//import com.intellij.ide.plugins.RawPluginDescriptor
//import com.intellij.ide.plugins.ReadModuleContext
//import com.intellij.mock.MockApplication
//import com.intellij.mock.MockProject
//import com.intellij.openapi.extensions.DefaultPluginDescriptor
//import com.intellij.platform.util.plugins.DataLoader
//import
//import com.intellij.util.NoOpXmlInterner
//import com.intellij.util.containers.ContainerUtil
//import com.intellij.util.lang.ZipFilePool
//import com.intellij.util.messages.ListenerDescriptor
//import com.intellij.util.messages.impl.MessageBusEx
//import org.jetbrains.kotlin.analysis.project.structure.KtModuleStructureInternals
//import org.jetbrains.kotlin.utils.SmartList
//import java.io.InputStream
//
//@KtModuleStructureInternals
//public object PluginStructureProvider {
//    private object ReadContext : ReadModuleContext {
//        override val interner get() = NoOpXmlInterner
//        override val isMissingIncludeIgnored: Boolean get() = false
//    }
//
//    private object ResourceDataLoader : DataLoader {
//        override val pool: ZipFilePool? get() = null
//        override fun load(path: String): InputStream? = ClassLoader.getSystemResource(path)?.openStream()
//        override fun toString(): String = "resources data loader"
//    }
//
//    private val pluginDescriptors = ContainerUtil.createConcurrentSoftKeySoftValueMap<String, RawPluginDescriptor>()
//
//    private fun getOrCalculatePluginDescriptor(relativePath: String): RawPluginDescriptor = pluginDescriptors.computeIfAbsent(relativePath) {
//        val descriptor = RawPluginDescriptor()
//        PluginXmlPathResolver.DEFAULT_PATH_RESOLVER.resolvePath(
//            readContext = ReadContext,
//            dataLoader = ResourceDataLoader,
//            relativePath = relativePath,
//            readInto = descriptor,
//        )
//
//        descriptor
//    }
//
//    public fun registerApplicationServices(application: MockApplication) {}
//
//    public fun registerProjectExtensionPoints(project: MockProject) {
//        for (extensionPointDescriptor in pluginDescriptor.projectContainerDescriptor.extensionPoints.orEmpty()) {
//            CoreApplicationEnvironment.registerExtensionPoint(
//                project.extensionArea,
//                extensionPointDescriptor.name,
//                Class.forName(extensionPointDescriptor.className),
//            )
//        }
//    }
//
//    public fun registerProjectServices(project: MockProject) {
//        for (serviceDescriptor in pluginDescriptor.projectContainerDescriptor.services) {
//            val serviceImplementationClass = Class.forName(serviceDescriptor.serviceImplementation)
//            val serviceInterface = serviceDescriptor.serviceInterface
//            if (serviceInterface != null) {
//                val serviceInterfaceClass = Class.forName(serviceInterface)
//
//                @Suppress("UNCHECKED_CAST")
//                project.registerServiceWithInterface(serviceInterfaceClass as Class<Any>, serviceImplementationClass as Class<Any>)
//            } else {
//                project.registerService(serviceImplementationClass)
//            }
//        }
//
//        registerProjectListeners(project)
//    }
//
//    public fun registerProjectListeners(project: MockProject) {
//        val listenerDescriptors = pluginDescriptor.projectContainerDescriptor.listeners.orEmpty().ifEmpty {
//            return
//        }
//
//        val pluginDescriptor = DefaultPluginDescriptor("analysis-api-fir-standalone")
//        val listenersMap = mutableMapOf<String, MutableList<ListenerDescriptor>>()
//        for (listenerDescriptor in listenerDescriptors) {
//            listenerDescriptor.pluginDescriptor = pluginDescriptor
//            listenersMap.computeIfAbsent(listenerDescriptor.topicClassName) { SmartList() }.add(listenerDescriptor)
//        }
//
//        (project.analysisMessageBus as MessageBusEx).setLazyListeners(listenersMap)
//    }
//
//    // workaround for ambiguity resolution
//    private fun <T> MockProject.registerServiceWithInterface(interfaceClass: Class<T>, implementationClass: Class<T>) {
//        registerService(interfaceClass, implementationClass)
//    }
//}
