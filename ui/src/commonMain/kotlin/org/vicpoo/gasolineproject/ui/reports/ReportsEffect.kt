package org.vicpoo.gasolineproject.ui.reports

import org.vicpoo.gasolineproject.domain.reports.ReportFile

sealed interface ReportsEffect {
    /** Abrir el menú de compartir del sistema con el archivo descargado. */
    data class Share(val file: ReportFile) : ReportsEffect
}
