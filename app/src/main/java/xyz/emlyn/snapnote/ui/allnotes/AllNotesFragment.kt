package xyz.emlyn.snapnote.ui.allnotes

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.ActionBar.LayoutParams
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.widget.ActionMenuView
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.core.view.marginTop
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import xyz.emlyn.snapnote.Constants
import xyz.emlyn.snapnote.MainActivity
import xyz.emlyn.snapnote.Note
import xyz.emlyn.snapnote.R
import xyz.emlyn.snapnote.databinding.FragmentAllnotesBinding
import java.sql.Timestamp
import java.time.Instant
import java.util.*
import kotlin.math.floor

class AllNotesFragment : Fragment() {

    private var _binding: FragmentAllnotesBinding? = null
    private val binding get() = _binding!!

    private var downCoord = Pair(0f,0f)
    private var lcoord = Pair(0f, 0f)
    private var screenwidth = 0f


    private lateinit var sp : SharedPreferences
    private var isRTL : Boolean? = null

    private var lastSwiped : View? = null
    private lateinit var tagIdx : Array<Int>

    private lateinit var noteLL : LinearLayout


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ViewModelProvider(this)[AllNotesViewModel::class.java]

        _binding = FragmentAllnotesBinding.inflate(inflater, container, false)
        activity!!.findViewById<Toolbar>(R.id.toolbar).getChildAt(1).visibility = View.VISIBLE


        Handler(Looper.getMainLooper()).postDelayed({
            isRTL = TextUtils.getLayoutDirectionFromLocale(
                Locale
                    .getDefault()
            ) == ViewCompat.LAYOUT_DIRECTION_RTL
            sp = activity!!.getSharedPreferences("xyz.emlyn.snapnote", MODE_PRIVATE)
            tagIdx = Constants.getTagChoices(context!!)[sp.getInt("palette", 0)].toTypedArray()
            activity!!.findViewById<ScrollView>(R.id.scrollView)
                .setOnTouchListener(this::swipeListener)
            populateNotes()


        }, 10)


        val sortMenu = activity!!.findViewById<Toolbar>(R.id.toolbar).getChildAt(1) as ActionMenuView
        sortMenu.setOnMenuItemClickListener(this::menuOnClick)



