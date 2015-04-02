grails {
    plugins {
        hibernateRemoteDatasource {
            defaults {
                sourceType = "Rest"
                queryType = "Rest"
                generalDefault  {
                    endpoint = "[:entityName|capitalize:false]/[:operation|lowerCase]/[:id]"
                }
                operations  {
                    READ {
                        method = "GET"
                        endpoint = "[:entityName|capitalize:false]/show/[:id]"
                        queryEndpoint = "[:entityName|capitalize:false]"
                    }
                    CREATE {
                        endpoint = "[:entityName|capitalize:false]/save"
                        method = "POST"
                    }
                    UPDATE {
                        method = "PUT"
                    }
                    DELETE {
                        method = "DELETE"
                    }
                }
            }
        }
    }
}