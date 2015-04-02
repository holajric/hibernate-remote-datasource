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
                    "number BETWEEN": "from=[:lowerBound|date:'Ymd':5|test:1|asd]&to=[:upperBound]"
            ],
            /*"paramMapping": [
                    "max": "limit"
            ],*/
            "supportedParams": ["max"],
            "local": ["test","number"],
            //"authetication": "Token",
            "authenticationParams": [
                "token": "asdasa4a5sd45qcyx5a4sd5"
            ],
            "allowed": [ Operation.READ, Operation.CREATE, Operation.UPDATE, Operation.DELETE ],
            "operations":[
                    (Operation.READ):[
                        "endpoint": "remoteTest/show/[:id]",
                        "queryEndpoint": "remoteTest",
                        "method": "GET",
                        "authenticationParams": [
                                "token": "readToken"
                        ]
                    ],
                    (Operation.CREATE):[
                            "endpoint": "remoteTest/save",
                            "method": "POST",
                            "authenticationParams": [
                                    "token": "createToken"
                            ]
                    ],
                    (Operation.UPDATE):[
                            "endpoint": "remoteTest/update/[:id]",
                            "method": "PUT",
                            "authenticationParams": [
                                    "token": "updateToken"
                            ]
                    ],
                    (Operation.DELETE):[
                            "endpoint": "remoteTest/delete/[:id]",
                            "method": "DELETE",
                            "authenticationParams": [
                                    "token": "deleteToken"
                            ]
                    ]
            ]
    ]

    static constraints = {
        test nullable: true
        number nullable: true
    };
};
