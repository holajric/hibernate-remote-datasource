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
 * This class is part of GORM API implementation. Instance API implements
 * instance methods, thus mostly modifying operations like save, delete
 * (these are reimplemented in this plugin) or transaction managing methods.
 * Unlike original implementation, which this implementation
 * extends, this contains synchronisation with remote data source.
 */
@Log4j
class RemoteDomainGormInstanceApi<D> extends HibernateGormInstanceApi<D> {
    /** Calling parser that is used for parsing method callings **/
    CallingParser callingParser

    /** List of allowed operation types **/
    private static final List<String> ALLOWED_OPERATIONS = [
            "create",
            "update",
            "delete"
    ]


    /**
     * Constructor initialises instance variables and set them to proper values,
     * it also creates two dynamic methods directSave and directGet, that represents
     * original get and save method from parent class.
     * @param persistentClass class enhanced by API
     * @param datastore datastore that is linked with enhanced class
     * @param classLoader class used for dynamic loading of classes
     * @param callingParser calling parser that is used for parsing method callings
     */
    RemoteDomainGormInstanceApi(Class<D> persistentClass, HibernateDatastore datastore, ClassLoader classLoader, CallingParser callingParser) {
        super(persistentClass, datastore, classLoader)
        this.callingParser = callingParser
        def mc = persistentClass.getMetaClass()
        mc."directSave" = { args -> super.save(delegate, args) }
        mc."directDelete" = { args -> super.delete(delegate, args) }
    }

    /**
     * Modified save method - saves instance, but in case domain is
     * remote, synchronization is executed before saving.
     * @param instance instance to be saved
     * @return saved instance
     */
    public D save(D instance) {
        if(CachedConfigParser.isRemote(instance.class)) {
            boolean isNew = (instance?.id == null)
            synchronize(isNew ? "create" : "update", instance)
        }
        super.save(instance)
    }

    /**
     * Modified save method - saves instance, but in case domain is
     * remote, synchronization is executed before saving.
     * @param instance instance to be saved
     * @param validate should be instance validated before saving?
     * @return saved instance
     */
    public D save(D instance, boolean validate) {
        if(CachedConfigParser.isRemote(instance.class)) {
            boolean isNew = (instance?.id == null)
            synchronize(isNew ? "create" : "update", instance)
        }
        super.save(instance, validate)
    }

    /**
     * Modified save method - saves instance, but in case domain is
     * remote, synchronization is executed before saving.
     * @param instance instance to be saved
     * @param params parameters of operation (flush, ...)
     * @return saved instance
     */
    public D save(D instance, java.util.Map params) {
        if(CachedConfigParser.isRemote(instance.class)) {
            boolean isNew = (instance?.id == null)
            synchronize(isNew ? "create" : "update", instance)
        }
        super.save(instance, params)
    }

    /**
     * Modified delete method - deletes instance, but in case domain is
     * remote, synchronization is executed before deletion.
     * @param instance instance to be deleted
     */
    public void delete(D instance) {
        if(CachedConfigParser.isRemote(instance.class)) {
            synchronize("delete", instance)
        }

        super.delete(instance)
    }

    /**
     * Modified delete method - deletes instance, but in case domain is
     * remote, synchronization is executed before deletion.
     * @param instance instance to be deleted
     * @param params parameters of operation (flush, ...)
     */
    public void delete(D instance, java.util.Map params) {
        if(CachedConfigParser.isRemote(instance.class)) {
            synchronize("delete", instance)
        }
        super.delete(instance, params)
    }

    /**
     * This method starts the synchronization process, it parses method calling and
     * then gives control to QueryExecutor.
     * @param operation operation executed
     * @param instance instance to be synchronized
     * @return success or failure
     */
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
            return QueryExecutor.executeInstanceQuery(queryDescriptor, instance)
        }
        if(!result)  {
            log.error "Transaction wasn't finished succesfully"
            return false
        }
        return result
    }
}
