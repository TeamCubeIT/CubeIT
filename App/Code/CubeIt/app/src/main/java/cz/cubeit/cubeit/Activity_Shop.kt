package cz.cubeit.cubeit

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.DisplayMetrics
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import kotlinx.android.synthetic.main.activity_shop.*
import kotlinx.android.synthetic.main.activity_shop.textViewInfoItem
import kotlinx.android.synthetic.main.popup_dialog.view.*
import kotlinx.android.synthetic.main.row_shop_inventory.view.*
import kotlinx.android.synthetic.main.row_shop_offer.view.*

var lastClicked = ""

class Activity_Shop : AppCompatActivity(){

    private var hidden = false
    var displayY = 0.0

    override fun onBackPressed() {
        val intent = Intent(this, Home::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        this.overridePendingTransition(0,0)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }
    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val viewRect = Rect()
        frameLayoutMenuShop.getGlobalVisibleRect(viewRect)

        if (!viewRect.contains(ev.rawX.toInt(), ev.rawY.toInt()) && frameLayoutMenuShop.y <= (displayY * 0.83).toFloat()) {

            ValueAnimator.ofFloat(frameLayoutMenuShop.y, displayY.toFloat()).apply {
                duration = (frameLayoutMenuShop.y/displayY * 160).toLong()
                addUpdateListener {
                    frameLayoutMenuShop.y = it.animatedValue as Float
                }
                start()
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI()
        Data.player.syncStats()
        setContentView(R.layout.activity_shop)
        textViewShopMoney.text = Data.player.cubeCoins.toString()
        textViewShopMoney.fontSizeType = CustomTextView.SizeType.title
        textViewShopMoney.setPadding(10, 10, 10, 10)

        val opts = BitmapFactory.Options()
        opts.inScaled = false
        imageViewActivityShop.setImageBitmap(BitmapFactory.decodeResource(resources, R.drawable.shop_bg, opts))

        val animUpText: Animation = AnimationUtils.loadAnimation(applicationContext,
                R.anim.animation_shop_text_up)
        val animDownText: Animation = AnimationUtils.loadAnimation(applicationContext,
                R.anim.animation_shop_text_down)

        textViewInfoItem.startAnimation(animDownText)
        val originalCoinY = imageViewShopCoin.y

        val dm = DisplayMetrics()
        val windowManager = this.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getMetrics(dm)
        displayY = dm.heightPixels.toDouble()

        supportFragmentManager.beginTransaction().add(R.id.frameLayoutMenuShop, Fragment_Menu_Bar.newInstance(R.id.imageViewActivityShop, R.id.frameLayoutMenuShop, R.id.homeButtonBackShop, R.id.imageViewMenuUpShop)).commitNow()
        frameLayoutMenuShop.y = dm.heightPixels.toFloat()

        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                handler.postDelayed({hideSystemUI()},1000)
            }
        }

        listViewShopInventory.adapter = ShopInventory(hidden, animUpText, animDownText, Data.player, textViewInfoItem, layoutInflater.inflate(R.layout.popup_dialog,null), this, listViewShopInventory, textViewShopMoney)
        listViewShopOffers.adapter = ShopOffer(hidden, animUpText, animDownText, Data.player, textViewInfoItem, listViewShopInventory.adapter as ShopInventory, this, textViewShopMoney, listViewShopInventory)
        listViewShopOffers.layoutParams.width = (displayY * 0.87).toInt()

        var animationRefresh = ValueAnimator()

        shopOfferRefresh.setOnClickListener {refresh: View ->
            val moneyReq = Data.player.level * 10

            if(!animationRefresh.isRunning){
                if(Data.player.cubeCoins >= moneyReq){
                    Data.player.cubeCoins -= moneyReq
                    for(i in 0 until Data.player.shopOffer.size){
                        Data.player.shopOffer[i] = GameFlow.generateItem(Data.player)
                        (listViewShopOffers.adapter as ShopOffer).notifyDataSetChanged()
                    }
                    lastClicked = ""

                    textViewShopMoney.text = Data.player.cubeCoins.toString()
                    textViewShopCoin.text = (moneyReq*-1).toString()

                    animationRefresh = ValueAnimator.ofFloat(originalCoinY, refresh.y).apply {
                        duration = 400
                        addUpdateListener {
                            imageViewShopCoin.y = it.animatedValue as Float
                            textViewShopCoin.y = it.animatedValue as Float - textViewShopCoin.height
                        }
                        addListener(object : Animator.AnimatorListener {
                            override fun onAnimationRepeat(animation: Animator?) {
                            }

                            override fun onAnimationCancel(animation: Animator?) {
                            }

                            override fun onAnimationStart(animation: Animator?) {
                                imageViewShopCoin.visibility = View.VISIBLE
                                textViewShopCoin.visibility = View.VISIBLE
                            }

                            override fun onAnimationEnd(animation: Animator?) {
                                imageViewShopCoin.visibility = View.GONE
                                textViewShopCoin.visibility = View.GONE
                            }
                        })
                        start()
                    }
                }
            }
        }
    }
    private class ShopInventory(var hidden:Boolean, val animUpText:Animation, val animDownText:Animation, val playerS:Player, val textViewInfoItem: CustomTextView, val viewInflater:View, val context:Context, val listView:ListView, val textViewMoney:TextView) : BaseAdapter() {

