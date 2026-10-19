package org.vicpoo.gasolineproject

import androidx.compose.ui.window.ComposeUIViewController
import org.koin.core.context.GlobalContext
import org.vicpoo.gasolineproject.core.di.initKoin
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    if (GlobalContext.getOrNull() == null) initKoin()
    return ComposeUIViewController { App() }
}
