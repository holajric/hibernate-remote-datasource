package development

import groovy.util.logging.Log4j
import query.Operation
import synchronisation.JournalLog

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
            log.info "You can not run transaction without any action to run"
            return false
        }
        def journalLog = new JournalLog(entity: className, instanceId: id ?: 0, operation: operation, isFinished: false)
        return runTransaction(journalLog, action)
    }

    public static boolean withTransaction(instance, Operation operation, Closure action) {
        if(action == null)  {
            log.info "You can not run transaction without any action to run"
            return false
        }
        def journalLog = new JournalLog(entity: instance.class.name, instanceId: instance?.id ?: 0, operation: operation, isFinished: false)
        return runTransaction(journalLog, action, instance)
    }

    private static boolean runTransaction(JournalLog journalLog, action, instance = null) {
        journalLog.save()
        if (!action()) {
            log.info "Action wasn't sucessful, transaction ends"
            return false
        }
        if(instance)
            journalLog.instanceId = instance?.id
        journalLog.isFinished = true
        journalLog.save(flush: true)
        return true
    }

}
