package org.vicpoo.gasolineproject.core.ui.components

import androidx.compose.runtime.Composable
import gasolineproject.shared.generated.resources.Res
import gasolineproject.shared.generated.resources.error_file_too_large
import gasolineproject.shared.generated.resources.error_forbidden
import gasolineproject.shared.generated.resources.error_invalid_file_type
import gasolineproject.shared.generated.resources.error_invalid_format
import gasolineproject.shared.generated.resources.error_no_company
import gasolineproject.shared.generated.resources.error_no_connection
import gasolineproject.shared.generated.resources.error_not_found
import gasolineproject.shared.generated.resources.error_out_of_range
import gasolineproject.shared.generated.resources.error_required
import gasolineproject.shared.generated.resources.error_server
import gasolineproject.shared.generated.resources.error_unauthorized
import gasolineproject.shared.generated.resources.error_unknown
import org.jetbrains.compose.resources.stringResource
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.ValidationReason

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