        override fun getCount(): Int {
            return playerS.inventorySlots / 4 + 1
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getItem(position: Int): Any {
            return "TEST STRING"
        }

        @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
        override fun getView(position: Int, convertView: View?, viewGroup: ViewGroup?): View {
            val rowMain: View

            if (convertView == null) {
                val layoutInflater = LayoutInflater.from(viewGroup!!.context)
                rowMain = layoutInflater.inflate(R.layout.row_shop_inventory, viewGroup, false)
                val viewHolder = ViewHolder(rowMain.buttonInventory1, rowMain.buttonInventory2, rowMain.buttonInventory3, rowMain.buttonInventory4)
                rowMain.tag = viewHolder
            } else rowMain = convertView
            val viewHolder = rowMain.tag as ViewHolder

            val index:Int = if(position == 0) 0 else{
                position*4
            }

            class Node(
                    val index: Int = 0,
                    val component: ImageView
            ){
                init {
                    if(this.index < playerS.inventory.size ){
                        if(playerS.inventory[this.index] != null){
                            component.setImageResource(playerS.inventory[this.index]!!.drawable)
                            component.setBackgroundResource(playerS.inventory[this.index]!!.getBackground())
                            component.isEnabled = true
                        }else{
                            component.setImageResource(0)
                            component.setBackgroundResource(R.drawable.emptyslot)
                            component.isEnabled = false
                        }
                    }else{
                        component.isEnabled = false
                        component.setBackgroundResource(0)
                        component.setImageResource(0)
                    }

                    component.setOnTouchListener(object : Class_OnSwipeTouchListener(context, component) {
                        override fun onClick() {
                            super.onClick()
                            //if(!hidden && lastClicked==="inventory0$position"){textViewInfoItem.startAnimation(animUpText);hidden = true}else if(hidden){textViewInfoItem.startAnimation(animDownText);hidden = false}
                            lastClicked="inventory${this@Node.index}$position"
                            textViewInfoItem.setHTMLText(playerS.inventory[this@Node.index]?.getStatsCompare()!!)
                        }

                        override fun onDoubleClick() {
                            super.onDoubleClick()
                            getDoubleClick(this@Node.index, context, viewInflater, listView, playerS, textViewMoney, textViewInfoItem)
                        }
                    })
                }

            }

            Node(index, viewHolder.buttonInventory1)
            Node(index + 1, viewHolder.buttonInventory2)
            Node(index + 2, viewHolder.buttonInventory3)
            Node(index + 3, viewHolder.buttonInventory4)

            return rowMain
        }
        private class ViewHolder(val buttonInventory1:ImageView, val buttonInventory2:ImageView, val buttonInventory3:ImageView, val buttonInventory4:ImageView)
    }

