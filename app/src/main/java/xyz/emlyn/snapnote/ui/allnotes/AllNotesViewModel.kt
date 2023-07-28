package xyz.emlyn.snapnote.ui.allnotes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AllNotesViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is all notes Fragment"
    }
    val text: LiveData<String> = _text
}