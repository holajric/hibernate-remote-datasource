package synchronisation

import query.Operation

class JournalLog {
    String entity // or instance hash, but it change by changing data
    String instanceId
    Operation operation
    boolean isFinished

    static constraints = {
    }
}
