package groovy.org.grails.datastore.remote.hibernate

import groovy.org.grails.datastore.remote.hibernate.query.execution.QueryExecutor
import groovy.util.logging.Log4j
import org.grails.datastore.gorm.finders.FinderMethod
import org.springframework.transaction.PlatformTransactionManager
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.codehaus.groovy.grails.orm.hibernate.HibernateGormStaticApi
import org.codehaus.groovy.grails.orm.hibernate.HibernateDatastore
import groovy.org.grails.datastore.remote.hibernate.parsers.calling.CallingParser
import groovy.org.grails.datastore.remote.hibernate.parsers.config.CachedConfigParser
import groovy.org.grails.datastore.remote.hibernate.query.Operation
import groovy.org.grails.datastore.remote.hibernate.sync.SynchronizationManager

/**
 * Created by richard on 1.3.15.
 */
@Log4j
class RemoteDomainGormStaticApi<D> extends HibernateGormStaticApi<D>{
    CallingParser callingParser
    RemoteDomainGormStaticApi(Class<D> persistentClass, HibernateDatastore datastore, List<FinderMethod> finders, ClassLoader classLoader, PlatformTransactionManager transactionManager, CallingParser callingParser) {
        super(persistentClass, datastore, finders, classLoader, transactionManager)
        this.callingParser = callingParser
    }


    @Override
    public D get(Serializable id)   {
        if(CachedConfigParser.isRemote(persistentClass)) {
            if(!SynchronizationManager.withCheckedTransaction(persistentClass.getName(), id, Operation.READ) {
                synchronize("findById", [id])
                return true
            }) {
                log.info "Transaction wasn't finished succesfully"
                //return null
            }
        }

        return super.get(id)
    }

    @Override
    List<D> getAll() {
        if(CachedConfigParser.isRemote(persistentClass))
            synchronize("findAll", [])
        return super.get(id)
    }

    @Override
    List<D> list(Map params) {
        if(CachedConfigParser.isRemote(persistentClass))
            synchronize("findAll", [])
        super.list(params)
    }

    @Override
    List<D> list() {
        if(CachedConfigParser.isRemote(persistentClass))
            synchronize("findAll", [])
        super.list()
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    def methodMissing(String methodName, Object args) {
        FinderMethod method = gormDynamicFinders.find { FinderMethod f -> f.isMethodMatch(methodName) }

        if (!method) {
            if(methodName == "directGet")   {
                def mc = persistentClass.getMetaClass()
                mc.static."directGet" = { Object[] varArgs ->
                    def argumentsForMethod = varArgs?.length == 1 && varArgs[0].getClass().isArray() ? varArgs[0] : varArgs
                    super.get(argumentsForMethod)
                }
                return super.get(args)
            }
            throw new MissingMethodException(methodName, persistentClass, args)
        }

        def mc = persistentClass.getMetaClass()
        if(CachedConfigParser.isRemote(persistentClass))
            synchronize(methodName, args)

        mc.static."$methodName" = { Object[] varArgs ->
            def argumentsForMethod = varArgs?.length == 1 && varArgs[0].getClass().isArray() ? varArgs[0] : varArgs
            if(CachedConfigParser.isRemote(persistentClass))
                synchronize(methodName, argumentsForMethod)
            method.invoke(delegate, methodName, argumentsForMethod)
        }

        return method.invoke(persistentClass, methodName, args)
	}

    boolean synchronize(methodName, args)   {
        def queryDescriptor
        if((queryDescriptor = callingParser.parseFinder(persistentClass.getName(), methodName, args)) == null)  {
            log.info "Query descriptor for $methodName($args) could not be generated"
            return false
        }
        return QueryExecutor.executeFinderQuery(queryDescriptor)
    }
}
