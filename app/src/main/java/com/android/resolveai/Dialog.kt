package com.android.resolveai

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class Dialog : DialogFragment() {
    private lateinit var editText: TextInputEditText
    private lateinit var button: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        var view = requireActivity().layoutInflater.inflate(R.layout.dialog, null)
        editText = view.findViewById(R.id.textInput)
        button = view.findViewById(R.id.changeInfoButton)
        button.setOnClickListener {
            dismiss()
        }
        var dialog = AlertDialog.Builder(activity)
        dialog.setView(view)

        return dialog.create()
    }
}