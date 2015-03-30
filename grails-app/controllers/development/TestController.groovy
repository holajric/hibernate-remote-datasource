package development

class TestController {

    def index() {
        println Test.list()
        def t = /*Test.findByName("AHOJ")?:*/new Test(name:"a",number: 10, test:"nope")
        t.save()
        t.name = "AHOJ"
        t.save()
        //Test.findAll()*.name
        t.delete()
        /*println Test.findAll()*.name*/
    }
}
