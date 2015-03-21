package development

import query.Operation

class Test {
    String name

    static remoteMapping = [
            "baseUrl": "http://demo5451883.mockable.io/",
            "sourceType": "Rest",
            "queryType": "Rest",
            "mapping": [
                    "name": "otherName" //shorthand syntax other possibility is full config map or can also skip in case it is same
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
