package org.codehaus.groovy.grails.web.mapping

import grails.web.mapping.UrlMappingsHolder
import org.grails.web.mapping.DefaultUrlMappingEvaluator
import org.grails.web.mapping.DefaultUrlMappingsHolder
import org.springframework.core.io.ByteArrayResource
import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class UrlMappingWithCustomValidatorTests extends AbstractGrailsControllerTests {

    def topLevelMapping = '''
mappings {
    "/help/$path**"(controller : "wiki", action : "show", id : "1") {
        constraints {
            path(validator : { val, obj -> ! val.startsWith("js") })
        }
    }
}
'''
    def UrlMappingsHolder holder

    protected void setUp() {
        super.setUp()
        def res = new ByteArrayResource(topLevelMapping.bytes)

        def evaluator = new DefaultUrlMappingEvaluator(servletContext)
        def mappings = evaluator.evaluateMappings(res)

        holder = new DefaultUrlMappingsHolder(mappings)
    }

    void testMatchWithCustomValidator() {
        def info = holder.match("/help/foo.html")
        assert info

        info = holder.match("/help/js/foo.js")
        assert !info
    }
}
