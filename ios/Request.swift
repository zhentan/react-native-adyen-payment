//
// Copyright (c) 2019 Adyen N.V.
//
// This file is open source and available under the MIT license. See the LICENSE file for more info.
//

import Foundation

internal protocol Request: Encodable {
    
    associatedtype ResponseType: Response
    
    var path: String { get }
    var method: String { get }
    
}

internal protocol Response: Decodable {}
