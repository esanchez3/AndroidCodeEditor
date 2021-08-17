package com.github.estivensh4.androidcodeeditor

interface OnSelectionActionPerformedListener {
    fun onSelectionFinished(usingSelectAllOption: Boolean)
    fun onCut()
    fun onCopy()
    fun onPaste()
    fun onUndo()
    fun onRedo()
}
