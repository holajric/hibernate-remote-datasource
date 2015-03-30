package development

import org.codehaus.groovy.grails.orm.hibernate.HibernateGormInstanceApi
import org.codehaus.groovy.grails.orm.hibernate.HibernateDatastore
import parsers.calling.CallingParser
import parsers.config.CachedConfigParser
import query.Operation
import synchronisation.JournalLog
import org.codehaus.groovy.grails.orm.hibernate.AbstractHibernateGormInstanceApi

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

    public synchronized D save(D instance) {
        if(CachedConfigParser.isRemote(instance.class)) {
            println "save"
            boolean isNew = (instance?.id == null)
            synchronize(isNew ? "create" : "update", instance)
        }
        super.save(instance)
    }

    public D save(D instance, boolean validate) {
        if(CachedConfigParser.isRemote(instance.class)) {
            boolean isNew = (instance?.id == null)
            synchronize(isNew ? "create" : "update", instance)
        }
        super.save(instance, validate)
    }

    public D save(D instance, java.util.Map params) {
        if(CachedConfigParser.isRemote(instance.class)) {
            boolean isNew = (instance?.id == null)
            synchronize(isNew ? "create" : "update", instance)
        }
        super.save(instance, params)
    }

    public void delete(D instance) {
        if(CachedConfigParser.isRemote(instance.class)) {
            println "delete"
            synchronize("delete", instance)
        }
        super.delete(instance)
    }

    public void delete(D instance, java.util.Map params) {
        if(CachedConfigParser.isRemote(instance.class)) {
            println "delete"
            synchronize("delete", instance)
        }
        super.delete(instance, params)
    }

    private boolean synchronize(String operation, D instance)    {
        println JournalLog.list()*.id
        println JournalLog.list()*.operation
        println JournalLog.list()*.isFinished
        if(JournalLog.countByEntityAndInstanceIdAndIsFinished(instance.class.name, instance?.id, false) > 0) {
            //some sync/lock exception/message
            println "locked"
            return
        }
        def operationLoc = Operation."${operation.toUpperCase()}"
        def log = new JournalLog(entity: instance.class.name, instanceId: instance?.id, operation: operationLoc, isFinished: false)
        log.save()
        def queryDescriptor = callingParserService.parseInstanceMethod(operation, instance)
        def result = queryExecutor.executeInstanceQuery(queryDescriptor, instance)
        log.isFinished = true
        log.save()
        return result
    }
}
