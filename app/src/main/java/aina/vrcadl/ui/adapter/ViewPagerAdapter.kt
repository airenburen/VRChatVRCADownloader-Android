package aina.vrcadl.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import aina.vrcadl.ui.fragment.AvatarListFragment
import aina.vrcadl.ui.fragment.DownloadsFragment

class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    
    private val fragments = listOf(
        AvatarListFragment(),
        DownloadsFragment()
    )
    
    private val titles = listOf("Avatar 列表", "下载任务")
    
    override fun getItemCount(): Int = fragments.size
    
    override fun createFragment(position: Int): Fragment = fragments[position]
    
    fun getPageTitle(position: Int): String = titles[position]
}
