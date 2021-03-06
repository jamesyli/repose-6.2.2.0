package features.filters.contenttypestripper

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Handling
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

class ContentTypeStripperTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/contenttypestripper", params)
        repose.start([waitOnJmxAfterStarting: false])
        waitUntilReadyToServiceRequests()
    }

    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }

        if (repose) {
            repose.stop()
        }
    }

    @Unroll
    def "should maintain the content-type header when there is a body #desc and maintain the integrity of the body on a #method request"() {
        when:
        def messageChain = deproxy.makeRequest([url: reposeEndpoint, requestBody: requestBody, headers: ["content-type": "text/plain"], method: method])
        def sentRequest = ((MessageChain) messageChain).getHandlings()[0]

        then:
        ((Handling) sentRequest).request.getHeaders().findAll("Content-Type").size() == 1
        ((Handling) sentRequest).request.body == requestBody

        where:
        desc                     | requestBody  | method //GET has any content automatically removed
        "over 8 characters"      | "I like pie" | "PUT"
        "over 8 characters"      | "I like pie" | "POST"
        "over 8 characters"      | "I like pie" | "DELETE"
        "less than 8 characters" | " Pie "      | "PUT"
        "less than 8 characters" | " Pie "      | "POST"
        "less than 8 characters" | " Pie "      | "DELETE"
    }

    @Unroll
    def "should remove the content-type header when there #desc for #method requests"() {
        when:
        def messageChain = deproxy.makeRequest([url: reposeEndpoint, requestBody: requestBody, headers: ["content-type": "apple/pie"], method: method])
        def sentRequest = ((MessageChain) messageChain).getHandlings()[0]

        then:
        ((Handling) sentRequest).request.getHeaders().findAll("Content-Type").size() == 0
        ((Handling) sentRequest).request.body == requestBody

        where:
        desc                                                          | requestBody                           | method
        "is no body"                                                  | ""                                    | "GET"
        "is no body"                                                  | ""                                    | "POST"
        "is no body"                                                  | ""                                    | "PUT"
        "is no body"                                                  | ""                                    | "DELETE"
        "is only whitespace in the first 8 characters"                | " \n \r \t  "                         | "POST"
        "is only whitespace in the first 8 characters"                | " \n \r \t  "                         | "PUT"
        "is only whitespace in the first 8 characters"                | " \n \r \t  "                         | "DELETE"
        "is only whitespace in the first 8 characters even with text" | " \n \r \t  unfortunately heres text" | "POST"
        "is less than 8 character white space body"                   | "    "                                | "POST"
    }
}
