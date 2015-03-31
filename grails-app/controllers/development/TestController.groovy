package development

import synchronisation.JournalLog

class TestController {

    def index() {
        /*JournalLog.list().each {
            println "${it.id} ${it.entity} ${it.instanceId} ${it.operation} ${it.isFinished}"
        }*/
        new RemoteTest(otherName: "TEST").save()
        println Test.findAllByNameAndIdInRange("TEST",1..7)*.id
        def t = Test.get(1)?: new Test(name:"qwbaaadsyxabc",number: 10, test:"nope")
        t.save()
        println Test.list()*.name
        t.name = "AHOJ"
        t.save()
        println Test.list()*.name
        t.delete()
        println Test.list()*.name
    }
}
