package development



import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@Transactional(readOnly = true)
class RemoteTestController {

    static responseFormats = ['json', 'xml']
    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond RemoteTest.list(params), [status: OK]
    }

    def show(Long id)   {
        def remoteTest = RemoteTest.get(id)
        if (remoteTest == null) {
            render status: NOT_FOUND
            return
        }
        respond remoteTest, [status: OK]
    }

    @Transactional
    def save(RemoteTest remoteTest) {
        if (remoteTest == null) {
            render status: NOT_FOUND
            return
        }

        remoteTest.validate()
        if (remoteTest.hasErrors()) {
            render status: NOT_ACCEPTABLE
            return
        }

        remoteTest.save flush:true
        respond remoteTest, [status: CREATED]
    }

    @Transactional
    def update(RemoteTest remoteTest) {
        if (remoteTest == null) {
            render status: NOT_FOUND
            return
        }

        remoteTest.validate()
        if (remoteTest.hasErrors()) {
            render status: NOT_ACCEPTABLE
            return
        }

        remoteTest.save flush:true
        respond remoteTest, [status: OK]
    }

    @Transactional
    def delete(RemoteTest remoteTest) {

        if (remoteTest == null) {
            render status: NOT_FOUND
            return
        }

        remoteTest.delete flush:true
        render status: NO_CONTENT
    }
}
