package com.messieurme.vktesttask.classes

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.Integer.max
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class PriorityArrayList<T> : ArrayList<T> {

    constructor() : super()
    constructor(size: Int) : super(size)
    constructor(collection: Collection<T>) : super(collection)

    private var lock = Mutex()

    suspend fun addLast(element: T): Boolean {
        lock.withLock {
            return super.add(element)
        }
    }

    suspend fun addAllPreLast(elements: Collection<T>): Boolean {
        lock.withLock {
            when (this.size) {
                0 -> super.addAll(elements)
                1 -> {
                    val first = this.first()
                    this.removeAt(0)
                    super.addAll(elements)
                    super.add(first)
                }
                else -> {
                    println("STRANGE SIZE ${this.size}")
                }
            }
        }
        return true
    }
}