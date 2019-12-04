package cz.cubeit.cubeit_test

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.view.animation.Animation
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import java.io.*
import kotlin.random.Random.Default.nextInt

object SystemFlow{
    var factionChange: Boolean = false

    open class GameActivity(
            private val contentLayoutId: Int,
            private val activityType: ActivityType,
            private val hasMenu: Boolean,
            private val menuID: Int = 0,
            private val menuUpColor: Int? = null
    ): AppCompatActivity(contentLayoutId){

        val dm = DisplayMetrics()
        var frameLayoutMenuBar: FrameLayout? = null
        var imageViewSwipeDown: ImageView? = null
        var imageViewMenuUp: ImageView? = null
        var menuFragment: Fragment_Menu_Bar? = null
        lateinit var parentViewGroup: ViewGroup
        lateinit var propertiesBar: GamePropertiesBar

        private fun hideSystemUI() {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }

        fun visualizeRewardWith(startingPoint: Coordinates, reward: Reward?, existingPropertiesBar: GamePropertiesBar? = null): ValueAnimator? {
            return visualizeReward(this, startingPoint, reward, existingPropertiesBar)
        }

        private fun clearFocus(){
            val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            var view = currentFocus
            if (view == null) {
                view = View(this)
            }
            imm.hideSoftInputFromWindow(view.windowToken, 0)

        }

        override fun onWindowFocusChanged(hasFocus: Boolean) {
            super.onWindowFocusChanged(hasFocus)
            if (hasFocus) hideSystemUI()
        }

        override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
            val menuViewRect = Rect()
            val propertyBarViewRect = Rect()
            frameLayoutMenuBar?.getGlobalVisibleRect(menuViewRect)
            propertiesBar.frameLayoutBar.getGlobalVisibleRect(propertyBarViewRect)

            if (!menuViewRect.contains(ev.rawX.toInt(), ev.rawY.toInt()) && frameLayoutMenuBar?.y ?: 0f <= (dm.heightPixels * 0.83).toFloat() && ev.action == MotionEvent.ACTION_UP && solidMenuBar() && !propertyBarViewRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                hideMenuBar()
            }
            return super.dispatchTouchEvent(ev)
        }

