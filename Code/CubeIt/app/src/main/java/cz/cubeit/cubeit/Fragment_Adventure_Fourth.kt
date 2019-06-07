package cz.cubeit.cubeit

import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_adventure_4.view.*


class Fragment_Adventure_Fourth : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view:View = inflater.inflate(R.layout.fragment_adventure_4, container, false)
        val opts = BitmapFactory.Options()
        opts.inScaled = false
        view.surface4.setImageBitmap(BitmapFactory.decodeResource(resources, R.drawable.map3, opts))

        return view
    }
}
