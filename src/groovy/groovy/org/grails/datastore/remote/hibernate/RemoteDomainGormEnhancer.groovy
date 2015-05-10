package groovy.org.grails.datastore.remote.hibernate

import org.grails.datastore.gorm.GormStaticApi
import org.springframework.transaction.PlatformTransactionManager
import org.grails.datastore.gorm.GormInstanceApi
import org.codehaus.groovy.grails.orm.hibernate.HibernateGormEnhancer
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.orm.hibernate.HibernateDatastore
import groovy.org.grails.datastore.remote.hibernate.parsers.calling.CallingParser

/**
 * Enhancer is important part of GORM API, this class is used for setting
 * which APIs should be used. Its most important functionality is providing
 * appropriate APIs instances.
 */
class RemoteDomainGormEnhancer extends HibernateGormEnhancer {
    /** Calling parser that is used for parsing method callings **/
    CallingParser callingParserService

    /**
     * Constructor initialises instance variables and set them to proper values.
     * @param datastore datastore that is linked with enhanced class
     * @param transactionManager application transaction manager
     * @param grailsApplication application context
     * @param callingParserService calling parser that is used for parsing method callings
     */
    RemoteDomainGormEnhancer(HibernateDatastore datastore, PlatformTransactionManager transactionManager, GrailsApplication grailsApplication, CallingParser callingParserService) {
        super(datastore, transactionManager, grailsApplication)
        this.callingParserService = callingParserService
    }

    /**
     * This method only returns instance of proper static API - RemoteDomainGormStaticApi
     * @param cls class that is enhanced
     * @return instance of RemoteDomainGormStaticApi
     */
    @Override
    protected <D> GormStaticApi<D> getStaticApi(Class<D> cls) {
        new RemoteDomainGormStaticApi<D>(cls, (HibernateDatastore)datastore, getFinders(), classLoader, transactionManager, callingParserService)
    }

    /**
     * This method only returns instance of proper instance API - RemoteDomainGormInstanceApi
     * @param cls class that is enhanced
     * @return instance of RemoteDomainGormInstanceApi
     */
    @Override
    protected def <D> GormInstanceApi<D> getInstanceApi(Class<D> cls) {
        return new RemoteDomainGormInstanceApi<D>(cls, datastore, classLoader, callingParserService)
    }
}