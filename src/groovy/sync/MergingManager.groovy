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
            "$mergingMethod"(local, remote, mapping)
            return true
        } catch(MissingMethodException ex)  {
            log.info "Invalid merging method $mergingMethod"
            return false
        }
    }

    private static void forceRemote(local, remote, mapping)    {
        mapping.each {
            try {
                if (remote["${it.value}"]) {
                    local?."${it.key}" = remote["${it.value}"]
                } else {
                    log.info "Response for attribute ${it.key} mapped by ${it.value} empty, skipping"
                }
            } catch(MissingPropertyException ex)    {
                log.info "Attribute ${it.key} of ${local} not found, skipping"
            }
        }
    }

    private static void forceLocal(local, remote, mapping)    {
        mapping.each {
            try {
                remote["${it.value}"]  = local?."${it.key}"
            } catch(MissingPropertyException ex)    {
                log.info "Attribute ${it.key} of ${local} not found, skipping"
            }
        }
    }

    private static void preferLocal(local, remote, mapping)    {

    }
    private static void preferRemote(local, remote, mapping)    {

    }

}
