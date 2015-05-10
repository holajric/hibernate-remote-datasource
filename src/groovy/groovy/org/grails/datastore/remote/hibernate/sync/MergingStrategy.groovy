package groovy.org.grails.datastore.remote.hibernate.sync

/**
 * Enumeration of strategies used for merging
 * local and remote data.
 * FORCE_LOCAL - Always prefer local data
 * FORCE_REMOTE - Always prefer remote data
 * PREFER_LOCAL - Prefers local data if it were changed
 * PREFER_REMOTE - Prefers remote data if it were changed
 */
public enum MergingStrategy {
    FORCE_LOCAL,
    FORCE_REMOTE,
    PREFER_LOCAL,
    PREFER_REMOTE
}