package development

import query.Operation

class Test {
    String name
    Integer number
    String test

    static remoteMapping = [
            "baseUrl": "http://localhost:8080/development/",
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
            "supportedParams": ["max"],
            "local": ["test","number"],
            "allowed": [ Operation.READ, Operation.CREATE, Operation.UPDATE, Operation.DELETE ],
            "operations":[
                    (Operation.READ):[
                        "endpoint": "remoteTest/show/[:id]",
                        "queryEndpoint": "remoteTest",
                        "method": "GET"
                    ],
                    (Operation.CREATE):[
                            "endpoint": "remoteTest/save",
                            "method": "POST"
                    ],
                    (Operation.UPDATE):[
                            "endpoint": "remoteTest/update/[:id]",
                            "method": "PUT"
                    ],
                    (Operation.DELETE):[
                            "endpoint": "remoteTest/delete/[:id]",
                            "method": "DELETE"
                    ]
            ]
    ]

    static constraints = {
    };
};
