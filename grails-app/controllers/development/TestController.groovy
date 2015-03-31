package development

class TestController {

    def index() {
        RemoteTest.get(1)?: new RemoteTest(otherName: "TEST").save()
        println Test.list()*.name
        def t = Test.get(2)?: new Test(name:"a",number: 10, test:"nope")
        t.save()
        println Test.list()*.name
        t.name = "AHOJ"
        t.save()
        println Test.list()*.name
        t.delete()
        println Test.list()*.name
    }
}
