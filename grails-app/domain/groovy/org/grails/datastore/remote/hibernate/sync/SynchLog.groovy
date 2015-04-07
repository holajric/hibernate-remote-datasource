package groovy.org.grails.datastore.remote.hibernate.sync

class SynchLog {
    String query
    String lastResponseHash
    boolean isFinished

    static constraints = {
        lastResponseHash nullable: true
    }
}
