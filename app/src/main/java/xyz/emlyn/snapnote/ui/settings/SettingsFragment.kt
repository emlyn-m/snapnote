package xyz.emlyn.snapnote.ui.settings

import android.animation.ValueAnimator
import android.content.ActivityNotFoundException
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.animation.doOnEnd
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.flexbox.FlexboxLayout
import xyz.emlyn.snapnote.Constants
import xyz.emlyn.snapnote.MainActivity
import xyz.emlyn.snapnote.Note
import xyz.emlyn.snapnote.R
import xyz.emlyn.snapnote.databinding.FragmentSettingsBinding
import java.nio.charset.Charset
import kotlin.math.roundToInt


class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var root : View

    private lateinit var sp : SharedPreferences
    private lateinit var importAction : ActivityResultLauncher<String>
    private lateinit var exportAction : ActivityResultLauncher<String>

    private var deleteIcoAnim : ValueAnimator? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ViewModelProvider(this)[SettingsViewModel::class.java]

        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        root = binding.root

        activity!!.findViewById<Toolbar>(R.id.toolbar).getChildAt(1).visibility = View.GONE


        // set version value
        root.findViewById<TextView>(R.id.version).text = getString(R.string.version).format(getString(R.string.ver_number))

        importAction = registerForActivityResult(ActivityResultContracts.GetContent()) { uri : Uri? ->
            if (uri != null) { handleImport(uri) }
        }

        exportAction = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri : Uri? ->
            if (uri != null) { handleExport(uri) }
        }

        Handler(Looper.getMainLooper()).postDelayed({

            activity!!.findViewById<TextView>(R.id.support).setOnClickListener { _ ->
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(context!!.getString(R.string.kofi_url)))
                try {
                    startActivity(browserIntent)
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(context,
                        String.format("%s\n%s", context!!.getText(R.string.no_browser), context!!.getString(R.string.kofi_url)),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            sp = activity!!.getSharedPreferences("xyz.emlyn.snapnote", MODE_PRIVATE)
            activity!!.findViewById<TextView>(R.id.delete).setOnTouchListener(this::deleteOnTouchListener)
            activity!!.findViewById<TextView>(R.id.importHeader).setOnClickListener(this::importAll)
            activity!!.findViewById<TextView>(R.id.exportHeader).setOnClickListener(this::exportAll)

            for (child in activity!!.findViewById<FlexboxLayout>(R.id.tcs_1).children) {
                child.setOnClickListener { tagOnclick(it, 0) }
            }
            for (child in activity!!.findViewById<FlexboxLayout>(R.id.tcs_2).children) {
                child.setOnClickListener { tagOnclick(it, 1) }
            }
            for (child in activity!!.findViewById<FlexboxLayout>(R.id.tcs_3).children) {
                child.setOnClickListener { tagOnclick(it, 2) }
            }
            for (child in activity!!.findViewById<FlexboxLayout>(R.id.tcs_4).children) {
                child.setOnClickListener { tagOnclick(it, 3) }
            }

            updateUI()
        }, 10)

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

//
//    sp.edit().clear().apply()
//    sp.edit().putStringSet("notes", HashSet<String>()).apply()
//    (activity!! as MainActivity).notes = ArrayList()
//    Toast
//    .makeText(context, resources.getString(R.string.all_deleted), Toast.LENGTH_SHORT)
//    .show()
//    updateUI()


    private fun deleteOnTouchListener(v : View, ev : MotionEvent) : Boolean {

        if (ev.action == MotionEvent.ACTION_DOWN && deleteIcoAnim == null) {
            val deleteIcon = activity!!.findViewById<View>(R.id.deleteProg)
            val diW = deleteIcon.measuredWidth
            deleteIcoAnim = ValueAnimator.ofFloat(0f, diW.toFloat())
            deleteIcoAnim!!.addUpdateListener {

                val clp = deleteIcon.layoutParams
                clp.width = (it.animatedValue as Float).roundToInt()
                deleteIcon.layoutParams = clp

                if ((it.animatedValue as Float) > 0.01f) { // first iteration
                    deleteIcon.visibility = View.VISIBLE
                    (v as TextView).text = activity!!.resources.getText(R.string.clearing)
                }
            }
            deleteIcoAnim!!.duration = 1000 // ms
            deleteIcoAnim!!.start()


            deleteIcoAnim!!.doOnEnd {
                (v as TextView).text = activity!!.resources.getText(R.string.cleared)
                Handler(Looper.getMainLooper()).postDelayed({
                    v.text = activity!!.resources.getText(R.string.reset_all_settings)
                    //todo: issue here if navigated away from settings tab before completion
                }, 1000)
                sp.edit().clear().apply()
                sp.edit().putStringSet("notes", HashSet<String>()).apply()
                (activity!! as MainActivity).notes = ArrayList()

                updateUI()
                deleteIcoAnim = null
                activity!!.findViewById<View>(R.id.deleteProg).visibility = View.INVISIBLE
            }

        } else if (ev.action == MotionEvent.ACTION_UP && deleteIcoAnim != null) {
            activity!!.findViewById<View>(R.id.deleteProg).visibility = View.INVISIBLE
            val clp = activity!!.findViewById<View>(R.id.deleteProg).layoutParams
            clp.width = 0
            activity!!.findViewById<View>(R.id.deleteProg).layoutParams = clp
            deleteIcoAnim!!.pause()
            deleteIcoAnim = null
            (v as TextView).text = activity!!.resources.getText(R.string.reset_all_settings)

        }

        return true
    }

    private fun tagOnclick(v : View, blockIdx : Int) {
        for (i in 0 until (v.parent as FlexboxLayout).childCount) {
            if ((v.parent as FlexboxLayout).getChildAt(i) == v) {
                sp.edit().putInt("tag$blockIdx", i).apply()
            }
        }
        updateUI()
    }

    private fun exportAll(@Suppress("UNUSED_PARAMETER") ignored : View) {
        exportAction.launch(getString(R.string.default_name))
    }

    private fun importAll(@Suppress("UNUSED_PARAMETER") ignored : View) {
        importAction.launch("application/octet-stream")

    }

    private fun handleImport(uri : Uri) {
        val fileIS = context!!.contentResolver.openInputStream(uri)

        val fileBytes = fileIS!!.readBytes()

        val colors = arrayOf(
            0,0,0,0
        )

        val notes = ArrayList<String>()
        val currNoteBytes = ArrayList<Byte>()

        for (i in fileBytes.indices) {
            if (i < 4) { // set colors
                colors[i] = fileBytes[i].toInt()

            } else { // set notes
                if (fileBytes[i] == Constants.HS_SEP.code.toByte()) {
                    notes.add(String(currNoteBytes.toByteArray(), Charset.forName("utf-8")))
                    currNoteBytes.clear()
                } else {
                    currNoteBytes.add(fileBytes[i])
                }
            }
        }

        sp.edit()
            .putInt("tag0", colors[0])
            .putInt("tag1", colors[1])
            .putInt("tag2", colors[2])
            .putInt("tag3", colors[3])
            .putStringSet("notes", notes.toTypedArray().toHashSet())
            .apply()

        val notesDec = ArrayList<Note>()
        for (note in notes) {
            notesDec.add(Note(
                note.substringBeforeLast(Constants.SEP1),
                note.substringAfterLast(Constants.SEP1).substringBeforeLast(Constants.SEP2),
                note.substringAfterLast(Constants.SEP2).substringBeforeLast(Constants.SEP3).toLong(),
                note.substringAfterLast(Constants.SEP3).substringBeforeLast(Constants.SEP4).toInt(),
                note.substringAfterLast(Constants.SEP4).toInt()
            ))
        }

        (activity as MainActivity).notes = notesDec

        updateUI()
    }

    private fun handleExport(uri : Uri) {
        /*
            App data format (so far):
                (3bytes) tagColor1 (3bytes) tagColor2 (3bytes) tagColor3 (3bytes) tagColor4 note1 HS_SEP note2 HS_SEP ... HS_SEP noteK[EOF]

         */

        val notesHS = sp.getStringSet("notes", HashSet<String>())
        val tags = arrayOf(
            sp.getInt("tag0", 0),
            sp.getInt("tag1", 0),
            sp.getInt("tag2", 0),
            sp.getInt("tag3", 0),
        )

        val notesBytes = arrayListOf<Byte>()
        for (i in tags.indices) {
            notesBytes.add(tags[i].toByte())
        }

        for (key in notesHS!!) {
            val keyBytes = key.toByteArray(Charset.forName("utf-8"))
            for (kb in keyBytes) {
                notesBytes.add(kb)
            }
            notesBytes.add(Constants.HS_SEP.code.toByte())

        }

        // write file
        val notesBytesArray = notesBytes.toByteArray()
        val fileOS = context!!.contentResolver.openOutputStream(uri)
        fileOS!!.write(notesBytesArray)
        fileOS.flush()

    }

    private fun updateUI() {

        val tags = arrayOf(
            sp.getInt("tag0", 0),
            sp.getInt("tag1", 0),
            sp.getInt("tag2", 0),
            sp.getInt("tag3", 0),
        )

        val tcs = arrayOf(
            activity!!.findViewById<FlexboxLayout>(R.id.tcs_1),
            activity!!.findViewById(R.id.tcs_2),
            activity!!.findViewById(R.id.tcs_3),
            activity!!.findViewById(R.id.tcs_4)
        )

        var blendMode = PorterDuff.Mode.LIGHTEN
        if ((context!!.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
            blendMode = PorterDuff.Mode.DARKEN
        }

        for (i in tcs.indices) {
            for (j in 0 until tcs[i].childCount) {
                if (tags[i] == j) {
                    tcs[i].getChildAt(j).background = activity!!.getDrawable(R.drawable.ic_circle_outline)
                } else {
                    tcs[i].getChildAt(j).background = activity!!.getDrawable(R.drawable.ic_circle)
                }
                tcs[i].getChildAt(j).backgroundTintList = ColorStateList.valueOf(Constants.getTagChoices(context)[i][j])
                tcs[i].getChildAt(j).backgroundTintMode = blendMode

            }
        }
        
    }

}
