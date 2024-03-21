package test

typealias StringAlias = String
typealias EntryAlias = Map.Entry<String, Int>

typealias NestedStringAlias = StringAlias

typealias ParameterizedListAlias<A> = List<A>

typealias NestedListAliasWithTypeArgument = ParameterizedListAlias<String>
typealias NestedListAliasWithAliasTypeArgument = ParameterizedListAlias<StringAlias>
typealias NestedListAliasWithNestedAliasTypeArgument = ParameterizedListAlias<NestedStringAlias>

typealias NestedParameterizedListAlias<A> = ParameterizedListAlias<ParameterizedListAlias<A>>

class TypeAliasExpansion {
    fun entryToString(entry: EntryAlias): StringAlias = entry.key

    val nestedStringAlias: NestedStringAlias = ""

    val parameterizedListAliasWithString: ParameterizedListAlias<String> = emptyList()
    val parameterizedListAliasWithStringAlias: ParameterizedListAlias<StringAlias> = emptyList()
    val parameterizedListAliasWithNestedStringAlias: ParameterizedListAlias<NestedStringAlias> = emptyList()
    val parameterizedListAliasWithParameterizedListAliasWithStringAlias: ParameterizedListAlias<ParameterizedListAlias<StringAlias>> =
        emptyList()

    val nestedListAliasWithTypeArgument: NestedListAliasWithTypeArgument = emptyList()
    val nestedListAliasWithAliasTypeArgument: NestedListAliasWithAliasTypeArgument = emptyList()
    val nestedListAliasWithNestedAliasTypeArgument: NestedListAliasWithNestedAliasTypeArgument = emptyList()

    val nestedParameterizedListAliasWithString: NestedParameterizedListAlias<String> = emptyList()
    val nestedParameterizedListAliasWithStringAlias: NestedParameterizedListAlias<StringAlias> = emptyList()
    val nestedParameterizedListAliasWithNestedStringAlias: NestedParameterizedListAlias<NestedStringAlias> = emptyList()
}
