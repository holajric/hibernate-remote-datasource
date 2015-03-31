package development

import query.Operation
import synchronisation.JournalLog

/**
 * Created by richard on 31.3.15.
 */
class SynchronizationManager {

    public static boolean withCheckedTransaction(String className, id, Operation operation, Closure action) {
        return runCheckedTransaction(className, id, operation, action)
    }

    public static boolean withCheckedTransaction(instance, Operation operation, Closure action) {
        return runCheckedTransaction(instance?.class.name, instance?.id?:0, operation, action, instance)
    }

    public static boolean runCheckedTransaction(String className, id, Operation operation, Closure action, instance = null)   {
        if (JournalLog.countByEntityAndInstanceIdAndIsFinished(className, id, false) > 0) {
            println "locked"
            return false
        }
        return instance ? withTransaction(instance, operation, action) : withTransaction(className, id, operation, action)
    }

    public static boolean withTransaction(String className, id, Operation operation, Closure action) {
            def log = new JournalLog(entity: className, instanceId: id?:0, operation: operation, isFinished: false)
            return runTransaction(log, action)
    }

    public static boolean withTransaction(instance, Operation operation, Closure action) {
            def log = new JournalLog(entity: instance.class.name, instanceId: instance?.id?:0, operation: operation, isFinished: false)
            return runTransaction(log, action, instance)
    }

    private static boolean runTransaction(JournalLog log, action, instance = null) {
        println "locking ${log.instanceId}"
        log.save(failOnError: true)
        println "action start"
        if (!action())
            return false
        println "action over"
        if(instance)
            log.instanceId = instance?.id?:0
        log.isFinished = true
        log.save(failOnError: true, flush: true)
        println "unlocking ${log.instanceId}"
        println "DeepInside: ${JournalLog.get(1)}"
        return true
    }

}
