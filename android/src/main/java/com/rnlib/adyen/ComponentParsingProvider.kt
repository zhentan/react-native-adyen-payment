/*
 * Copyright (c) 2019 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by caiof on 24/4/2019.
 */

package com.rnlib.adyen

import android.app.Application
import android.content.Context
import androidx.fragment.app.Fragment
import com.adyen.checkout.base.ComponentAvailableCallback
import com.adyen.checkout.base.ComponentView
import com.adyen.checkout.base.PaymentComponent
import com.adyen.checkout.base.PaymentComponentProvider
import com.adyen.checkout.base.PaymentComponentState
import com.adyen.checkout.base.component.BaseConfigurationBuilder
import com.adyen.checkout.base.component.Configuration
import com.adyen.checkout.base.model.paymentmethods.PaymentMethod
import com.adyen.checkout.base.model.payments.request.PaymentMethodDetails
import com.adyen.checkout.base.util.PaymentMethodTypes
import com.adyen.checkout.bcmc.BcmcComponent
import com.adyen.checkout.bcmc.BcmcConfiguration
import com.adyen.checkout.bcmc.BcmcView
import com.adyen.checkout.card.CardConfiguration
import com.adyen.checkout.card.SpinCardView
import com.adyen.checkout.card.CardComponent
import com.adyen.checkout.core.exception.CheckoutException
import com.adyen.checkout.core.log.LogUtil
import com.adyen.checkout.core.log.Logger
import com.adyen.checkout.dotpay.DotpayComponent
import com.adyen.checkout.dotpay.DotpayConfiguration
import com.adyen.checkout.dotpay.DotpayRecyclerView
import com.adyen.checkout.entercash.EntercashComponent
import com.adyen.checkout.entercash.EntercashConfiguration
import com.adyen.checkout.entercash.EntercashRecyclerView
import com.adyen.checkout.eps.EPSComponent
import com.adyen.checkout.eps.EPSConfiguration
import com.adyen.checkout.eps.EPSRecyclerView
import com.adyen.checkout.googlepay.GooglePayComponent
import com.adyen.checkout.googlepay.GooglePayConfiguration
import com.adyen.checkout.ideal.IdealComponent
import com.adyen.checkout.ideal.IdealConfiguration
import com.adyen.checkout.ideal.IdealRecyclerView
import com.adyen.checkout.molpay.MolpayComponent
import com.adyen.checkout.molpay.MolpayConfiguration
import com.adyen.checkout.molpay.MolpayRecyclerView
import com.adyen.checkout.openbanking.OpenBankingComponent
import com.adyen.checkout.openbanking.OpenBankingConfiguration
import com.adyen.checkout.openbanking.OpenBankingRecyclerView
import com.adyen.checkout.sepa.SepaComponent
import com.adyen.checkout.sepa.SepaConfiguration
import com.adyen.checkout.sepa.SepaView
import com.adyen.checkout.wechatpay.WeChatPayComponent
import com.adyen.checkout.wechatpay.WeChatPayConfiguration
import com.adyen.checkout.afterpay.AfterPayComponent
import com.adyen.checkout.afterpay.AfterPayConfiguration
import com.adyen.checkout.afterpay.AfterPayView

class ComponentParsingProvider {
    companion object {
        val TAG = LogUtil.getTag()
    }
}

@Suppress("ComplexMethod")
internal fun <T : Configuration> getDefaultConfigFor(
    @PaymentMethodTypes.SupportedPaymentMethod
    paymentMethod: String,
    context: Context,
    adyenComponentConfiguration: AdyenComponentConfiguration
): T {

    val specificRequirementConfigs = listOf(PaymentMethodTypes.SCHEME, PaymentMethodTypes.GOOGLE_PAY)

    if (specificRequirementConfigs.contains(paymentMethod)) {
        throw CheckoutException("Cannot provide default config for $paymentMethod. Please add it to the DropInConfiguration with required fields.")
    }

    // get default builder for Configuration type
    val builder: BaseConfigurationBuilder<out Configuration> = when (paymentMethod) {
        PaymentMethodTypes.IDEAL -> {
            IdealConfiguration.Builder(context)
        }
        PaymentMethodTypes.MOLPAY_THAILAND,
        PaymentMethodTypes.MOLPAY_MALAYSIA,
        PaymentMethodTypes.MOLPAY_VIETNAM -> {
            MolpayConfiguration.Builder(context)
        }
        PaymentMethodTypes.EPS -> {
            EPSConfiguration.Builder(context)
        }
        PaymentMethodTypes.OPEN_BANKING -> {
            OpenBankingConfiguration.Builder(context)
        }
        PaymentMethodTypes.DOTPAY -> {
            DotpayConfiguration.Builder(context)
        }
        PaymentMethodTypes.ENTERCASH -> {
            EntercashConfiguration.Builder(context)
        }
        PaymentMethodTypes.SEPA -> {
            SepaConfiguration.Builder(context)
        }
        PaymentMethodTypes.WECHAT_PAY_SDK -> {
            WeChatPayConfiguration.Builder(context)
        }
        else -> {
            throw CheckoutException("Unable to find component configuration for type - $paymentMethod")
        }
    }

    builder.setShopperLocale(adyenComponentConfiguration.shopperLocale)
    builder.setEnvironment(adyenComponentConfiguration.environment)

    @Suppress("UNCHECKED_CAST")
    return builder.build() as T
}

