package xyz.emlyn.snapnote.ui.newnote

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.text.method.KeyListener
import android.text.method.TextKeyListener
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.graphics.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import xyz.emlyn.snapnote.Constants
import xyz.emlyn.snapnote.MainActivity
import xyz.emlyn.snapnote.Note
import xyz.emlyn.snapnote.R
import xyz.emlyn.snapnote.databinding.FragmentNewnoteBinding
import java.time.Instant
import java.util.*
import kotlin.math.*


class NewNoteFragment : Fragment() {

    private var _binding: FragmentNewnoteBinding? = null
    private val binding get() = _binding!! // This property is only valid between onCreateView and onDestroyView.

    private lateinit var cardContainer : ConstraintLayout
    private var currentCard : CardView? = null
    private var backupCard : CardView? = null

    private var keyDown = Triple(-1f,-1f, 0L)
    private var pivotX = 0f
    private var pivotY = 0f

    private val cardRotationSensitivity = 2.0
    private val cardRotationFrequency = 1f/5

    private var rotAnimator : ViewPropertyAnimator? = null

    private var allNoteIds = HashSet<String>()

    private val thresholdSave = 20

    private var prevColor : Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ViewModelProvider(this)[NewNoteViewModel::class.java]

        _binding = FragmentNewnoteBinding.inflate(inflater, container, false)
        activity!!.findViewById<Toolbar>(R.id.toolbar).getChildAt(1).visibility = View.GONE


        Handler(Looper.getMainLooper()).postDelayed({
            cardContainer = activity!!.findViewById(R.id.noteContainer)
            currentCard = activity!!.findViewById(R.id.newNoteCard)
            backupCard = activity!!.findViewById(R.id.newNoteCardBackup)

            getNoteIds()
            setCardParameters(currentCard!!)
            setCardParameters(backupCard!!)

            currentCard!!.setOnTouchListener(this::currentCardOnTouchListener)
            currentCard!!.bringToFront()
            currentCard!!.findViewById<EditText>(R.id.newNoteET).keyListener = TextKeyListener.getInstance()
            backupCard!!.findViewById<EditText>(R.id.newNoteET).keyListener = null

        }, 10)

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getNoteIds() {
        val noteHS = activity!!.getSharedPreferences("xyz.emlyn.snapnote", Context.MODE_PRIVATE).getStringSet("notes", HashSet())
        for (note in noteHS!!) {
            allNoteIds.add(note.substringBefore(Constants.SEP1))
        }
    }

    private fun setCardParameters(targetCard : CardView) {

        var backupId : String
        do {
             backupId = generateId()
        } while (allNoteIds.contains(backupId))
        targetCard.findViewById<TextView>(R.id.noteid).text = backupId
        prevColor = generateColor(prevColor)
        targetCard.findViewById<TextView>(R.id.noteid).setBackgroundColor(prevColor)

    }

