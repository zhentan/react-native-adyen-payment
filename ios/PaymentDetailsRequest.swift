//
// Copyright (c) 2019 Adyen N.V.
//
// This file is open source and available under the MIT license. See the LICENSE file for more info.
//

import Foundation

import Adyen

internal struct PaymentDetailsRequest: Request {
    
    internal typealias ResponseType = PaymentsResponse
    
    internal let path = "api/v1/adyen/payment_details"
    internal let method = "POST"
    
    internal let details: AdditionalDetails
    
    internal let paymentData: String
    
    internal func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(details.encodable, forKey: .details)
        try container.encode(paymentData, forKey: .paymentData)
    }
    
    private enum CodingKeys: String, CodingKey {
        case details
        case paymentData
    }
}
