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
        println JournalLog.list()*.instanceId
        println JournalLog.list()*.operation
        println JournalLog.list()*.isFinished
        try {
            if (JournalLog.countByEntityAndInstanceIdAndIsFinished(className, id, false) > 0) {
                //some sync/lock exception/message
                return false
            }
        } catch(ex) {
            //print ex
            return false
        }
        return instance ? withTransaction(instance, operation, action) : withTransaction(className, id, operation, action)
    }

    public static boolean withTransaction(String className, id, Operation operation, Closure action) {
        try {
            def log = new JournalLog(entity: className, instanceId: id?:0, operation: operation, isFinished: false)
            return runTransaction(log, action)
        }   catch(ex)    {
            //print ex
            return false
        }
    }

    public static boolean withTransaction(instance, Operation operation, Closure action) {
        try {
            def log = new JournalLog(entity: instance.class.name, instanceId: instance?.id?:0, operation: operation, isFinished: false)
            return runTransaction(log, action, instance)
        }   catch(ex)    {
            //print ex
            return false
        }
    }

    private static boolean runTransaction(JournalLog log, action, instance = null) {
        try {
            log.save(failOnError: true)
            if (!action())
                return false
            if(instance)
                log.instanceId = instance?.id?:0
            log.isFinished = true
            log.save(failOnError: true)
        }   catch(ex)    {
            //print ex
            return false
        }
        return true
    }

}
