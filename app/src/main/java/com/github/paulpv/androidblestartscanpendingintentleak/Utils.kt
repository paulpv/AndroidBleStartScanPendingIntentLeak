package com.github.paulpv.androidblestartscanpendingintentleak

/**
 * Identical to [.repr], but grammatically intended for Strings.
 *
 * @param value value
 * @return "null", or '\"' + value.toString + '\"', or value.toString()
 */
fun quote(value: Any?): String? {
    return repr(value, false)
}

/**
 * Identical to [.quote], but grammatically intended for Objects.
 *
 * @param value value
 * @return "null", or '\"' + value.toString + '\"', or value.toString()
 */
fun repr(value: Any?): String? {
    return repr(value, false)
}

/**
 * @param value    value
 * @param typeOnly typeOnly
 * @return "null", or '\"' + value.toString + '\"', or value.toString(), or getShortClassName(value)
 */
fun repr(value: Any?, typeOnly: Boolean): String? {
    if (value == null) {
        return "null"
    }

    if (value is String) {
        return '\"'.toString() + value.toString() + '\"'.toString()
    }

    if (typeOnly) {
        return getShortClassName(value)
    }

    return if (value is Array<*>) {
        toString(value as Array<*>?)
    } else value.toString()
}

fun getShortClassName(o: Any?): String? {
    return getShortClassName(o?.javaClass)
}

fun toString(items: Array<*>?): String {
    val sb = StringBuilder()

    if (items == null) {
        sb.append("null")
    } else {
        sb.append('[')
        for (i in items.indices) {
            if (i != 0) {
                sb.append(", ")
            }
            val item = items[i]
            sb.append(repr(item))
        }
        sb.append(']')
    }

    return sb.toString()
}
