import groovy.org.grails.datastore.remote.hibernate.RemoteDomainGormEnhancer
import grails.util.Environment
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.grails.datastore.mapping.transactions.DatastoreTransactionManager
import org.codehaus.groovy.grails.orm.hibernate.HibernateDatastore
import groovy.org.grails.datastore.remote.hibernate.parsers.calling.GormApiParser

/**
 * Plugin descriptor class, it is necesary for all Grails plugins,
 * it contains basic descriptions and special methods that have access
 * to specific parts of application such as application context, spring beans
 * or it can catch application events.
 */
class HibernateRemoteDatasourceGrailsPlugin {
    // the plugin version
    def version = "0.1"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "2.4 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp",
        "grails-app/domain/development/**",
        "grails-app/controllers/development/**"
    ]

    // TODO Fill in these fields
    def title = "Hibernate Remote Datasource" // Headline display name of the plugin
    def author = "Richard Holaj"
    def authorEmail = "holajr22@gmail.com"
    def description = '''\
This plugin allows developer to automatically synchronize domain classes with remote datasources.
'''

    // URL to the plugin's documentation
    def documentation = "https://github.com/holajric/hibernate-remote-datasource"

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
    def license = "MIT"

    // Details of company behind the plugin (if there is one)
//    def organization = [ name: "My Company", url: "http://www.my-company.com/" ]

    // Any additional developers beyond the author specified above.
//    def developers = [ [ name: "Joe Bloggs", email: "joe@bloggs.net" ]]

    // Location of the plugin's issue tracker.
//    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPMYPLUGIN" ]

    // Online location of the plugin's browseable source code.
//    def scm = [ url: "http://svn.codehaus.org/grails-plugins/" ]
    def loadAfter = ['core', 'dataSource']

    def doWithWebDescriptor = { xml ->

    }

    def doWithSpring = {
        mergeConfig(application)
    }

    /** Merges default configuration from plugin with application configuration.
     *  @param app application context
     */
    private void mergeConfig(GrailsApplication app) {
        ConfigObject currentConfig = app.config
        ConfigSlurper slurper = new ConfigSlurper(Environment.getCurrent().getName());
        ConfigObject secondaryConfig = slurper.parse(app.classLoader.loadClass("HibernateRemoteDatasourceDefaultConfig"))
        ConfigObject config = new ConfigObject();
        config.putAll(secondaryConfig.merge(currentConfig))
        app.config = config;
    }

    def doWithDynamicMethods = { ctx ->

    }

    /**
     * Enhances application datastores with plugin implementation of GORM API and sets this API as default
     */
    def doWithApplicationContext = { applicationContext ->
        applicationContext.getBeansOfType(HibernateDatastore).values().each { HibernateDatastore datastore ->
            def enhancer = new RemoteDomainGormEnhancer(datastore, new DatastoreTransactionManager(datastore: datastore),applicationContext.grailsApplication, new GormApiParser())
            enhancer.enhance()
        }
    }

    def onChange = { event ->

    }

    /** Changes saved configuration if config file is changed **/
    def onConfigChange = { event ->
        this.mergeConfig(application)
    }

    def onShutdown = { event ->

    }
}
