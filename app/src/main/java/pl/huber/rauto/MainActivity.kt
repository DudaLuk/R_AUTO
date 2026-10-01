package pl.huber.rauto

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import kotlin.math.ceil

class MainActivity : Activity() {
    private val ink = Color.rgb(26, 57, 51)
    private val green = Color.rgb(19, 109, 91)
    private val cream = Color.rgb(245, 243, 237)
    private val handler = Handler(Looper.getMainLooper())
    private val engine = RaceEngine()
    private val microphone = AudioMonitor(this)
    private val prefs by lazy { getSharedPreferences("progress", MODE_PRIVATE) }
    private lateinit var rewards: RewardSystem
    private lateinit var garageSystem: GarageSystem
    private lateinit var road: RoadView
    private lateinit var status: TextView
    private lateinit var stats: TextView
    private lateinit var garage: TextView
    private lateinit var exercise: EditText
    private lateinit var mode: Spinner
    private lateinit var duration: Spinner
    private lateinit var start: Button
    private lateinit var correct: Button
    private lateinit var retry: Button
    private lateinit var end: Button
    private lateinit var level: ProgressBar
    private lateinit var thresholdLabel: TextView
    private lateinit var thresholdSlider: SeekBar
    private var laboratory = false
    private var automatic = false
    private var sessionStarted = false
    private var generation = 0
    private var gate = SoundGate()
    private var rDetector = RDetector()
    private var lastFrame = 0L
    private var threshold = -35.0
    private var lastReward = 0L
    private var totalStars = 0
    private var activeForeground = false
    private var chosenColor = 0
    private val capybaraComboEvery = 3
    private val capybaraBonusCoins = 15
    private val colors = intArrayOf(Color.rgb(246,183,69),Color.rgb(232,109,85),Color.rgb(69,147,202))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rewards = RewardSystem(prefs)
        garageSystem = GarageSystem(prefs, rewards.points)
        totalStars = prefs.getInt("stars",0)
        chosenColor = prefs.getInt("car",0).coerceIn(0,2)
        threshold = prefs.getInt("threshold",-35).toDouble()
        val scroll = ScrollView(this).apply { setBackgroundColor(cream); isFillViewport = true }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20),dp(14),dp(20),dp(18))
        }
        scroll.addView(root)
        // Android 15 edge-to-edge: respect system bars without a support library.
        scroll.setOnApplyWindowInsetsListener { v, insets ->
            v.setPadding(insets.systemWindowInsetLeft,insets.systemWindowInsetTop,
                insets.systemWindowInsetRight,insets.systemWindowInsetBottom)
            insets
        }
        setContentView(scroll)
        root.addView(label("R AUTO  /  MAŁE KROKI, WIELKA PODRÓŻ",12,true))
        root.addView(label("Jedziemy po gwiazdki!",28,true))
        root.addView(label("Ćwicz z dorosłym. Każda udana próba dodaje energii.",15))
        garage = label("",14,true)
        root.addView(garage)
        road = RoadView(this).apply { carColor = colors[chosenColor] }
        applyGarageToGame()
        root.addView(road,LinearLayout.LayoutParams(-1,dp(225)).apply { topMargin=dp(12); bottomMargin=dp(10) })
        stats = label("Gotowi do drogi?",19,true)
        root.addView(stats)
        status = label("Wybierz ćwiczenie ustalone z logopedą i naciśnij Start.",15)
        root.addView(status)
        root.addView(label("TRYB ZABAWY",12,true))
        mode = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,
                listOf("Z rodzicem — dorosły ocenia próbę","Automatyczne R — BETA (offline)","Laboratorium — reaguje na dźwięk"))
            onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    automatic = position == 1
                    laboratory = position == 2
                    updateControls()
                    status.text = if (laboratory) "TEST: każdy głośniejszy dźwięk może napędzić auto. To NIE jest ocena głoski r."
                        else if (automatic) "Automatyczny detektor R działa lokalnie na telefonie. To funkcja BETA, nie diagnoza logopedyczna."
                        else "Rodzic ocenia próbę zgodnie ze wskazówkami logopedy."
                }
            }
        }
        root.addView(mode)
        root.addView(label("TWOJE ĆWICZENIE",12,true))
        exercise = EditText(this).apply {
            hint = "Wpisz ćwiczenie zalecone przez logopedę"
            setText(prefs.getString("exercise","")); textSize=16f
            maxLines=2
            filters=arrayOf(android.text.InputFilter.LengthFilter(120))
        }
        root.addView(exercise)
        duration = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,
                listOf("Krótka trasa • 60 sekund","Dłuższa trasa • 120 sekund"))
        }
        root.addView(duration)
        level = ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal).apply { max=100 }
        root.addView(level,LinearLayout.LayoutParams(-1,dp(12)))
        thresholdLabel=label("",13)
        root.addView(thresholdLabel)
        thresholdSlider=SeekBar(this).apply {
            max=40; progress=(threshold+55).toInt().coerceIn(0,40)
            contentDescription="Próg głośności detektora dźwięku"
            setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    prefs.edit().putInt("threshold",threshold.toInt()).apply()
                }
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    threshold=(progress-55).toDouble(); updateThreshold()
                }
            })
        }
        root.addView(thresholdSlider)
        start=button("Start podróży") { onStartPressed() }
        root.addView(start)
        val actions=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
        correct=button("Udana próba ★") { reward() }
        retry=button("Jeszcze próbujemy",false) {
            engine.retry(); rewards.missedAttempt(); updateGarage(); status.text="Spokojnie, spróbuj jeszcze raz. Auto jedzie dalej."
        }
        actions.addView(correct,LinearLayout.LayoutParams(0,dp(64),1f).apply { rightMargin=dp(4) })
        actions.addView(retry,LinearLayout.LayoutParams(0,dp(64),1f).apply { leftMargin=dp(4) })
        root.addView(actions)
        end=button("Zakończ trasę",false) {
            pauseGame();sessionStarted=false;updateControls()
            status.text="Trasa zakończona. Zdobyte gwiazdki są zapisane."
            stats.text="Gotowi na następną podróż?"
        }
        root.addView(end)
        root.addView(button("Garaż i zdobyte gwiazdki",false) { showGarage() })
        root.addView(button("Dla dorosłego • jak korzystać",false) { help() })
        root.addView(label("OFFLINE • BEZ KONTA • BEZ ZAPISYWANIA GŁOSU",11,true))
        updateThreshold(); updateGarage(); updateControls()
    }
    private fun dp(value:Int)=(value*resources.displayMetrics.density).toInt()
    private fun label(value:String,size:Int,bold:Boolean=false)=TextView(this).apply {
        text=value; textSize=size.toFloat(); setTextColor(ink)
        setPadding(0,dp(5),0,dp(5))
        if(bold) setTypeface(null,Typeface.BOLD)
    }
    private fun button(value:String,primary:Boolean=true,action:()->Unit)=Button(this).apply {
        text=value; isAllCaps=false; textSize=14f; gravity=Gravity.CENTER
        setTextColor(if(primary) Color.WHITE else ink)
        background=GradientDrawable().apply {
            setColor(if(primary) green else Color.rgb(228,233,222));cornerRadius=dp(14).toFloat()
        }
        layoutParams=LinearLayout.LayoutParams(-1,dp(52)).apply { topMargin=dp(6);bottomMargin=dp(6) }
        setOnClickListener { action() }
    }
    private fun updateThreshold() {
        if(::thresholdLabel.isInitialized) thresholdLabel.text="Próg: ${threshold.toInt()} dBFS • w prawo = potrzeba głośniejszego dźwięku"
    }
    private fun updateGarage() {
        garage.text="★ $totalStars  •  🪙 ${garageSystem.coins}  •  ${rewards.points} XP  •  poziom ${rewards.level}  •  seria ${rewards.streak}  •  kapibara co 3 R"
    }
    private fun applyGarageToGame() {
        if(!::garageSystem.isInitialized) return
        engine.configure(
            garageSystem.baseSpeed,
            garageSystem.boostSpeed,
            garageSystem.boostDuration,
            garageSystem.acceleration
        )
        if(::road.isInitialized) {
            road.engineLevel=garageSystem.engineLevel
            road.turboLevel=garageSystem.turboLevel
            road.tiresLevel=garageSystem.tiresLevel
            road.bodyLevel=garageSystem.bodyLevel
            road.invalidate()
        }
    }
    private fun garageReward(): Int {
        val base = 5 + minOf(rewards.streak, 5)
        val milestone = if(totalStars > 0 && totalStars % 10 == 0) 20 else 0
        val earned = base + milestone
        garageSystem.earn(earned)
        return earned
    }
    private fun capybaraComboBonus(): Int {
        if(rewards.streak <= 0 || rewards.streak % capybaraComboEvery != 0) return 0
        road.showCapybaraDance()
        garageSystem.earn(capybaraBonusCoins)
        return capybaraBonusCoins
    }
    private fun updateControls() {
        if(!::start.isInitialized) return
        mode.isEnabled=!sessionStarted; exercise.isEnabled=!sessionStarted; duration.isEnabled=!sessionStarted
        correct.isEnabled=engine.running && !laboratory && !automatic
        retry.isEnabled=engine.running && !laboratory && !automatic
        if(::end.isInitialized) end.isEnabled=sessionStarted
        start.text=if(engine.running) "Pauza" else if(sessionStarted) "Wznów podróż" else "Start podróży"
        level.visibility=if(laboratory || automatic) View.VISIBLE else View.GONE
        thresholdLabel.visibility=if(laboratory || automatic) View.VISIBLE else View.GONE
        thresholdSlider.visibility=if(laboratory) View.VISIBLE else View.GONE
        if(automatic) thresholdLabel.text="R-score: analiza lokalna • wynik orientacyjny" else updateThreshold()
        if(engine.running) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    private fun onStartPressed() {
        if(engine.running) { pauseGame(); return }
        if(!sessionStarted && exercise.text.toString().isBlank() && !laboratory) {
            exercise.error="Wpisz ćwiczenie ustalone z logopedą"; return
        }
        if((laboratory || automatic) && checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO),101);return
        }
        begin()
    }
    private fun begin() {
        if(!activeForeground) return
        prefs.edit().putString("exercise",exercise.text.toString().trim()).apply()
        if(!sessionStarted) { engine.start(if(duration.selectedItemPosition==0) 60 else 120);sessionStarted=true }
        else engine.resume()
        lastFrame=SystemClock.elapsedRealtime();lastReward=0L
        status.text=if(laboratory) "Nasłuch testowy: dźwięk daje dopalacz. Brak oceny wymowy."
            else if (automatic) "Słucham lokalnie. Mów spokojnie — gdy wykryję cechy R, auto dostanie dopalacz."
            else "Twoje ćwiczenie: ${exercise.text.toString().trim()}"
        updateControls()
        handler.removeCallbacks(tick);handler.post(tick)
        if(laboratory || automatic) startMicrophone()
    }
    private fun startMicrophone() {
        gate=SoundGate()
        rDetector.reset()
        val token=++generation
        microphone.start({ db, durationMs, samples, count ->
            // Analysis runs on the microphone thread; only compact UI updates go to main thread.
            val evaluation = if(automatic) rDetector.accept(samples,count,db,durationMs) else null
            handler.post {
                if(token==generation && engine.running) {
                    if(laboratory) {
                        level.progress=((db+60)/60*100).toInt().coerceIn(0,100)
                        if(gate.accept(db,threshold,durationMs)) {
                            engine.reward(false)
                            status.text="Słyszę dźwięk! To test mikrofonu, nie ocena r."
                        }
                    } else if(automatic && evaluation!=null) {
                        level.progress=evaluation.score
                        if(evaluation.detected) {
                            rewardAutomatic(evaluation.score)
                        } else if(evaluation.score >= 45) {
                            status.text="Słyszę próbę… R-score ${evaluation.score}/100. Jeszcze odrobina i auto przyspieszy."
                        }
                    }
                }
            }
        }, { message ->
            handler.post { if(token==generation) { pauseGame(); status.text="$message Tryb rodzica działa bez mikrofonu." } }
        })
    }
    private fun stopMicrophone() { generation++;microphone.stop();rDetector.reset();if(::level.isInitialized) level.progress=0 }
    private fun rewardAutomatic(score:Int) {
        val now=SystemClock.elapsedRealtime()
        if(!engine.running || !automatic || now-lastReward<1200) return
        lastReward=now;engine.reward(true);totalStars++
        val earned=rewards.correctAttempt()
        val coins=garageReward()
        val comboCoins=capybaraComboBonus()
        prefs.edit().putInt("stars",totalStars).apply();updateGarage()
        status.text=if(comboCoins>0)
            "Brawo! Słyszę R • ${score}/100 • +$earned XP • +$coins 🪙 • KAPIBARA! +$comboCoins 🪙 ★"
        else
            "Brawo! Słyszę R • ${score}/100 • +$earned XP • +$coins 🪙 ★"
    }
    private fun reward() {
        val now=SystemClock.elapsedRealtime()
        if(!engine.running || laboratory || now-lastReward<800) return
        lastReward=now;engine.reward(true);totalStars++
        val earned = rewards.correctAttempt()
        val coins=garageReward()
        val comboCoins=capybaraComboBonus()
        prefs.edit().putInt("stars",totalStars).apply();updateGarage()
        status.text=if(comboCoins>0)
            "Brawo! +$earned XP • +$coins 🪙 • seria ${rewards.streak}. KAPIBARA TAŃCZY! +$comboCoins 🪙 ★"
        else
            "Brawo! +$earned XP • +$coins 🪙 • seria ${rewards.streak}. ★"
    }
    private val tick=object:Runnable {
        override fun run() {
            if(!engine.running) return
            val now=SystemClock.elapsedRealtime()
            val completed=engine.update((now-lastFrame)/1000f);lastFrame=now
            road.speed=engine.speed;road.distance=engine.distance;road.invalidate()
            stats.text="${engine.speed.toInt()} km/h  •  ${ceil(engine.remaining).toInt()} s  •  ${if(laboratory) "TEST" else if(automatic) "R BETA • ★ ${engine.stars}" else "★ ${engine.stars}"}"
            if(completed) {
                stopMicrophone();sessionStarted=false;updateControls()
                status.text=if(laboratory) "Koniec testu mikrofonu. Nie oceniano poprawności wymowy."
                    else if(automatic) "Meta! Automatyczny detektor przyznał ${engine.stars} gwiazdek. Wynik ma charakter zabawowy."
                    else "Meta! Zdobyte gwiazdki: ${engine.stars}. Czas na odpoczynek."
            } else handler.postDelayed(this,16)
        }
    }
    private fun pauseGame() {
        engine.pause();handler.removeCallbacks(tick);stopMicrophone();updateControls()
        if(sessionStarted) status.text="Przerwa. Naciśnij Wznów, kiedy będziecie gotowi."
    }
    private fun showGarage() {
        pauseGame()
        val scroll=ScrollView(this)
        val content=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setPadding(dp(18),dp(8),dp(18),dp(12))
        }
        scroll.addView(content)
        val balance=label("",16,true)
        content.addView(balance)
        content.addView(label("Ulepszenia nie wpływają na ocenę wymowy — zmieniają tylko zabawę i wygląd auta.",13))
        content.addView(label("Bonus specjalny: po 3 poprawnych R pod rząd na ulicę wbiega tańcząca kapibara i daje +$capybaraBonusCoins monet.",12))

        fun refreshBalance() {
            balance.text="★ $totalStars gwiazdek   •   🪙 ${garageSystem.coins} monet   •   ${rewards.points} XP"
        }
        fun addUpgrade(upgrade:GarageSystem.Upgrade, description:String) {
            val row=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(0,dp(7),0,dp(7)) }
            val title=label("",15,true)
            val info=label(description,12)
            val buy=button("",false) { }
            fun refresh() {
                val current=garageSystem.level(upgrade)
                val cost=garageSystem.nextCost(upgrade)
                title.text="${upgrade.title}  •  poziom $current/${upgrade.maxLevel}"
                buy.text=if(cost==null) "MAX ✓" else "Ulepsz za $cost 🪙"
                buy.isEnabled=cost!=null
            }
            buy.setOnClickListener {
                when(val result=garageSystem.buy(upgrade)) {
                    is GarageSystem.PurchaseResult.Bought -> {
                        applyGarageToGame(); refreshBalance(); refresh()
                        Toast.makeText(this,"${upgrade.title}: poziom ${result.level}!",Toast.LENGTH_SHORT).show()
                    }
                    is GarageSystem.PurchaseResult.NotEnough -> Toast.makeText(this,"Brakuje ${result.missing} monet",Toast.LENGTH_SHORT).show()
                    GarageSystem.PurchaseResult.MaxLevel -> Unit
                }
            }
            refresh(); row.addView(title); row.addView(info); row.addView(buy); content.addView(row)
        }

        refreshBalance()
        content.addView(label("ULEPSZENIA",12,true))
        addUpgrade(GarageSystem.Upgrade.ENGINE,"Większa prędkość normalna i mocniejszy dopalacz.")
        addUpgrade(GarageSystem.Upgrade.TURBO,"Dopalacz po udanej próbie działa dłużej.")
        addUpgrade(GarageSystem.Upgrade.TIRES,"Auto szybciej reaguje i sprawniej się rozpędza.")
        addUpgrade(GarageSystem.Upgrade.BODY,"Odblokowuje paski, spojler i bardziej sportowy wygląd.")

        content.addView(label("LAKIERY",12,true))
        val paintNames=arrayOf("Słoneczny","Koralowy","Niebieski")
        val paintStars=intArrayOf(0,10,30)
        for(i in paintNames.indices) {
            val unlocked=totalStars>=paintStars[i]
            val selected=i==chosenColor
            val text=when {
                selected -> "${paintNames[i]} • wybrany ✓"
                unlocked -> "${paintNames[i]} • wybierz"
                else -> "${paintNames[i]} • potrzeba ${paintStars[i]} ★"
            }
            val b=button(text,false) {
                if(totalStars>=paintStars[i]) {
                    chosenColor=i;road.carColor=colors[i];road.invalidate()
                    prefs.edit().putInt("car",i).apply()
                    Toast.makeText(this,"Wybrano lakier: ${paintNames[i]}",Toast.LENGTH_SHORT).show()
                } else Toast.makeText(this,"Jeszcze ${paintStars[i]-totalStars} gwiazdek",Toast.LENGTH_SHORT).show()
            }
            content.addView(b)
        }

        AlertDialog.Builder(this)
            .setTitle("Garaż R Auto")
            .setView(scroll)
            .setNegativeButton("Zamknij",null)
            .show()
    }
    private fun help() {
        pauseGame()
        AlertDialog.Builder(this).setTitle("R Auto • dla dorosłego")
            .setMessage("To prototyp gry wspierającej ćwiczenia ustalone z logopedą. Nie dobiera terapii i nie zastępuje oceny logopedy.\n\nTryb rodzica: wpisz zalecone ćwiczenie. Potwierdź udaną próbę przyciskiem — auto przyspieszy i zdobędzie gwiazdkę.\n\nAutomatyczne R — BETA: aplikacja analizuje dźwięk lokalnie na telefonie i szuka cech typowych dla dźwięcznego, drżącego R. Wynik jest orientacyjny: może czasem zaliczyć podobny dźwięk albo nie rozpoznać poprawnej próby. Nie zapisuje nagrań i nie korzysta z internetu.\n\nLaboratorium: auto reaguje tylko na głośność, także na klaskanie, telewizor i inne głoski. Nie przyznaje gwiazdek.\n\nMikrofon działa tylko podczas aktywnej sesji. Nagrania nie są zapisywane ani wysyłane. Po wyjściu z aplikacji gra pauzuje.\n\nGaraż: udane próby dają XP, gwiazdki i monety. Monety można wydawać na silnik, turbo, opony i karoserię. Ulepszenia zmieniają wyłącznie zabawę i wygląd auta — nie wpływają na działanie detektora R.\n\nBonus specjalny: po każdych 3 poprawnych R pod rząd pojawia się tańcząca kapibara i wpada dodatkowe $capybaraBonusCoins monet.\n\nNie wymagaj długiego, ciągłego rrrr. Róbcie przerwy. Ćwiczenia i kryteria poprawności ustalcie z logopedą.")
            .setPositiveButton("Rozumiem",null).show()
    }
    override fun onResume() { super.onResume();activeForeground=true }
    override fun onPause() { activeForeground=false;pauseGame();super.onPause() }
    override fun onDestroy() { handler.removeCallbacksAndMessages(null);stopMicrophone();super.onDestroy() }
    override fun onRequestPermissionsResult(requestCode:Int,permissions:Array<out String>,grantResults:IntArray) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults)
        if(requestCode==101) {
            if(grantResults.firstOrNull()==PackageManager.PERMISSION_GRANTED) {
                status.text="Mikrofon dostępny. Naciśnij Start, aby rozpocząć test."
            } else {
                status.text="Brak dostępu do mikrofonu. Wybierz tryb rodzica lub przyznaj dostęp w ustawieniach telefonu."
            }
        }
    }
}
