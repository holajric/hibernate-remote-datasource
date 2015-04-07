package groovy.org.grails.datastore.remote.hibernate.query.execution

import grails.transaction.Transactional
import groovy.util.logging.Log4j
import org.codehaus.groovy.grails.web.json.JSONObject
import groovy.org.grails.datastore.remote.hibernate.parsers.config.CachedConfigParser
import groovy.org.grails.datastore.remote.hibernate.query.QueryDescriptor
import groovy.org.grails.datastore.remote.hibernate.query.Operation
import groovy.org.grails.datastore.remote.hibernate.query.builder.RemoteQuery
import groovy.org.grails.datastore.remote.hibernate.sync.MergingManager
import groovy.org.grails.datastore.remote.hibernate.sync.MergingStrategy
import groovy.org.grails.datastore.remote.hibernate.sync.SynchronizationManager
import groovy.org.grails.datastore.remote.hibernate.sync.JournalLog
import groovy.org.grails.datastore.remote.hibernate.sync.SynchLog

@Log4j
@Transactional
class QueryExecutor {

    static boolean executeFinderQuery(QueryDescriptor desc)  {
        SynchLog synchLog = SynchLog.findByQuery(desc.toString().hashCode().toString())
        if(synchLog && !synchLog.isFinished)    {
            log.info "Query $desc already in progress, quitting"
            return false
        }
        if(!synchLog) {
            synchLog = new SynchLog(query: desc.toString().hashCode(),isFinished: false).save()
        }
        synchLog.isFinished = false
        synchLog.save()
        if(!isValidDescriptor(desc))    {
            log.error "Descriptor $desc is invalid"
            synchLog.isFinished = true
            synchLog.save(flush:true)
            return false
        }
        if(!CachedConfigParser.isOperationAllowed(desc)) {
            log.info "Operation ${desc.operation} not allowed for class ${desc.entityName}"
            synchLog.isFinished = true
            synchLog.save(flush:true)
            return false
        }
        def remoteQuery
        if((remoteQuery = CachedConfigParser.getQueryBuilder(desc)?.generateQuery(desc)) == null)   {
            log.error "RemoteQuery could not be created for ${desc.entityName} ${desc.operation}"
            synchLog.isFinished = true
            synchLog.save(flush:true)
            return false
        }
        def connector
        if((connector = CachedConfigParser.getDataSourceConnector(desc)) == null)   {
            log.error "DataSourceConnector could not be loaded for ${desc.entityName} ${desc.operation}"
            synchLog.isFinished = true
            synchLog.save(flush:true)
            return false
        }

        if(synchLog.lastResponseHash && (connector.read(CachedConfigParser.getQueryBuilder(desc)?.generateHashQuery(desc))?.first()?."hash" == synchLog.lastResponseHash)) {
            log.info "Remote data weren't changed, quitting"
            synchLog.isFinished = true
            synchLog.save(flush: true)
            return true
        }
        List<JSONObject> responses
        if((responses = connector.read(remoteQuery, CachedConfigParser.getAuthenticator(desc))) == null)    {
            log.error "Data could not be read from ${remoteQuery}"
            synchLog.isFinished = true
            synchLog.save(flush:true)
            return false
        }

        if(synchLog.lastResponseHash && (synchLog.lastResponseHash == responses.toString().hashCode().toString()))   {
            log.info "Remote data weren't changed, quitting"
            synchLog.isFinished = true
            synchLog.save(flush:true)
            return true
        }
        synchLog.lastResponseHash = responses.toString().hashCode().toString()
        synchLog.save(flush:true)
        boolean processingResult = processResponses(responses, desc)
        synchLog.isFinished = true
        synchLog.save(flush:true)
        return processingResult
    }

