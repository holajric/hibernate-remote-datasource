package development

class TestController {

    def index() {
        def t = Test.findByNameAndNumberAndTest("teasdasdasdstasdas", 10, "nope")?:new Test(name:"teasdasdasdstasdas",number: 10, test:"nope")
        t.save()
        println Test.findAll()*.name
    }
}
