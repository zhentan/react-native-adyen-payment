package com.rnlib.adyen.ui.component

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.adyen.checkout.components.ComponentError
import com.adyen.checkout.components.ComponentView
import com.adyen.checkout.components.PaymentComponent
import com.adyen.checkout.components.PaymentComponentState
import com.adyen.checkout.components.ViewableComponent
import com.adyen.checkout.components.base.Configuration
import com.adyen.checkout.components.base.OutputData
import com.adyen.checkout.components.model.payments.request.PaymentMethodDetails
import com.adyen.checkout.components.util.CurrencyUtils
import com.adyen.checkout.core.exception.CheckoutException
import com.adyen.checkout.core.log.LogUtil
import com.adyen.checkout.core.log.Logger
import com.adyen.checkout.dropin.R
import com.adyen.checkout.dropin.databinding.FragmentGenericComponentBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.rnlib.adyen.getViewFor
import com.rnlib.adyen.ui.base.BaseComponentDialogFragment
import kotlinx.android.synthetic.main.frag_generic_component.*

class GenericComponentDialogFragment : BaseComponentDialogFragment() {

    private lateinit var componentView: ComponentView<in OutputData, ViewableComponent<*, *, *>>
    private lateinit var binding: FragmentGenericComponentBinding

    companion object : BaseCompanion<GenericComponentDialogFragment>(GenericComponentDialogFragment::class.java) {
        private val TAG = LogUtil.getTag()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentGenericComponentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setPaymentPendingInitialization(pending: Boolean) {
        if (!componentView.isConfirmationRequired()) return
        binding.payButton.isVisible = !pending
        if (pending) binding.progressBar.show()
        else binding.progressBar.hide()
    }

    override fun highlightValidationErrors() {
        componentView.highlightValidationErrors()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Logger.d(TAG, "onViewCreated")
        binding.header.text = paymentMethod.name

        if (!adyenComponentConfiguration.amount.isEmpty) {
            val value = CurrencyUtils.formatAmount(adyenComponentConfiguration.amount,
                adyenComponentConfiguration.shopperLocale)
            binding.payButton.text = String.format(resources.getString(R.string.pay_button_with_value), value)
        }

        try {
            componentView = getViewFor(requireContext(), paymentMethod.type!!)
            attachComponent(component, componentView)
        } catch (e: CheckoutException) {
            handleError(ComponentError(e))
        }
    }

    override fun onChanged(paymentComponentState: PaymentComponentState<in PaymentMethodDetails>?) {
        componentDialogViewModel.componentStateChanged(component.getState(), componentView.isConfirmationRequired())
    }

    private fun attachComponent(
        component: PaymentComponent<PaymentComponentState<in PaymentMethodDetails>, Configuration>,
        componentView: ComponentView<in OutputData, ViewableComponent<*, *, *>>
    ) {
        component.observe(viewLifecycleOwner, this)
        component.observeErrors(viewLifecycleOwner, createErrorHandlerObserver())
        binding.componentContainer.addView(componentView as View)
        componentView.attach(component as ViewableComponent<*, *, *>, viewLifecycleOwner)

        if (componentView.isConfirmationRequired()) {
            binding.payButton.setOnClickListener { componentDialogViewModel.payButtonClicked() }
            setInitViewState(BottomSheetBehavior.STATE_EXPANDED)
            (componentView as View).requestFocus()
        } else {
            binding.payButton.visibility = View.GONE
        }
    }
}
