package org.vicpoo.gasolineproject.platform.di

import org.koin.core.module.Module

/*
 * Capa platform: lo que depende del sistema operativo (GPS, conectividad, push, cámara, archivos,
 * permisos y sincronización en segundo plano). Implementa interfaces de domain; la UI solo usa
 * los lanzadores de Compose (rememberCameraCapture, rememberLocationPermission, ...).
 */

/** Servicios del sistema de cada plataforma (Android / iOS). */
expect val platformModule: Module
