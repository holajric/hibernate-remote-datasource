package groovy.org.grails.datastore.remote.hibernate.sync

import groovy.org.grails.datastore.remote.hibernate.query.Operation

/**
 * Domain class for logging operations executed on instances and it is used for comparing
 * if data were changed since last calling.
 * It persists data that identifies instance, its state(finished/unfinished) and hashes
 * for monitoring changes.
 */
class JournalLog {
    /**
     * Full name of logged entity
     */
    String entity
    /**
     * Instance id of logged entity
     */
    String instanceId
    /**
     * Operation executed on entity
     */
    Operation operation
    /**
     * Time when entity was last updated with this operation
     */
    Date lastUpdated
    /**
     * Hash of local data before change
     */
    String lastInstanceHash
    /**
     * Hash of latest remote data received for this entity
     */
    String lastRemoteHash
    /**
     * Flag indicating if this operation is finished
     */
    boolean isFinished = false
    /**
     * Hashes of local data attributes before change
     */
    Map<String, String> lastAttrHashes = [:]
    /**
     * Hashes of latest remote data attributes received for this entity
     */
    Map<String, String> lastRemoteAttrHashes = [:]


    static constraints = {
        lastInstanceHash nullable: true
        lastRemoteHash nullable: true
        lastUpdated nullable: true
        lastAttrHashes nullable: true
        lastRemoteAttrHashes nullable: true
    }
}
