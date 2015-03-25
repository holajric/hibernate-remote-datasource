package synchronisation

import query.Operation

class JournalLog {
    String instanceHash // or id + entity
    Operation operation
    boolean isFinished

    static constraints = {
    }
}
