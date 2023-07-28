package xyz.emlyn.snapnote.ui.newnote

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class NewNoteViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is new note Fragment"
    }
    val text: LiveData<String> = _text
}