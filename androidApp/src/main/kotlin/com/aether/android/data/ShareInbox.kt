package com.aether.android.data

import kotlinx.coroutines.flow.MutableStateFlow

/** Text shared into AETHER from another app (the browser's Share sheet), waiting to be opened in the Study Hub reader. */
object ShareInbox {
    val pending = MutableStateFlow<String?>(null)
}
