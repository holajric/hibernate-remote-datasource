package development

import org.grails.datastore.gorm.finders.FinderMethod
import org.springframework.transaction.PlatformTransactionManager
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.codehaus.groovy.grails.orm.hibernate.HibernateGormStaticApi
import org.codehaus.groovy.grails.orm.hibernate.HibernateDatastore
import parsers.calling.CallingParser
import parsers.config.CachedConfigParser
import query.QueryDescriptor

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
            def queryDescriptor = callingParser.parseFinder(persistentClass.getName(), "findById", [id])
            QueryExecutor.executeQuery(queryDescriptor)
        }
        super.get(id)
    }

    @Override
    List<D> findAll(example, Map args) {
        if(CachedConfigParser.isRemote(persistentClass)) {
            def queryDescriptor = callingParser.parseFinder(persistentClass.getName(), "findAll", [])
            QueryExecutor.executeQuery(queryDescriptor)
        }
        super.findAll(example, args)
    }

    @Override
    List<D> list(Map params) {
        if(CachedConfigParser.isRemote(persistentClass)) {
            def queryDescriptor = callingParser.parseFinder(persistentClass.getName(), "findAll", [])
            QueryExecutor.executeQuery(queryDescriptor)
        }
        super.list(params)
    }

    @Override
    List<D> list() {
        if(CachedConfigParser.isRemote(persistentClass)) {
            def queryDescriptor = callingParser.parseFinder(persistentClass.getName(), "findAll", [])
            QueryExecutor.executeQuery(queryDescriptor)
        }
        super.list()
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    def methodMissing(String methodName, Object args) {
        println "TU"
        FinderMethod method = gormDynamicFinders.find { FinderMethod f -> f.isMethodMatch(methodName) }
        if (!method) {
            throw new MissingMethodException(methodName, persistentClass, args)
        }

        def mc = persistentClass.getMetaClass()
        if(CachedConfigParser.isRemote(persistentClass)) {
            def queryDescriptorFirst = callingParser.parseFinder(persistentClass.getName(), methodName, args)
            QueryExecutor.executeQuery(queryDescriptorFirst)
        }
        mc.static."$methodName" = { Object[] varArgs ->
            def argumentsForMethod = varArgs?.length == 1 && varArgs[0].getClass().isArray() ? varArgs[0] : varArgs
            if(CachedConfigParser.isRemote(persistentClass)) {
                def queryDescriptor = callingParser.parseFinder(persistentClass.getName(), methodName, argumentsForMethod)
                QueryExecutor.executeQuery(queryDescriptor)
            }
            method.invoke(delegate, methodName, argumentsForMethod)
        }

        return method.invoke(persistentClass, methodName, args)
	}
}
