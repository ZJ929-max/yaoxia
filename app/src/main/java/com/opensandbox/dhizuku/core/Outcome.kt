package com.opensandbox.dhizuku.core

/** 一次操作的结果：要么带值成功，要么带原因失败。 */
sealed class Outcome<out T> {

    data class Ok<out T>(val value: T) : Outcome<T>()

    data class Failure(val reason: String) : Outcome<Nothing>()

    val isSuccess: Boolean get() = this is Ok

    fun valueOrNull(): T? = (this as? Ok)?.value

    fun errorOrNull(): String? = (this as? Failure)?.reason

    companion object {
        fun <T> ok(value: T): Outcome<T> = Ok(value)

        fun <T> fail(reason: String): Outcome<T> = Failure(reason)

        fun <T> fail(throwable: Throwable): Outcome<T> = Failure(
            throwable.javaClass.simpleName + (throwable.message?.let { ": $it" } ?: "")
        )

        fun <T> catching(block: () -> T): Outcome<T> =
            try {
                ok(block())
            } catch (throwable: Throwable) {
                fail(throwable)
            }
    }
}
