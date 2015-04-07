package groovy.org.grails.datastore.remote.hibernate

import groovy.org.grails.datastore.remote.hibernate.query.execution.QueryExecutor
import groovy.util.logging.Log4j
import org.codehaus.groovy.grails.orm.hibernate.HibernateGormInstanceApi
import org.codehaus.groovy.grails.orm.hibernate.HibernateDatastore
import groovy.org.grails.datastore.remote.hibernate.parsers.calling.CallingParser
import groovy.org.grails.datastore.remote.hibernate.parsers.config.CachedConfigParser
import groovy.org.grails.datastore.remote.hibernate.query.Operation
import groovy.org.grails.datastore.remote.hibernate.sync.SynchronizationManager

/**
 * Created by richard on 1.3.15.
 */
@Log4j
class RemoteDomainGormInstanceApi<D> extends HibernateGormInstanceApi<D> {
    CallingParser callingParser
    QueryExecutor queryExecutor
    private static final List<String> ALLOWED_OPERATIONS = [
            "create",
            "update",
            "delete"
    ]

    RemoteDomainGormInstanceApi(Class<D> persistentClass, HibernateDatastore datastore, ClassLoader classLoader, CallingParser callingParser) {
        super(persistentClass, datastore, classLoader)
        this.callingParser = callingParser
        this.queryExecutor = new QueryExecutor()
        def mc = persistentClass.getMetaClass()
        mc."directSave" = { args -> super.save(delegate, args) }
        mc."directDelete" = { args -> super.delete(delegate, args) }
    }

    public D save(D instance) {
        if(CachedConfigParser.isRemote(instance.class)) {
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
            synchronize("delete", instance)
        }

        super.delete(instance)
    }

    public void delete(D instance, java.util.Map params) {
        if(CachedConfigParser.isRemote(instance.class)) {
            synchronize("delete", instance)
        }
        super.delete(instance, params)
    }

    private boolean synchronize(String operation, D instance)    {
        if(!ALLOWED_OPERATIONS.contains(operation))  {
            log.warn "Operation $operation is not allowed"
            return false
        }
        def operationLoc = Operation."${operation.toUpperCase()}"
        def result = SynchronizationManager.withCheckedTransaction(instance, operationLoc)  {
            def queryDescriptor
            if((queryDescriptor = callingParser.parseInstanceMethod(operation, instance)) == null)  {
                log.error "Query descriptor for $operation of $instance  could not be generated"
                return false
            }
            return queryExecutor.executeInstanceQuery(queryDescriptor, instance)
        }
        if(!result)  {
            log.error "Transaction wasn't finished succesfully"
            return false
        }
        return result
    }
}
