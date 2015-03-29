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
        boolean isNew = (instance?.id == null)
        synchronize(isNew ? "create" : "update", instance)
        super.save(instance)
    }

    public D save(D instance, boolean validate) {
        boolean isNew = (instance?.id == null)
        synchronize(isNew ? "create" : "update", instance)
        super.save(instance, validate)
    }

    public D save(D instance, java.util.Map params) {
        boolean isNew = (instance?.id == null)
        synchronize(isNew ? "create" : "update", instance)
        super.save(instance, params)
    }

    public void delete(D instance) {
        synchronize("delete", instance)
        super.delete(instance)
    }

    public void delete(D instance, java.util.Map params) {
        synchronize("delete", instance)
        super.delete(instance, params)
    }

    private boolean synchronize(String operation, D instance)    {
        println JournalLog.list()*.isFinished
        if(CachedConfigParser.isRemote(instance.class)) {
            if(JournalLog.findByEntityAndInstanceIdAndIsFinished(instance.class.name, instance?.id, false)) {
                //some sync/lock exception/message
                println "locked"
                return
            }
            def operationLoc = Operation."${operation.toUpperCase()}"
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
