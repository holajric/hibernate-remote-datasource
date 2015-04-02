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
        def t = Test.get(3)?: new Test(name:"qwbaaadsyxabc",number: 10, test:"nope")
        t.save()
        println Test.list()*.name
        t.name = "AHOJ"
        t.save()
        println Test.list()*.name
        //t.delete()
        println Test.list()*.name
    }
}