    static boolean executeInstanceQuery(QueryDescriptor desc, Object instance)   {
        SynchLog synchLog
        if(desc.operation == Operation.UPDATE)  {
            synchLog = SynchLog.findByQuery(desc.toString().hashCode().toString())
            if(synchLog && !synchLog.isFinished)    {
                log.info "Query $desc already in progress, quitting"
                return false
            }
            if(!synchLog) {
                synchLog = new SynchLog(query: desc.toString().hashCode(),isFinished: false).save()
            }
            synchLog.isFinished = false
            synchLog.save()
        }
        if(!isValidDescriptor(desc))    {
            log.error "Descriptor $desc is invalid"
            if(desc.operation == Operation.UPDATE)  {
                synchLog.isFinished = true
                synchLog.save(flush:true)
            }
            return false
        }
        if(instance == null)    {
            log.error "Instance is required"
            if(desc.operation == Operation.UPDATE)  {
                synchLog.isFinished = true
                synchLog.save(flush:true)
            }
            return false
        }
        if(!CachedConfigParser.isOperationAllowed(desc)) {
            log.info "Operation ${desc.operation} not allowed for class ${desc.entityName}"
            if(desc.operation == Operation.UPDATE)  {
                synchLog.isFinished = true
                synchLog.save(flush:true)
            }
            return false
        }
        def mapping
        if((mapping = CachedConfigParser.getAttributeMap(desc)) == null)    {
            log.error "Mapping for class ${desc.entityName} could not be loaded"
            if(desc.operation == Operation.UPDATE)  {
                synchLog.isFinished = true
                synchLog.save(flush:true)
            }
            return false
        }
        def remoteQuery
        if((remoteQuery = CachedConfigParser.getQueryBuilder(desc)?.generateQuery(desc)) == null)   {
            log.error "RemoteQuery could not be created for ${desc.entityName} ${desc.operation}"
            if(desc.operation == Operation.UPDATE)  {
                synchLog.isFinished = true
                synchLog.save(flush:true)
            }
            return false
        }
        remoteQuery.dataJson = [:]
        mapping.each {
            if(!instance?."$it.key") {
                log.info "Instance ${instance} does not have an attribute ${it.key}, skipping"
            }   else    {
                remoteQuery?.dataJson?."$it.value" =  instance?."$it.key"
            }
        }
        def connector
        if((connector = CachedConfigParser.getDataSourceConnector(desc)) == null)   {
            log.error "DataSourceConnector could not be loaded for ${desc.entityName} ${desc.operation}"
            if(desc.operation == Operation.UPDATE)  {
                synchLog.isFinished = true
                synchLog.save(flush:true)
            }
            return false
        }
        if(desc.operation == Operation.DELETE)  {
            if(connector.doAction(remoteQuery, CachedConfigParser.getAuthenticator(desc)) == false) {
                log.error "Action could not be done from ${remoteQuery}"
                return false
            }
            return true
        }
        if(desc.operation == Operation.UPDATE) {
            if(synchLog.lastResponseHash && (connector.read(CachedConfigParser.getQueryBuilder(desc)?.generateHashQuery(desc))?."hash" == synchLog.lastResponseHash)) {
                log.info "Remote data weren't changed, quitting"
                synchLog.isFinished = true
                synchLog.save(flush: true)
                return true
            }
        }
        List<JSONObject> responses
        if((responses = connector.write(remoteQuery, CachedConfigParser.getAuthenticator(desc))) == null)    {
            log.error "Data could not be read from ${remoteQuery}"
            if(desc.operation == Operation.UPDATE)  {
                synchLog.isFinished = true
                synchLog.save(flush:true)
            }
            return false
        }
        if(desc.operation == Operation.UPDATE) {
            if (synchLog.lastResponseHash && (synchLog.lastResponseHash == responses.toString().hashCode().toString())) {
                log.info "Remote data weren't changed, quitting"
                synchLog.isFinished = true
                synchLog.save(flush: true)
                return true
            }
            synchLog.lastResponseHash = responses.toString().hashCode().toString()
            synchLog.save(flush: true)
            boolean processingResult = processResponses(responses, desc)
            synchLog.isFinished = true
            synchLog.save(flush: true)
            return processingResult
        }
        return processResponses(responses, desc, instance)
    }

    private static boolean isValidDescriptor(QueryDescriptor desc)    {
        if(!desc.entityName || desc.entityName.empty)   {
            log.error "Descriptor entityName is required"
            return false
        }

        if(!desc.operation)    {
            log.error "Descriptor operation is required"
            return false
        }

        return true
    }

