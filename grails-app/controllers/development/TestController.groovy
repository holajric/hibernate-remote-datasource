package development

class TestController {

    def index() {
        /*println Test.findAllByNumberBetween(1,12)
        def t = Test.get(1) ?: new Test(name:"qwbaaadsyxabc",number: 10, test:"nope")*/
        new RemoteTest(otherName: "TEST").save()
        new RemoteTest(otherName: "TEST").save()
        /*t.save()
        new RemoteTest(otherName: "TEST2").save()
        println Test.list(sort: "id", order: 'asc')*.name
        t.name = "AHOJ2"
        t.save()
        t = Test.get(2)*/
        println Test.list(sort: "id", order: 'asc')*.name
        /*println Test.list(sort: "id", order: 'asc')*.name
        t?.delete()
        println Test.list(sort: 'id', order: 'asc')*.name*/
    }
}
