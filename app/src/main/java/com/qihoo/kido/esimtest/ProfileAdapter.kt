package com.qihoo.kido.esimtest

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.clj.fastble.data.BleDevice
import com.velocate.lpa.v22.core.EuiccProfile

/**
 * @author yolo.huang
 * @date 2019-11-09
 */
class ProfileAdapter(val context: Context):RecyclerView.Adapter<ProfileAdapter.ProfileViewHolder>() {

    var watchList: MutableList<BleDevice>? = null

    var itemClick: ItemClick ? = null


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileViewHolder {
        val profileView = LayoutInflater.from(context).inflate(R.layout.item_euicc_profile,null)
        return ProfileViewHolder(profileView)
    }

    override fun getItemCount(): Int {
        return watchList?.size?:0
    }

    override fun onBindViewHolder(holder: ProfileViewHolder, position: Int) {
        val device = watchList?.get(position)
        holder.imei.text = device?.name
        holder.imei.setOnClickListener { itemClick?.onClick(position) }
    }


    class ProfileViewHolder(itemView: View):
        RecyclerView.ViewHolder(itemView) {
        val imei = itemView.findViewById<TextView>(R.id.tv_watch)

    }

    interface ItemClick {
        fun onClick(positon: Int)
    }

    fun setData(watchList: MutableList<BleDevice>?){
        this.watchList = watchList
        notifyDataSetChanged()
    }
}
fun EuiccProfile.toStringSelf():String{
    return "EuiccProfile: \n" +
            " iccid: " + iccid + "\n" +
            " name: " + name + "\n" +
            " nickname: " + nickname + "\n" +
            " provider: " + provider + "\n" +
            "status:" + status
}