    private class ShopOffer(
            var hidden:Boolean,
            val animUpText: Animation,
            val animDownText: Animation,
            val player:Player,
            val textViewInfoItem: CustomTextView,
            val InventoryShop:BaseAdapter,
            private val context:Context,
            val textViewMoney: TextView,
            val inventory: ListView
    ) : BaseAdapter() {

        override fun getCount(): Int {
            return 2
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getItem(position: Int): Any {
            return "TEST STRING"
        }

        @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
        override fun getView(position: Int, convertView: View?, viewGroup: ViewGroup?): View {
            val rowMain: View

            val index:Int = if(position == 0) 0 else{
                position*4
            }

            if (convertView == null) {
                val layoutInflater = LayoutInflater.from(viewGroup!!.context)
                rowMain = layoutInflater.inflate(R.layout.row_shop_offer, viewGroup, false)
                val viewHolder = ViewHolder(rowMain.buttonOffer1, rowMain.buttonOffer2, rowMain.buttonOffer3, rowMain.buttonOffer4)
                rowMain.tag = viewHolder
            } else rowMain = convertView

            val viewHolder = rowMain.tag as ViewHolder

            rowMain.isEnabled = false

            class Node(
                    val index: Int = 0,
                    val component: ImageView
            ){
                init {
                    if(Data.player.shopOffer[this.index] != null){
                        this.component.apply {
                            setImageResource(Data.player.shopOffer[this@Node.index]!!.drawable)
                            setBackgroundResource(Data.player.shopOffer[this@Node.index]!!.getBackground())
                            isClickable = true
                        }
                    }else this.component.isClickable = false

                    component.setOnTouchListener(object : Class_OnSwipeTouchListener(context, component) {
                        override fun onClick() {
                            super.onClick()
                            lastClicked="offer${this@Node.index}$position"
                            textViewInfoItem.setHTMLText(Data.player.shopOffer[this@Node.index]?.getStatsCompare(true)!!)
                        }

                        override fun onDoubleClick() {
                            super.onDoubleClick()
                            component.isPressed = false
                            getDoubleClickOffer(this@Node.index, player, textViewInfoItem, textViewMoney, inventory)
                            notifyDataSetChanged()
                            InventoryShop.notifyDataSetChanged()
                        }
                    })
                }
            }

            Node(index + 0, viewHolder.buttonOffer1)
            Node(index + 1, viewHolder.buttonOffer2)
            Node(index + 2, viewHolder.buttonOffer3)
            Node(index + 3, viewHolder.buttonOffer4)

            return rowMain
        }
        private class ViewHolder(val buttonOffer1:ImageView, val buttonOffer2:ImageView, val buttonOffer3:ImageView, val buttonOffer4:ImageView)
    }

    companion object {
        private fun getDoubleClick(index:Int, context:Context, view:View, listViewInventoryShop:ListView, player:Player, textViewMoney:TextView, textViewInfoItem:TextView){
            val window = PopupWindow(context)
            window.contentView = view
            val buttonYes:Button = view.buttonYes
            val buttonNo:ImageView = view.buttonCloseDialog
            val info:TextView = view.textViewInfo
            info.text = "Are you sure you want to sell ${Data.player.inventory[index]?.name} ?"
            window.isOutsideTouchable = false
            window.isFocusable = true
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            buttonYes.setOnClickListener {
                Data.player.cubeCoins+=Data.player.inventory[index]!!.priceCubeCoins
                Data.player.inventory[index]=null
                (listViewInventoryShop.adapter as ShopInventory).notifyDataSetChanged()
                textViewMoney.text = Data.player.cubeCoins.toString()
                textViewInfoItem.visibility = View.INVISIBLE
                window.dismiss()
            }
            buttonNo.setOnClickListener {
                window.dismiss()
            }
            window.showAtLocation(view, Gravity.CENTER,0,0)
        }

        private fun getDoubleClickOffer(index:Int, player:Player, textViewInfoItem:TextView, textViewMoney: TextView, listViewInventoryShop: ListView){
            if(Data.player.cubeCoins >= Data.player.shopOffer[index]!!.priceCubeCoins && Data.player.cubix >= Data.player.shopOffer[index]!!.priceCubix){
                if(Data.player.inventory.contains(null)){
                    Data.player.cubeCoins -= Data.player.shopOffer[index]!!.priceCubeCoins
                    textViewMoney.text = Data.player.cubeCoins.toString()
                    Data.player.shopOffer[index]!!.priceCubeCoins /= 2
                    Data.player.inventory[Data.player.inventory.indexOf(null)] = Data.player.shopOffer[index]
                    Data.player.shopOffer[index] = GameFlow.generateItem(player)
                    textViewInfoItem.visibility = View.INVISIBLE
                }else{
                    listViewInventoryShop.startAnimation(AnimationUtils.loadAnimation(textViewMoney.context, R.anim.animation_shaky_short))
                    //Snackbar.make(textViewInfoItem, "Not enough space!", Snackbar.LENGTH_SHORT).show()
                }
            }else{
                textViewMoney.startAnimation(AnimationUtils.loadAnimation(textViewMoney.context, R.anim.animation_shaky_short))
                //Snackbar.make(textViewInfoItem, "Not enough cube coins!", Snackbar.LENGTH_SHORT).show()
            }
        }
    }
}