    private fun currentCardOnTouchListener(@Suppress("UNUSED_PARAMETER") targetCard : View, ev : MotionEvent) : Boolean {

        if (ev.action == MotionEvent.ACTION_DOWN) {

            val imm =
                context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(
                currentCard!!.findViewById<EditText>(R.id.newNoteET).windowToken, 0
            )


            pivotX = currentCard!!.x + currentCard!!.width / 2f
            pivotY = currentCard!!.y + currentCard!!.height.toFloat()

            currentCard!!.pivotX = pivotX
            currentCard!!.pivotY = pivotY

            keyDown = Triple(ev.rawX, ev.rawY, Instant.now().toEpochMilli())

            rotAnimator = currentCard!!.animate()
        }

        if (ev.action == MotionEvent.ACTION_MOVE) {

            val rx1 = keyDown.first - pivotX
            val ry1 = keyDown.second + pivotY

            val rx2 = ev.rawX - pivotX
            val ry2 = ev.rawY + pivotY

            val mag1 = (rx1.pow(2) + ry1.pow(2)).pow(0.5f)
            val mag2 = (rx2.pow(2) + ry2.pow(2)).pow(0.5f)


            val thetaRad = -acos((rx1*rx2 + ry1*ry2) / (mag1*mag2))
            var theta = (((thetaRad * 180) / PI) * cardRotationSensitivity).toFloat()
            theta = if (rx2 - rx1 < 0) { -abs(theta) }
                    else { abs(theta) }

            val dTheta = abs(currentCard!!.rotation - theta)

            rotAnimator?.rotation(theta)
            rotAnimator?.duration = (dTheta * cardRotationFrequency).toLong()
            rotAnimator?.start()

            if (abs(currentCard!!.rotation) > thresholdSave &&
                (currentCard!!.findViewById<EditText>(R.id.newNoteET).text.toString() != "")) {

                val imm =
                    context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(
                    currentCard!!.findViewById<EditText>(R.id.newNoteET).windowToken, 0
                )

                saveToSP(currentCard as CardView)
                currentCard!!.setOnTouchListener(null)
                val lcc = currentCard!!
                currentCard = backupCard
                backupCard = (layoutInflater.inflate(
                    R.layout.layout_card,
                    cardContainer
                ) as ConstraintLayout).getChildAt(cardContainer.childCount - 1) as CardView
                currentCard!!.setOnTouchListener(this::currentCardOnTouchListener)
                currentCard!!.bringToFront()
                setCardParameters(backupCard!!)

                currentCard!!.findViewById<EditText>(R.id.newNoteET).keyListener = TextKeyListener.getInstance()
                backupCard!!.findViewById<EditText>(R.id.newNoteET).keyListener = null



                lcc.bringToFront()
                removeCard(lcc, lcc.rotation)
            }

        }

        if (ev.action == MotionEvent.ACTION_UP) {


            if (euclidean(keyDown.first, keyDown.second, ev.rawX, ev.rawY) < 50f) {
                if (currentCard!!.findViewById<EditText>(R.id.newNoteET).hasFocus()) {

                    currentCard!!.findViewById<EditText>(R.id.newNoteET).clearFocus()
                    val imm =
                        context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(
                        currentCard!!.findViewById<EditText>(R.id.newNoteET).windowToken, 0
                    )

                } else {
                    currentCard!!.bringToFront()

                    currentCard!!.findViewById<EditText>(R.id.newNoteET).isFocusableInTouchMode =
                        true
                    currentCard!!.findViewById<EditText>(R.id.newNoteET).requestFocus()
                    val imm =
                        context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(
                        currentCard!!.findViewById<EditText>(R.id.newNoteET),
                        InputMethodManager.SHOW_IMPLICIT
                    )
                }

            } else {
                rotAnimator?.cancel()
                currentCard!!.rotation = 0f
            }

            keyDown = Triple(-1f,-1f,0L)
        }

        return true
    }

    private fun removeCard(targetCard : CardView, rot : Float) {


        val px50dip = dipToPixels(context!!, 50f)
        val tclp = targetCard.layoutParams
        val tcWidthInit = targetCard.measuredWidth.toFloat()
        val tcRadInit = targetCard.radius

        val tctv = targetCard.findViewById<TextView>(R.id.noteid)
        val tctvif = tctv.measuredHeight.toFloat() / targetCard.measuredHeight.toFloat()
        val tctvic = tctv.currentTextColor.toColor()
        val bodyet = targetCard.findViewById<EditText>(R.id.newNoteET)
        val bodyetic = bodyet.currentTextColor.toColor()

        val pivX = targetCard.pivotX; val pivY = targetCard.pivotY

        val tcHitRect = Rect()
        targetCard.getHitRect(tcHitRect)
        targetCard.resetPivot()
        val pivlessHR = Rect()
        targetCard.getHitRect(pivlessHR)

        val xInit = targetCard.x - pivlessHR.exactCenterX() + tcHitRect.exactCenterX()
        val yInit = targetCard.y - pivlessHR.exactCenterY() + tcHitRect.exactCenterY()

        val xFinal = pivX-px50dip/2f
        val yFinal = pivY-px50dip

        val sizeAnim = ValueAnimator.ofFloat(targetCard.measuredHeight.toFloat(), px50dip)

        targetCard.x = 0f
        targetCard.y = 0f

        sizeAnim.addUpdateListener {

            tclp.height = (it.animatedValue as Float).toInt()
            tclp.width = (((px50dip - tcWidthInit) * it.animatedFraction) + tcWidthInit).toInt()

            targetCard.layoutParams = tclp

            targetCard.radius = (tclp.height/2f - tcRadInit) * it.animatedFraction + tcRadInit

            targetCard.rotation = (-rot)*it.animatedFraction + rot


            tctv.height = (targetCard.measuredHeight * ((1-tctvif) *it.animatedFraction + tctvif)).toInt()
            tctv.setTextColor(Color.argb(max(0f, 0.5f-it.animatedFraction), tctvic.red(), tctvic.green(), tctvic.blue()))
            bodyet.setTextColor(Color.argb(max(0f, 0.5f-it.animatedFraction), bodyetic.red(), bodyetic.green(), bodyetic.blue()))


            targetCard.x = (xFinal - xInit) * it.animatedFraction + xInit
            targetCard.y = (yFinal - yInit) * it.animatedFraction + yInit

        }



        sizeAnim.doOnEnd {

            val tickView : ConstraintLayout = layoutInflater.inflate(R.layout.layout_tick, null) as ConstraintLayout
            targetCard.addView(tickView)
            val tvlp = tickView.layoutParams
            tvlp.width = dipToPixels(context!!, 30f).toInt()
            tvlp.height = dipToPixels(context!!, 30f).toInt()
            tickView.layoutParams = tvlp

            tickView.x = dipToPixels(context!!, 10f)
            tickView.y = dipToPixels(context!!, 12f)

            val ivlp = tickView.getChildAt(0).layoutParams
            ivlp.width = tvlp.width
            tickView.getChildAt(0).layoutParams = ivlp

            val tickAnim = ValueAnimator.ofFloat(0f, tvlp.width.toFloat())
            tickAnim.addUpdateListener {
                val ctvlp = tickView.layoutParams
                ctvlp.width = (it.animatedValue as Float).toInt()
                tickView.layoutParams = ctvlp
            }
            tickAnim.duration = 200

            tickAnim.doOnEnd {
                val destroyAnim = ValueAnimator.ofFloat(1f, 1.2f, 0f)
                destroyAnim.addUpdateListener {
                    targetCard.radius = targetCard.measuredHeight/2f
                    targetCard.scaleX = (it.animatedValue as Float)
                    targetCard.scaleY = (it.animatedValue as Float)

                }
                destroyAnim.doOnEnd {
                    (targetCard.parent as ViewGroup).removeView(targetCard)
                }
                destroyAnim.duration = 200
                destroyAnim.start()
            }

            tickAnim.start()

        }

        sizeAnim.duration = 300
        sizeAnim.start()
    }

