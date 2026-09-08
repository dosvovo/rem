package com.varedog.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var dao: ReminderDao
    private lateinit var adapter: ReminderAdapter
    private val selected = Calendar.getInstance()

    private lateinit var bannerOverlay: LinearLayout
    private lateinit var bannerAlarm: LinearLayout
    private lateinit var bannerBattery: LinearLayout

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CrashHandler.consume(this)?.let {
            startActivity(
                Intent(this, CrashReportActivity::class.java)
                    .putExtra(CrashReportActivity.EXTRA_TEXT, it)
            )
        }

        setContentView(R.layout.activity_main)

        dao = AppDatabase.get(this).reminderDao()
        adapter = ReminderAdapter { reminder -> confirmDelete(reminder) }

        bannerOverlay = findViewById(R.id.banner_overlay)
        bannerAlarm = findViewById(R.id.banner_alarm)
        bannerBattery = findViewById(R.id.banner_battery)

        bannerOverlay.setOnClickListener { requestOverlayPermission() }
        bannerAlarm.setOnClickListener { requestExactAlarmPermission() }
        bannerBattery.setOnClickListener { requestIgnoreBattery() }

        val recycler = findViewById<RecyclerView>(R.id.recycler)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        findViewById<View>(R.id.fab).setOnClickListener { showCreateDialog() }
        requestNotificationPermissionIfNeeded()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                dao.observeAll().collectLatest { list ->
                    adapter.submit(list)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionBanners()
    }

    private fun refreshPermissionBanners() {
        bannerOverlay.visibility =
            if (Settings.canDrawOverlays(this)) View.GONE else View.VISIBLE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarm = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            bannerAlarm.visibility =
                if (alarm.canScheduleExactAlarms()) View.GONE else View.VISIBLE
        } else {
            bannerAlarm.visibility = View.GONE
        }
        val power = getSystemService(Context.POWER_SERVICE) as PowerManager
        bannerBattery.visibility =
            if (power.isIgnoringBatteryOptimizations(packageName)) View.GONE else View.VISIBLE
    }

    private fun requestOverlayPermission() {
        startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
        )
    }

    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
        }
    }

    private fun requestIgnoreBattery() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100
                )
            }
        }
    }

    private fun confirmDelete(reminder: Reminder) {
        AlertDialog.Builder(this)
            .setMessage("删除提醒「${reminder.content.take(20)}」？")
            .setPositiveButton("删除") { _, _ ->
                AlarmScheduler.cancel(this, reminder.id)
                lifecycleScope.launch { dao.deleteById(reminder.id) }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showCreateDialog() {
        selected.timeInMillis = System.currentTimeMillis()
        selected.add(Calendar.MINUTE, 5)

        val view = LayoutInflater.from(this).inflate(R.layout.dialog_create, null)
        val contentInput = view.findViewById<EditText>(R.id.input_content)
        val dateButton = view.findViewById<Button>(R.id.button_date)
        val timeButton = view.findViewById<Button>(R.id.button_time)
        val repeatSpinner = view.findViewById<Spinner>(R.id.spinner_repeat)
        val weekdaySpinner = view.findViewById<Spinner>(R.id.spinner_weekday)
        val monthdaySpinner = view.findViewById<Spinner>(R.id.spinner_monthday)
        val subRow = view.findViewById<LinearLayout>(R.id.row_sub)

        fun updateDateLabel() {
            dateButton.text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Date(selected.timeInMillis))
            timeButton.text = SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(Date(selected.timeInMillis))
        }
        updateDateLabel()

        dateButton.setOnClickListener {
            DatePickerDialog(
                this, { _, y, m, d ->
                    selected.set(Calendar.YEAR, y)
                    selected.set(Calendar.MONTH, m)
                    selected.set(Calendar.DAY_OF_MONTH, d)
                    updateDateLabel()
                },
                selected.get(Calendar.YEAR),
                selected.get(Calendar.MONTH),
                selected.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        timeButton.setOnClickListener {
            TimePickerDialog(
                this, { _, h, min ->
                    selected.set(Calendar.HOUR_OF_DAY, h)
                    selected.set(Calendar.MINUTE, min)
                    selected.set(Calendar.SECOND, 0)
                    updateDateLabel()
                },
                selected.get(Calendar.HOUR_OF_DAY),
                selected.get(Calendar.MINUTE),
                true
            ).show()
        }

        repeatSpinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            arrayOf("不重复", "每天", "每周", "每月")
        )
        weekdaySpinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            Reminder.WEEKDAY_NAMES
        )
        monthdaySpinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            (1..28).map { "$it 号" }
        )
        repeatSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                weekdaySpinner.visibility = if (pos == 2) View.VISIBLE else View.GONE
                monthdaySpinner.visibility = if (pos == 3) View.VISIBLE else View.GONE
                subRow.visibility = if (pos == 2 || pos == 3) View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
        }

        AlertDialog.Builder(this)
            .setTitle("新建悬浮提醒")
            .setView(view)
            .setPositiveButton("保存") { _, _ ->
                val content = contentInput.text.toString().trim()
                if (content.isEmpty()) {
                    Toast.makeText(this, "先写提醒内容", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (selected.timeInMillis <= System.currentTimeMillis()) {
                    Toast.makeText(this, "时间已过，请重新选择", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val repeat = when (repeatSpinner.selectedItemPosition) {
                    1 -> RepeatType.DAILY
                    2 -> RepeatType.WEEKLY
                    3 -> RepeatType.MONTHLY
                    else -> RepeatType.NONE
                }
                val reminder = Reminder(
                    content = content,
                    triggerAtMillis = selected.timeInMillis,
                    repeatType = repeat,
                    weekday = if (repeat == RepeatType.WEEKLY) weekdaySpinner.selectedItemPosition + 1 else null,
                    monthday = if (repeat == RepeatType.MONTHLY) monthdaySpinner.selectedItemPosition + 1 else null
                )
                lifecycleScope.launch {
                    val id = dao.insert(reminder)
                    AlarmScheduler.schedule(this@MainActivity, reminder.copy(id = id))
                    Toast.makeText(
                        this@MainActivity,
                        "已设置：${dateFormat.format(Date(reminder.triggerAtMillis))}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
}

class ReminderAdapter(
    private val onDelete: (Reminder) -> Unit
) : RecyclerView.Adapter<ReminderAdapter.Holder>() {

    private val items = mutableListOf<Reminder>()

    fun submit(list: List<Reminder>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val content: TextView = view.findViewById(R.id.item_content)
        val time: TextView = view.findViewById(R.id.item_time)
        val delete: View = view.findViewById(R.id.item_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reminder, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.content.text = item.content
        holder.time.text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            .format(Date(item.triggerAtMillis)) + " · " + item.repeatLabel()
        holder.delete.setOnClickListener { onDelete(item) }
    }

    override fun getItemCount(): Int = items.size
}
