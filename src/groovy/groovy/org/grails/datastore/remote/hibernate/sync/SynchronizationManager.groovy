package groovy.org.grails.datastore.remote.hibernate.sync

import groovy.util.logging.Log4j
import groovy.org.grails.datastore.remote.hibernate.query.Operation

/**
 * This class provides methods for transaction management of synchronisation
 * using JournalLog domains.
 */
@Log4j
class SynchronizationManager {

    /**
     * Alias for runCheckedTransaction in case you don't have instance
     * @param className name of domain class you run transaction for
     * @param id id of instance or id that created instance should have
     * @param operation operation you want to run
     * @param action action you want to execute
     * @return success or failure of transaction
     */
    public static boolean withCheckedTransaction(String className, id, Operation operation, Closure action) {
        return runCheckedTransaction(className, id, operation, action)
    }

    /**
     * Alias for runCheckedTransaction in case you have instance
     * @param instance instance you run transaction for
     * @param operation operation you want to run
     * @param action action you want to execute
     * @return success or failure of transaction
     */
    public static boolean withCheckedTransaction(instance, Operation operation, Closure action) {
        return runCheckedTransaction(instance?.class.name, instance?.id, operation, action, instance)
    }

    /**
     * Checks if transaction for domain class with given className and for id doesn't
     * already exist(if it does it returns false) and then delegates control to withTransaction
     * @param className name of domain class you run transaction for
     * @param id id of instance or id that created instance should have
     * @param operation operation you want to run
     * @param action action you want to execute
     * @param instance instance you run transaction for (nullable)
     * @return success or failure of transaction
     */
    private static boolean runCheckedTransaction(String className, id, Operation operation, Closure action, instance = null)   {
        if (id && JournalLog.countByEntityAndInstanceIdAndIsFinished(className, id, false) > 0) {
            log.info "Transaction for $className with id $id already runs"
            return false
        }
        return instance ? withTransaction(instance, operation, action) : withTransaction(className, id, operation, action)
    }

    /**
     * Creates or updates existing journalLog, marks it as unfinished and then delegates control to runTransaction.
     * Version if you don't have instance.
     * @param className name of domain class you run transaction for
     * @param id id of instance or id that created instance should have
     * @param operation operation you want to run
     * @param action action you want to execute
     * @return success or failure of transaction
     */
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

    /**
     * Creates or updates existing journalLog, marks it as unfinished and then delegates control to runTransaction.
     * Version if you have instance.
     * @param instance instance you run transaction for
     * @param operation operation you want to run
     * @param action action you want to execute
     * @return success or failure of transaction
     */
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

    /**
     * Executes given action and then marks journalLog as finished.
     * @param journalLog journalLog for transaction
     * @param action action to be executed
     * @param instance instance for transaction (nullable)
     * @return success or failure
     */
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
