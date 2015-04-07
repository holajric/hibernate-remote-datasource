package groovy.org.grails.datastore.remote.hibernate

import org.grails.datastore.gorm.GormStaticApi
import org.springframework.transaction.PlatformTransactionManager
import org.grails.datastore.gorm.GormInstanceApi
import org.codehaus.groovy.grails.orm.hibernate.HibernateGormEnhancer
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.orm.hibernate.HibernateDatastore
import groovy.org.grails.datastore.remote.hibernate.parsers.calling.CallingParser

/**
 * Created by richard on 1.3.15.
 */
class RemoteDomainGormEnhancer extends HibernateGormEnhancer {
    CallingParser callingParserService
    RemoteDomainGormEnhancer(HibernateDatastore datastore, PlatformTransactionManager transactionManager, GrailsApplication grailsApplication, CallingParser callingParserService) {
        super(datastore, transactionManager, grailsApplication)
        this.callingParserService = callingParserService
    }

    @Override
    protected <D> GormStaticApi<D> getStaticApi(Class<D> cls) {
        new RemoteDomainGormStaticApi<D>(cls, (HibernateDatastore)datastore, getFinders(), classLoader, transactionManager, callingParserService)
    }

    @Override
    protected def <D> GormInstanceApi<D> getInstanceApi(Class<D> cls) {
        return new RemoteDomainGormInstanceApi<D>(cls, datastore, classLoader, callingParserService)
    }
}