package development

import org.codehaus.groovy.grails.orm.hibernate.HibernateGormInstanceApi
import org.codehaus.groovy.grails.orm.hibernate.HibernateDatastore
import parsers.calling.CallingParser
import parsers.config.CachedConfigParser

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
        if(CachedConfigParser.isRemote(instance.class)) {
            def queryDescriptor = callingParserService.parseInstanceMethod(instance.id ? "update" : "save", instance)
            queryExecutor.executeQuery(queryDescriptor, instance)
        }
        super.save(instance)
    }

    public D save(D instance, boolean validate) {
        if(CachedConfigParser.isRemote(instance.class)) {
            def queryDescriptor = callingParserService.parseInstanceMethod(instance.id ? "update" : "save", instance)
            queryExecutor.executeQuery(queryDescriptor, instance)
        }
        super.save(instance,validate)
    }

    public D save(D instance, java.util.Map params) {
        if(CachedConfigParser.isRemote(instance.class)) {
            def queryDescriptor = callingParserService.parseInstanceMethod(instance.id ? "update" : "save", instance)
            queryExecutor.executeQuery(queryDescriptor, instance)
        }
        super.save(instance,params)
    }

    public void delete(D instance) {
        if(CachedConfigParser.isRemote(instance.class)) {
            def queryDescriptor = callingParserService.parseInstanceMethod("delete", instance)
            queryExecutor.executeQuery(queryDescriptor, instance)
        }

        super.delete(instance)
    }

    public void delete(D instance, java.util.Map params) {
        if(CachedConfigParser.isRemote(instance.class)) {
            def queryDescriptor = callingParserService.parseInstanceMethod("delete", instance)
            queryExecutor.executeQuery(queryDescriptor, instance)
        }
        super.delete(instance,params)
    }
}
