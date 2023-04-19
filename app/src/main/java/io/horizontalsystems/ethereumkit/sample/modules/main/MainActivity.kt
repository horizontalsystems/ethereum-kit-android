package io.horizontalsystems.ethereumkit.sample.modules.main

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.horizontalsystems.ethereumkit.sample.R
import io.horizontalsystems.ethereumkit.sample.modules.addresswatch.AddressWatchActivity
import io.horizontalsystems.ethereumkit.sample.modules.uniswapV3.UniswapV3Fragment
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    private val balanceFragment = BalanceFragment()
    private val transactionsFragment = TransactionsFragment()
    private val sendReceiveFragment = SendReceiveFragment()
    private val swapFragment = SwapFragment()
    private val uniswapV3Fragment = UniswapV3Fragment()
    private val nftsFragment = NftsFragment()
    private val fm = supportFragmentManager
    private var active: Fragment = balanceFragment

    lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuAddressWatch -> {
                    val intent = Intent(this, AddressWatchActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        val navigation = findViewById<BottomNavigationView>(R.id.navigation)
        navigation.setOnNavigationItemSelectedListener(this)

        fm.beginTransaction().add(R.id.fragment_container, uniswapV3Fragment, "6").hide(uniswapV3Fragment).commit()
        fm.beginTransaction().add(R.id.fragment_container, nftsFragment, "5").hide(nftsFragment).commit()
        fm.beginTransaction().add(R.id.fragment_container, swapFragment, "4").hide(swapFragment).commit()
        fm.beginTransaction().add(R.id.fragment_container, sendReceiveFragment, "3").hide(sendReceiveFragment).commit()
        fm.beginTransaction().add(R.id.fragment_container, transactionsFragment, "2").hide(transactionsFragment).commit()
        fm.beginTransaction().add(R.id.fragment_container, balanceFragment, "1").commit()

        navigation.selectedItemId = R.id.navigation_nfts

        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        viewModel.init()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val fragment = when (item.itemId) {
            R.id.navigation_home -> balanceFragment
            R.id.navigation_transactions -> transactionsFragment
            R.id.navigation_send_receive -> sendReceiveFragment
//            R.id.navigation_swap -> swapFragment
            R.id.navigation_uniswapV3 -> uniswapV3Fragment
            R.id.navigation_nfts -> nftsFragment
            else -> null
        }

        if (fragment != null) {
            supportFragmentManager
                    .beginTransaction()
                    .hide(active)
                    .show(fragment)
                    .commit()

            active = fragment
            findViewById<Toolbar>(R.id.toolbar)?.title = item.title

            return true
        }

        return false
    }

}
