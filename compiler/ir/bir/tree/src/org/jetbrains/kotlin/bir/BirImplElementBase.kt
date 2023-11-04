/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir

import kotlin.experimental.or

abstract class BirImplElementBase : BirElementBase() {
    // bits reservation: 0 - parent, 1-n - child element lists, n-14 - all other properties, 15 - dynamic properties
    private var observedPropertiesBitSet: Short = 0
    private var dependentIndexedElements: Any? = null // null | BirImplElementBase | Array<BirImplElementBase?>

    final override val parent: BirElementBase?
        get() {
            recordPropertyRead(PARENT_PROPERTY_ID)
            return _parent as? BirElementBase
        }

    internal fun getParentRecordingRead(): BirElementParent? {
        recordPropertyRead(PARENT_PROPERTY_ID)
        return _parent
    }

    final override fun setParentWithInvalidation(new: BirElementParent?) {
        if (_parent !== new) {
            _parent = new
            invalidate(PARENT_PROPERTY_ID)
        }
    }


    protected fun initChild(new: BirElement?) {
        childReplaced(null, new)
    }

    internal fun childReplaced(old: BirElement?, new: BirElement?) {
        if (old != null) {
            old as BirImplElementBase

            old.setParentWithInvalidation(null)
            root?.elementDetached(old)
        }

        if (new != null) {
            new as BirImplElementBase

            val oldParent = new._parent
            if (oldParent != null) {
                new.replacedWithInternal(null)
                new.setParentWithInvalidation(this)
                if (oldParent is BirImplElementBase) {
                    oldParent.invalidate()
                }

                root?.elementMoved(new, oldParent)
            } else {
                new.setParentWithInvalidation(this)
                root?.elementAttached(new)
            }
        }
    }


    internal open fun replaceChildProperty(old: BirElement, new: BirElement?) {
        throwChildForReplacementNotFound(old)
    }

    protected fun throwChildForReplacementNotFound(old: BirElement): Nothing {
        throw IllegalStateException("The child property $old not found in its parent $this")
    }


    override fun replaceWith(new: BirElement?) {
        if (this === new) return

        val parent = replacedWithInternal(new as BirImplElementBase?)
        if (parent is BirImplElementBase) {
            parent.childReplaced(this, new)
            parent.invalidate()
        }
    }

    internal fun replacedWithInternal(new: BirImplElementBase?): BirElementParent? {
        val parent = _parent
        if (parent is BirImplElementBase) {
            val list = getContainingList() as BirImplChildElementList<*>?
            if (list != null) {
                val found = if (new == null && !list.isNullable) {
                    list.removeInternal(this)
                } else {
                    @Suppress("UNCHECKED_CAST")
                    list as BirChildElementList<BirImplElementBase?>
                    list.replaceInternal(this, new)
                }

                if (!found) {
                    list.parent.throwChildForReplacementNotFound(this)
                }
            } else {
                parent.replaceChildProperty(this, new)
            }
        }
        return parent
    }

    protected fun throwChildElementRemoved(propertyName: String): Nothing {
        throw IllegalStateException("The child property $propertyName has been removed from this element $this")
    }


    final override fun <T> getDynamicProperty(token: BirElementDynamicPropertyToken<*, T>): T? {
        recordPropertyRead(DYNAMIC_PROPERTY_ID)
        return super.getDynamicProperty(token)
    }

    final override fun <T> setDynamicProperty(token: BirElementDynamicPropertyToken<*, T>, value: T?): Boolean {
        val changed = super.setDynamicProperty(token, value)
        if (changed) {
            invalidate(DYNAMIC_PROPERTY_ID)
        }
        return changed
    }