        return binding.root
    }


    private fun menuOnClick(item : MenuItem) : Boolean {

        var sortMode = 0

        if (item.itemId == R.id.sort_id) { sortMode = 0; }
        if (item.itemId == R.id.sort_name) { sortMode = 1; }
        if (item.itemId == R.id.sort_date) { sortMode = 2; }
        if (item.itemId == R.id.sort_tag) { sortMode = 3; }

        (activity as MainActivity).resortNotes(sortMode)

        val notesEnc = ArrayList<String>()
        for (note in (activity as MainActivity).notes) {
            notesEnc.add(note.renderToString())
        }

        sp.edit()
            .putStringSet("notes", notesEnc.toHashSet())
            .apply()

        populateNotes()

        return false

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun tagColorSelectorOnclick(v : View) {
        val mainContent = (v.parent.parent as ConstraintLayout).findViewById<ConstraintLayout>(R.id.mainNoteContentCL)
        val notesEnc = ArrayList<String>()
        /* update sharedprefs */
        val id = mainContent.findViewById<TextView>(R.id.existingNoteId).text.toString()
        for (note in (activity!! as MainActivity).notes) {
            if (note.id == id) {
                if (v.backgroundTintList != null) {
                    note.tagColor = v.backgroundTintList!!.defaultColor
                } else { note.tagColor = Color.argb(0,0,0,0) }
                /* no need to reset - ids unique */
            }
            notesEnc.add(note.renderToString())
        }
        sp.edit().putStringSet("notes", notesEnc.toTypedArray().toHashSet()).apply()
        populateNotes()
    }

    private fun readableTime(timestampms : Long) : String {

        val minuteMS = 60000 /* 1 minute */
        val hourMS = 3600000 /* 1 hour */
        val dayMS = 8.64e+7

        val currtimestamp = System.currentTimeMillis()
        val timestampTS = Timestamp.from(Instant.now())
        val oldTimestampTS = Timestamp.from(Instant.ofEpochMilli(timestampms))

        if (currtimestamp - timestampms < minuteMS) {
            return context!!.getString(R.string.now)
        } else if (currtimestamp - timestampms < hourMS) {
            val dMinute = (currtimestamp - timestampms).floorDiv(minuteMS).toInt()
            return resources.getQuantityString(R.plurals.mins_ago, dMinute).format(dMinute)
        } else if (currtimestamp - timestampms < dayMS) {
            val dHour = (currtimestamp - timestampms).floorDiv(hourMS).toInt()
            return resources.getQuantityString(R.plurals.hours_ago, dHour).format(dHour)
        } else {
            val dateSB = StringBuilder()
            dateSB.append(oldTimestampTS.date)
            dateSB.append(" ")
            dateSB.append(resources.getStringArray(R.array.human_months)[oldTimestampTS.month])
            if (oldTimestampTS.year < timestampTS.year) {
                dateSB.append(" ")
                dateSB.append(oldTimestampTS.year)
            }
            return dateSB.toString()
        }

    }

    private fun populateNotes() {
        noteLL = activity!!.findViewById(R.id.notes)
        noteLL.removeAllViews()

        screenwidth = noteLL.measuredWidth.toFloat()

        val notes = (activity!! as MainActivity).notes

        if (notes.size == 0) {
            activity!!.findViewById<TextView>(R.id.nonotes).visibility = View.VISIBLE
        }

        var blendMode = PorterDuff.Mode.LIGHTEN
        if ((context!!.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
            blendMode = PorterDuff.Mode.DARKEN
        }

        val palette = sp.getInt("palette", 0)

        for ((i, note) in notes.withIndex()) {

            val latestNewNote : View = layoutInflater.inflate(R.layout.existing_note_layout, noteLL, false)
            latestNewNote.findViewById<TextView>(R.id.existingNoteId).text = note.id
            latestNewNote.findViewById<TextView>(R.id.noteContent).text = note.text
            latestNewNote.findViewById<TextView>(R.id.noteDate).text = readableTime(note.datetime)
            latestNewNote.findViewById<View>(R.id.colorBlock).backgroundTintList = ColorStateList.valueOf(note.bgColor)
            latestNewNote.findViewById<View>(R.id.tagBlock).backgroundTintList = ColorStateList.valueOf(note.tagColor)
            latestNewNote.setOnTouchListener(this::swipeListener)

            noteLL.addView(latestNewNote)

            if (i == 0 && 1 == notes.size) {
                latestNewNote.findViewById<View>(R.id.colorBlock).background = ContextCompat.getDrawable(context!!, R.drawable.rounded_r)
            }
            else if (i==0) {
                latestNewNote.findViewById<View>(R.id.colorBlock).background = ContextCompat.getDrawable(context!!, R.drawable.rounded_tr)
            }
            else if (i+1==notes.size) {
                latestNewNote.findViewById<View>(R.id.colorBlock).background = ContextCompat.getDrawable(context!!, R.drawable.rounded_br)
            }
            if (isRTL!!) {
                latestNewNote.findViewById<View>(R.id.colorBlock).rotationY = 180F
            }

            for ((j,vv) in latestNewNote.findViewById<FlexboxLayout>(R.id.colorSelectFB).children.withIndex()) {
                vv.setOnClickListener(this::tagColorSelectorOnclick)
                if (j == 0 && note.tagColor == Color.argb(0,0,0,0)) { vv.background = activity!!.getDrawable(R.drawable.ic_circle_notag_outline) }
                if (j > 0) {
                    if (note.tagColor == Constants.getTagChoices(context)[palette][j-1]) {
                        vv.background = activity!!.getDrawable(R.drawable.ic_circle_outline)
                    }
                    vv.backgroundTintList = ColorStateList.valueOf(Constants.getTagChoices(context)[palette][j-1])
                    vv.backgroundTintMode = blendMode
                }
            }
        }
    }

    private fun swipeListener(v : View, ev : MotionEvent) : Boolean {

        val maxSwipePos = dipToPixels(context!!, 200f)


        if (ev.action == MotionEvent.ACTION_DOWN) {
            downCoord = Pair(ev.x, ev.y)
            lcoord = downCoord

        }

        if (ev.action == MotionEvent.ACTION_MOVE) {

            if (v is ScrollView) {
                return false
            } else {
                lastSwiped = v
            }

            val deltaMotion = ev.x - lcoord.first

            var translationPrime = lastSwiped!!.findViewById<FlexboxLayout>(R.id.colorSelectFB).translationX + deltaMotion

            if (translationPrime < -maxSwipePos && isRTL!!) { translationPrime = -maxSwipePos; }
            if (translationPrime > maxSwipePos && !isRTL!!) { translationPrime = maxSwipePos; }

            val translationAnim = ObjectAnimator.ofFloat(v.findViewById<FlexboxLayout>(R.id.colorSelectFB).translationX, translationPrime)
            translationAnim.addUpdateListener {

                lastSwiped!!.findViewById<FlexboxLayout>(R.id.colorSelectFB).translationX =
                    translationAnim.animatedValue as Float
                lastSwiped!!.findViewById<ConstraintLayout>(R.id.mainNoteContentCL).translationX =
                    translationAnim.animatedValue as Float

            }
            translationAnim.duration = 0 /* might change this - who knows?? */
            translationAnim.start()


            lcoord = Pair(ev.x, ev.y)
        }

        if (ev.action == MotionEvent.ACTION_UP) {

            if (lastSwiped == null) { return true; }

            if (isRTL!!) {
                if (lastSwiped!!.findViewById<FlexboxLayout>(R.id.colorSelectFB).translationX > (2*screenwidth)/3) { removeNote(v) }
            } else {
                if (lastSwiped!!.findViewById<FlexboxLayout>(R.id.colorSelectFB).translationX < -(2*screenwidth)/3) { removeNote(v) }
            }

            val dxf: Float = if (isRTL!!) {
                if (ev.x - downCoord.first < -maxSwipePos/2f) {
                    -maxSwipePos; } else {
                    0f; }
            } else {
                if (ev.x - downCoord.first > maxSwipePos/2f) {
                    maxSwipePos; } else {
                    0f; }
            }

            val mSwipeAnimator = ValueAnimator.ofFloat(v.findViewById<FlexboxLayout>(R.id.colorSelectFB).translationX, dxf).apply {
                duration = 100 //ms

                addUpdateListener { anim ->
                    lastSwiped!!.findViewById<FlexboxLayout>(R.id.colorSelectFB).translationX =
                        anim.animatedValue as Float
                    lastSwiped!!.findViewById<ConstraintLayout>(R.id.mainNoteContentCL).translationX =
                        anim.animatedValue as Float
                }
            }
            mSwipeAnimator.start()
        }

        return true
    }


    @Suppress("SameParameterValue")
    private fun dipToPixels(context: Context, dipValue: Float): Float {
        val metrics: DisplayMetrics = context.resources.displayMetrics
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics)
    }

    private fun removeNote(v : View) {

        val id = v.findViewById<TextView>(R.id.existingNoteId).text
        val notes = (activity!! as MainActivity).notes
        val newNotes = ArrayList<Note>()
        for (note in notes) {
            if (note.id != id) { newNotes.add(note) }
        }
        (activity!! as MainActivity).notes = newNotes
        val notesEnc = ArrayList<String>()
        if (newNotes.size != 0) {
            for (i in 0 until notes.size) {
                notesEnc.add(newNotes[i].renderToString())
            }
        }
        sp.edit().putStringSet("notes", notesEnc.toHashSet()).commit()

        val fVal = if (isRTL!!) { 5000f } else { -5000f }

        val removeAnimator = ValueAnimator.ofFloat(v.translationX, fVal)
        removeAnimator.duration = 200
        removeAnimator.addUpdateListener {
            v.translationX = removeAnimator.animatedValue as Float
            if ((removeAnimator.animatedValue as Float) == fVal) {

                populateNotes()
            }
        }
        removeAnimator.start()
    }
}