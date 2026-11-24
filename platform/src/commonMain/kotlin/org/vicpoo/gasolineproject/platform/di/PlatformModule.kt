package org.vicpoo.gasolineproject.platform.di

import org.koin.core.module.Module

/*
 * Capa platform: lo que depende del sistema operativo (GPS, conectividad, push, cámara, archivos,
 * permisos, sincronización en segundo plano, base local cifrada, motor HTTP, tokens en Keystore /
 * Keychain y datos del teléfono). Implementa interfaces de domain; la UI solo usa los lanzadores de
 * Compose (rememberCameraCapture, rememberLocationPermission, ...).
 */

/**
 * Servicios del sistema de cada plataforma (Android / iOS). Es el ÚNICO `expect val Module` del
 * proyecto: todo lo nativo se registra aquí y las demás capas lo reciben por interfaz. Los otros
 * expect/actual son solo composables (cámara, documentos, compartir, permisos y el mapa en ui);
 * ArchitectureTest lo revisa.
 */
expect val platformModule: Module