        override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
            if(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP){
                respondOnMediaButton(this)
            }
            return super.onKeyDown(keyCode, event)
        }

        fun showMenuBar(){
            if(hasMenu && frameLayoutMenuBar != null){
                ValueAnimator.ofFloat(frameLayoutMenuBar?.y ?: 0f, (dm.heightPixels * 0.83).toFloat()).apply {
                    duration = 600
                    addUpdateListener {
                        frameLayoutMenuBar?.y = it.animatedValue as Float
                    }
                    start()
                }
            }
        }

        fun solidMenuBar(): Boolean{
            return frameLayoutMenuBar?.y == (dm.heightPixels - (frameLayoutMenuBar?.height ?: 0)).toFloat()
        }

        fun hideMenuBar(){
            if(hasMenu && frameLayoutMenuBar != null){
                ValueAnimator.ofFloat(frameLayoutMenuBar?.y ?: 0f, dm.heightPixels.toFloat()).apply {
                    duration = 600
                    addUpdateListener {
                        frameLayoutMenuBar?.y = it.animatedValue as Float
                    }
                    start()
                }
            }
        }

        fun initMenuBar(layoutID: Int){
            if(hasMenu && frameLayoutMenuBar != null){
                frameLayoutMenuBar?.post {
                    menuFragment = Fragment_Menu_Bar.newInstance(layoutID, frameLayoutMenuBar?.id ?: 0, imageViewSwipeDown?.id ?: 0, imageViewMenuUp?.id ?: 0)

                    supportFragmentManager.beginTransaction()
                            .replace(parentViewGroup.findViewWithTag<FrameLayout>("frameLayoutMenuBar$activityType").id, menuFragment!!, "menuBarFragment$activityType").commitAllowingStateLoss()

                    Handler().postDelayed({
                        menuFragment?.setUpSecondAction(View.OnClickListener { if(propertiesBar.isShown) propertiesBar.hide() else propertiesBar.show() })
                    }, 500)
                }
            }
        }

        fun hasMenu(): Boolean{
            return hasMenu && frameLayoutMenuBar != null
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            hideSystemUI()
            setContentView(contentLayoutId)
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.defaultDisplay.getRealMetrics(dm)
            parentViewGroup = this.window.decorView.rootView.findViewById(android.R.id.content)

            propertiesBar = GamePropertiesBar(this)

            window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
                if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                    Handler().postDelayed({hideSystemUI()},1000)
                }
            }
            parentViewGroup.post {
                clearFocus()
            }

            if(hasMenu){
                frameLayoutMenuBar = FrameLayout(this)
                imageViewSwipeDown = ImageView(this)
                imageViewMenuUp = ImageView(this)

                parentViewGroup.post {
                    frameLayoutMenuBar?.apply {
                        layoutParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT)
                        layoutParams.height = ((dm.heightPixels * 0.175).toInt())
                        layoutParams.width = dm.widthPixels
                        y = (dm.heightPixels).toFloat()
                        tag = "frameLayoutMenuBar$activityType"
                        id = View.generateViewId()                  //generate new ID, since adding fragment requires IDs
                    }
                    imageViewSwipeDown?.apply {
                        layoutParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT)
                        layoutParams.height = ((dm.heightPixels * 0.175).toInt())
                        layoutParams.width = ((dm.heightPixels * 0.175).toInt())
                        x = (dm.widthPixels * 0.5 - dm.heightPixels * 0.175).toFloat()
                        y = -(dm.heightPixels * 0.175).toFloat()
                        setImageResource(R.drawable.home_button)
                        setBackgroundResource(R.drawable.emptyspellslotlarge)
                        tag = "imageViewSwipeDown$activityType"
                        id = View.generateViewId()
                    }

                    imageViewMenuUp?.apply {
                        layoutParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT)
                        layoutParams.height = ((dm.widthPixels * 0.07).toInt())
                        layoutParams.width = ((dm.widthPixels * 0.07).toInt())
                        x = (dm.widthPixels - (dm.widthPixels * 0.07) - 4f).toFloat()
                        y = dm.heightPixels - (dm.widthPixels * 0.07).toFloat()
                        tag = "imageViewMenuUp$activityType"
                        id = View.generateViewId()
                        setBackgroundResource(R.drawable.arrow_up)
                        background?.setColorFilter(resources.getColor(menuUpColor ?: R.color.loginColor), PorterDuff.Mode.SRC_ATOP)
                    }
                    parentViewGroup.apply {
                        addView(imageViewSwipeDown)
                        addView(imageViewMenuUp)
                        addView(frameLayoutMenuBar)
                        invalidate()
                    }

                    frameLayoutMenuBar?.post {
                        imageViewSwipeDown?.background?.setColorFilter(resources.getColor(R.color.loginColor), PorterDuff.Mode.SRC_ATOP)
                    }
                    initMenuBar(menuID)
                }
            }
        }
    }

    fun showSocials(activity: GameActivity): Fragment_Socials{
        val parent: ViewGroup = activity.window.decorView.findViewById(android.R.id.content)
        val frameLayoutSocials = FrameLayout(activity)
        val fragmentSocials = Fragment_Socials()

        parent.removeView(parent.findViewWithTag<FrameLayout>("frameLayoutSocials"))

        frameLayoutSocials.apply {
            layoutParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT)
            layoutParams.height = activity.dm.heightPixels
            layoutParams.width = (activity.dm.widthPixels * 0.6).toInt()
            y = 0f
            x = ((activity.dm.widthPixels * 0.5) - (activity.dm.heightPixels * 0.5)).toFloat()
            tag = "frameLayoutSocials"
            id = View.generateViewId()                  //generate new ID, since adding fragment requires IDs
        }

        parent.addView(frameLayoutSocials)
        parent.invalidate()
        frameLayoutSocials.post {
            (activity as AppCompatActivity).supportFragmentManager.beginTransaction().replace(parent.findViewWithTag<FrameLayout>("frameLayoutSocials").id, fragmentSocials, "frameLayoutSocials").commitAllowingStateLoss()
        }

        return fragmentSocials
    }

    /**
     * SoundPool for short audio clips only, max 1 MB.
     * @since Alpha 0.5.0.2, DEV version
     * @author Jakub Kostka
     */
    fun playComponentSound(context: Context, raw: Int = R.raw.creeper){
        if(!Data.player.soundEffects) return

        val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        val sounds = SoundPool.Builder()
                .setAudioAttributes(attributes)
                .setMaxStreams(5)
                .build()

        val componentSound = sounds.load(context, raw, 1)       //raw has to processed beforehand

        sounds.setOnLoadCompleteListener { soundPool, sampleId, status ->
            Log.d("sounds_status", status.toString())
            if(status == 0){
                sounds.play(componentSound, 1f, 1f, 1, 0, 1f)
            }
        }
    }

    class ItemDragListener(v: View) : View.DragShadowBuilder(v) {

        //creates new instance of the drawable, so it doesn't pass the reference of the ImageView and messes it up
        private val shadow = (view as? ImageView)?.drawable?.constantState?.newDrawable()

        // Defines a callback that sends the drag shadow dimensions and touch point back to the
        // system.
        override fun onProvideShadowMetrics(size: Point, touch: Point) {
            // Sets the width of the shadow to half the width of the original View
            val width: Int = view.width

            // Sets the height of the shadow to half the height of the original View
            val height: Int = view.height

            // The drag shadow is a ColorDrawable. This sets its dimensions to be the same as the
            // Canvas that the system will provide. As a result, the drag shadow will fill the
            // Canvas.
            shadow?.setBounds(0, 0, width, height)

            // Sets the size parameter's width and height values. These get back to the system
            // through the size parameter.
            size.set(width, height)

            // Sets the touch point's position to be in the middle of the drag shadow
            touch.set(width / 2, height / 2)
        }

        // Defines a callback that draws the drag shadow in a Canvas that the system constructs
        // from the dimensions passed in onProvideShadowMetrics().
        override fun onDrawShadow(canvas: Canvas) {
            // Draws the ColorDrawable in the Canvas passed in from the system.
            shadow?.draw(canvas)
        }
    }

    fun vibrateAsError(context: Context, length: Long = 35){
        if(Data.player.vibrateEffects){
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v!!.vibrate(VibrationEffect.createOneShot(length, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                v!!.vibrate(length)
            }
        }
    }

    data class MorseVibration(
            val timing: MutableList<Long>,
            val amplitudes: MutableList<Int>
    ){
        var handlers: MutableList<Handler> = mutableListOf()

        fun addChar(timing: MutableList<Long>, amplitudes: MutableList<Int>){
            this.timing.addAll(timing)
            this.amplitudes.addAll(amplitudes)
        }

        fun detachHandlers(){
            for(i in handlers){
                i.removeCallbacksAndMessages(null)
            }
        }
    }

    //TODO vytvoř obdobu visualize reward baru nabízející vypnutí hudby, změny hlasitosti atd.
    fun respondOnMediaButton(context: Context){
        Toast.makeText(context, "Shhhhh!", Toast.LENGTH_SHORT).show()
    }

    fun translateIntoMorse(text: String, textView: CustomTextView? = null): MorseVibration {
        val gapLetterLength: Long = 200     //174
        val gapWordLength: Long = 500       //1214

        val soundSpace: Long = 20
        val dit: Long = 60      //58
        val dah: Long = 200     //174
        val morseMap = hashMapOf(
                'a' to longArrayOf(dit, soundSpace, dah, soundSpace),
                'b' to longArrayOf(dah, soundSpace, dit,soundSpace, dit,soundSpace, dit, soundSpace),
                'c' to longArrayOf(dah,soundSpace, dit,soundSpace, dah,soundSpace, dit, soundSpace),
                'd' to longArrayOf(dah,soundSpace, dit,soundSpace, dit, soundSpace),
                'e' to longArrayOf(dit, soundSpace),
                'f' to longArrayOf(dit,soundSpace, dit,soundSpace, dah,soundSpace, dit, soundSpace),
                'g' to longArrayOf(dah, soundSpace, dah, soundSpace, dit, soundSpace),
                'h' to longArrayOf(dit, soundSpace, dit, soundSpace, dit, soundSpace, dit, soundSpace),
                'i' to longArrayOf(dit, soundSpace, dit, soundSpace),
                'j' to longArrayOf(dit, soundSpace, dah, soundSpace, dah, soundSpace, dah, soundSpace),
                'k' to longArrayOf(dah, soundSpace, dit, soundSpace, dah, soundSpace),
                'l' to longArrayOf(dit, soundSpace, dah, soundSpace, dit, soundSpace, dit, soundSpace),
                'm' to longArrayOf(dah, soundSpace, dah, soundSpace),
                'n' to longArrayOf(dah, soundSpace, dit, soundSpace),
                'o' to longArrayOf(dah, soundSpace, dah,soundSpace, dah, soundSpace),
                'p' to longArrayOf(dit, soundSpace, dah,soundSpace, dah, soundSpace, dit, soundSpace),
                'q' to longArrayOf(dah, soundSpace, dah,soundSpace, dit, soundSpace, dah, soundSpace),
                'r' to longArrayOf(dit, soundSpace, dah ,soundSpace,dit, soundSpace),
                's' to longArrayOf(dit, soundSpace, dit,soundSpace, dit, soundSpace),
                't' to longArrayOf(dah, soundSpace),
                'u' to longArrayOf(dit, soundSpace, dit, soundSpace, dah, soundSpace),
                'w' to longArrayOf(dit, soundSpace, dah, soundSpace, dah, soundSpace),
                'x' to longArrayOf(dah, soundSpace, dit, soundSpace, dit, soundSpace, dah, soundSpace),
                'y' to longArrayOf(dah, soundSpace, dit, soundSpace, dah, soundSpace, dah, soundSpace),
                'z' to longArrayOf(dah, soundSpace, dah, soundSpace, dit, soundSpace, dit, soundSpace)
        )

        var lengthPrevious: Long = 100
        val morse = MorseVibration(mutableListOf(dah), mutableListOf(0))
        for(i in text.toLowerCase()){
            val newHandler = Handler()
            morse.handlers.add(newHandler)
            if(i == ' '){
                morse.addChar(mutableListOf(gapWordLength), mutableListOf(0))

                lengthPrevious += gapWordLength
                newHandler.postDelayed({
                    textView?.text = (textView?.text.toString() ?: "") + " "
                }, lengthPrevious)
            }else {
                val amplitudes = mutableListOf<Int>()
                for(j in 0 until (morseMap[i]?.toMutableList() ?: mutableListOf()).size / 2){
                    amplitudes.add(255)
                    amplitudes.add(0)
                }
                morse.addChar(morseMap[i]?.toMutableList() ?: mutableListOf(), amplitudes)
                morse.addChar(mutableListOf(gapLetterLength), mutableListOf(0))

                lengthPrevious += (morseMap[i]?.toMutableList() ?: mutableListOf()).sum() + gapLetterLength
                newHandler.postDelayed({
                    textView?.text = (textView?.text.toString() ?: "") + i.toString()
                }, lengthPrevious)
            }
        }

        return morse
    }

    fun resolveLayoutLocation(activity: Activity, x: Float, y: Float, viewX: Int, viewY: Int): Coordinates{     //calculates the best location of dynamicly sized pop-up window and dynamicly placed click location
        val parent = activity.window.decorView.rootView

        return Coordinates(
                if(x >= parent.width - x){
                    if(x - viewX < 0){
                        0f
                    }else {
                        x - viewX
                    }
                }else {
                    if(x > parent.width){
                        parent.width.toFloat()
                    }else {
                        x
                    }
                },

                if(y in parent.height / 2 * 0.8 .. parent.height / 2 * 1.2){
                    ((parent.height / 2) - (viewY / 2)).toFloat()

                }else if(y >= parent.height / 2){
                    if(y - viewY < 0){
                        0f
                    }else {
                        Log.d("viewY", viewY.toString())
                        y - viewY
                    }
                }else {
                    Log.d("y-viewY2", (y + viewY).toString() + " / " + parent.height.toString())
                    if(y + viewY > parent.height){
                        parent.height - viewY.toFloat()
                    }else {
                        y
                    }
                }
                /*kotlin.math.max(kotlin.math.abs(x), kotlin.math.abs(parent.width - x)),
                kotlin.math.max(kotlin.math.abs(y), kotlin.math.abs(parent.height - y))*/
        )
    }

    /**
     * Component (Fragment) overlay to show user's game properties (money, experience etc.).
     * Can be used for animations(throughout key Fragment object) and changes its values via animations ("fragmentBar.animateChanges()").
     *@property activity: Activity - used for display metrics and attaching generated view on the activity's main viewGroup
     *@property duration: Long?
     * @since Alpha 0.5.0.2, DEV version
     * @author Jakub Kostka
     */
    class GamePropertiesBar(
            val activity: GameActivity,
            val duration: Long? = null
    ){
        private val parent: ViewGroup = activity.window.decorView.findViewById(android.R.id.content)
        val fragmentBar = FragmentGamePropertiesBar()
        val frameLayoutBar: FrameLayout = FrameLayout(parent.context)
        var isShown = false
        var attached = false

        fun updateProperties(){
            if(!isShown){
                show()?.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        fragmentBar.animateChanges()
                    }
                })
            }else fragmentBar.animateChanges()
        }

        @SuppressLint("ClickableViewAccessibility")
        fun attach(): ValueAnimator? {
            if(attached) return null

            attached = true

            var clickableTemp = false
            var initialTouchX = 0f
            var originalX = (activity.dm.widthPixels * 0.25).toFloat()
            frameLayoutBar.apply {
                layoutParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT)
                layoutParams.height = ((activity.dm.heightPixels * 0.1).toInt())
                layoutParams.width = (activity.dm.widthPixels * 0.5).toInt()
                y = (-(activity.dm.heightPixels * 0.1)).toFloat()
                x = Data.requestedBarX ?: originalX
                tag = "frameLayoutBar"
                id = View.generateViewId()                  //generate new ID, since adding fragment requires IDs
            }

            Handler().postDelayed({
                fragmentBar.getActionBackground().setOnTouchListener { _, motionEvent ->
                    when (motionEvent.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialTouchX = motionEvent.rawX

                            clickableTemp = true
                            Handler().postDelayed({
                                clickableTemp = false
                            }, 100)
                            originalX = frameLayoutBar.x
                        }
                        MotionEvent.ACTION_UP -> {
                            if(clickableTemp){
                                this@GamePropertiesBar.hide()
                            }else {
                                Data.requestedBarX = frameLayoutBar.x
                            }
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val requestedX = (originalX + (motionEvent.rawX - initialTouchX))
                            frameLayoutBar.x = when {
                                requestedX < 0 -> {
                                    0f
                                }
                                requestedX > activity.dm.widthPixels * 0.5 -> {
                                    (activity.dm.widthPixels * 0.5).toFloat()
                                }
                                else -> {
                                    requestedX
                                }
                            }
                        }
                    }
                    true
                }
            }, 600)

            parent.addView(frameLayoutBar)
            parent.invalidate()
            frameLayoutBar.post {
                (activity as AppCompatActivity).supportFragmentManager.beginTransaction().replace(parent.findViewWithTag<FrameLayout>("frameLayoutBar").id, fragmentBar, "barFragment").commitAllowingStateLoss()
            }

            if(duration != null){
                Handler().postDelayed({
                    this.detach()
                }, duration)
            }

            return show()
        }

        /**
         * animation not promised
         */
        fun hide(): ValueAnimator? {
            if(!isShown) return null

            isShown = false

            return ObjectAnimator.ofFloat(frameLayoutBar.y, (-(activity.dm.heightPixels * 0.1).toFloat())).apply{
                duration = 600
                addUpdateListener {
                    frameLayoutBar.y = it.animatedValue as Float
                }
                start()
            }
        }

        /**
         * animation not promised
         */
        fun show(): ValueAnimator? {
            if(isShown) return null

            return if(!attached){
                attach()
            }else {
                isShown = true
                ObjectAnimator.ofFloat((-(activity.dm.heightPixels * 0.1)).toFloat(), 0f).apply{
                    duration = 600
                    addUpdateListener {
                        frameLayoutBar.y = it.animatedValue as Float
                    }
                    start()
                }
            }
        }

        fun detach(){
            if(!attached) return

            hide()?.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    isShown = false
                    attached = false
                    parent.removeView(frameLayoutBar)
                }
            })
        }
    }

    /**
     * Animation overlay, primarily used to show damage dealt by any action in fight.
     *
     * @property activity: Activity - used for display metrics and attaching generated views on the activity's main viewGroup
     * @property startingPoint: Coordinates - use "getLocationOnScreen(IntArray(2))" to return correct position of the clicked view, regular x, y may not work.
     * @return ObjectAnimator. Override onAnimationEnd method to end the animation properly, or use native method ObjectAnimator.cancel().
     * @since Alpha 0.5.0.2, DEV version
     * @author Jakub Kostka
     */
    fun makeActionText(activity: GameActivity, startingPoint: Coordinates, text: String, color: Int = R.color.loginColor, sizeType: CustomTextView.SizeType = CustomTextView.SizeType.adaptive): ObjectAnimator{
        val parent = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)

        val textView = CustomTextView(activity)
        textView.apply {
            fontSizeType = sizeType
            layoutParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT)
            visibility = View.VISIBLE
            setHTMLText("<b>$text</b>")
            tag = "actionText-${startingPoint.x}-${startingPoint.y}"
            setTextColor(activity.resources.getColor(color))
        }

        parent.addView(textView)
        textView.post {

            textView.x = startingPoint.x + textView.width / 2
            textView.invalidate()
            ObjectAnimator.ofFloat(startingPoint.y - textView.height / 2, (startingPoint.y - textView.height / 2) / 4).apply{
                duration = 600
                addUpdateListener {
                    textView.y = it.animatedValue as Float
                }
                addListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator) {}

                    override fun onAnimationEnd(animation: Animator) {
                        parent.removeView(textView)
                        Log.d("makeActionText", "post-ended, x: ${textView.x}, y: ${textView.y}, width: ${textView.width}, height: ${textView.height}")
                    }

                    override fun onAnimationCancel(animation: Animator) {}

                    override fun onAnimationRepeat(animation: Animator) {}
                })
                start()
            }
        }

        return ObjectAnimator()
    }

    /**
     * Animation overlay, universally used to show user's property bar with values and change them with animation.
     *
     * @property activity: Activity - used for display metrics and attaching generated views on the activity's main viewGroup
     * @property startingPoint: Coordinates - use "getLocationOnScreen(IntArray(2))" to return correct position of the clicked view, regular x, y may not work.
     * @return ObjectAnimator. Override onAnimationEnd method to end the animation properly, or use native method ObjectAnimator.cancel().
     * @since Alpha 0.5.0.2, DEV version
     * @author Jakub Kostka
     */
    fun visualizeReward(activity: GameActivity, startingPoint: Coordinates, reward: Reward?, existingPropertiesBar: GamePropertiesBar? = null): ValueAnimator? {
        val parent = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)
        val activityWidth = activity.dm.widthPixels

        val propertiesBar = existingPropertiesBar ?: GamePropertiesBar(activity)

        val floatingCoins: MutableList<ImageView> = mutableListOf()
        val floatingXps: MutableList<ImageView> = mutableListOf()
        val floatingCubix: MutableList<ImageView> = mutableListOf()

        fun process(){
            if((reward?.cubeCoins ?: 0) > 0){
                for(i in 0 until nextInt(3, 10)){
                    val currentCoin = ImageView(activity)

                    floatingCoins.add(i, ImageView(activity))
                    parent.addView(currentCoin)
                    parent.invalidate()

                    currentCoin.post {
                        currentCoin.apply {
                            setImageResource(R.drawable.coin_basic)

                            ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_CONSTRAINT, ConstraintLayout.LayoutParams.MATCH_CONSTRAINT)
                            layoutParams?.width = (activityWidth * 0.05).toInt()
                            layoutParams?.height = (activityWidth * 0.05).toInt()
                            visibility = View.VISIBLE
                        }
                    }

                    val newX = nextInt((startingPoint.x - (activityWidth * 0.075)).toInt(), (startingPoint.x + (activityWidth * 0.075)).toInt()).toFloat()
                    val newY = nextInt((startingPoint.y - (activityWidth * 0.075)).toInt(), (startingPoint.y + (activityWidth * 0.075)).toInt()).toFloat()

                    //travel animation CC
                    val travelXCC = ObjectAnimator.ofFloat(newX, propertiesBar.fragmentBar.getGlobalCoordsCubeCoins().x).apply{
                        duration = 600
                        addUpdateListener {
                            currentCoin.x = it.animatedValue as Float
                        }
                    }
                    val travelYCC = ObjectAnimator.ofFloat(newY, propertiesBar.fragmentBar.getGlobalCoordsCubeCoins().y).apply{
                        duration = 600
                        addUpdateListener {
                            currentCoin.y = it.animatedValue as Float
                        }
                        addListener(object : Animator.AnimatorListener {
                            override fun onAnimationStart(animation: Animator) {}

                            override fun onAnimationEnd(animation: Animator) {
                                parent.removeView(currentCoin)
                                propertiesBar.updateProperties()
                                Handler().postDelayed({
                                    if(propertiesBar.isShown && existingPropertiesBar == null) propertiesBar.detach()
                                }, 620)
                            }

                            override fun onAnimationCancel(animation: Animator) {}

                            override fun onAnimationRepeat(animation: Animator) {}
                        })
                    }

                    //spread animation CC
                    ObjectAnimator.ofFloat(startingPoint.x, newX).apply{
                        duration = 200
                        addUpdateListener {
                            currentCoin.x = it.animatedValue as Float
                        }
                        start()
                    }
                    ObjectAnimator.ofFloat(startingPoint.y, newY).apply{
                        duration = 200
                        addUpdateListener {
                            currentCoin.y = it.animatedValue as Float
                        }
                        addListener(object : Animator.AnimatorListener {
                            override fun onAnimationStart(animation: Animator) {}

                            override fun onAnimationEnd(animation: Animator) {
                                travelXCC.start()
                                travelYCC.start()
                            }

                            override fun onAnimationCancel(animation: Animator) {}

                            override fun onAnimationRepeat(animation: Animator) {}
                        })
                        start()
                    }
                }
            }

            if((reward?.experience ?: 0) > 0){
                for(i in 0 until nextInt(3, 7)){
                    val currentXp = ImageView(activity)

                    floatingXps.add(i, ImageView(activity))
                    parent.addView(currentXp)
                    parent.invalidate()

                    currentXp.post {
                        currentXp.apply {
                            setImageResource(R.drawable.xp)

                            layoutParams!!.width = (activityWidth * 0.05).toInt()
                            layoutParams!!.height = (activityWidth * 0.05).toInt()
                            ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_CONSTRAINT, ConstraintLayout.LayoutParams.MATCH_CONSTRAINT)
                            visibility = View.VISIBLE
                        }
                    }

                    val newX = nextInt((startingPoint.x - (activityWidth * 0.075)).toInt(), (startingPoint.x + (activityWidth * 0.075)).toInt()).toFloat()
                    val newY = nextInt((startingPoint.y - (activityWidth * 0.075)).toInt(), (startingPoint.y + (activityWidth * 0.075)).toInt()).toFloat()

                    //travel animation XP
                    val travelXXP = ObjectAnimator.ofFloat(newX, propertiesBar.fragmentBar.getGlobalCoordsExperience().x).apply{
                        duration = 700
                        addUpdateListener {
                            currentXp.x = it.animatedValue as Float
                        }
                    }
                    val travelYXP = ObjectAnimator.ofFloat(newY, propertiesBar.fragmentBar.getGlobalCoordsExperience().y).apply{
                        duration = 700
                        addUpdateListener {
                            currentXp.y = it.animatedValue as Float
                        }
                        addListener(object : Animator.AnimatorListener {
                            override fun onAnimationStart(animation: Animator) {}

                            override fun onAnimationEnd(animation: Animator) {
                                parent.removeView(currentXp)
                                propertiesBar.updateProperties()
                                Handler().postDelayed({
                                    if(propertiesBar.isShown && existingPropertiesBar == null) propertiesBar.detach()
                                }, 700)
                            }

                            override fun onAnimationCancel(animation: Animator) {}

                            override fun onAnimationRepeat(animation: Animator) {}
                        })
                    }

                    //spread animation XP
                    ObjectAnimator.ofFloat(startingPoint.x, newX).apply{
                        duration = 300
                        addUpdateListener {
                            currentXp.x = it.animatedValue as Float
                        }
                        start()
                    }
                    ObjectAnimator.ofFloat(startingPoint.y, newY).apply{
                        duration = 300
                        addUpdateListener {
                            currentXp.y = it.animatedValue as Float
                        }
                        addListener(object : Animator.AnimatorListener {
                            override fun onAnimationStart(animation: Animator) {}

                            override fun onAnimationEnd(animation: Animator) {
                                travelXXP.start()
                                travelYXP.start()
                            }

                            override fun onAnimationCancel(animation: Animator) {}

                            override fun onAnimationRepeat(animation: Animator) {}
                        })
                        start()
                    }
                }
            }

            if((reward?.cubix ?: 0) > 0){
                for(i in 0 until nextInt(3, 7)){
                    val currentCubix = ImageView(activity)

                    floatingCubix.add(i, ImageView(activity))
                    parent.addView(currentCubix)
                    parent.invalidate()

                    currentCubix.post {
                        currentCubix.apply {
                            setImageResource(R.drawable.crystal)

                            layoutParams!!.width = (activityWidth * 0.05).toInt()
                            layoutParams!!.height = (activityWidth * 0.05).toInt()
                            ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_CONSTRAINT, ConstraintLayout.LayoutParams.MATCH_CONSTRAINT)
                            visibility = View.VISIBLE
                        }
                    }

                    val newX = nextInt((startingPoint.x - (activityWidth * 0.075)).toInt(), (startingPoint.x + (activityWidth * 0.075)).toInt()).toFloat()
                    val newY = nextInt((startingPoint.y - (activityWidth * 0.075)).toInt(), (startingPoint.y + (activityWidth * 0.075)).toInt()).toFloat()

                    //travel animation Cubix
                    val travelXCubix = ObjectAnimator.ofFloat(newX, propertiesBar.fragmentBar.getGlobalCoordsCubix().x).apply{
                        duration = 700
                        addUpdateListener {
                            currentCubix.x = it.animatedValue as Float
                        }
                    }
                    val travelYCubix = ObjectAnimator.ofFloat(newY, propertiesBar.fragmentBar.getGlobalCoordsCubix().y).apply{
                        duration = 700
                        addUpdateListener {
                            currentCubix.y = it.animatedValue as Float
                        }
                        addListener(object : Animator.AnimatorListener {
                            override fun onAnimationStart(animation: Animator) {}

                            override fun onAnimationEnd(animation: Animator) {
                                parent.removeView(currentCubix)
                                propertiesBar.updateProperties()
                                Handler().postDelayed({
                                    if(propertiesBar.isShown && existingPropertiesBar == null) propertiesBar.detach()
                                }, 700)
                            }

                            override fun onAnimationCancel(animation: Animator) {}

                            override fun onAnimationRepeat(animation: Animator) {}
                        })
                    }

                    //spread animation Cubix
                    ObjectAnimator.ofFloat(startingPoint.x, newX).apply{
                        duration = 300
                        addUpdateListener {
                            currentCubix.x = it.animatedValue as Float
                        }
                        start()
                    }
                    ObjectAnimator.ofFloat(startingPoint.y, newY).apply{
                        duration = 300
                        addUpdateListener {
                            currentCubix.y = it.animatedValue as Float
                        }
                        addListener(object : Animator.AnimatorListener {
                            override fun onAnimationStart(animation: Animator) {}

                            override fun onAnimationEnd(animation: Animator) {
                                travelXCubix.start()
                                travelYCubix.start()
                            }

                            override fun onAnimationCancel(animation: Animator) {}

                            override fun onAnimationRepeat(animation: Animator) {}
                        })
                        start()
                    }
                }
            }

            Handler().postDelayed({
                reward?.receive()
            }, 400)
        }

        if(propertiesBar.isShown){
            process()
        }else {
            propertiesBar.attach()?.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    process()
                }
            })
        }
        return ObjectAnimator.ofFloat(0f, 0f).apply{
            duration = 1500
            start()
        }
    }

    /**
     * Loading screen overlay, universally used to make user wait, with having just darken background, not entire screen.
     *
     * @property activity: Activity - used for display metrics and attaching generated views on the activity's main viewGroup
     * @return ObjectAnimator. Override onAnimationEnd method to end the animation properly, or use native method ObjectAnimator.cancel().
     * @since Alpha 0.5.0.2, DEV version
     * @author Jakub Kostka
     */
    fun createLoading(activity: GameActivity, startAutomatically: Boolean = true, cancelable: Boolean = false, listener: View.OnClickListener? = null): ObjectAnimator {
        val parent = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)
        val context = parent.context

        val activityWidth = activity.dm.widthPixels

        val loadingBg = ImageView(context)
        loadingBg.apply {
            tag = "customLoadingBg"
            layoutParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT)
            visibility = View.VISIBLE
            isClickable = true
            isFocusable = true
            setImageResource(R.drawable.darken_background)
            alpha = 0.8f
        }

        val loadingImage = ImageView(context)
        loadingImage.apply {
            tag = "customLoadingImage"
            setImageResource(R.drawable.icon_web)
            visibility = View.VISIBLE
            layoutParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_CONSTRAINT, ConstraintLayout.LayoutParams.MATCH_CONSTRAINT)
            layoutParams.width = (activityWidth * 0.1).toInt()
            layoutParams.height = (activityWidth * 0.1).toInt()
            x = ((activityWidth / 2 - (activityWidth * 0.1 / 2).toInt()).toFloat())
            y = (activityWidth * 0.05).toFloat()
        }

        parent.addView(loadingBg)
        parent.addView(loadingImage)
        if(cancelable){
            val loadingCancel = Button(context, null, 0, R.style.AppTheme_Button)
            loadingCancel.apply {
                tag = "customLoadingCancel"
                visibility = View.VISIBLE
                text = "cancel"
                layoutParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT)
                x = ((activityWidth / 2 - (activityWidth * 0.1 / 2).toInt()).toFloat())
                y = (activity.dm.heightPixels * 0.4).toFloat()
                setOnClickListener(listener)
            }
            parent.addView(loadingCancel)
        }

        val rotateAnimation: ObjectAnimator = ObjectAnimator.ofFloat(loadingImage ,
                "rotation", 0f, 360f)

        rotateAnimation.addListener(object : AnimatorListenerAdapter() {

            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                parent.removeView(parent.findViewWithTag<ImageView>("customLoadingImage"))
                parent.removeView(parent.findViewWithTag<FrameLayout>("customLoadingBg"))
                if(cancelable) parent.removeView(parent.findViewWithTag<Button>("customLoadingCancel"))
            }

            /*override fun onAnimationStart(animation: Animator?) {
                super.onAnimationStart(animation)
                loadingBg.bringToFront()
                loadingImage.bringToFront()
            }*/
        })
        rotateAnimation.duration = 900
        rotateAnimation.repeatCount = Animation.INFINITE

        if(startAutomatically) loadingImage.post {
            rotateAnimation.start()
        }

        return rotateAnimation
    }

    class BackgroundSoundService : Service() {

        override fun onBind(arg0: Intent): IBinder? {
            return null
        }

        override fun onCreate() {
            super.onCreate()
            Data.mediaPlayer = MediaPlayer.create(this, Data.playedSong)
            Data.mediaPlayer!!.isLooping = true                                            // Set looping
            Data.mediaPlayer!!.setVolume(100f, 100f)

            Data.mediaPlayer!!.setOnCompletionListener {
                Data.mediaPlayer?.release()
            }
        }

        override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
            Data.mediaPlayer?.start()
            return START_NOT_STICKY
        }

        override fun onStart(intent: Intent, startId: Int) {
        }

        fun pause() {
            Data.mediaPlayer?.stop()
            Data.mediaPlayer?.release()
            Data.mediaPlayer = null
        }

        override fun onDestroy() {
            Data.mediaPlayer?.stop()
            Data.mediaPlayer?.release()
        }

        override fun onLowMemory() {
        }
    }

    class LifecycleListener(val context: Context) : LifecycleObserver {

        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        fun onMoveToForeground() {
            context.stopService(Intent(context, ClassCubeItHeadService::class.java))
            Data.player.syncStats()
            if (Data.player.music && Data.player.username != "player") {
                val svc = Intent(context, Data.bgMusic::class.java)
                Handler().postDelayed({
                    context.startService(svc)
                }, 500)
            }
            Data.player.online = true
            Data.player.uploadSingleItem("online")
            if (Data.player.currentStoryQuest != null && Data.player.currentStoryQuest!!.progress == 0) Data.player.currentStoryQuest = null
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        fun onMoveToBackground() {
            if (Data.player.music && Data.mediaPlayer != null) {
                val svc = Intent(context, Data.bgMusic::class.java)
                context.stopService(svc)
            }
            Data.player.online = false
            Data.player.uploadPlayer()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Data.player.appearOnTop) {
                if (Settings.canDrawOverlays(context)) {
                    context.startService(Intent(context, ClassCubeItHeadService::class.java))
                }
            }
        }
    }

    fun showNotification(titleInput: String, textInput: String, context: Context): androidx.appcompat.app.AlertDialog {
        val builder = androidx.appcompat.app.AlertDialog.Builder(context)
        builder.setTitle(titleInput)
        builder.setMessage(textInput)
        val dialog: androidx.appcompat.app.AlertDialog = builder.create()
        dialog.show()
        return dialog
    }

    fun isStoragePermissionGranted(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v("", "Permission is granted")
                true
            } else {

                Log.v("", "Permission is revoked")
                ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
                false
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v("", "Permission is granted")
            true
        }
    }

    fun screenShot(view: View): Bitmap {
        System.gc()
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    fun bitmapToURI(context: Context, inImage: Bitmap, title: String, description: String?): Uri {
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(context.contentResolver, inImage, title, description)
        return Uri.parse(path?:"")
    }

    @Throws(IOException::class)
    fun writeObject(context: Context, fileName: String, objectG: Any) {
        context.openFileOutput(fileName, Context.MODE_PRIVATE).close()

        val fos = context.openFileOutput(fileName, Context.MODE_PRIVATE)
        val oos = ObjectOutputStream(fos)
        oos.reset()
        oos.writeObject(objectG)
        oos.flush()
        oos.close()
        fos.close()
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    fun readObject(context: Context, fileName: String): Any {
        val file = context.getFileStreamPath(fileName)
        if (!file.exists()) {
            file.createNewFile()
        }
        val fis = context.openFileInput(fileName)
        return if (file.readText() != "") {
            val ois = ObjectInputStream(fis)
            try{
                ois.readObject()
            }catch(e1: java.io.NotSerializableException){
                return 0
            }catch (e2: InvalidClassException){
                return 0
            }
        } else {
            0
        }
    }

    fun readFileText(context: Context, fileName: String): String {
        val file = context.getFileStreamPath(fileName)
        if (!file.exists() || file.readText() == "") {
            file.createNewFile()
            file.writeText("0")
        }
        return file.readText()
    }

    fun writeFileText(context: Context, fileName: String, content: String) {
        val file = context.getFileStreamPath(fileName)
        file.delete()
        file.createNewFile()
        file.writeText(content)
    }

    fun exceptionFormatter(errorIn: String): String {

        return if (errorIn.contains("com.google.firebase.auth")) {

            val regex = Regex("com.google.firebase.auth.\\w+: ")
            errorIn.replace(regex, "Error: ")
        } else {
            Log.d("ExceptionFormatterError", "Failed to format exception, falling back to source")
            errorIn
        }
    }
}