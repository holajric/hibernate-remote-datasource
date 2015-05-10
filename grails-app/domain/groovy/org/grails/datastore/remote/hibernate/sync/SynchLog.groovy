package groovy.org.grails.datastore.remote.hibernate.sync
/**
 * Domain class for logging executed queries used for comparing if data were changed since last calling.
 * It persists query itself, its state(finished/unfinished) and hash of last response for thie query.
 **/
class SynchLog {
    /**
     * Query descriptor in string form
     */
    String query
    /**
     * Hash of last response received for this query
     */
    String lastResponseHash
    /**
     * Flag indicating if this query is finished
     */
    boolean isFinished

    static constraints = {
        lastResponseHash nullable: true
    }
}
