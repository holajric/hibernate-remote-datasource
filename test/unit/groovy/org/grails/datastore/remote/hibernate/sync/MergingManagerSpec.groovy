package groovy.org.grails.datastore.remote.hibernate.sync

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import grails.test.mixin.domain.DomainClassUnitTestMixin
import spock.lang.Specification
import development.Test
import spock.lang.Unroll
import groovy.org.grails.datastore.remote.hibernate.query.QueryDescriptor
import groovy.org.grails.datastore.remote.hibernate.query.Operation
import grails.test.mixin.Mock

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@Mock(JournalLog)
@TestMixin(GrailsUnitTestMixin)
class MergingManagerSpec extends Specification {

    def setup() {

    }

    def cleanup() {
    }

    @Unroll
    void "test if #givenStrategy of #givenRemote with #givenLog is #expectedRemote and #expectedLocal"() {
        given:
        def local = new Test(name: "test")
        def remote = givenRemote
        String localAttr = "name"
        String remoteAttr = "otherName"
        JournalLog journalLog = givenLog
        and:
        MergingManager."$givenStrategy"(local, remote, localAttr, remoteAttr, journalLog)
        expect:
        local?."$localAttr" == expectedLocal?."$localAttr"
        remote["$remoteAttr"] == expectedRemote["$remoteAttr"]
        where:
        givenStrategy << ["forceLocal", "forceRemote", "forceRemote",
                          "preferLocal", "preferLocal", "preferLocal",
                          "preferLocal", "preferLocal", "preferRemote", "preferRemote",
                          "preferRemote", "preferRemote", "preferRemote"]
        givenRemote << [["otherName":"test2"], ["otherName":"test2"], [:],
                        ["otherName":"test2"], ["otherName":"test2"], ["otherName":"test2"],
                        ["otherName":"test2"], [:], ["otherName":"test2"], ["otherName":"test2"],
                        ["otherName":"test2"], ["otherName":"test2"], [:]]
        givenLog << [null, null, null,
                     new JournalLog(lastAttrHashes: ["name":"test".hashCode().toString()], lastRemoteAttrHashes: ["otherName":"test2".hashCode().toString()]),
                     new JournalLog(lastAttrHashes: ["name":"test".hashCode().toString()], lastRemoteAttrHashes: ["otherName":"test".hashCode().toString()]),
                     new JournalLog(lastAttrHashes: ["name":"test2".hashCode().toString()], lastRemoteAttrHashes: ["otherName":"test2".hashCode().toString()]),
                     new JournalLog(lastAttrHashes: ["name":"test2".hashCode().toString()], lastRemoteAttrHashes: ["otherName":"test".hashCode().toString()]),
                     new JournalLog(lastAttrHashes: ["name":"test".hashCode().toString()], lastRemoteAttrHashes: ["otherName":"test".hashCode().toString()]),
                     new JournalLog(lastAttrHashes: ["name":"test".hashCode().toString()], lastRemoteAttrHashes: ["otherName":"test2".hashCode().toString()]),
                     new JournalLog(lastAttrHashes: ["name":"test".hashCode().toString()], lastRemoteAttrHashes: ["otherName":"test".hashCode().toString()]),
                     new JournalLog(lastAttrHashes: ["name":"test2".hashCode().toString()], lastRemoteAttrHashes: ["otherName":"test2".hashCode().toString()]),
                     new JournalLog(lastAttrHashes: ["name":"test2".hashCode().toString()], lastRemoteAttrHashes: ["otherName":"test".hashCode().toString()]),
                     new JournalLog(lastAttrHashes: ["name":"test".hashCode().toString()], lastRemoteAttrHashes: ["otherName":"test".hashCode().toString()])]
        expectedRemote << [["otherName":"test"], ["otherName":"test2"], [:],
                           ["otherName":"test"], ["otherName":"test2"], ["otherName":"test"],
                           ["otherName":"test"], [:], ["otherName":"test2"], ["otherName":"test2"],
                           ["otherName":"test"], ["otherName":"test2"], [:] ]
        expectedLocal <<[new Test(name: "test"), new Test(name: "test2"), new Test(name: "test"),
                         new Test(name: "test"), new Test(name: "test2"), new Test(name: "test"),
                         new Test(name: "test"), new Test(name: "test"), new Test(name: "test2"), new Test(name: "test2"),
                         new Test(name: "test"), new Test(name: "test2"), new Test(name: "test")]
    }
}
