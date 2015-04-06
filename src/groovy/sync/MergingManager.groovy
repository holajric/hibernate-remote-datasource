package sync

import groovy.util.logging.Log4j

/**
 * Created by richard on 5.4.15.
 */
@Log4j
class MergingManager {
    public static boolean merge(local, remote, mapping, MergingStrategy strategy)  {
        def mergingMethod = strategy.toString().toLowerCase()
        if(!mergingMethod || mergingMethod.isAllWhitespace()){
            log.info "Merging method is required"
            return false
        }
        mergingMethod = mergingMethod.replaceAll(/_\w/){ it[1].toUpperCase() }
        try {
            mapping.each {
                "$mergingMethod"(local, remote, it.key , it.value)
            }
            return true
        } catch(MissingMethodException ex)  {
            log.info "Invalid merging method $mergingMethod"
            return false
        }
    }

    private static void forceRemote(local, remote, String localAttr, String remoteAttr)    {
        try {
            if (remote["$remoteAttr"]) {
                local?."$localAttr" = remote["$remoteAttr"]
            } else {
                log.info "Response $remoteAttr for attribute $localAttr is empty, skipping"
            }
        } catch(MissingPropertyException ex)    {
            log.info "Attribute ${localAttr} of ${local} not found, skipping"
        }
    }

    private static void forceLocal(local, remote, String localAttr, String remoteAttr)    {
        try {
            remote["$remoteAttr"]  = local?."$localAttr"
        } catch(MissingPropertyException ex)    {
            log.info "Attribute ${localAttr} of ${local} not found, skipping"
        }
    }

    private static void preferLocal(local, remote, String localAttr, String remoteAttr)    {

    }
    private static void preferRemote(local, remote, String localAttr, String remoteAttr)    {

    }

}
