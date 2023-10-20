package xyz.emlyn.snapnote.ui.settings

import android.animation.ValueAnimator
import android.app.TaskStackBuilder
import android.content.*
import android.content.Context.MODE_PRIVATE
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.opengl.Visibility
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.renderscript.Sampler.Value
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet.Constraint
import androidx.constraintlayout.widget.ConstraintSet.GONE
import androidx.core.animation.doOnEnd
import androidx.core.os.postDelayed
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import xyz.emlyn.snapnote.Constants
import xyz.emlyn.snapnote.MainActivity
import xyz.emlyn.snapnote.R
import xyz.emlyn.snapnote.databinding.FragmentSettingsBinding


class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var root : View

    private lateinit var sp : SharedPreferences
    private lateinit var importAction : ActivityResultLauncher<String>
    private lateinit var exportAction : ActivityResultLauncher<String>

    private var deleteAllCancelled = false;

    private var deleteProgAnim : ValueAnimator? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ViewModelProvider(this)[SettingsViewModel::class.java]

        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        root = binding.root

        activity!!.findViewById<Toolbar>(R.id.toolbar).getChildAt(1).visibility = View.GONE

        sp = activity!!.getSharedPreferences("xyz.emlyn.snapnote", MODE_PRIVATE)
        Handler(Looper.getMainLooper()).postDelayed({
            activity!!.findViewById<SwitchCompat>(R.id.registerAsCamera).setOnCheckedChangeListener(this::cameraShortcut)
            activity!!.findViewById<SwitchCompat>(R.id.registerAsCamera).isChecked = sp.getBoolean("cameraShortcut", false)

            activity!!.findViewById<ConstraintLayout>(R.id.importCL).setOnClickListener(this::importOCL)
            activity!!.findViewById<ConstraintLayout>(R.id.exportCL).setOnClickListener(this::exportOCL)

            activity!!.findViewById<ConstraintLayout>(R.id.deleteCL).setOnTouchListener(this::deleteOTL)


            activity!!.findViewById<LinearLayout>(R.id.themeLight).setOnClickListener(this::themeOnclick)
            activity!!.findViewById<LinearLayout>(R.id.themeDark).setOnClickListener(this::themeOnclick)
            activity!!.findViewById<LinearLayout>(R.id.themeSys).setOnClickListener(this::themeOnclick)

            activity!!.findViewById<LinearLayout>(R.id.themeLight).backgroundTintList = ColorStateList.valueOf(context!!.getColor(R.color.note_background_accent))
            activity!!.findViewById<LinearLayout>(R.id.themeDark).backgroundTintList = ColorStateList.valueOf(context!!.getColor(R.color.note_background_accent))
            activity!!.findViewById<LinearLayout>(R.id.themeSys).backgroundTintList = ColorStateList.valueOf(context!!.getColor(R.color.note_background_accent))


            val currentThemeMode = sp.getInt("theme", 2)
            if (currentThemeMode == 0) {
                // light
                activity!!.findViewById<LinearLayout>(R.id.themeLight).backgroundTintList =
                    ColorStateList.valueOf(context!!.getColor(R.color.note_background_accent_pale))
            } else if (currentThemeMode == 1) {
                // dark
                activity!!.findViewById<LinearLayout>(R.id.themeDark).backgroundTintList =
                    ColorStateList.valueOf(context!!.getColor(R.color.note_background_accent_pale))
            } else if (currentThemeMode == 2) {
                // follow system default
                activity!!.findViewById<LinearLayout>(R.id.themeSys).backgroundTintList =
                    ColorStateList.valueOf(context!!.getColor(R.color.note_background_accent_pale))
            }


            val gradients = Constants.getNoteGradients(context!!)
            val tags = Constants.getTagChoices(context!!)

            // generate color palettes
            val palettes = arrayOf(
                activity!!.findViewById<ConstraintLayout>(R.id.palette1),
                activity!!.findViewById(R.id.palette2),
                activity!!.findViewById(R.id.palette3),
                activity!!.findViewById(R.id.palette4),
            )

            for (i in 0..3) {

                palettes[i].setOnClickListener { setActiveTheme(i); }

                val gd = GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    intArrayOf(gradients[i][0], gradients[i][1])
                )
                (palettes[i].getChildAt(0) as LinearLayout).getChildAt(0).setBackgroundDrawable(gd)

                for (j in 0..3) {
                    (palettes[i].getChildAt(1) as LinearLayout).getChildAt(j).setBackgroundColor(tags[i][j])
                }
            }

            setActiveTheme(sp.getInt("palette", 0))


        }, 10)

        return root
    }

    private fun setActiveTheme(themeIdx : Int) {

        sp.edit().putInt("palette", themeIdx).apply()

        val palettes = arrayOf(
            activity!!.findViewById<ConstraintLayout>(R.id.palette1),
            activity!!.findViewById(R.id.palette2),
            activity!!.findViewById(R.id.palette3),
            activity!!.findViewById(R.id.palette4),
        )

        for (i in 0..3) {
            if (i == themeIdx) {
                palettes[i].backgroundTintList =
                    ColorStateList.valueOf(context!!.getColor(R.color.note_background_accent_pale))
            } else {
                palettes[i].backgroundTintList =
                    ColorStateList.valueOf(context!!.getColor(R.color.note_background))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun refreshSettingsFragment() {
        //todo: this
        activity!!.recreate()
    }

    fun deleteOTL(v : View, ev : MotionEvent) : Boolean {

        if (ev.action == MotionEvent.ACTION_DOWN) {
            deleteAllCancelled = false

            activity!!.findViewById<TextView>(R.id.deleteTitle).visibility = View.INVISIBLE
            val maxDLPWidth = activity!!.findViewById<ImageView>(R.id.deleteProgress).measuredWidth
            deleteProgAnim = ValueAnimator.ofFloat(0f, 1f)
            v.findViewById<ImageView>(R.id.deleteIco).background = context!!.getDrawable(R.drawable.trash_open)
            val initDlplp = activity!!.findViewById<ImageView>(R.id.deleteProgress).layoutParams as ConstraintLayout.LayoutParams
            initDlplp.width = 0
            activity!!.findViewById<ImageView>(R.id.deleteProgress).layoutParams = initDlplp
            activity!!.findViewById<ImageView>(R.id.deleteProgress).visibility = View.VISIBLE


            deleteProgAnim!!.addUpdateListener {
                run {
                    val dlplp =
                        activity!!.findViewById<ImageView>(R.id.deleteProgress).layoutParams as ConstraintLayout.LayoutParams
                    dlplp.width = (it.animatedFraction * maxDLPWidth).toInt()
                    activity!!.findViewById<ImageView>(R.id.deleteProgress).layoutParams = dlplp

                }
            }

            deleteProgAnim!!.doOnEnd {

                v.findViewById<ImageView>(R.id.deleteIco).background =
                    context!!.getDrawable(R.drawable.trash)
                val dlplp = activity!!.findViewById<ImageView>(R.id.deleteProgress).layoutParams
                dlplp.width = 0
                activity!!.findViewById<ImageView>(R.id.deleteProgress).layoutParams = dlplp

                if (!deleteAllCancelled) {

                    activity!!.findViewById<TextView>(R.id.deleteTitle).text =
                        getString(R.string.all_deleted)
                    activity!!.findViewById<TextView>(R.id.deleteTitle).visibility = View.VISIBLE
                    activity!!.findViewById<ImageView>(R.id.deleteProgress).visibility =
                        View.INVISIBLE

                    sp.edit().clear().commit()
                    refreshSettingsFragment()

                }
            }

            deleteProgAnim!!.duration = 3000.toLong()
            deleteProgAnim!!.start()

        } else if (ev.action == MotionEvent.ACTION_UP) {
            deleteAllCancelled = true
            activity!!.findViewById<TextView>(R.id.deleteTitle).visibility = View.VISIBLE
            activity!!.findViewById<ImageView>(R.id.deleteProgress).visibility = View.INVISIBLE
            deleteProgAnim?.cancel()

        }

        return false
    }

    fun importOCL(v : View) {

    }

    fun exportOCL(v : View) {

    }

    fun themeOnclick(v : View) {

        var newThemeId = -1
        val vLL = (v as LinearLayout)

        activity!!.findViewById<LinearLayout>(R.id.themeLight).backgroundTintList = ColorStateList.valueOf(context!!.getColor(R.color.note_background_accent))
        activity!!.findViewById<LinearLayout>(R.id.themeDark).backgroundTintList = ColorStateList.valueOf(context!!.getColor(R.color.note_background_accent))
        activity!!.findViewById<LinearLayout>(R.id.themeSys).backgroundTintList = ColorStateList.valueOf(context!!.getColor(R.color.note_background_accent))

        v.backgroundTintList = ColorStateList.valueOf(context!!.getColor(R.color.note_background_accent_pale))
        if ((vLL.getChildAt(0) as TextView).text == getString(R.string.light)) {
            newThemeId = 0
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        } else if ((vLL.getChildAt(0) as TextView).text == getString(R.string.dark)) {
            newThemeId = 1
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else if ((vLL.getChildAt(0) as TextView).text == getString(R.string.system)) {
            newThemeId = 2
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

        sp.edit().putInt("theme", newThemeId).apply()
        activity!!.findViewById<ConstraintLayout>(R.id.container).invalidate()
        //TaskStackBuilder.create(activity)
            //.addNextIntent(Intent(activity, MainActivity::class.java))
            //.addNextIntent(activity!!.intent)
            //.startActivities()

    }

    fun cameraShortcut(switch : CompoundButton, newState : Boolean) {
        sp.edit().putBoolean("cameraShortcut", newState).apply()

        val pm: PackageManager = context!!.packageManager
        val compName =
            ComponentName(getString(R.string.package_name), getString(R.string.package_name) + ".CameraShortcut")
        if (newState) {
            pm.setComponentEnabledSetting(
                compName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        } else {
            pm.setComponentEnabledSetting(
                compName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }


}
