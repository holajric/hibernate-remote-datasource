package development

import grails.transaction.Transactional
import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClass
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
        if(CachedConfigParser.isRemote(persistentClass)) {
            println "start: ${JournalLog.findById(1)?.isFinished}"
            println "get"
            SynchronizationManager.withCheckedTransaction(persistentClass.getName(), id, Operation.READ) {
                println "startysynch"
                synchronize("findById", [id])
                println "inside: ${JournalLog.findById(1)?.isFinished}"
                println "endsynch"
                return true
            }
            println "pre-super ${persistentClass} ${Thread.currentThread().getStackTrace()}"
        }

        def res = super.get(id)
        println "end: ${JournalLog.findById(1).isFinished}"
        return res
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
                    println "pre-direct-get"
                    super.get(argumentsForMethod)
                }
                println "pre-direct-get"
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
        def queryDescriptor = callingParser.parseFinder(persistentClass.getName(), methodName, args)
        println queryDescriptor
        println "startExec"
        def res =  QueryExecutor.executeFinderQuery(queryDescriptor)
        println "EndExec"
        return res
    }
}