    private fun saveToSP(targetView : CardView) {
        val sp = activity!!.getSharedPreferences("xyz.emlyn.snapnote", Context.MODE_PRIVATE)

        val notes = (activity!! as MainActivity).notes
        val noteSet = ArrayList<String>()
        for (note in notes) {
            noteSet.add(
                note.renderToString()
            )
        }

        // note format: id:text:timestamp:color:tag
        val text = targetView.findViewById<EditText>(R.id.newNoteET).text.toString()
        val id = targetView.findViewById<TextView>(R.id.noteid).text.toString()
        val color = (targetView.findViewById<TextView>(R.id.noteid).background as ColorDrawable).color
        val timestamp = System.currentTimeMillis()
        noteSet.add("%s%s%s%s%d%s%s%s%d".format(
            id, Constants.SEP1,
            text, Constants.SEP2,
            timestamp, Constants.SEP3,
            color.toString(), Constants.SEP4,
            Color.argb(0,0,0,0))
        )
        notes.add(Note(id, text, timestamp, color, Color.argb(0,0,0,0)))

        allNoteIds.add(id)

        sp.edit()
            .putStringSet("notes", noteSet.toTypedArray().toHashSet())
            .apply()
        (activity!! as MainActivity).notes = notes


    }

    private fun euclidean(x1 : Float, y1 : Float, x2 : Float, y2 : Float) : Float {
        return ((x1-x2).pow(2) + (y1-y2).pow(2)).pow(.5f)
    }


    private fun generateId(): String {
        val allowedChars = "QWERTYUIOPASDFGHJKLZXCVBNM0123456789"
        val random = Random()
        val sb = StringBuilder(13)
        sb.append("Note ")
        for (i in 0 until 6) {
            sb.append(allowedChars[random.nextInt(allowedChars.length)])
            if (i % 2 == 1 && i != 5) { sb.append("-"); }
        }
        return sb.toString()
    }

    //todo: what am i meant to do re. prevColor
    private fun generateColor(prevColor : Int): Int {
        val rnd = Random()
        var red : Int; var green : Int; var blue : Int
        do {
            red = rnd.nextInt(256)
            green = rnd.nextInt(256)
            blue = rnd.nextInt(256)
        } while (!validateColor(Color.rgb(red,green,blue)))
        return Color.argb(255, red, green, blue)
    }

    private fun validateColor(color : Int) : Boolean {

        // 0 if invalid, 1 if valid

        val whiteContrastValid = (1.05) / (color.luminance + .05) > 3
        val blackContrastValid = (color.luminance + .05) / (0.05)  > 3
        val blueCheckValid = color.blue < 150

        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)
        val prevHue = hsl[0]
        ColorUtils.colorToHSL(prevColor, hsl)
        val prevHueDiffValid = abs(min(Math.floorMod((prevHue - hsl[0]).toInt(), 360), Math.floorMod((hsl[0] - prevHue).toInt(), 360))) > 50

        return whiteContrastValid && blackContrastValid && blueCheckValid && prevHueDiffValid

    }

    @Suppress("SameParameterValue")
    private fun dipToPixels(context: Context, dipValue: Float): Float {
        val metrics: DisplayMetrics = context.resources.displayMetrics
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics)
    }

}