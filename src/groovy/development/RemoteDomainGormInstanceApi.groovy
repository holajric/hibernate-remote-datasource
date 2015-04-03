package development

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.codehaus.groovy.grails.orm.hibernate.HibernateGormInstanceApi
import org.codehaus.groovy.grails.orm.hibernate.HibernateDatastore
import org.grails.datastore.gorm.finders.FinderMethod
import parsers.calling.CallingParser
import parsers.config.CachedConfigParser
import query.Operation

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
        def mc = persistentClass.getMetaClass()
        mc."directSave" = { args -> super.save(delegate, args) }
        mc."directDelete" = { args -> super.delete(delegate, args) }
    }

    public D save(D instance) {
        if(CachedConfigParser.isRemote(instance.class)) {
            boolean isNew = (instance?.id == null)
            //println (isNew ? "create" : "update")
            //println "startSync"
            synchronize(isNew ? "create" : "update", instance)
            //println "endSync"
        }
        //println "preparing super save"
        def res = super.save(instance)
        //println "ended super save ${res}"
        return res
    }

    public D save(D instance, boolean validate) {
        if(CachedConfigParser.isRemote(instance.class)) {
            boolean isNew = (instance?.id == null)
            //println (isNew ? "create" : "update")
            //println "startSync"
            synchronize(isNew ? "create" : "update", instance)
            //println "endSync"
        }
        super.save(instance, validate)
    }

    public D save(D instance, java.util.Map params) {
        if(CachedConfigParser.isRemote(instance.class)) {
            boolean isNew = (instance?.id == null)
            //println (isNew ? "create" : "update")
            //println "startSync"
            synchronize(isNew ? "create" : "update", instance)
            //println "endSync"
        }
        super.save(instance, params)
    }

    public void delete(D instance) {
        if(CachedConfigParser.isRemote(instance.class)) {
            //println "delete"
            //println "startSync"
            synchronize("delete", instance)
            //println "endSync"
        }

        super.delete(instance)
    }

    public void delete(D instance, java.util.Map params) {
        if(CachedConfigParser.isRemote(instance.class)) {
            //println "delete"
            //println "startSync"
            synchronize("delete", instance)
            //println "endSync"
        }
        super.delete(instance, params)
    }

    private boolean synchronize(String operation, D instance)    {
        def operationLoc = Operation."${operation.toUpperCase()}"
        def result = SynchronizationManager.withCheckedTransaction(instance, operationLoc)  {
            def queryDescriptor = callingParserService.parseInstanceMethod(operation, instance)
            //println queryDescriptor
            //println "startEx"
            return queryExecutor.executeInstanceQuery(queryDescriptor, instance)
            //println "endEx"
        }
        return result
    }
}
