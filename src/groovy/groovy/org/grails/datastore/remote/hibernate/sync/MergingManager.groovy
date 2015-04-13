package groovy.org.grails.datastore.remote.hibernate.sync

import groovy.org.grails.datastore.remote.hibernate.parsers.config.CachedConfigParser
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
            log.error "Merging method is required"
            return false
        }
        mergingMethod = mergingMethod.replaceAll(/_\w/){ it[1].toUpperCase() }
        try {
            JournalLog journalLog = JournalLog.findByEntityAndInstanceIdAndOperation(local.class.name, onIndex(remote, mapping["id"]), desc.operation)
            mapping.each {
                try {
                    if(journalLog.lastAttrHashes["$it.key"] == local?."$it.key"?.toString()?.hashCode()?.toString() &&
                       journalLog.lastRemoteAttrHashes["$it.value"] == onIndex(remote, it.value)?.toString()?.hashCode()?.toString()) {
                        log.info "Attribute ${it.key} not changed, skipping"
                    }   else    {
                        "$mergingMethod"(local, remote, it.key, it.value, journalLog)
                        journalLog.lastAttrHashes["$it.key"] = local?."$it.key"?.toString()?.hashCode()?.toString()
                        journalLog.lastRemoteAttrHashes["$it.value"] = onIndex(remote, it.value)?.toString()?.hashCode()?.toString()
                        journalLog.save(flush:true)
                    }
                }   catch(MissingPropertyException ex)    {
                    log.info "Attribute ${it.key} of ${local} not found, skipping"
                }
            }
            return true
        } catch(MissingMethodException ex)  {
            log.error "Invalid merging method $mergingMethod"
            return false
        }
    }

    private static void forceRemote(local, remote, String localAttr, String remoteAttr, JournalLog journalLog)    {
        if (onIndex(remote, remoteAttr)) {
        	if(CachedConfigParser.mapping[local.class.name]["mappingTransformations"] && CachedConfigParser.mapping[local.class.name]["mappingTransformations"][localAttr])	{
                local?."$localAttr" = CachedConfigParser.mapping[local.class.name]["mappingTransformations"][localAttr](onIndex(remote, remoteAttr))
            }	else	{
            	local?."$localAttr" = onIndex(remote, remoteAttr)
        	}
        } else {
            log.info "Response $remoteAttr for attribute $localAttr is empty, skipping"
        }
    }

    private static void forceLocal(local, remote, String localAttr, String remoteAttr, JournalLog journalLog)    {
        putOnIndex(remote, remoteAttr, local?."$localAttr")
    }

    private static void preferLocal(local, remote, String localAttr, String remoteAttr, JournalLog journalLog)    {
        if(journalLog.lastAttrHashes["$localAttr"] == local?."$localAttr"?.toString()?.hashCode()?.toString() &&
                journalLog.lastRemoteAttrHashes[remoteAttr] != onIndex(remote, remoteAttr)?.toString()?.hashCode()?.toString()) {
            if (onIndex(remote, remoteAttr)) {
                if(CachedConfigParser.mapping[local.class.name]["mappingTransformations"] && CachedConfigParser.mapping[local.class.name]["mappingTransformations"][localAttr])	{
                    local?."$localAttr" = CachedConfigParser.mapping[local.class.name]["mappingTransformations"][localAttr](onIndex(remote, remoteAttr))
                }	else	{
                    local?."$localAttr" = onIndex(remote, remoteAttr)
                }
            } else {
                log.info "Response $remoteAttr for attribute $localAttr is empty, skipping"
            }
        } else  {
            putOnIndex(remote, remoteAttr, local?."$localAttr")
        }
    }
    private static void preferRemote(local, remote, String localAttr, String remoteAttr, JournalLog journalLog)    {
        if(journalLog.lastAttrHashes["$localAttr"] != local?."$localAttr"?.toString()?.hashCode()?.toString() &&
           journalLog.lastRemoteAttrHashes[remoteAttr] == onIndex(remote, remoteAttr)?.toString()?.hashCode()?.toString()) {
            putOnIndex(remote, remoteAttr, local?."$localAttr")
        } else  {
            if (onIndex(remote,remoteAttr)) {
                if(CachedConfigParser.mapping[local.class.name]["mappingTransformations"] && CachedConfigParser.mapping[local.class.name]["mappingTransformations"][localAttr])	{
                    local?."$localAttr" = CachedConfigParser.mapping[local.class.name]["mappingTransformations"][localAttr](onIndex(remote, remoteAttr))
                }	else	{
                    local?."$localAttr" = onIndex(remote, remoteAttr)
                }
            } else {
                log.info "Response $remoteAttr for attribute $localAttr is empty, skipping"
            }
        }
    }

    private static Object onIndex(collection, String dottedIndex)   {
        def result = collection
        def indexes = dottedIndex.tokenize(".")
        if(dottedIndex == null || dottedIndex.empty)
            return false
        if(indexes.size() == 1) {
            return result[indexes[0]]
        }
        result = result[indexes[0]] ?: false
        if(!result)
            return false
        indexes[1..-1].each{
            if(result?."$it" == null || result?."$it" == "null"  || !result?.containsKey(it))
                return false
            else
                result = result[it]
        }
        return result
    }

    private static void putOnIndex(collection, String dottedIndex, value)   {
        def ref = collection
        if(dottedIndex == null || dottedIndex.empty)
            return
        def indexes = dottedIndex.tokenize(".")
        if(indexes.size() == 1) {
            ref[indexes[0]] = value
            return
        }

        indexes[0..-2].each{
            ref = ref[it]
        }
        ref[indexes[-1]] = value
    }

}
