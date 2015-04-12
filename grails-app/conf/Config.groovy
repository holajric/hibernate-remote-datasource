// configuration for plugin testing - will not be included in the plugin zip

log4j = {
    // Example of changing the log pattern for the default console
    // appender:
    //
    root {
        debug 'stdout'
        info 'stdout'
        additivity = false
    }

    appenders {
        console name:'stdout', layout:pattern(conversionPattern: '%c{2} %m%n')
    }
    info  'groovy.org.grails.datastore.remote.hibernate.auth',
          'groovy.org.grails.datastore.remote.hibernate.connectors',
          'development',
          'groovy.org.grails.datastore.remote.hibernate.parsers',
          'groovy.org.grails.datastore.remote.hibernate.query'

    error  'org.codehaus.groovy.grails.web.servlet',  //  controllers
           'org.codehaus.groovy.grails.web.pages', //  GSP
           'org.codehaus.groovy.grails.web.sitemesh', //  layouts
           'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
           'org.codehaus.groovy.grails.web.mapping', // URL mapping
           'org.codehaus.groovy.grails.commons', // core / classloading
           'org.codehaus.groovy.grails.plugins', // plugins
           'org.codehaus.groovy.grails.orm.hibernate', // hibernate integration
           'org.springframework',
           'org.hibernate',
           'net.sf.ehcache.hibernate'
}
