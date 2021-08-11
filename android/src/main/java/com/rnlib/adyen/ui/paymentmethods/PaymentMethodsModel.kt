package com.rnlib.adyen.ui.paymentmethods

import com.adyen.checkout.components.model.paymentmethods.PaymentMethod

class PaymentMethodsModel {
    var storedPaymentMethods: MutableList<PaymentMethod> = mutableListOf()
    var paymentMethods: MutableList<PaymentMethod> = mutableListOf()
}