internal fun checkComponentAvailability(
    application: Application,
    paymentMethod: PaymentMethod,
    adyenComponentConfiguration: AdyenComponentConfiguration,
    callback: ComponentAvailableCallback<in Configuration>
) {
    try {
        val type = paymentMethod.type ?: throw CheckoutException("PaymentMethod is null")

        val provider = getProviderForType(type)
        val configuration = adyenComponentConfiguration.getConfigurationFor<Configuration>(type, application)

        provider.isAvailable(application, paymentMethod, configuration, callback)
    } catch (e: CheckoutException) {
        Logger.e(ComponentParsingProvider.TAG, "Unable to initiate ${paymentMethod.type}", e)
        callback.onAvailabilityResult(false, paymentMethod, null)
    }
}

@Suppress("ComplexMethod")
internal fun getProviderForType(type: String): PaymentComponentProvider<PaymentComponent<*>, Configuration> {
    @Suppress("UNCHECKED_CAST")
    return when (type) {
        PaymentMethodTypes.IDEAL -> IdealComponent.PROVIDER 
        PaymentMethodTypes.MOLPAY_THAILAND,
        PaymentMethodTypes.MOLPAY_MALAYSIA,
        PaymentMethodTypes.MOLPAY_VIETNAM -> MolpayComponent.PROVIDER
        PaymentMethodTypes.EPS -> EPSComponent.PROVIDER
        PaymentMethodTypes.OPEN_BANKING -> OpenBankingComponent.PROVIDER
        PaymentMethodTypes.DOTPAY -> DotpayComponent.PROVIDER
        PaymentMethodTypes.ENTERCASH -> EntercashComponent.PROVIDER
        PaymentMethodTypes.SCHEME -> CardComponent.PROVIDER
        PaymentMethodTypes.GOOGLE_PAY -> GooglePayComponent.PROVIDER
        PaymentMethodTypes.SEPA -> SepaComponent.PROVIDER
        PaymentMethodTypes.BCMC -> BcmcComponent.PROVIDER
        PaymentMethodTypes.WECHAT_PAY_SDK -> WeChatPayComponent.PROVIDER
        PaymentMethodTypes.AFTER_PAY -> AfterPayComponent.PROVIDER
        else -> {
            throw CheckoutException("Unable to find component for type - $type")
        }
    } as PaymentComponentProvider<PaymentComponent<*>, Configuration>
}

/**
 * Provides a [PaymentComponent] from a [PaymentComponentProvider] using the [PaymentMethod] reference.
 *
 * @param fragment The Activity/Fragment which the PaymentComponent lifecycle will be bound to.
 * @param paymentMethod The payment method to be parsed.
 * @throws CheckoutException In case a component cannot be created.
 */
