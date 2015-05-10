package development

import groovy.org.grails.datastore.remote.hibernate.query.Operation

class Test {
    String name
    Integer number
    String test

    static remoteMapping = [
            "baseUrl": "http://localhost:8080/development/",
            /*"sourceType": "Rest",
            "queryType": "Rest",*/
            //"supportedParams": ["max"],
            "local": ["test", "number"],
            //"authetication": "Token",
            /*"authenticationParams": [
                "token": "asdasa4a5sd45qcyx5a4sd5"
            ],*/
            "allowed": [ Operation.READ, Operation.CREATE, Operation.UPDATE, Operation.DELETE ],
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
            "operations":[
                    (Operation.READ):[
                        "endpoint": "remoteTest/show/[:id]",
                        "queryEndpoint": "remoteTest",
                        "hashEndpoint": "remoteTest/show/[:id]?hash=true",
                        "hashQueryEndpoint": "remoteTest?hash=true"
                        //"hashFunction": { data -> data.hashCode().toString()},
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

    public String toString()    {
        "class Test ${this.id} ${this.name} ${this.number} ${this.test}"
    }
};
