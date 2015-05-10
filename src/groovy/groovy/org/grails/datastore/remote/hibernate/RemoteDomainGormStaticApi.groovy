package groovy.org.grails.datastore.remote.hibernate

import groovy.org.grails.datastore.remote.hibernate.query.execution.QueryExecutor
import groovy.util.logging.Log4j
import org.apache.commons.lang.StringUtils
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

import java.beans.Introspector

/**
 * This class is part of GORM API implementation. Static API implements
 * static methods, thus mostly selection queries like get, list, getAll,
 * dynamic finders (these are reimplemented in this plugin) or criteria
 * queries and others. Unlike original implementation, which this implementation
 * extends, this contains synchronisation with remote data source.
 */
@Log4j
class RemoteDomainGormStaticApi<D> extends HibernateGormStaticApi<D>{
    /** Calling parser that is used for parsing method callings **/
    CallingParser callingParser

    /**
     * Constructor initialises instance variables and set them to proper values.
     * @param persistentClass class enhanced by API
     * @param datastore datastore that is linked with enhanced class
     * @param finders list of proper dynamic finder definitions
     * @param classLoader class used for dynamic loading of classes
     * @param transactionManager application transaction manager
     * @param callingParser calling parser that is used for parsing method callings
     */
    RemoteDomainGormStaticApi(Class<D> persistentClass, HibernateDatastore datastore, List<FinderMethod> finders, ClassLoader classLoader, PlatformTransactionManager transactionManager, CallingParser callingParser) {
        super(persistentClass, datastore, finders, classLoader, transactionManager)
        this.callingParser = callingParser
    }

    /**
     * Modified get method - gets instance by id, but in case domain is
     * remote, synchronization is executed before getting instance.
     * @param id id of instance that should be retrieved
     * @return instance with given id or null if not found
     */
    @Override
    public D get(Serializable id)   {
        if(CachedConfigParser.isRemote(persistentClass)) {
            if(!SynchronizationManager.withCheckedTransaction(persistentClass.getName(), id, Operation.READ) {
                synchronize("findById", [id])
                return true
            }) {
                log.error "Transaction wasn't finished succesfully"
                //return null
            }
        }

        return super.get(id)
    }

    /**
     * Modified getAll method - gets all instances of domain class,
     * but in case domain is remote, synchronization is executed
     * before getting instances.
     * @return list of instances
     */
    @Override
    List<D> getAll() {
        if(CachedConfigParser.isRemote(persistentClass))
            synchronize("findAll", [])
        return super.getAll()
    }

    /**
     * Modified list method - gets all instances of domain class,
     * but in case domain is remote, synchronization is executed
     * before getting instances.
     * @param params parameters for method (sort, order, max, ...)
     * @return list of instances
     */
    @Override
    List<D> list(Map params) {
        if(CachedConfigParser.isRemote(persistentClass))
            synchronize("findAll", [params])
        super.list(params)
    }

    /**
     * Modified list method - gets all instances of domain class,
     * but in case domain is remote, synchronization is executed
     * before getting instances.
     * @return list of instances
     */
    @Override
    List<D> list() {
        if(CachedConfigParser.isRemote(persistentClass))
            synchronize("findAll", [])
        super.list()
    }

