package development

import org.grails.datastore.gorm.finders.FinderMethod
import org.springframework.transaction.PlatformTransactionManager
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.codehaus.groovy.grails.orm.hibernate.HibernateGormStaticApi
import org.codehaus.groovy.grails.orm.hibernate.HibernateDatastore
import parsers.calling.CallingParser
import parsers.config.CachedConfigParser
import query.Operation
import synchronisation.JournalLog

/**
 * Created by richard on 1.3.15.
 */
class RemoteDomainGormStaticApi<D> extends HibernateGormStaticApi<D>{
    CallingParser callingParser
    RemoteDomainGormStaticApi(Class<D> persistentClass, HibernateDatastore datastore, List<FinderMethod> finders, ClassLoader classLoader, PlatformTransactionManager transactionManager, CallingParser callingParserService) {
        super(persistentClass, datastore, finders, classLoader, transactionManager)
        this.callingParser = callingParserService
    }

    @Override
    public D get(Serializable id)   {
        if(JournalLog.findByEntityAndInstanceIdAndIsFinished(persistentClass.getName(), id, false)) {
            //some sync/lock exception/message
            return super.get(id)
        }
        def log = new JournalLog(entity: persistentClass.getName() ,instanceId: id, operation: Operation.READ, isFinished: false).save()
        println synchronize("findById", [id])
        log.isFinished = true
        log.save()
        super.get(id)
    }

    @Override
    List<D> list(Map params) {
        synchronize("findAll", [])
        super.list(params)
    }

    @Override
    List<D> list() {
        synchronize("findAll", [])
        super.list()
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    def methodMissing(String methodName, Object args) {
        FinderMethod method = gormDynamicFinders.find { FinderMethod f -> f.isMethodMatch(methodName) }

        if (!method) {
            throw new MissingMethodException(methodName, persistentClass, args)
        }

        def mc = persistentClass.getMetaClass()
        synchronize(methodName, args)

        mc.static."$methodName" = { Object[] varArgs ->
            def argumentsForMethod = varArgs?.length == 1 && varArgs[0].getClass().isArray() ? varArgs[0] : varArgs
            synchronize(methodName, argumentsForMethod)
            method.invoke(delegate, methodName, argumentsForMethod)
        }

        return method.invoke(persistentClass, methodName, args)
	}

    boolean synchronize(methodName, args)   {
        if(CachedConfigParser.isRemote(persistentClass)) {
            def queryDescriptor = callingParser.parseFinder(persistentClass.getName(), methodName, args)
            return QueryExecutor.executeQuery(queryDescriptor)
        }
        true
    }
}
