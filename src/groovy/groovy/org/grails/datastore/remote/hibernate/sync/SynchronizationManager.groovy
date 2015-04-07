package groovy.org.grails.datastore.remote.hibernate.sync

import groovy.util.logging.Log4j
import groovy.org.grails.datastore.remote.hibernate.query.Operation

/**
 * Created by richard on 31.3.15.
 */
@Log4j
class SynchronizationManager {

    public static boolean withCheckedTransaction(String className, id, Operation operation, Closure action) {
        return runCheckedTransaction(className, id, operation, action)
    }

    public static boolean withCheckedTransaction(instance, Operation operation, Closure action) {
        return runCheckedTransaction(instance?.class.name, instance?.id, operation, action, instance)
    }

    private static boolean runCheckedTransaction(String className, id, Operation operation, Closure action, instance = null)   {
        if (id && JournalLog.countByEntityAndInstanceIdAndIsFinished(className, id, false) > 0) {
            log.info "Transaction for $className with id $id already runs"
            return false
        }
        return instance ? withTransaction(instance, operation, action) : withTransaction(className, id, operation, action)
    }

    public static boolean withTransaction(String className, id, Operation operation, Closure action) {
        if(action == null)  {
            log.error "You can not run transaction without any action to run"
            return false
        }
        JournalLog journalLog = null
        if(id && id!=0)
            journalLog = JournalLog.findByEntityAndInstanceIdAndOperation(className, id, operation)
        if(journalLog == null)
            journalLog = new JournalLog(entity: className, instanceId: id ?: 0, operation: operation)
        journalLog.isFinished = false
        return runTransaction(journalLog, action)
    }

    public static boolean withTransaction(instance, Operation operation, Closure action) {
        if(action == null)  {
            log.error "You can not run transaction without any action to run"
            return false
        }
        JournalLog journalLog = null
        if(instance?.id)
            journalLog = JournalLog.findByEntityAndInstanceIdAndOperation(instance.class.name, instance?.id, operation)
        if(journalLog == null)
            journalLog = new JournalLog(entity: instance.class.name, instanceId: instance?.id ?: 0, operation: operation)
        journalLog.isFinished = false
        return runTransaction(journalLog, action, instance)
    }

    private static boolean runTransaction(JournalLog journalLog, action, instance = null) {
        journalLog.save(failOnError: true)
        if (!action()) {
            log.error "Action wasn't sucessful, transaction ends"
            return false
        }
        journalLog = JournalLog.get(journalLog.id)
        if(instance)
            journalLog.instanceId = instance?.id
        journalLog.isFinished = true
        journalLog.save(flush: true)
        return true
    }

}