@Suppress("ComplexMethod", "LongMethod")
internal fun getComponentFor(
    fragment: Fragment,
    paymentMethod: PaymentMethod,
    adyenComponentConfiguration: AdyenComponentConfiguration
): PaymentComponent<PaymentComponentState<in PaymentMethodDetails>> {
    val context = fragment.requireContext()

    val component = when (paymentMethod.type) {
        PaymentMethodTypes.IDEAL -> {
            val idealConfig: IdealConfiguration = adyenComponentConfiguration.getConfigurationFor(PaymentMethodTypes.IDEAL, context)
            IdealComponent.PROVIDER.get(fragment, paymentMethod, idealConfig)
        }
        PaymentMethodTypes.MOLPAY_THAILAND -> {
            val molpayConfig: MolpayConfiguration = adyenComponentConfiguration.getConfigurationFor(PaymentMethodTypes.MOLPAY_THAILAND, context)
            MolpayComponent.PROVIDER.get(fragment, paymentMethod, molpayConfig)
        }
        PaymentMethodTypes.MOLPAY_MALAYSIA -> {
            val molpayConfig: MolpayConfiguration = adyenComponentConfiguration.getConfigurationFor(PaymentMethodTypes.MOLPAY_MALAYSIA, context)
            MolpayComponent.PROVIDER.get(fragment, paymentMethod, molpayConfig)
        }
        PaymentMethodTypes.MOLPAY_VIETNAM -> {
            val molpayConfig: MolpayConfiguration = adyenComponentConfiguration.getConfigurationFor(PaymentMethodTypes.MOLPAY_VIETNAM, context)
            MolpayComponent.PROVIDER.get(fragment, paymentMethod, molpayConfig)
        }
        PaymentMethodTypes.EPS -> {
            val epsConfig: EPSConfiguration = adyenComponentConfiguration.getConfigurationFor(PaymentMethodTypes.EPS, context)
            EPSComponent.PROVIDER.get(fragment, paymentMethod, epsConfig)
        }
        PaymentMethodTypes.OPEN_BANKING -> {
            val openBankingConfig: OpenBankingConfiguration = adyenComponentConfiguration.getConfigurationFor(PaymentMethodTypes.OPEN_BANKING, context)
            OpenBankingComponent.PROVIDER.get(fragment, paymentMethod, openBankingConfig)
        }
        PaymentMethodTypes.DOTPAY -> {
            val dotpayConfig: DotpayConfiguration = adyenComponentConfiguration.getConfigurationFor(PaymentMethodTypes.DOTPAY, context)
            DotpayComponent.PROVIDER.get(fragment, paymentMethod, dotpayConfig)
        }
        PaymentMethodTypes.ENTERCASH -> {
            val entercashConfig: EntercashConfiguration = adyenComponentConfiguration.getConfigurationFor(PaymentMethodTypes.ENTERCASH, context)
            EntercashComponent.PROVIDER.get(fragment, paymentMethod, entercashConfig)
        }
        PaymentMethodTypes.SCHEME -> {
            val cardConfig: CardConfiguration = adyenComponentConfiguration.getConfigurationFor(PaymentMethodTypes.SCHEME, context)
            CardComponent.PROVIDER.get(fragment, paymentMethod, cardConfig)
        }
        PaymentMethodTypes.GOOGLE_PAY -> {
            val googlePayConfiguration: GooglePayConfiguration = adyenComponentConfiguration.getConfigurationFor(PaymentMethodTypes.GOOGLE_PAY, context)
            GooglePayComponent.PROVIDER.get(fragment, paymentMethod, googlePayConfiguration)
        }
        PaymentMethodTypes.SEPA -> {
            val sepaConfiguration: SepaConfiguration = adyenComponentConfiguration.getConfigurationFor(PaymentMethodTypes.SEPA, context)
            SepaComponent.PROVIDER.get(fragment, paymentMethod, sepaConfiguration)
        }
        PaymentMethodTypes.BCMC -> {
            val bcmcConfiguration: BcmcConfiguration = adyenComponentConfiguration.getConfigurationFor(PaymentMethodTypes.BCMC, context)
            BcmcComponent.PROVIDER.get(fragment, paymentMethod, bcmcConfiguration)
        }
        PaymentMethodTypes.WECHAT_PAY_SDK -> {
            val weChatPayConfiguration: WeChatPayConfiguration = adyenComponentConfiguration.getConfigurationFor(PaymentMethodTypes.WECHAT_PAY_SDK, context)
            WeChatPayComponent.PROVIDER.get(fragment, paymentMethod, weChatPayConfiguration)
        }
        PaymentMethodTypes.AFTER_PAY -> {
            val afterPayConfiguration: AfterPayConfiguration = adyenComponentConfiguration.getConfigurationFor(PaymentMethodTypes.AFTER_PAY, context)
            AfterPayComponent.PROVIDER.get(fragment, paymentMethod, afterPayConfiguration)
        }
        else -> {
            throw CheckoutException("Unable to find component for type - ${paymentMethod.type}")
        }
    }
    component.setCreatedForDropIn()
    return component as PaymentComponent<PaymentComponentState<in PaymentMethodDetails>>
}

/**
 * Provides a [ComponentView] to be used in Drop-in using the [PaymentMethod] reference.
 * View type is defined by our UI specifications.
 *MolpayRecyclerView.java
 * @param context The context used to create the View
 * @param paymentMethod The payment method to be parsed.
 */
internal fun getViewFor(
    context: Context,
    paymentMethod: PaymentMethod
): ComponentView<PaymentComponent<PaymentComponentState<in PaymentMethodDetails>>> {
    @Suppress("UNCHECKED_CAST")
    return when (paymentMethod.type) {
        PaymentMethodTypes.IDEAL -> IdealRecyclerView(context)
        PaymentMethodTypes.MOLPAY_THAILAND,
        PaymentMethodTypes.MOLPAY_MALAYSIA,
        PaymentMethodTypes.MOLPAY_VIETNAM -> MolpayRecyclerView(context)
        PaymentMethodTypes.EPS -> EPSRecyclerView(context)
        PaymentMethodTypes.DOTPAY -> DotpayRecyclerView(context)
        PaymentMethodTypes.OPEN_BANKING -> OpenBankingRecyclerView(context)
        PaymentMethodTypes.ENTERCASH -> EntercashRecyclerView(context)
        PaymentMethodTypes.SCHEME -> SpinCardView(context)
        PaymentMethodTypes.SEPA -> SepaView(context)
        PaymentMethodTypes.BCMC -> BcmcView(context)
        PaymentMethodTypes.AFTER_PAY -> AfterPayView(context)
        // GooglePay and WeChatPay do not require a View in Drop-in
        else -> {
            throw CheckoutException("Unable to find view for type - ${paymentMethod.type}")
        }
    } as ComponentView<PaymentComponent<in PaymentComponentState<in PaymentMethodDetails>>>
}
