package org.vicpoo.gasolineproject.core.ui.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.ValidationReason
import org.vicpoo.gasolineproject.core.ui.resources.Res
import org.vicpoo.gasolineproject.core.ui.resources.error_file_too_large
import org.vicpoo.gasolineproject.core.ui.resources.error_forbidden
import org.vicpoo.gasolineproject.core.ui.resources.error_invalid_file_type
import org.vicpoo.gasolineproject.core.ui.resources.error_invalid_format
import org.vicpoo.gasolineproject.core.ui.resources.error_no_company
import org.vicpoo.gasolineproject.core.ui.resources.error_no_connection
import org.vicpoo.gasolineproject.core.ui.resources.error_not_found
import org.vicpoo.gasolineproject.core.ui.resources.error_out_of_range
import org.vicpoo.gasolineproject.core.ui.resources.error_required
import org.vicpoo.gasolineproject.core.ui.resources.error_server
import org.vicpoo.gasolineproject.core.ui.resources.error_unauthorized
import org.vicpoo.gasolineproject.core.ui.resources.error_unknown

/** Traduce un AppError al texto que ve el usuario (strings en composeResources/values/strings.xml). */
@Composable
fun AppError.asText(): String = when (this) {
    AppError.NoConnection -> stringResource(Res.string.error_no_connection)
    AppError.Unauthorized -> stringResource(Res.string.error_unauthorized)
    is AppError.Forbidden -> message?.takeIf { it.isNotBlank() } ?: stringResource(Res.string.error_forbidden)
    AppError.NotFound -> stringResource(Res.string.error_not_found)
    AppError.NoCompany -> stringResource(Res.string.error_no_company)
    is AppError.Validation -> reason.asText()
    is AppError.Server -> message?.takeIf { it.isNotBlank() } ?: stringResource(Res.string.error_server)
    is AppError.Unknown -> stringResource(Res.string.error_unknown)
}

@Composable
fun ValidationReason.asText(): String = when (this) {
    ValidationReason.REQUIRED -> stringResource(Res.string.error_required)
    ValidationReason.INVALID_FORMAT -> stringResource(Res.string.error_invalid_format)
    ValidationReason.OUT_OF_RANGE -> stringResource(Res.string.error_out_of_range)
    ValidationReason.INVALID_FILE_TYPE -> stringResource(Res.string.error_invalid_file_type)
    ValidationReason.FILE_TOO_LARGE -> stringResource(Res.string.error_file_too_large)
}
