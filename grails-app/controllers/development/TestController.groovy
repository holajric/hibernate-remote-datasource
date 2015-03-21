package development

class TestController {

    def index() {
       /* def t = new Test(name:"test").save()
        def t2 =  new Test(name:"test").save()
        t2.name = "test2"
        t2.save()*/
        //def remote = new RemoteTest(otherName: "test").save(failOnError: true, flush: true)
        println Test.list()*.name//findByNameAndIdBetween("test", 1, 10)
    }
}
