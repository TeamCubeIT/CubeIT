package cz.cubeit.cubeit

import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupWindow
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_story_quest_template_0.view.*
import kotlinx.android.synthetic.main.popup_dialog.view.*

class Fragment_Story_Quest_Template_0 : Fragment() {

    lateinit var viewTemplate0: View

    companion object{
        fun newInstance(questID: String = "0001", slideNum: Int = 0): Fragment_Story_Quest_Template_0{
            val fragment = Fragment_Story_Quest_Template_0()
            val args = Bundle()
            args.putString("questID", questID)
            args.putInt("slideNum", slideNum)
            fragment.arguments = args
            return fragment
        }
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        val slide: StorySlide = storyQuests.filter { it.ID == arguments!!.getString("questID") }[0].slides[arguments!!.getInt("slideNum")]

        if (isVisibleToUser && arguments!!.getInt("slideNum") != 0) {
            viewTemplate0.textView0Template0.animateText(slide.textContent)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewTemplate0 = inflater.inflate(R.layout.fragment_story_quest_template_0, container, false)
        val slide: StorySlide = storyQuests.filter { it.ID == arguments!!.getString("questID") }[0].slides[arguments!!.getInt("slideNum")]

        if(!this.isAdded)return viewTemplate0
        viewTemplate0.textView0Template0.animateText(slide.textContent)

        val opts = BitmapFactory.Options()
        opts.inScaled = false
        viewTemplate0.imageView0Template0.setImageBitmap(BitmapFactory.decodeResource(resources, slide.images[0].drawable, opts))
        viewTemplate0.imageView1Template0.setImageBitmap(BitmapFactory.decodeResource(resources, slide.images[1].drawable, opts))

        viewTemplate0.layoutFragmentStoryTemplate0.setOnTouchListener(object : Class_OnSwipeTouchListener(viewTemplate0.context) {
            override fun onDoubleClick() {
                super.onDoubleClick()
                (this@Fragment_Story_Quest_Template_0.parentFragment as Fragment_Story).skipSlide()
                Log.d("story skip", "true")
            }
        })

        viewTemplate0.buttonFragmentStorySkip0.setOnClickListener {
            val viewP = layoutInflater.inflate(R.layout.popup_dialog, container, false)
            val window = PopupWindow(context)
            window.contentView = viewP
            val buttonYes: Button = viewP.buttonYes
            val buttonNo:Button = viewP.buttonClose
            val info: TextView = viewP.textViewInfo
            info.text = "Are you sure?"
            window.isOutsideTouchable = false
            window.isFocusable = true
            buttonYes.setOnClickListener {
                (this.parentFragment as Fragment_Story).skipStory()
                Log.d("story skip", "true")
                window.dismiss()
            }
            buttonNo.setOnClickListener {
                window.dismiss()
            }
            window.showAtLocation(viewP, Gravity.CENTER,0,0)
        }
        return viewTemplate0
    }
}
