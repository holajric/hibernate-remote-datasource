package development

import query.Operation
import query.Operator

class Test {
    String name

    static remoteMapping = [
            "baseUrl": "http://demo5451883.mockable.io/",
            "sourceType": "Rest",
            "queryType": "Rest",
            "mapping": [
                    "name": "otherName"
            ],
            "queryMapping": [
                    "date BETWEEN": "from=[:lowerBound]&to=[:upperBound]"
            ],
            "allowed": [ Operation.READ ],
            "operations":[
                    (Operation.READ):[
                        "endpoint": "remoteTest/show/[:id]",
                        "queryEndpoint": "remoteTest",
                        "method": "GET"
                    ]
            ]
    ]

    static constraints = {
    };
};