    internal fun <T> getOrPutDynamicProperty(token: BirElementDynamicPropertyToken<*, T>, compute: () -> T): T {
        // todo: why asserts do run?
        //assert(root?.isInsideElementClassification != true)

        val arrayMap = dynamicProperties
        if (arrayMap == null) {
            val value = compute()
            initializeDynamicProperties(token, value)
            invalidate(DYNAMIC_PROPERTY_ID)
            return value
        }

        val keyIndex = findDynamicPropertyIndex(arrayMap, token)
        if (keyIndex >= 0) {
            @Suppress("UNCHECKED_CAST")
            return arrayMap[keyIndex + 1] as T
        } else {
            val value = compute()
            val valueIndex = -keyIndex + 1
            arrayMap[valueIndex] = value
            invalidate(DYNAMIC_PROPERTY_ID)
            return value
        }
    }

    // todo: fine-grained control of which data to copy
    internal fun copyDynamicProperties(from: BirElementBase) {
        invalidate(DYNAMIC_PROPERTY_ID)
        dynamicProperties = from.dynamicProperties?.copyOf()
    }


    internal fun invalidate() {
        root?.elementIndexInvalidated(this)
    }

    internal fun invalidate(propertyId: Int) {
        if ((observedPropertiesBitSet.toInt() and (1 shl propertyId)) != 0) {
            invalidate()
        }
    }

    internal fun recordPropertyRead(propertyId: Int) {
        val root = root ?: return
        val classifiedElement = root.mutableElementCurrentlyBeingClassified ?: return
        if (classifiedElement === this) {
            observedPropertiesBitSet = observedPropertiesBitSet or (1 shl propertyId).toShort()
        } else {
            registerDependentElement(classifiedElement)
        }
    }

    private fun registerDependentElement(dependentElement: BirImplElementBase) {
        if (dependentElement === this) {
            return
        }

        val RESIZE_GRADUALITY = 4
        var elementsOrSingle = dependentIndexedElements
        when (elementsOrSingle) {
            null -> {
                dependentIndexedElements = dependentElement
            }
            is BirImplElementBase -> {
                if (elementsOrSingle === dependentElement) {
                    return
                }

                val elements = arrayOfNulls<BirImplElementBase>(RESIZE_GRADUALITY)
                elements[0] = elementsOrSingle
                elements[1] = dependentElement
                dependentIndexedElements = elements
            }
            else -> {
                @Suppress("UNCHECKED_CAST")
                elementsOrSingle as Array<BirImplElementBase?>

                var newIndex = 0
                while (newIndex < elementsOrSingle.size) {
                    val e = elementsOrSingle[newIndex]
                    if (e == null) {
                        break
                    } else if (e === dependentElement) {
                        return
                    }
                    newIndex++
                }

                if (newIndex == elementsOrSingle.size) {
                    elementsOrSingle = elementsOrSingle.copyOf(elementsOrSingle.size + RESIZE_GRADUALITY)
                    dependentIndexedElements = elementsOrSingle
                }
                elementsOrSingle[newIndex] = dependentElement
            }
        }
    }

    internal fun invalidateDependentElements() {
        when (val elementsOrSingle = dependentIndexedElements) {
            null -> {}
            is BirImplElementBase -> {
                dependentIndexedElements = null
                elementsOrSingle.invalidate()
            }
            else -> {
                @Suppress("UNCHECKED_CAST")
                var array = elementsOrSingle as Array<BirImplElementBase?>
                var arraySize = array.size
                var i = 0
                while (i < arraySize) {
                    val e = array[i] ?: break
                    val arrayIsFull = array[arraySize - 1] != null

                    array[i] = null
                    e.invalidate()

                    if (arrayIsFull && array !== dependentIndexedElements) {
                        @Suppress("UNCHECKED_CAST")
                        array = dependentIndexedElements as Array<BirImplElementBase?>
                        arraySize = array.size
                    }

                    i++
                }
            }
        }
    }


    fun unsafeDispose() {
        acceptChildrenLite {
            (it as BirImplElementBase).setParentWithInvalidation(null)
            //childDetached(it)
        }
        // todo: mark as disposed
    }

    companion object {
        private const val PARENT_PROPERTY_ID = 0
        private const val DYNAMIC_PROPERTY_ID = 15
    }
}