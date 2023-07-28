package xyz.emlyn.snapnote

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.ActionMenuView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import xyz.emlyn.snapnote.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    var notes = ArrayList<Note>()
    var notesViews = ArrayList<View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val titleBar = findViewById<Toolbar>(R.id.toolbar)
        titleBar.inflateMenu(R.menu.sort_menu)
        (titleBar.getChildAt(1) as ActionMenuView).overflowIcon = applicationContext.getDrawable(R.drawable.ic_baseline_sort_24)


        val navView: BottomNavigationView = binding.navView

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        val navController = navHostFragment.navController
        AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        navView.setupWithNavController(navController)

        navView.setBackgroundColor(ContextCompat.getColor(applicationContext, android.R.color.transparent))

        val sp = getSharedPreferences("xyz.emlyn.snapnote", Context.MODE_PRIVATE)
        val notesArr = sp.getStringSet("notes", HashSet<String>())!!.toTypedArray()

        for (note in notesArr) {
            notes.add(Note(
                note.substringBeforeLast(Constants.SEP1),
                note.substringAfterLast(Constants.SEP1).substringBeforeLast(Constants.SEP2),
                note.substringAfterLast(Constants.SEP2).substringBeforeLast(Constants.SEP3).toLong(),
                note.substringAfterLast(Constants.SEP3).substringBeforeLast(Constants.SEP4).toInt(),
                note.substringAfterLast(Constants.SEP4).toInt()
            ))
        }

        resortNotes(sp.getInt("sortMode", 0))


    }

    fun generateNoteViews() {

    }


    fun resortNotes(sortMode : Int) {

        // 0: By ID
        // 1: By name (A-z)
        // 2: By Date (New-Old)
        // 3: By tag

        getSharedPreferences("xyz.emlyn.snapnote", Context.MODE_PRIVATE).edit()
            .putInt("sortMode", sortMode)
            .apply()
        if (sortMode == 0) { notes = ArrayList(notes.sortedWith(Note.idCmp)) }
        if (sortMode == 1) { notes = ArrayList(notes.sortedWith(Note.txCmp)) }
        if (sortMode == 2) { notes = ArrayList(notes.sortedWith(Note.dtCmp)) }
        if (sortMode == 3) { notes = ArrayList(notes.sortedWith(Note.tgCmp)) }
    }

}