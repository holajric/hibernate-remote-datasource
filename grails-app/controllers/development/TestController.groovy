package development

import synchronisation.JournalLog

class TestController {

    def index() {
        /*JournalLog.list().each {
            println "${it.id} ${it.entity} ${it.instanceId} ${it.operation} ${it.isFinished}"
        }*/
        new RemoteTest(otherName: "TEST").save()
        new RemoteTest(otherName: "TEST").save()
        println Test.findAllByNumberBetween(1,12)*.id //NOT WORKING CHECK
        def t = Test.get(1) ?: new Test(name:"qwbaaadsyxabc",number: 10, test:"nope")
        t.save()
        println Test.list()*.number
        t.name = "AHOJ"
        t.save()
        t = Test.get(2)
        println Test.list()*.name
        t?.delete()
        println Test.list()*.name
    }
}
