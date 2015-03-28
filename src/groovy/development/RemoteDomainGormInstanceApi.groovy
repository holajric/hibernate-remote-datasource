package development

import org.codehaus.groovy.grails.orm.hibernate.HibernateGormInstanceApi
import org.codehaus.groovy.grails.orm.hibernate.HibernateDatastore
import parsers.calling.CallingParser
import parsers.config.CachedConfigParser
import query.Operation
import synchronisation.JournalLog

/**
 * Created by richard on 1.3.15.
 */
class RemoteDomainGormInstanceApi<D> extends HibernateGormInstanceApi<D> {
    CallingParser callingParserService
    QueryExecutor queryExecutor

    RemoteDomainGormInstanceApi(Class<D> persistentClass, HibernateDatastore datastore, ClassLoader classLoader, CallingParser callingParserService) {
        super(persistentClass, datastore, classLoader)
        this.callingParserService = callingParserService
        this.queryExecutor = new QueryExecutor()
    }

    public D save(D instance) {
        println "save"
        boolean isNew = (instance?.id == null)
        D savedInstance = super.save(instance)
        synchronize(isNew ? "save" : "update", instance)
        savedInstance
    }

    public D save(D instance, boolean validate) {
        boolean isNew = (instance?.id == null)
        D savedInstance = super.save(instance, validate)
        synchronize(isNew ? "save" : "update", instance)
        savedInstance
    }

    public D save(D instance, java.util.Map params) {
        boolean isNew = (instance?.id == null)
        D savedInstance = super.save(instance, params)
        synchronize(isNew ? "save" : "update", instance)
        savedInstance
    }

    public void delete(D instance) {
        super.delete(instance)
        synchronize("delete", instance)
    }

    public void delete(D instance, java.util.Map params) {
        super.delete(instance, params)
        synchronize("delete", instance)
    }

    private boolean synchronize(operation, instance)    {
        println "syncing"
        if(CachedConfigParser.isRemote(instance.class)) {
            if(JournalLog.findByEntityAndInstanceIdAndIsFinished(instance.class.name, instance?.id, false)) {
                //some sync/lock exception/message
                println "locked"
                return
            }
            def operationLoc
            switch(operation)   {
                case "delete":
                    operationLoc = Operation.DELETE
                    break
                case "save":
                    operationLoc = Operation.CREATE
                    break
                case "update":
                    operationLoc = Operation.UPDATE
                    break
            }
            println "locking"
            def log = new JournalLog(entity: instance.class.name, instanceId: instance?.id, operation: operationLoc, isFinished: false)
            log.save()
            def queryDescriptor = callingParserService.parseInstanceMethod(operation, instance)
            println queryDescriptor
            def result = queryExecutor.executeInstanceQuery(queryDescriptor, instance)
            log.isFinished = true
            println "unlocking"
            log.save()
            return result
        }
        return true
    }
}
