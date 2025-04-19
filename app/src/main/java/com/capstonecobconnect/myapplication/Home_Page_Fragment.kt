package com.capstonecobconnect.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.capstonecobconnect.myapplication.customization.CustomizeShoes
import com.capstonecobconnect.myapplication.customization.ShoeSelection
import com.capstonecobconnect.myapplication.services.ServiceRequestFormActivity
import com.capstonecobconnect.myapplication.store.client.MarketPlaceActivity
import com.capstonecobconnect.myapplication.wallet.Wallet_View
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Home_Page_Fragment : Fragment() {
    private val userSignedIn = true
    private var doubleBackToExitPressedOnce = false

    private var firestore: FirebaseFirestore? = null
    private var auth: FirebaseAuth? = null
    private var notificationPopup: PopupWindow? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home__page, container, false)

        initializeFirebase()

        val services = view.findViewById<LinearLayout>(R.id.cServices)


        // Set click listener
        services.setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val intent = Intent(requireContext(), ServiceRequestFormActivity::class.java)
            intent.putExtra("USER_ID", userId)
            startActivity(intent)
        }

        val cMarket = view.findViewById<LinearLayout>(R.id.c_market)

        cMarket.setOnClickListener {
            val intent = Intent(requireContext(), MarketPlaceActivity::class.java)
            startActivity(intent)
        }

        val cCustomize = view.findViewById<LinearLayout>(R.id.c_customize)

        cCustomize.setOnClickListener {
            val intent = Intent(requireContext(), ShoeSelection::class.java)
            startActivity(intent)
        }

        setupWalletButton(view)






                /*
                //setupMarketPlaceButton(view)
                setupPolishingServiceButton(view)
                setupStitchingServiceButton(view)
                setupCleaningServiceButton(view)
                setupCustomizationServiceButton(view)
                //setupNotificationIcon(view)

                 */
        handleBackPress()

        return view
    }

    private fun initializeFirebase() {
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
    }

    private fun setupWalletButton(view: View) {
        val walletLin = view.findViewById<LinearLayout>(R.id.wallet_lin)
        val userId = FirebaseAuth.getInstance().currentUser?.uid


        walletLin.setOnClickListener {
            val intent = Intent(requireContext(), Wallet_View::class.java)
            intent.putExtra("USER_ID", userId)
            startActivity(intent)
        }
    }

    /*
    private fun setupMarketPlaceButton(view: View) {
        val polishingServiceBtn = view.findViewById<Button>(R.id.marketPlaceBtn)

        polishingServiceBtn.setOnClickListener {
            val intent = Intent(requireContext(), MarketPlaceActivity::class.java)
            startActivity(intent)
        }
    }

     */


    /*
    private fun setupPolishingServiceButton(view: View) {
        val polishingServiceBtn = view.findViewById<ImageView>(R.id.polishingService)

        polishingServiceBtn.setOnClickListener {
            val intent = Intent(requireContext(), PolishingActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupStitchingServiceButton(view: View) {
        val polishingServiceBtn = view.findViewById<ImageView>(R.id.stitchingService)

        polishingServiceBtn.setOnClickListener {
            val intent = Intent(requireContext(), StitchingActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupCleaningServiceButton(view: View) {
        val polishingServiceBtn = view.findViewById<ImageView>(R.id.cleaningService)

        polishingServiceBtn.setOnClickListener {
            val intent = Intent(requireContext(), CleaningActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupCustomizationServiceButton(view: View) {
        val polishingServiceBtn = view.findViewById<ImageView>(R.id.customizationService)

        polishingServiceBtn.setOnClickListener {
            val intent = Intent(requireContext(), CustomizationActivity::class.java)
            startActivity(intent)
        }
    }

     */

    /*
    X
    }

     */

    private fun showNotificationBubble(anchor: View) {
        if (notificationPopup == null) {
            val bubbleView =
                LayoutInflater.from(context).inflate(R.layout.notification_bubble, null)
            notificationPopup = PopupWindow(
                bubbleView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            )
            notificationPopup!!.setOnDismissListener {}
        }

        if (notificationPopup!!.isShowing) {
            notificationPopup!!.dismiss()
        } else {
            notificationPopup!!.showAsDropDown(anchor, -50, 0) // Adjust the position as needed
        }
    }

    private fun handleBackPress() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (userSignedIn) {
                        if (doubleBackToExitPressedOnce) {
                            requireActivity().finishAffinity() // Close the application
                        } else {
                            doubleBackToExitPressedOnce = true
                            Toast.makeText(
                                requireContext(),
                                "Please click BACK again to exit",
                                Toast.LENGTH_SHORT
                            ).show()

                            Handler(Looper.getMainLooper()).postDelayed({
                                doubleBackToExitPressedOnce = false
                            }, 2000)
                        }
                    } else {
                        isEnabled = false
                        requireActivity().onBackPressed()
                    }
                }
            })
    }
}