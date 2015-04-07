package groovy.org.grails.datastore.remote.hibernate.sync

import groovy.org.grails.datastore.remote.hibernate.query.Operation

class JournalLog {
    String entity
    String instanceId
    Operation operation
    Date lastUpdated
    String lastInstanceHash
    String lastRemoteHash
    boolean isFinished = false
    Map<String, String> lastAttrHashes = [:]
    Map<String, String> lastRemoteAttrHashes = [:]


    static constraints = {
        lastInstanceHash nullable: true
        lastRemoteHash nullable: true
        lastUpdated nullable: true
        lastAttrHashes nullable: true
        lastRemoteAttrHashes nullable: true
    }
}
