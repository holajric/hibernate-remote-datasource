package groovy.org.grails.datastore.remote.hibernate.sync

import groovy.org.grails.datastore.remote.hibernate.parsers.config.CachedConfigParser
import groovy.util.logging.Log4j
import groovy.org.grails.datastore.remote.hibernate.query.QueryDescriptor

/**
 * This class provides method for solving data conflicts by local and remote
 * data merges with usage of different strategies.
 */
@Log4j
class MergingManager {
    /**
     * Merge local and remote data object (changes them both to contain same data)
     * according to chosen merging strategy.
     * @param local local data object (as appropriate entity)
     * @param remote remote data object (as Map)
     * @param mapping name mapping between local and remote attributes
     * @param strategy chosen merging strategy
     * @param desc query descriptor
     * @return success or failure of merging
     */
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

    /**
     * Merges given attributes based on journalLog using FORCE_REMOTE merging strategy
     * @param local local data object (as appropriate entity)
     * @param remote remote data object (as Map)
     * @param localAttr name of local attribute to be merged
     * @param remoteAttr name of remote attribute to be merged
     * @param journalLog journalLog for this instance
     */
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

    /**
     * Merges given attributes based on journalLog using FORCE_LOCAL merging strategy
     * @param local local data object (as appropriate entity)
     * @param remote remote data object (as Map)
     * @param localAttr name of local attribute to be merged
     * @param remoteAttr name of remote attribute to be merged
     * @param journalLog journalLog for this instance
     */
    private static void forceLocal(local, remote, String localAttr, String remoteAttr, JournalLog journalLog)    {
        putOnIndex(remote, remoteAttr, local?."$localAttr")
    }

    /**
     * Merges given attributes based on journalLog using PREFER_LOCAL merging strategy
     * @param local local data object (as appropriate entity)
     * @param remote remote data object (as Map)
     * @param localAttr name of local attribute to be merged
     * @param remoteAttr name of remote attribute to be merged
     * @param journalLog journalLog for this instance
     */
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

    /**
     * Merges given attribute based on journalLog using PREFER_REMOTE merging strategy
     * @param local local data object (as appropriate entity)
     * @param remote remote data object (as Map)
     * @param localAttr name of local attribute to be merged
     * @param remoteAttr name of remote attribute to be merged
     * @param journalLog journalLog for this instance
     */
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

    /**
     * Gets data from multiple indexed position in collection
     * identified by index in dotted format.
     * @param collection collection data should be retrieved from
     * @param dottedIndex position identifier in pattern index1.index2
     * @return value on position or false if there is no value
     */
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

    /**
     * Puts given data on multiple indexed position in collection
     * identified by index in dotted format.
     * @param collection collection data should be inserted in
     * @param dottedIndex position identifier in pattern index1.index2
     * @param value value to be inserted
     */
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