    /**
     * Modified methodMissing method, it procceses and dynamicaly executes dynamic finders,
     * when they are called for the first time, but in case domain is remote,
     * synchronization is executed before executing finder.
     * @param methodName name of method, that couldn't be found - finder
     * @param args arguments of method, that couldn't be found
     * @return result of dynamic called method
     */
    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    def methodMissing(String methodName, Object args) {
        String normalizedMethodName = methodName
        Integer argToSkip = -1
        String splitter
        String containsAttribute = ""
        if (CachedConfigParser.isRemote(persistentClass)) {
            if (methodName.contains("Contains")) {
                def (operation, query) = methodName.split(/By/)
                def splitted
                boolean first = true
                normalizedMethodName = operation
                if (query && query.contains("And")) {
                    splitted = query.split("And")
                    splitter = "And"
                } else if (query && query.contains("Or")) {
                    splitted = query.split("Or")
                    splitter = "Or"
                }   else    {
                    splitted = query ? [query] : []
                }
                int counter = 0
                splitted.each { it ->
                    if (!it.contains("Contains")) {
                        if(it.contains("Between"))
                            counter += 2
                        else if(!it.contains("IsNotNull") && !it.contains("IsNull"))  {
                            counter++
                        }
                        if (first) {
                            normalizedMethodName += "By"
                            first = false
                        } else {
                            normalizedMethodName += splitter
                        }
                        normalizedMethodName += it
                    } else {
                        argToSkip = counter
                        containsAttribute = Introspector.decapitalize(it.replace("Contains", ""))
                    }
                }
            }
        }
        FinderMethod method = gormDynamicFinders.find { FinderMethod f -> f.isMethodMatch(normalizedMethodName) }

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

        def normalizedArgs = args.clone()
        if(argToSkip != -1) {
            normalizedArgs = normalizedArgs.toList()
            normalizedArgs.remove(argToSkip)
            normalizedArgs = normalizedArgs as Object[]
        }
        mc.static."$methodName" = { Object[] varArgs ->
            def argumentsForMethod = varArgs?.length == 1 && varArgs[0].getClass().isArray() ? varArgs[0] : varArgs
            def normalizedArgumentsForMethod = argumentsForMethod.clone()
            if(argToSkip != -1) {
                normalizedArgumentsForMethod = normalizedArgumentsForMethod.toList()
                normalizedArgumentsForMethod.remove(argToSkip)
                normalizedArgumentsForMethod = normalizedArgumentsForMethod as Object[]
            }
            if(CachedConfigParser.isRemote(persistentClass))
                synchronize(methodName, argumentsForMethod)
            def result
            if(normalizedMethodName == "findAll") {
                if(normalizedArgumentsForMethod.size() > 0)
                    result = super.list(normalizedArgumentsForMethod[0])
                else
                    result = super.list()
            }
            else
                result  = method.invoke(persistentClass, normalizedMethodName, normalizedArgumentsForMethod)
            if(!containsAttribute.empty)    {
                result = result.findAll {
                    contains(it?."$containsAttribute", argumentsForMethod[argToSkip])
                }
            }
            return result
        }
        def result
        if(normalizedMethodName == "findAll") {
            if(normalizedArgs.size() > 0)
                result = super.list(normalizedArgs[0])
            else
                result = super.list()
        }
        else
            result  = method.invoke(persistentClass, normalizedMethodName, normalizedArgs)
        if(!containsAttribute.empty)    {
            result = result.findAll {
                contains(it?."$containsAttribute", args[argToSkip])
            }
        }

        return result
	}

    /**
     * This method starts the synchronization process, it parses method calling and
     * then gives control to QueryExecutor.
     * @param methodName name of called method
     * @param args args of called method
     * @return sucess or failure
     */
    boolean synchronize(methodName, args)   {
        def queryDescriptor
        if((queryDescriptor = callingParser.parseFinder(persistentClass.getName(), methodName, args)) == null)  {
            log.error "Query descriptor for $methodName($args) could not be generated"
            return false
        }
        return QueryExecutor.executeFinderQuery(queryDescriptor)
    }

    /**
     * Helper method checking if attribute contains value
     * @param attribute
     * @param value
     * @return logical result of operation
     */
    boolean contains(attribute, value)  {
        boolean isCollection = attribute instanceof Collection
        boolean isList = attribute instanceof List
        boolean isSet = attribute instanceof Set
        boolean isArray = attribute != null && attribute.getClass().isArray()
        boolean isMap = attribute instanceof Map
        if(isCollection || isList || isSet || isArray)
            return attribute.contains(value)
        if(isMap)
            return attribute.containsValue(value)
        return attribute == value
    }
}
