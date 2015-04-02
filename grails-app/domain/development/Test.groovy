package development

import query.Operation

class Test {
    String name
    Integer number
    String test

    static remoteMapping = [
            "baseUrl": "http://localhost:8080/development/",
            /*"sourceType": "Rest",
            "queryType": "Rest",*/
            "generalDefault": [
                    "endpoint": "remoteTest/[:operation|lowerCase]/[:id]"
            ],
            "mapping": [
                    "name": "otherName"
            ],
            "queryMapping": [
                    "number BETWEEN": "from=[:lowerBound]&to=[:upperBound]" //from=[:lowerBound|date:'Ymd':5|test:1|asd
            ],
            /*"paramMapping": [
                    "max": "limit"
            ],*/
            //"supportedParams": ["max"],
            "local": ["test","number"],
            //"authetication": "Token",
            /*"authenticationParams": [
                "token": "asdasa4a5sd45qcyx5a4sd5"
            ],*/
            //"allowed": [ Operation.READ, Operation.CREATE, Operation.UPDATE, Operation.DELETE ],
            "operations":[
                    (Operation.READ):[
                        "endpoint": "remoteTest/show/[:id]",
                        "queryEndpoint": "remoteTest",
                        /*"method": "GET",
                        "authenticationParams": [
                                "token": "readToken"
                        ]*/
                    ],
                    (Operation.CREATE):[
                        "endpoint": "remoteTest/save",
                    ]
            ]
    ]

    static constraints = {
        test nullable: true
        number nullable: true
    };
};
