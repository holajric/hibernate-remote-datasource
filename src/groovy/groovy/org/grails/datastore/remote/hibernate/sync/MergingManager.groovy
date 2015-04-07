package groovy.org.grails.datastore.remote.hibernate.sync

import groovy.util.logging.Log4j
import groovy.org.grails.datastore.remote.hibernate.query.QueryDescriptor

/**
 * Created by richard on 5.4.15.
 */
@Log4j
class MergingManager {
    public static boolean merge(local, remote, mapping, MergingStrategy strategy, QueryDescriptor desc)  {
        def mergingMethod = strategy.toString().toLowerCase()
        if(!mergingMethod || mergingMethod.isAllWhitespace()){
            log.info "Merging method is required"
            return false
        }
        mergingMethod = mergingMethod.replaceAll(/_\w/){ it[1].toUpperCase() }
        try {
            JournalLog journalLog = JournalLog.findByEntityAndInstanceIdAndOperation(local.class.name, remote[mapping["id"]], desc.operation)
            mapping.each {
                try {
                    if(journalLog.lastAttrHashes["$it.key"] == local?."$it.key"?.toString()?.hashCode()?.toString() &&
                       journalLog.lastRemoteAttrHashes["$it.value"] == remote["$it.value"]?.toString()?.hashCode()?.toString()) {
                        log.info "Attribute ${it.key} not changed, skipping"
                    }   else    {
                        "$mergingMethod"(local, remote, it.key, it.value, journalLog)
                        journalLog.lastAttrHashes["$it.key"] = local?."$it.key"?.toString()?.hashCode()?.toString()
                        journalLog.lastRemoteAttrHashes["$it.value"] = remote["$it.value"]?.toString()?.hashCode()?.toString()
                        journalLog.save(flush:true)
                    }
                }   catch(MissingPropertyException ex)    {
                    log.info "Attribute ${it.key} of ${local} not found, skipping"
                }
            }
            return true
        } catch(MissingMethodException ex)  {
            log.info "Invalid merging method $mergingMethod"
            return false
        }
    }

    private static void forceRemote(local, remote, String localAttr, String remoteAttr, JournalLog journalLog)    {
        if (remote["$remoteAttr"]) {
            local?."$localAttr" = remote["$remoteAttr"]
        } else {
            log.info "Response $remoteAttr for attribute $localAttr is empty, skipping"
        }
    }

    private static void forceLocal(local, remote, String localAttr, String remoteAttr, JournalLog journalLog)    {
        remote["$remoteAttr"]  = local?."$localAttr"
    }

    private static void preferLocal(local, remote, String localAttr, String remoteAttr, JournalLog journalLog)    {
        if(journalLog.lastAttrHashes["$localAttr"] == local?."$localAttr"?.toString()?.hashCode()?.toString() &&
           journalLog.lastRemoteAttrHashes["$remoteAttr"] != remote["$remoteAttr"]?.toString()?.hashCode()?.toString()) {
            if (remote["$remoteAttr"]) {
                local?."$localAttr" = remote["$remoteAttr"]
            } else {
                log.info "Response $remoteAttr for attribute $localAttr is empty, skipping"
            }
        } else  {
            remote["$remoteAttr"]  = local?."$localAttr"
        }
    }
    private static void preferRemote(local, remote, String localAttr, String remoteAttr, JournalLog journalLog)    {
        if(journalLog.lastAttrHashes["$localAttr"] != local?."$localAttr"?.toString()?.hashCode()?.toString() &&
           journalLog.lastRemoteAttrHashes["$remoteAttr"] == remote["$remoteAttr"]?.toString()?.hashCode()?.toString()) {
            remote["$remoteAttr"]  = local?."$localAttr"
        } else  {
            if (remote["$remoteAttr"]) {
                local?."$localAttr" = remote["$remoteAttr"]
            } else {
                log.info "Response $remoteAttr for attribute $localAttr is empty, skipping"
            }
        }
    }

}