    private static boolean processResponses(List<JSONObject> responses, QueryDescriptor desc, instance = null)   {
        def mapping
        if((mapping = CachedConfigParser.getAttributeMap(desc)) == null)    {
            log.error "Mapping for class ${desc.entityName} could not be loaded"
            return false
        }
        if(!mapping["id"])    {
            mapping["id"] = "id"
        }
        ResponseFilter filter = new ResponseFilter()
        responses.each { response ->
            if(!response[mapping["id"]])    {
                log.warn "There is no id in response, response can not be processed"
                return false
            }
            if (filter.isValid(response, desc)) {
                def instanceTemp
                try {
                    if((instanceTemp = instance ?: Class.forName(desc.entityName)?.directGet(response[mapping["id"]?:"id"]) ?: Class.forName(desc.entityName).newInstance()) == null) {
                        log.error "Instance could not be found or created"
                        return false
                    }
                }   catch(ClassNotFoundException ex)    {
                    log.error "Class ${desc.entityName} could not be found."
                    return false
                }   catch(MissingMethodException ex)    {
                    log.error "Class ${desc.entityName} is not domain."
                    return false
                }
                if(!JournalLog.countByEntityAndInstanceIdAndIsFinished(desc.entityName, response[mapping["id"]], false)) {
                    if(!SynchronizationManager.withTransaction(instanceTemp.class.name, response[mapping["id"]], desc.operation) {
                        //Add instance check too
                        JournalLog journalLog = JournalLog.findByEntityAndInstanceIdAndOperation(instanceTemp.class.name, response[mapping["id"]], desc.operation)
                        if(journalLog.lastRemoteHash == response.toString().hashCode().toString() && journalLog.lastInstanceHash == instanceTemp.hashCode().toString())  {
                            log.info "Up to date, skipping"
                            return true
                        }
                        journalLog.lastRemoteHash = response.toString().hashCode().toString()
                        journalLog.lastInstanceHash = instanceTemp.hashCode().toString()
                        journalLog.save(flush: true)
                        if(!buildInstance(mapping, response, instanceTemp, desc)) {
                            log.error "Instance ${instanceTemp} could not be builded from response ${response}"
                            return false
                        }
                        return true
                    })  {
                        log.error "Transaction wasn't finished succesfully"
                        return false
                    }
                }   else    {
                    JournalLog journalLog = JournalLog.findByEntityAndInstanceIdAndOperation(instanceTemp.class.name, response[mapping["id"]], desc.operation)
                    if(journalLog.lastRemoteHash == response.toString().hashCode().toString()  && journalLog.lastInstanceHash == instanceTemp.hashCode().toString())  {
                        log.info "Up to date, skipping"
                    }   else {
                        journalLog.lastRemoteHash = response.toString().hashCode().toString()
                        journalLog.lastInstanceHash = instanceTemp.hashCode().toString()
                        journalLog.save(flush: true)
                        if (!buildInstance(mapping, response, instanceTemp, desc)) {
                            log.error "Instance ${instanceTemp} could not be builded from response ${response}"
                            return false
                        }
                    }
                }
            }
        }

        return true
    }

    private static boolean buildInstance(mapping, response, instanceTemp, QueryDescriptor desc) {
        if(instanceTemp == null)    {
            log.error "Target instance is required"
            return false
        }
        JournalLog journalLog = JournalLog.findByEntityAndInstanceIdAndOperation(instanceTemp.class.name, response[mapping["id"]], desc.operation)
        def oldResponse = response
        def oldAttrs = journalLog.lastAttrHashes
        def oldRemoteAttrs = journalLog.lastRemoteAttrHashes
        def operation
        if((operation = CachedConfigParser.getQueryOperation(desc)) == null) {
            log.error "Operation configuration for ${desc.entityName} ${desc.operation} could not be loaded"
            return null
        }
        if(!MergingManager.merge(instanceTemp, response, mapping, operation["mergingStrategy"]?:MergingStrategy.PREFER_REMOTE, desc))    {
            log.warn "Merge unsucessful"
            journalLog.lastAttrHashes = oldAttrs
            journalLog.lastRemoteAttrHashes = oldRemoteAttrs
            journalLog.save(flush:true)
            return false
        }
        if(oldResponse != response) {
            RemoteQuery query
            Operation originalOperation = desc.operation
            desc.operation = oldResponse?.id ? Operation.UPDATE : Operation.CREATE
            if(CachedConfigParser.isOperationAllowed(desc)) {
                if((query = CachedConfigParser.getQueryBuilder(desc)?.generateQuery(desc)) == null)    {
                    log.warn "Query for $desc could not be generated"
                    desc.operation = originalOperation
                    journalLog.lastAttrHashes = oldAttrs
                    journalLog.lastRemoteAttrHashes = oldRemoteAttrs
                    journalLog.save(flush:true)
                    return false
                }
                if((CachedConfigParser.getDataSourceConnector(desc)?.write(query, response)) == null)   {
                    log.warn "$query with data: $response wasn't sucesful"
                    desc.operation = originalOperation
                    journalLog.lastAttrHashes = oldAttrs
                    journalLog.lastRemoteAttrHashes = oldRemoteAttrs
                    journalLog.save(flush:true)
                    return false
                }
                journalLog.lastRemoteHash = response.toString().hashCode().toString()
                journalLog.lastInstanceHash = instanceTemp.hashCode().toString()
                journalLog.save(flush: true)
            } else {
                journalLog.lastAttrHashes = oldAttrs
                journalLog.lastRemoteAttrHashes = oldRemoteAttrs
                journalLog.save(flush:true)
            }
            desc.operation = originalOperation
        }
        try {
            instanceTemp.validate()
            if(instanceTemp.hasErrors())   {
                log.error "Instance could not be saved for following reasons ${instanceTemp.errors}"
                return false
            }
            instanceTemp.directSave()
            return true
        } catch(MissingMethodException ex)    {
            log.error "${instanceTemp} is not domain class"
            return false
        }
    }

}
