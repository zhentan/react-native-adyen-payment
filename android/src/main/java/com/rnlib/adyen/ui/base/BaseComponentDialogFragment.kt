package com.rnlib.adyen.ui.base

import androidx.lifecycle.Observer
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.adyen.checkout.components.ComponentError
import com.adyen.checkout.components.PaymentComponent
import com.adyen.checkout.components.PaymentComponentState
import com.adyen.checkout.components.base.Configuration
import com.adyen.checkout.components.model.paymentmethods.PaymentMethod
import com.adyen.checkout.components.model.paymentmethods.StoredPaymentMethod
import com.adyen.checkout.components.model.payments.request.PaymentMethodDetails
import com.adyen.checkout.core.exception.CheckoutException
import com.adyen.checkout.core.log.LogUtil
import com.adyen.checkout.core.log.Logger
import com.adyen.checkout.dropin.ui.ComponentDialogViewModel
import com.adyen.checkout.dropin.ui.ComponentFragmentState
import com.adyen.checkout.dropin.ui.base.DropInBottomSheetDialogFragment
import androidx.lifecycle.observe

import com.rnlib.adyen.AdyenComponentConfiguration
import com.rnlib.adyen.getComponentFor

private const val DROP_IN_CONFIGURATION = "DROP_IN_CONFIGURATION"
private const val STORED_PAYMENT_METHOD = "STORED_PAYMENT_METHOD"
private const val NAVIGATED_FROM_PRESELECTED = "NAVIGATED_FROM_PRESELECTED"
private const val PAYMENT_METHOD = "PAYMENT_METHOD"

@Suppress("TooManyFunctions")
abstract class BaseComponentDialogFragment : DropInBottomSheetDialogFragment(), Observer<PaymentComponentState<in PaymentMethodDetails>> {

    companion object {
        private val TAG = LogUtil.getTag()
    }

    protected val componentDialogViewModel: ComponentDialogViewModel by viewModels()

    var paymentMethod: PaymentMethod = PaymentMethod()
    var storedPaymentMethod: StoredPaymentMethod = StoredPaymentMethod()
    lateinit var component: PaymentComponent<PaymentComponentState<in PaymentMethodDetails>, Configuration>
    lateinit var adyenComponentConfiguration: AdyenComponentConfiguration
    private var isStoredPayment = false
    private var navigatedFromPreselected = false

    open class BaseCompanion<T : BaseComponentDialogFragment>(private var classes: Class<T>) {

        fun newInstance(
            paymentMethod: PaymentMethod,
            adyenComponentConfiguration: AdyenComponentConfiguration
        ): T {
            val args = Bundle()
            args.putParcelable(PAYMENT_METHOD, paymentMethod)
            args.putParcelable(DROP_IN_CONFIGURATION, adyenComponentConfiguration)

            val dialogFragment = classes.newInstance()
            dialogFragment.arguments = args
            return dialogFragment
        }

        fun newInstance(
            storedPaymentMethod: StoredPaymentMethod,
            adyenComponentConfiguration: AdyenComponentConfiguration,
            navigatedFromPreselected: Boolean
        ): T {
            val args = Bundle()
            args.putParcelable(STORED_PAYMENT_METHOD, storedPaymentMethod)
            args.putParcelable(DROP_IN_CONFIGURATION, adyenComponentConfiguration)
            args.putBoolean(NAVIGATED_FROM_PRESELECTED, navigatedFromPreselected)

            val dialogFragment = classes.newInstance()
            dialogFragment.arguments = args
            return dialogFragment
        }
    }

    abstract override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?

    abstract override fun onViewCreated(view: View, savedInstanceState: Bundle?)

    abstract override fun onChanged(paymentComponentState: PaymentComponentState<in PaymentMethodDetails>?)

    protected abstract fun setPaymentPendingInitialization(pending: Boolean)

    protected abstract fun highlightValidationErrors()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            storedPaymentMethod = it.getParcelable(STORED_PAYMENT_METHOD) ?: storedPaymentMethod
            paymentMethod = it.getParcelable(PAYMENT_METHOD) ?: paymentMethod
            isStoredPayment = !storedPaymentMethod.type.isNullOrEmpty()
            navigatedFromPreselected = it.getBoolean(NAVIGATED_FROM_PRESELECTED, false)
            adyenComponentConfiguration = it.getParcelable(DROP_IN_CONFIGURATION)
                ?: throw IllegalArgumentException("DropIn Configuration is null")
        }

        try {
            component = if (isStoredPayment)
                getComponentFor(
                    this,
                    storedPaymentMethod,
                    adyenComponentConfiguration
                )
            else
                getComponentFor(this, paymentMethod, adyenComponentConfiguration)
        } catch (e: CheckoutException) {
            handleError(ComponentError(e))
            return
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        observeState()
        super.onActivityCreated(savedInstanceState)
    }

    private fun observeState() {
        componentDialogViewModel.componentFragmentState.observe(viewLifecycleOwner) {
            Logger.v(TAG, "state: $it")
            setPaymentPendingInitialization(it == ComponentFragmentState.AWAITING_COMPONENT_INITIALIZATION)
            when (it) {
                ComponentFragmentState.INVALID_UI -> highlightValidationErrors()
                ComponentFragmentState.PAYMENT_READY -> {
                    startPayment()
                    componentDialogViewModel.paymentStarted()
                }
                else -> { // do nothing
                }
            }
        }
    }

    override fun onBackPressed(): Boolean {
        Logger.d(TAG, "onBackPressed - $navigatedFromPreselected")
        if (navigatedFromPreselected) {
            protocol.showPreselectedDialog()
        } else {
            protocol.showPaymentMethodsDialog()
        }
        return true
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        Logger.d(TAG, "onCancel")
        protocol.terminateDropIn()
    }

    fun startPayment() {
        val componentState = component.getState()
        try {
            if (componentState != null) {
                if (componentState.isValid) {
                    protocol.requestPaymentsCall(componentState)
                } else {
                    throw CheckoutException("PaymentComponentState are not valid.")
                }
            } else {
                throw CheckoutException("PaymentComponentState are null.")
            }
        } catch (e: CheckoutException) {
            handleError(ComponentError(e))
        }
    }

    protected fun createErrorHandlerObserver(): Observer<ComponentError> {
        return Observer {
            if (it != null) {
                Logger.e(TAG, "ComponentError", it.exception)
                handleError(it)
            }
        }
    }

    fun handleError(componentError: ComponentError) {
        Logger.e(TAG, componentError.errorMessage)
        protocol.showError(getString(com.adyen.checkout.dropin.R.string.component_error), componentError.errorMessage, true)
    }
}

