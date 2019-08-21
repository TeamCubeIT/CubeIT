package cz.cubeit.cubeit

import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.PorterDuff
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_faction_edit.view.*
import kotlinx.android.synthetic.main.row_faction_invitation.view.*


class Fragment_Faction_Edit : Fragment() {
    var inviteAllies: MutableList<String> = Data.player.allies.toTypedArray().toMutableList()
    lateinit var allies: BaseAdapter
    lateinit var invited: BaseAdapter

    override fun onResume() {
        super.onResume()
        //(activity!! as Activity_Faction_Base).tabLayoutFactionTemp.visibility = View.VISIBLE
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view:View = inflater.inflate(R.layout.fragment_faction_edit, container, false)

        fun init(){
            view.editTextFactionEditDescription.setText(Data.player.faction!!.description)
            view.editTextFactionEditName.setText(Data.player.faction!!.name)
            view.editTextFactionEditTax.setText(Data.player.faction!!.taxPerDay.toString())
            view.editTextFactionEditWarnMsg.setText(Data.player.faction!!.warnMessage)
            view.editTextFactionEditInvitationMsg.setText(Data.player.faction!!.invitationMessage)

            (view.listViewFactionEditAllies.adapter as FactionMemberList).notifyDataSetChanged()
            (view.listViewFactionEditInvited.adapter as FactionMemberList).notifyDataSetChanged()
        }

        if(Data.player.faction != null){
            for(i in inviteAllies.toTypedArray()){
                if(Data.player.faction!!.pendingInvitationsPlayer.contains(i)) inviteAllies.remove(i)
            }
            view.listViewFactionEditAllies.adapter = FactionMemberList(this, inviteAllies, true, resources)
            view.listViewFactionEditInvited.adapter = FactionMemberList(this, Data.player.faction!!.pendingInvitationsPlayer, false, resources)

            allies = (view.listViewFactionEditAllies.adapter as FactionMemberList)
            invited = (view.listViewFactionEditInvited.adapter as FactionMemberList)

            init()

            if(Data.player.factionRole == FactionRole.MODERATOR){
                view.editTextFactionEditName.isEnabled = false
            }
        }

        return view
    }

    fun update(){
        allies.notifyDataSetChanged()
        invited.notifyDataSetChanged()
    }


    class FactionMemberList(val parent: Fragment_Faction_Edit, var collection: MutableList<String> = Data.player.allies, val add: Boolean = true, val resources: Resources) : BaseAdapter() {

        override fun getCount(): Int {
            return collection.size
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getItem(position: Int): Any {
            return "TEST STRING"
        }

        override fun getView(position: Int, convertView: View?, viewGroup: ViewGroup?): View {
            val rowMain: View

            if (convertView == null) {
                val layoutInflater = LayoutInflater.from(viewGroup!!.context)
                rowMain = layoutInflater.inflate(R.layout.row_faction_invitation, viewGroup, false)
                val viewHolder = ViewHolder(rowMain.imageViewFactionCreateRowIcon, rowMain.textViewFactionCreateRowName)
                rowMain.tag = viewHolder
            } else {
                rowMain = convertView
            }
            val viewHolder = rowMain.tag as ViewHolder

            val opts = BitmapFactory.Options()
            opts.inScaled = false

            if(add){
                viewHolder.symbol.setImageBitmap(BitmapFactory.decodeResource(resources, R.drawable.plus_icon, opts))
                viewHolder.symbol.drawable.setColorFilter(resources.getColor(R.color.itemborder_uncommon), PorterDuff.Mode.SRC_ATOP)
            }else {
                viewHolder.symbol.setImageBitmap(BitmapFactory.decodeResource(resources, R.drawable.minus_icon, opts))
                viewHolder.symbol.drawable.setColorFilter(resources.getColor(R.color.progress_hp), PorterDuff.Mode.SRC_ATOP)
            }
            viewHolder.username.text = collection[position]

            rowMain.setOnClickListener {
                rowMain.isEnabled = false
                handler.postDelayed({rowMain.isEnabled = true}, 50)
                if(add){
                    (parent.activity!! as Activity_Faction_Base).inviteList.add(collection[position])
                    Data.player.faction!!.pendingInvitationsPlayer.add(collection[position])
                    parent.inviteAllies.remove(collection[position])
                }else {
                    parent.inviteAllies.add(collection[position])
                    (parent.activity!! as Activity_Faction_Base).inviteList.remove(collection[position])
                    Data.player.faction!!.pendingInvitationsPlayer.remove(collection[position])
                }
                parent.update()
            }

            return rowMain
        }
        private class ViewHolder(val symbol: ImageView, val username: TextView)
    }
}
