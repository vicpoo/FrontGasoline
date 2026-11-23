package org.vicpoo.gasolineproject

import androidx.compose.ui.window.ComposeUIViewController
import org.koin.core.context.GlobalContext
import org.vicpoo.gasolineproject.di.initKoin
import platform.UIKit.UIViewController

/**
 * URL de APIGasoline en iOS (misma que Android). Ojo: iOS bloquea HTTP sin TLS (App Transport
 * Security); para probar en iOS hay que agregar la excepción del dominio en Info.plist.
 */
private const val IOS_API_BASE_URL = "http://13.222.72.73/api/"

fun MainViewController(): UIViewController {
    if (GlobalContext.getOrNull() == null) initKoin(apiBaseUrl = IOS_API_BASE_URL)
    return ComposeUIViewController { App() }
}
