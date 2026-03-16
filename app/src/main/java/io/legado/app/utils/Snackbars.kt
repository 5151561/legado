@file:Suppress("unused")

package io.legado.app.utils

import android.view.View
import androidx.annotation.StringRes
import com.google.android.material.snackbar.Snackbar
import io.legado.app.ui.theme.applyLegadoSnackbarStyle

fun View.makeLegadoSnackbar(
    message: CharSequence,
    duration: Int
): Snackbar = Snackbar
    .make(this, message, duration)
    .applyLegadoSnackbarStyle()

fun View.makeLegadoSnackbar(
    @StringRes message: Int,
    duration: Int
): Snackbar = Snackbar
    .make(this, message, duration)
    .applyLegadoSnackbarStyle()

/**
 * Display the Snackbar with the [Snackbar.LENGTH_SHORT] duration.
 *
 * @param message the message text resource.
 */
@JvmName("snackbar2")
fun View.snackbar(
    @StringRes message: Int
) = makeLegadoSnackbar(message, Snackbar.LENGTH_SHORT)
    .apply { show() }

/**
 * Display Snackbar with the [Snackbar.LENGTH_LONG] duration.
 *
 * @param message the message text resource.
 */
@JvmName("longSnackbar2")
fun View.longSnackbar(
    @StringRes message: Int
) = makeLegadoSnackbar(message, Snackbar.LENGTH_LONG)
    .apply { show() }

/**
 * Display Snackbar with the [Snackbar.LENGTH_INDEFINITE] duration.
 *
 * @param message the message text resource.
 */
@JvmName("indefiniteSnackbar2")
fun View.indefiniteSnackbar(
    @StringRes message: Int
) = makeLegadoSnackbar(message, Snackbar.LENGTH_INDEFINITE)
    .apply { show() }

/**
 * Display the Snackbar with the [Snackbar.LENGTH_SHORT] duration.
 *
 * @param message the message text.
 */
@JvmName("snackbar2")
fun View.snackbar(
    message: CharSequence
) = makeLegadoSnackbar(message, Snackbar.LENGTH_SHORT)
    .apply { show() }

/**
 * Display Snackbar with the [Snackbar.LENGTH_LONG] duration.
 *
 * @param message the message text.
 */
@JvmName("longSnackbar2")
fun View.longSnackbar(
    message: CharSequence
) = makeLegadoSnackbar(message, Snackbar.LENGTH_LONG)
    .apply { show() }

/**
 * Display Snackbar with the [Snackbar.LENGTH_INDEFINITE] duration.
 *
 * @param message the message text.
 */
@JvmName("indefiniteSnackbar2")
fun View.indefiniteSnackbar(
    message: CharSequence
) = makeLegadoSnackbar(message, Snackbar.LENGTH_INDEFINITE)
    .apply { show() }

/**
 * Display the Snackbar with the [Snackbar.LENGTH_SHORT] duration.
 *
 * @param message the message text resource.
 */
@JvmName("snackbar2")
fun View.snackbar(
    message: Int,
    @StringRes actionText:
    Int, action: (View) -> Unit
) = makeLegadoSnackbar(message, Snackbar.LENGTH_SHORT)
    .setAction(actionText, action)
    .apply { show() }

/**
 * Display Snackbar with the [Snackbar.LENGTH_LONG] duration.
 *
 * @param message the message text resource.
 */
@JvmName("longSnackbar2")
fun View.longSnackbar(
    @StringRes message: Int,
    @StringRes actionText: Int,
    action: (View) -> Unit
) = makeLegadoSnackbar(message, Snackbar.LENGTH_LONG)
    .setAction(actionText, action)
    .apply { show() }

/**
 * Display Snackbar with the [Snackbar.LENGTH_INDEFINITE] duration.
 *
 * @param message the message text resource.
 */
@JvmName("indefiniteSnackbar2")
fun View.indefiniteSnackbar(
    @StringRes message: Int,
    @StringRes actionText: Int,
    action: (View) -> Unit
) = makeLegadoSnackbar(message, Snackbar.LENGTH_INDEFINITE)
    .setAction(actionText, action)
    .apply { show() }

/**
 * Display the Snackbar with the [Snackbar.LENGTH_SHORT] duration.
 *
 * @param message the message text.
 */
@JvmName("snackbar2")
fun View.snackbar(
    message: CharSequence,
    actionText: CharSequence,
    action: (View) -> Unit
) = makeLegadoSnackbar(message, Snackbar.LENGTH_SHORT)
    .setAction(actionText, action)
    .apply { show() }

/**
 * Display Snackbar with the [Snackbar.LENGTH_LONG] duration.
 *
 * @param message the message text.
 */
@JvmName("longSnackbar2")
fun View.longSnackbar(
    message: CharSequence,
    actionText: CharSequence,
    action: (View) -> Unit
) = makeLegadoSnackbar(message, Snackbar.LENGTH_LONG)
    .setAction(actionText, action)
    .apply { show() }

/**
 * Display Snackbar with the [Snackbar.LENGTH_INDEFINITE] duration.
 *
 * @param message the message text.
 */
@JvmName("indefiniteSnackbar2")
fun View.indefiniteSnackbar(
    message: CharSequence,
    actionText: CharSequence,
    action: (View) -> Unit
) = makeLegadoSnackbar(message, Snackbar.LENGTH_INDEFINITE)
    .setAction(actionText, action)
    .apply { show() }
