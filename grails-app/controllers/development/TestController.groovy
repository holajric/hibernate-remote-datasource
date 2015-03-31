package development

import synchronisation.JournalLog

class TestController {

    def index() {
        JournalLog.list().each {
            println "${it.id} ${it.entity} ${it.instanceId} ${it.operation} ${it.isFinished}"
        }
        /*RemoteTest.get(1) ?:*/ new RemoteTest(otherName: "TEST").save()
        //println Test.findAllByNameAndIdInList("TEST",[1,3])*.id
        def t = /*Test.get(1)?: */new Test(name:"a",number: 10, test:"nope")
        t.save()
        println Test.list()*.name
        /*t.name = "AHOJ"
        t.save()
        println Test.list()*.name
        t.delete()
        println Test.list()*.name*/
    }
}
