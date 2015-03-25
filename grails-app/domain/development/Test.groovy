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
            "local": ["test","number"],
            "allowed": [ Operation.READ, Operation.CREATE ],
            "operations":[
                    (Operation.READ):[
                        "endpoint": "remoteTest/show/[:id]",
                        "queryEndpoint": "remoteTest",
                        "method": "GET"
                    ],
                    (Operation.CREATE):[
                            "endpoint": "remoteTest/save",
                            "method": "POST"
                    ]
            ]
    ]

    static constraints = {
    };
};
