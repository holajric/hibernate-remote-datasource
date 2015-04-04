package development



import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional
import grails.converters.JSON

@Transactional(readOnly = true)
class RemoteTestController {

    static responseFormats = ['json', 'xml']
    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        response.status = 200
        if(params.hash && params.hash == "true")  {
            render(contentType: 'text/json') {
                [hash: (RemoteTest.list(params) as JSON).toString().hashCode().toString()]
            }
        }   else {
            render RemoteTest.list(params) as JSON
        }
    }

    def show(RemoteTest remoteTest) {
        if (remoteTest == null) {
            render status: NOT_FOUND
            return
        }
        response.status = 200
        render remoteTest as JSON
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
        response.status = 201
        render remoteTest as JSON
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
        response.status = 200
        render remoteTest as JSON
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
