package development

import query.Operation

class Test {
    String name
    Integer number
    String test

    static remoteMapping = [
            "baseUrl": "http://demo5451883.mockable.io/",
            "sourceType": "Rest",
            "queryType": "Rest",
            "mapping": [
                    "name": "otherName"
            ],
            "queryMapping": [
                    "number BETWEEN": "from=:lowerBound&to=:upperBound"
            ],
            "paramMapping": [
                    "max": "limit"
            ],
            "local": ["number"],
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
