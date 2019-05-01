package io.github.saltyJeff.launchpad

fun <T> Collection<T>.csvJoin(mapper: (T) -> CharSequence): String = this.joinToString(separator = ",", transform = mapper)
fun <T> Collection<T>.csvJoin(): String = this.joinToString(separator = ",")