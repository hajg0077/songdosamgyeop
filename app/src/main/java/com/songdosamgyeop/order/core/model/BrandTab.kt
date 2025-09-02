package com.songdosamgyeop.order.core.model

import kotlinx.coroutines.flow.MutableStateFlow

enum class BrandTab { SONGDO, BULBAEK, HONG, COMMON }

private val brandTab = MutableStateFlow(BrandTab.SONGDO)
private val category = MutableStateFlow<String?>(null)
private val query = MutableStateFlow<String?>(null)